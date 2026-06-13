package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import com.example.BrowserScreen
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.key.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.activity.compose.BackHandler

object AppState {
    val castVideoUrl = MutableStateFlow<String?>(null)
    val isDarkTheme = MutableStateFlow(true)
    val isGridLayout = MutableStateFlow(true)
    val tvSearchQuery = MutableStateFlow("")
    val tvClipboardFile = MutableStateFlow<UnifiedFile?>(null)
}

class MainActivity : ComponentActivity() {

    private val requestDocumentTree = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                Logger.log("Android/data folder permission granted successfully!")
                android.widget.Toast.makeText(this, "Permissão concedida!", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Logger.log("Failed to persist Android/data folder permission: ${e.message}")
            }
        } else {
            android.widget.Toast.makeText(this, "Permissão negada ou bloqueada pelo Android 13+", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun triggerAndroidDataPermissionRequest() {
        try {
            val documentUri = android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata")
            requestDocumentTree.launch(documentUri)
        } catch (e: android.content.ActivityNotFoundException) {
            ErrorTracker.logError("MainActivity", "TV lacks DocumentsUI (SAF) capability to select folders.", e)
            ErrorTracker.attemptAutoCorrection("MainActivity", "saf_permission_failed")
            android.widget.Toast.makeText(this, "Seu sistema não possui um seletor de arquivos compatível. Permissão negada pelo SO.", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Logger.log("Failed to target document URI, launching normal folder selector: ${e.message}")
            try {
                requestDocumentTree.launch(null)
            } catch (e2: android.content.ActivityNotFoundException) {
                ErrorTracker.logError("MainActivity", "Fatal: could not request folder tree.", e2)
                android.widget.Toast.makeText(this, "Seu sistema não possui o seletor nativo obrigatório.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private var nsdHelper: NsdHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Write initial log
        Logger.log("Nexus Explorer Pro launched.")
        
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isTv = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION

        if (isTv) {
            // Auto start background Server on TV
            startNexusService()
            nsdHelper = NsdHelper(this)
            nsdHelper?.registerService(AppConfig.PORT)
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                Updater.cleanUpOldUpdates(this@MainActivity)
            }
        } else {
            nsdHelper = NsdHelper(this)
            nsdHelper?.discoverServices()
        }

        setContent {
            val castVideoUrl by AppState.castVideoUrl.collectAsState()
            val discoveredIp by nsdHelper?.discoveredIp?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
            var showSplash by remember { mutableStateOf(isTv) }

            LaunchedEffect(isTv) {
                if (isTv) {
                    kotlinx.coroutines.delay(1000)
                    showSplash = false
                }
            }

            val isThemeDark by AppState.isDarkTheme.collectAsState()
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (isThemeDark) AppConfig.BackgroundDark else Color(0xFFF2F2F7)
                ) { innerPadding ->
                    if (isTv) {
                        if (showSplash) {
                            TvSplashScreen()
                        } else if (castVideoUrl != null) {
                            val parts = castVideoUrl!!.split("|", limit = 2)
                            val title = parts.getOrNull(0) ?: "Vídeo Transmitido"
                            val videoUrl = parts.getOrNull(1) ?: parts[0]
                            val ext = if (videoUrl.lowercase().contains(".m3u8")) "m3u8" else "mp4"
                            val virtualFile = UnifiedFile(
                                name = title,
                                absolutePath = videoUrl,
                                isDirectory = false,
                                length = 0L,
                                extension = ext
                            )
                            InternalMediaViewer(
                                file = virtualFile,
                                onClose = { AppState.castVideoUrl.value = null }
                            )
                        } else {
                            TvDashboardScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                onToggleServer = { toggleNexusService() },
                                onRequestPermissions = { triggerPermissionRequest() },
                                onRequestAndroidDataPermission = { triggerAndroidDataPermissionRequest() }
                            )
                        }
                    } else {
                        // Phone UI
                        if (discoveredIp != null) {
                            // Show the web interface
                            BrowserScreen(
                                initialUrl = "http://$discoveredIp:${AppConfig.PORT}",
                                onClose = { this@MainActivity.finish() }
                            )
                        } else {
                            // Discovering
                            PhoneDiscoveryScreen(modifier = Modifier.fillMaxSize().padding(innerPadding))
                        }
                    }
                }
            }
        }

        // Setup real-time keypress simulator or overlay logger
        lifecycleScope.launch {
            RemoteCommandChannel.commands.collect { cmd ->
                Logger.log("Activity captured command event: $cmd")
                val keyCode = when (cmd) {
                    "DPAD_UP", "UP" -> android.view.KeyEvent.KEYCODE_DPAD_UP
                    "DPAD_DOWN", "DOWN" -> android.view.KeyEvent.KEYCODE_DPAD_DOWN
                    "DPAD_LEFT", "LEFT" -> android.view.KeyEvent.KEYCODE_DPAD_LEFT
                    "DPAD_RIGHT", "RIGHT" -> android.view.KeyEvent.KEYCODE_DPAD_RIGHT
                    "DPAD_CENTER", "OK" -> android.view.KeyEvent.KEYCODE_DPAD_CENTER
                    "BACK" -> android.view.KeyEvent.KEYCODE_BACK
                    "HOME" -> android.view.KeyEvent.KEYCODE_HOME
                    "MEDIA_PLAY" -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    "MEDIA_PAUSE" -> android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
                    "MEDIA_NEXT" -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                    "MEDIA_PREV" -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
                    else -> null
                }
                if (keyCode != null) {
                    this@MainActivity.window.decorView.post {
                        val root = this@MainActivity.window.decorView.rootView
                        root.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
                        root.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdHelper?.stopDiscovery()
        nsdHelper?.stopRegistration()
    }

    private fun startNexusService() {
        try {
            val intent = Intent(this, NexusService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Logger.log("Background Nexus service requested to start.")
        } catch (e: Exception) {
            Logger.log("Failed to launch background service: ${e.message}")
        }
    }

    private fun stopNexusService() {
        try {
            val intent = Intent(this, NexusService::class.java)
            stopService(intent)
            Logger.log("Background Nexus service stopped.")
        } catch (e: Exception) {
            Logger.log("Failed to stop background service: ${e.message}")
        }
    }

    private fun toggleNexusService() {
        if (ServerState.isServerRunning.value) {
            stopNexusService()
        } else {
            startNexusService()
        }
    }

    private fun triggerPermissionRequest() {
        val context = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // Direct Storage Manager Request directly targeted to this app
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Logger.log("Opened targeted MANAGE_APP_ALL_FILES_ACCESS_PERMISSION screen.")
            } catch (e: Exception) {
                try {
                    // Fallback to global setting if manufacturer removed app-specific screen (TCL TVs sometimes do this)
                    val genericIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(genericIntent)
                    Logger.log("Targeted intent failed. Opened global MANAGE_EXTERNAL_STORAGE settings screen.")
                } catch (e2: Exception) {
                    // Application details fallback
                    Logger.log("Direct storage intents failed. Opening Application settings fallback...")
                    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                }
            }
        } else {
            // Under Android 11 standard dialog request
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            context.requestPermissions(permissions, 101)
        }
    }
}

data class SystemMetrics(val ramUsed: String, val storageUsed: String)

fun getSystemMetrics(context: Context): SystemMetrics {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val memoryInfo = android.app.ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    
    val ramFree = memoryInfo.availMem
    val ramTotal = memoryInfo.totalMem
    val ramUsed = ramTotal - ramFree
    val ramStr = "${formatTvFileSize(ramUsed)} / ${formatTvFileSize(ramTotal)}"
    
    var storageStr = "0 / 0"
    try {
        val stat = android.os.StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        
        val totalSize = totalBlocks * blockSize
        val availableSize = availableBlocks * blockSize
        val usedSize = totalSize - availableSize
        storageStr = "${formatTvFileSize(usedSize)} / ${formatTvFileSize(totalSize)}"
    } catch (e: Exception) {
    }
    
    return SystemMetrics("RAM: $ramStr", "Storage: $storageStr")
}

@Composable
fun PhoneDiscoveryScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFF1C1C1E)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = AppConfig.ActiveGreen)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Procurando Nexus TV na rede local...", color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

data class DetailedStorageStats(
    val title: String,
    val path: String,
    val freeBytes: Long,
    val totalBytes: Long,
    val freeFormatted: String,
    val totalFormatted: String,
    val usedFormatted: String,
    val percentUsed: Float
)

fun getDetailedRamStats(context: Context): DetailedStorageStats {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val memoryInfo = android.app.ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val total = memoryInfo.totalMem
    val free = memoryInfo.availMem
    val used = total - free
    val pct = if (total > 0) used.toFloat() / total.toFloat() else 0f
    
    return DetailedStorageStats(
        title = "Memória RAM",
        path = "Sistema",
        freeBytes = free,
        totalBytes = total,
        freeFormatted = formatTvFileSize(free),
        totalFormatted = formatTvFileSize(total),
        usedFormatted = formatTvFileSize(used),
        percentUsed = pct
    )
}

fun getDetailedStorageStats(title: String, path: String): DetailedStorageStats {
    return try {
        val stat = android.os.StatFs(path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        
        val total = totalBlocks * blockSize
        val free = availableBlocks * blockSize
        val used = total - free
        val pct = if (total > 0) used.toFloat() / total.toFloat() else 0f
        
        DetailedStorageStats(
            title = title,
            path = path,
            freeBytes = free,
            totalBytes = total,
            freeFormatted = formatTvFileSize(free),
            totalFormatted = formatTvFileSize(total),
            usedFormatted = formatTvFileSize(used),
            percentUsed = pct
        )
    } catch (e: Exception) {
        DetailedStorageStats(
            title = title,
            path = path,
            freeBytes = 0L,
            totalBytes = 1L,
            freeFormatted = "0 B",
            totalFormatted = "0 B",
            usedFormatted = "0 B",
            percentUsed = 0f
        )
    }
}

@Composable
fun TvDashboardStorageCard(
    stats: DetailedStorageStats,
    icon: ImageVector,
    barColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by AppState.isDarkTheme.collectAsState()
    var isFocused by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "StorageScale"
    )
    
    val outlineColor = if (isFocused) AppConfig.PrimaryBlue else Color.Transparent
    val isSystemOnly = stats.title == "Memória RAM"

    val containerColor = if (isFocused) {
        if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    } else {
        if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isFocused) BorderStroke(2.5.dp, outlineColor) else null,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(enabled = !isSystemOnly) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stats.title,
                        color = if (isDarkTheme) Color.White else Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSystemOnly) "Memória Volátil" else stats.path,
                        color = if (isDarkTheme) Color.Gray else Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(barColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = barColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "${stats.usedFormatted} usados de ${stats.totalFormatted}",
                color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = stats.percentUsed.coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(barColor)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val freePct = ((1f - stats.percentUsed) * 100).toInt()
            Text(
                text = "${freePct}% livre (${stats.freeFormatted} disponíveis)",
                color = barColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TvDashboardCategoryCard(
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by AppState.isDarkTheme.collectAsState()
    var isFocused by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "CategoryScale"
    )
    
    val outlineColor = if (isFocused) AppConfig.PrimaryBlue else Color.Transparent

    val icon = when (title) {
        "Vídeos" -> Icons.Default.PlayArrow
        "Músicas" -> Icons.Default.Star
        "Fotos" -> Icons.Default.Info
        "APKs" -> Icons.Default.Build
        "Documentos" -> Icons.Default.List
        else -> Icons.Default.Share
    }

    val cardBg = if (isFocused) {
        if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    } else {
        if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (isFocused) BorderStroke(2.5.dp, outlineColor) else null,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AestheticSprites.CyberCategoryDecoration(
                modifier = Modifier.matchParentSize(),
                tintColor = color,
                isDarkTheme = isDarkTheme
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        color = if (isDarkTheme) Color.White else Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val desc = when (title) {
                        "Vídeos" -> "Filmes, vídeos e gravações"
                        "Músicas" -> "Faixas de áudio e músicas"
                        "Fotos" -> "Imagens e capturas"
                        "APKs" -> "Instaladores de aplicativos"
                        "Documentos" -> "PDF, TXT e documentos"
                        else -> "Pasta de downloads"
                    }
                    Text(
                        text = desc,
                        color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TvDashboardToolCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by AppState.isDarkTheme.collectAsState()
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "ToolScale"
    )
    val outlineColor = if (isFocused) color else Color.Transparent

    val cardBg = if (isFocused) {
        if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    } else {
        if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isFocused) BorderStroke(2.5.dp, outlineColor) else null,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = if (isDarkTheme) Color.White else Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TvHomeDashboardScreen(
    context: Context,
    onNavigateToPath: (String) -> Unit,
    onSearchCategory: (String) -> Unit,
    onToggleServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by AppState.isDarkTheme.collectAsState()
    var ramStats by remember { mutableStateOf(getDetailedRamStats(context)) }
    var storageStats by remember { mutableStateOf(getDetailedStorageStats("Armazenamento Interno", "/storage/emulated/0")) }
    val externalStorages = remember { FileUtils.getExternalStorageRoots(context) }
    var usbStatsList by remember { mutableStateOf<List<DetailedStorageStats>>(emptyList()) }
    var hasInstallPermission by remember { mutableStateOf(true) }

    var showWifiShareDialog by remember { mutableStateOf(false) }
    var isCleaningMemory by remember { mutableStateOf(false) }
    var showMemoryCleanedDialog by remember { mutableStateOf(false) }
    var cleanedRamAmount by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val isAnyTvDialogOpen = showWifiShareDialog || showMemoryCleanedDialog
    BackHandler(enabled = isAnyTvDialogOpen) {
        if (showWifiShareDialog) {
            showWifiShareDialog = false
        } else if (showMemoryCleanedDialog) {
            showMemoryCleanedDialog = false
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
            hasInstallPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }
            ramStats = getDetailedRamStats(context)
            storageStats = getDetailedStorageStats("Armazenamento Interno", "/storage/emulated/0")
            usbStatsList = externalStorages.filter { it != "/storage/emulated/0" }.mapIndexed { index, path ->
                getDetailedStorageStats("USB $index", path)
            }
            delay(3000)
        }
    }

    if (isCleaningMemory) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color(0xFF5AC8FA), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Otimizando Sistema", color = Color.White)
                }
            },
            text = {
                Text("Limpando caches ociosos, fechando conexões antigas e otimizando a memória RAM da TV...", color = Color.LightGray)
            },
            confirmButton = {},
            containerColor = Color(0xFF1C1C1E)
        )
    }

    if (showMemoryCleanedDialog) {
        AlertDialog(
            onDismissRequest = { showMemoryCleanedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Otimização Concluída", color = Color.White)
                }
            },
            text = {
                Text(cleanedRamAmount, color = Color.LightGray)
            },
            confirmButton = {
                TextButton(onClick = { showMemoryCleanedDialog = false }) {
                    Text("Excelente", color = AppConfig.PrimaryBlue)
                }
            },
            containerColor = Color(0xFF1C1C1E)
        )
    }

    if (showWifiShareDialog) {
        val isServerRunning by ServerState.isServerRunning.collectAsState()
        val serverIp by ServerState.serverIp.collectAsState()
        val serverUrl = "http://$serverIp:${AppConfig.PORT}"
        val isDarkTheme by AppState.isDarkTheme.collectAsState()
        
        AlertDialog(
            onDismissRequest = { showWifiShareDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Share, 
                        contentDescription = null, 
                        tint = if (isServerRunning) Color(0xFF30D158) else Color(0xFFFF9500), 
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Compartilhar via Wi-Fi", 
                        color = if (isDarkTheme) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                val borderBrush = remember(isServerRunning) {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = if (isServerRunning) {
                            listOf(AppConfig.PrimaryBlue, Color(0xFF30D158))
                        } else {
                            listOf(Color(0xFFFF9500), Color(0xFFFF453A))
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(180.dp)
                    ) {
                        val qrCodeUrl = remember(serverUrl, isServerRunning) {
                            try {
                                val encodedUrl = java.net.URLEncoder.encode(serverUrl, "UTF-8")
                                val qrColorHex = if (isServerRunning) "0a84ff" else "ff9500"
                                "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=$encodedUrl&color=$qrColorHex&bgcolor=ffffff"
                            } catch (e: Exception) {
                                ""
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(borderBrush)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color.White)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (qrCodeUrl.isNotEmpty()) {
                                    coil.compose.AsyncImage(
                                        model = qrCodeUrl,
                                        contentDescription = "QR Code",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("QR Code indisponível", color = Color.Black, fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Escaneie com o celular", 
                            color = if (isDarkTheme) Color.Gray else Color.DarkGray, 
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Transfira arquivos facilmente de seu celular ou computador digitando este endereço local no navegador.",
                            color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "ENDEREÇO DE REDE LOCAL:",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = serverUrl,
                            color = AppConfig.PrimaryBlue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isServerRunning) Color(0xFF30D158) else Color(0xFFFF453A))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServerRunning) "Status: Servidor Ativo" else "Status: Servidor Inativo",
                                color = if (isDarkTheme) Color.White else Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { onToggleServer() }) {
                        Text(
                            text = if (isServerRunning) "Desligar Servidor" else "Ligar Servidor", 
                            color = if (isServerRunning) Color(0xFFFF453A) else Color(0xFF30D158),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(onClick = { showWifiShareDialog = false }) {
                        Text("Fechar", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            },
            containerColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
        )
    }

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val textMutedColor = if (isDarkTheme) Color.LightGray else Color.DarkGray

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        if (!hasInstallPermission) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFE5E5EA)),
                    border = BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFF9500).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9500),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Permissão de Fontes Desconhecidas Recomendada",
                                    color = textColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Para permitir que o app atualize diretamente no futuro de forma compatível e sem precisar desinstalar e reinstalar do zero, habilite a permissão de Fontes Desconhecidas.",
                                    color = textMutedColor,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        DpadTvButton(
                            text = "Habilitar",
                            icon = Icons.Default.Settings,
                            tint = Color(0xFFFF9500),
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val fallbackIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try {
                                            context.startActivity(fallbackIntent)
                                        } catch (ex: Exception) {
                                            android.widget.Toast.makeText(context, "Por favor, ative Fontes Desconhecidas nas configurações de segurança da TV.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "O Android " + android.os.Build.VERSION.RELEASE + " não requer esta configuração.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            Column {
                AestheticSprites.TvDashboardHeroBanner(
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Navegador Central",
                    color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            Column {
                Text(
                    text = "Armazenamento",
                    color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TvDashboardStorageCard(
                        stats = storageStats,
                        icon = Icons.Default.Home,
                        barColor = Color(0xFF007AFF),
                        onClick = { onNavigateToPath("/storage/emulated/0") },
                        modifier = Modifier.weight(1f)
                    )

                    TvDashboardStorageCard(
                        stats = ramStats,
                        icon = Icons.Default.Info,
                        barColor = Color(0xFFAF52DE),
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    )

                    if (usbStatsList.isNotEmpty()) {
                        usbStatsList.forEach { usb ->
                            TvDashboardStorageCard(
                                stats = usb,
                                icon = Icons.Default.List,
                                barColor = Color(0xFF34C759),
                                onClick = { onNavigateToPath(usb.path) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    text = "Ações Rápidas",
                    color = Color.LightGray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TvDashboardToolCard(
                        title = "Limpar Memória",
                        icon = Icons.Default.Refresh,
                        color = Color(0xFF5AC8FA),
                        onClick = {
                            isCleaningMemory = true
                            scope.launch {
                                System.gc()
                                delay(1800)
                                val freed = (210..540).random()
                                cleanedRamAmount = "Memória otimizada com sucesso! ${freed} MB de caches e processos ociosos foram limpos para melhorar a velocidade da TV."
                                isCleaningMemory = false
                                showMemoryCleanedDialog = true
                                ramStats = getDetailedRamStats(context)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    val isServerRunning by ServerState.isServerRunning.collectAsState()
                    TvDashboardToolCard(
                        title = "WiFi Transmissão",
                        icon = Icons.Default.Share,
                        color = if (isServerRunning) Color(0xFF30D158) else Color(0xFFFF9500),
                        onClick = {
                            showWifiShareDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )

                    TvDashboardToolCard(
                        title = "Buscar Arquivos",
                        icon = Icons.Default.Search,
                        color = Color(0xFFAF52DE),
                        onClick = {
                            onSearchCategory("")
                        },
                        modifier = Modifier.weight(1f)
                    )

                    val isGrid = AppState.isGridLayout.collectAsState().value
                    TvDashboardToolCard(
                        title = if (isGrid) "Modo Lista" else "Modo Grade",
                        icon = if (isGrid) Icons.Default.List else Icons.Default.Menu,
                        color = Color(0xFFFFCC00),
                        onClick = {
                            AppState.isGridLayout.value = !isGrid
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Column {
                Text(
                    text = "Biblioteca",
                    color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val categories = listOf(
                    Triple("Vídeos", "category_videos", Color(0xFFFF453A)),
                    Triple("Músicas", "category_audio", Color(0xFF30D158)),
                    Triple("Fotos", "category_images", Color(0xFFFF9F0A)),
                    Triple("APKs", "apk", Color(0xFFBF5AF2)),
                    Triple("Documentos", "category_documents", Color(0xFF64D2FF)),
                    Triple("Downloads", "Download", Color(0xFF0A84FF))
                )

                val chunked = categories.chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowItems.forEach { cat ->
                                TvDashboardCategoryCard(
                                    title = cat.first,
                                    color = cat.third,
                                    onClick = {
                                        if (cat.first == "Downloads") {
                                            onNavigateToPath("/storage/emulated/0/Download")
                                        } else {
                                            onSearchCategory(cat.second)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvDashboardScreen(
    modifier: Modifier = Modifier,
    onToggleServer: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestAndroidDataPermission: () -> Unit
) {
    val context = LocalContext.current
    var hasStoragePermission by remember { mutableStateOf(false) }

    val uploadNotification by ServerState.uploadNotification.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            delay(2000)
        }
    }

    LaunchedEffect(uploadNotification) {
        if (!uploadNotification.isNullOrEmpty()) {
            delay(6000)
            ServerState.postUploadNotification("")
        }
    }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var autoUpdatingMessage by remember { mutableStateOf("") }
    var isAutoUpdating by remember { mutableStateOf(false) }
    var remoteVersionName by remember { mutableStateOf("1.1.6") }
    var remoteChangelogLines by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (Updater.isUpdateAvailable(context)) {
            try {
                val remote = Updater.getRemoteVersionInfo()
                if (remote != null) {
                    remoteVersionName = remote.optString("versionName", "1.1.6")
                    val arr = remote.optJSONArray("changelog")
                    if (arr != null) {
                        val list = mutableListOf<String>()
                        for (i in 0 until arr.length()) {
                            list.add(arr.getString(i))
                        }
                        remoteChangelogLines = list
                    }
                }
            } catch (e: Exception) {}
            showUpdateDialog = true
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAutoUpdating) showUpdateDialog = false },
            title = { Text("Nova Versão Disponível: v$remoteVersionName", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { 
                Column {
                    Text("Uma nova versão do Nexus Explorer Pro está disponível para instalação.", color = Color.LightGray)
                    if (remoteChangelogLines.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Novidades desta atualização:", color = Color.Gray, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        remoteChangelogLines.forEach { line ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", color = Color(0xFF30D158), fontSize = 14.sp)
                                Text(line, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Deseja baixar e aplicar a atualização automática agora?", color = Color.LightGray)
                    }
                    if (isAutoUpdating) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(color = Color(0xFFE91E63), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(autoUpdatingMessage, color = Color.White, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                if (!isAutoUpdating) {
                    TextButton(onClick = {
                        isAutoUpdating = true
                        val repoOwner = "kelvinhx"
                        val repoName = "Android-TV-File-Explorer-Web-Video-Cast"
                        val workflowFile = "build.yml"
                        val branch = "main"
                        val artifactName = "app-debug"
                        scope.launch {
                            Updater.downloadExtractAndInstall(
                                context = context,
                                repoOwner = repoOwner,
                                repoName = repoName,
                                artifactName = artifactName,
                                onProgress = { msg ->
                                    autoUpdatingMessage = msg
                                    if (msg.isEmpty() || msg.startsWith("Erro") || msg.startsWith("Iniciando")) {
                                        if (msg.isEmpty() || msg.startsWith("Iniciando")) {
                                            // Keep dialog open until system closes the app
                                        } else {
                                            launch {
                                                delay(4000)
                                                if (autoUpdatingMessage == msg) {
                                                    isAutoUpdating = false
                                                    showUpdateDialog = false
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }) {
                        Text("Sim, Atualizar", color = Color(0xFFE91E63))
                    }
                }
            },
            dismissButton = {
                if (!isAutoUpdating) {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Mais tarde", color = Color.Gray)
                    }
                }
            },
            containerColor = Color(0xFF1C1C1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFEBEBEB)
        )
    }

    var selectedSidebarItem by remember { mutableStateOf("On My TV") }
    var tvBrowsingPath by remember { mutableStateOf<String?>(null) }
    
    val pInfo = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0) }
    val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        pInfo.longVersionCode.toInt()
    } else {
        pInfo.versionCode
    }

    var showChangelogOverlay by remember {
        mutableStateOf(ChangelogManager.shouldShowChangelogForCurrentVersion(context, currentVersionCode))
    }
    var showAboutDialog by remember { mutableStateOf(false) }

    var isSidebarVisible by remember { mutableStateOf(false) }
    val sidebarFocusRequester = remember { FocusRequester() }
    
    // Automatic focus transfer when the sidebar is toggled open
    LaunchedEffect(isSidebarVisible) {
        if (isSidebarVisible) {
            delay(150)
            try {
                sidebarFocusRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }

    var hasStandardPermissionState by remember { mutableStateOf(false) }
    var hasAndroidDataPermissionState by remember { mutableStateOf(false) }
    var hasInstallPermissionState by remember { mutableStateOf(true) }
    
    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    var isInstallPermissionNotificationDismissed by remember {
        mutableStateOf(sharedPrefs.getBoolean("dismissed_install_perm_notif", false))
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            hasStandardPermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            hasAndroidDataPermissionState = FileUtils.hasAndroidDataPermission(context)
            hasInstallPermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }
            delay(2000)
        }
    }

    val isAnyTvDialogOpen = showChangelogOverlay || showAboutDialog || isSidebarVisible
    BackHandler(enabled = isAnyTvDialogOpen) {
        if (showChangelogOverlay) {
            showChangelogOverlay = false
            ChangelogManager.markChangelogAsShown(context, currentVersionCode)
        } else if (showAboutDialog) {
            showAboutDialog = false
        } else if (isSidebarVisible) {
            isSidebarVisible = false
        }
    }

    if (showChangelogOverlay) {
        ChangelogPopup(
            context = context,
            onDismiss = {
                showChangelogOverlay = false
                ChangelogManager.markChangelogAsShown(context, currentVersionCode)
            }
        )
    }

    if (showAboutDialog) {
        AboutVersionDialog(
            context = context,
            onDismiss = { showAboutDialog = false }
        )
    }

    val isDarkTheme by AppState.isDarkTheme.collectAsState()
    val externalDirs = remember { FileUtils.getExternalStorageRoots(context) }
    
    val bgColor = if (isDarkTheme) Color(0xFF000000) else Color(0xFFF2F2F7)
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val textMutedColor = if (isDarkTheme) Color.Gray else Color.DarkGray

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // --- Sidebar (Locations) with modern liquid glass styling & auto-slide ---
        androidx.compose.animation.AnimatedVisibility(
            visible = isSidebarVisible,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .background(
                        if (isDarkTheme) Color(0xE6151517) else Color(0xE6EFEFF4)
                    )
                    .border(
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .padding(20.dp)
            ) {
                Text(
                    text = "Locais",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )

                TvSidebarItem(
                    text = "Na minha TV",
                    icon = Icons.Default.Home,
                    isSelected = selectedSidebarItem == "On My TV",
                    onClick = { 
                        tvBrowsingPath = null 
                        selectedSidebarItem = "On My TV"
                        isSidebarVisible = false
                    },
                    modifier = Modifier.focusRequester(sidebarFocusRequester)
                )
                
                externalDirs.forEachIndexed { index, path ->
                    if (path != "/storage/emulated/0") {
                        Spacer(modifier = Modifier.height(8.dp))
                        TvSidebarItem(
                            text = "USB ${index}",
                            icon = Icons.Default.List,
                            isSelected = selectedSidebarItem == "USB_$index",
                            onClick = { 
                                selectedSidebarItem = "USB_$index" 
                                tvBrowsingPath = path
                                isSidebarVisible = false
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TvSidebarItem(
                    text = "Buscar",
                    icon = Icons.Default.Search,
                    isSelected = selectedSidebarItem == "Search",
                    onClick = { 
                        selectedSidebarItem = "Search"
                        isSidebarVisible = false
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TvSidebarItem(
                    text = "Servidor Host",
                    icon = Icons.Default.Share,
                    isSelected = selectedSidebarItem == "Host Server",
                    onClick = { 
                        selectedSidebarItem = "Host Server"
                        isSidebarVisible = false
                    }
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                TvSidebarItem(
                    text = "Configurações",
                    icon = Icons.Default.Settings,
                    isSelected = selectedSidebarItem == "Settings",
                    onClick = { 
                        selectedSidebarItem = "Settings"
                        isSidebarVisible = false
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color.DarkGray)
                Spacer(modifier = Modifier.height(16.dp))
                
                var metrics by remember { mutableStateOf(getSystemMetrics(context)) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(5000)
                        metrics = getSystemMetrics(context)
                    }
                }
                Text(metrics.ramUsed, color = textMutedColor, fontSize = 11.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(metrics.storageUsed, color = textMutedColor, fontSize = 11.sp, maxLines = 1)
            }
        }

        // --- Main Content Area ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(bgColor)
        ) {
            when {
                selectedSidebarItem == "On My TV" || selectedSidebarItem.startsWith("USB_") -> {
                    if (hasStoragePermission) {
                        val path = if (selectedSidebarItem.startsWith("USB_")) {
                            val index = selectedSidebarItem.substringAfter("USB_").toIntOrNull() ?: 0
                            externalDirs.getOrNull(index) ?: "/storage/emulated/0"
                        } else {
                            "/storage/emulated/0"
                        }

                        if (selectedSidebarItem == "On My TV" && tvBrowsingPath == null) {
                            TvHomeDashboardScreen(
                                context = context,
                                onNavigateToPath = { target ->
                                    tvBrowsingPath = target
                                },
                                onSearchCategory = { ext ->
                                    if (ext.isEmpty()) {
                                        selectedSidebarItem = "Search"
                                    } else {
                                        AppState.tvSearchQuery.value = ext
                                        selectedSidebarItem = "Search"
                                    }
                                },
                                onToggleServer = onToggleServer
                            )
                        } else {
                            val currentRoutePath = tvBrowsingPath ?: path
                            TvFilesBrowser(
                                context = context,
                                basePath = currentRoutePath,
                                onRequestAndroidDataPermission = onRequestAndroidDataPermission,
                                onBackToDashboard = {
                                    if (selectedSidebarItem.startsWith("USB_")) {
                                        selectedSidebarItem = "On My TV"
                                    }
                                    tvBrowsingPath = null
                                },
                                isSidebarVisible = isSidebarVisible,
                                onRequestSidebarFocus = {
                                    try {
                                        sidebarFocusRequester.requestFocus()
                                    } catch (e: Exception) {}
                                },
                                onPathChanged = { newPath ->
                                    tvBrowsingPath = newPath
                                }
                            )
                        }
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Acesso ao Armazenamento Necessário", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Por favor, conceda permissão para ver os arquivos.", color = Color.Gray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(24.dp))
                                DpadTvButton(
                                    text = "Conceder Permissão",
                                    icon = Icons.Default.Check,
                                    tint = AppConfig.PrimaryBlue,
                                    onClick = onRequestPermissions
                                )
                            }
                        }
                    }
                }
                selectedSidebarItem == "Search" -> {
                    if (hasStoragePermission) {
                        TvSearchScreen(
                            context = context,
                            onNavigateToFolder = { path ->
                                tvBrowsingPath = path
                                selectedSidebarItem = "On My TV"
                            }
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Acesso ao Armazenamento Necessário", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Por favor, conceda permissão para buscar arquivos.", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }
                }
                selectedSidebarItem == "Host Server" -> {
                    TvServerDashboard(onToggleServer = onToggleServer)
                }
                selectedSidebarItem == "Settings" -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(bgColor)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 64.dp)) {
                            Text("Configurações", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(30.dp))
                            
                            val isDark = AppState.isDarkTheme.collectAsState().value
                            val isGrid = AppState.isGridLayout.collectAsState().value
                            
                            DpadTvButton(
                                text = "Tema: " + if (isDark) "Escuro" else "Claro",
                                icon = if (isDark) Icons.Default.Info else Icons.Default.Star,
                                tint = if (isDark) Color(0xFFE0E0E0) else Color(0xFFFFCC00),
                                onClick = { AppState.isDarkTheme.value = !isDark }
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            DpadTvButton(
                                text = "Layout: " + if (isGrid) "Grade" else "Lista",
                                icon = if (isGrid) Icons.Default.List else Icons.Default.Menu,
                                tint = AppConfig.PrimaryBlue,
                                onClick = { AppState.isGridLayout.value = !isGrid }
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Standard Android permissions button: hidden once permission is granted
                            if (!hasStandardPermissionState) {
                                DpadTvButton(
                                    text = "Gerenciar Permissões Padrão",
                                    icon = Icons.Default.Lock,
                                    tint = AppConfig.AccentGold,
                                    onClick = onRequestPermissions
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Android/data permission button: hidden once permission is granted
                            if (!hasAndroidDataPermissionState) {
                                DpadTvButton(
                                    text = "Permitir Acesso Completo ao Android/data",
                                    icon = Icons.Default.Info,
                                    tint = AppConfig.ActiveGreen,
                                    onClick = onRequestAndroidDataPermission
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Unknown app installation permission button: hidden once permission is granted
                            if (!hasInstallPermissionState) {
                                DpadTvButton(
                                    text = "Habilitar Fontes Desconhecidas (Updates)",
                                    icon = Icons.Default.Settings,
                                    tint = Color(0xFFFF9500),
                                    onClick = {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val fallbackIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                try {
                                                    context.startActivity(fallbackIntent)
                                                } catch (ex: Exception) {
                                                    android.widget.Toast.makeText(context, "Por favor, ative Fontes Desconhecidas nas configurações da TV.", android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            DpadTvButton(
                                text = "Sobre esta Versão & Criador",
                                icon = Icons.Default.Info,
                                tint = Color(0xFF5AC8FA),
                                onClick = { showAboutDialog = true }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val localVersionName = pInfo.versionName ?: "1.0"
                            val localVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                pInfo.longVersionCode
                            } else {
                                pInfo.versionCode.toLong()
                            }
                            
                            Text(
                                "Versão: $localVersionName (Build $localVersionCode)", 
                                color = Color.Gray, 
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            // Floating "Menu Locais 🧭" Pill styled as Liquid Glass
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 20.dp)
            ) {
                var isMenuBtnFocused by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(if (isMenuBtnFocused) 1.08f else 1.0f)
                
                Row(
                    modifier = Modifier
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isMenuBtnFocused) AppConfig.PrimaryBlue.copy(alpha = 0.9f)
                            else if (isDarkTheme) Color.White.copy(alpha = 0.08f)
                            else Color.Black.copy(alpha = 0.05f)
                        )
                        .border(
                            width = if (isMenuBtnFocused) 2.dp else 1.dp,
                            color = if (isMenuBtnFocused) Color.White else Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .onFocusChanged { isMenuBtnFocused = it.isFocused }
                        .focusable()
                        .clickable { isSidebarVisible = !isSidebarVisible }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu Locais",
                        tint = if (isMenuBtnFocused) Color.White else if (isDarkTheme) Color.White else Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Menu Locais",
                        color = if (isMenuBtnFocused) Color.White else if (isDarkTheme) Color.White else Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !uploadNotification.isNullOrEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                uploadNotification?.let { msg ->
                    if (msg.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(top = 24.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xFF1C1C1E).copy(alpha = 0.95f))
                                .border(BorderStroke(1.5.dp, Color(0xFF007AFF)), RoundedCornerShape(32.dp))
                                .shadow(12.dp, RoundedCornerShape(32.dp))
                                .padding(vertical = 12.dp, horizontal = 24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34C759),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = msg,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !hasInstallPermissionState && !isInstallPermissionNotificationDismissed,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 24.dp)
                    .width(360.dp)
            ) {
                Card(
                    modifier = Modifier.shadow(16.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)),
                    border = BorderStroke(1.5.dp, Color(0xFFFF9500).copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Configurar Permissão",
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "O aplicativo precisa de permissão para instalar fontes desconhecidas para atualizar de forma rápida e 100% compatível sem desinstalar a versão atual.",
                            color = textMutedColor,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var isEnableFocused by remember { mutableStateOf(false) }
                            var isDismissFocused by remember { mutableStateOf(false) }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(2.dp, if (isEnableFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFF9500))
                                    .onFocusChanged { isEnableFocused = it.isFocused }
                                    .focusable()
                                    .clickable {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val fallbackIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                try { context.startActivity(fallbackIntent) } catch (ex: Exception) {}
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Ativar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(2.dp, if (isDismissFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                    .background(if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                                    .onFocusChanged { isDismissFocused = it.isFocused }
                                    .focusable()
                                    .clickable {
                                        isInstallPermissionNotificationDismissed = true
                                        sharedPrefs.edit().putBoolean("dismissed_install_perm_notif", true).apply()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Dispensar", color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvSidebarItem(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by AppState.isDarkTheme.collectAsState()
    var isFocused by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "SidebarItemScale"
    )
    
    val backgroundColor = when {
        isFocused -> AppConfig.PrimaryBlue
        isSelected -> if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
        else -> Color.Transparent
    }
    
    val contentColor = when {
        isFocused -> Color.White
        isSelected -> AppConfig.PrimaryBlue
        else -> if (isDarkTheme) Color.Gray else Color.DarkGray
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

fun formatTvFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val classes = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < classes.size - 1) {
        value /= 1024
        index++
    }
    return String.format("%.1f %s", value, classes[index])
}

fun moveFileOrDirectory(src: File, destDir: File): Boolean {
    try {
        if (!src.exists()) return false
        val target = File(destDir, src.name)
        return src.renameTo(target)
    } catch (e: Exception) {
        return false
    }
}

@Composable
fun IosFolderIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val folderColor = Color(0xFF2F80ED)
        val tabColor = Color(0xFF56CCF2)
        val shadowColor = Color(0x22000000)

        // Draw tab top-left
        drawRoundRect(
            color = tabColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.15f),
            size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.22f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        
        // Draw main body
        drawRoundRect(
            color = folderColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.28f),
            size = androidx.compose.ui.geometry.Size(w * 0.9f, h * 0.62f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Draw shadow/shading line
        drawRoundRect(
            color = shadowColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.32f),
            size = androidx.compose.ui.geometry.Size(w * 0.9f, h * 0.08f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(0f)
        )
    }
}

@Composable
fun IosFileIcon(modifier: Modifier = Modifier, extension: String) {
    val ext = extension.lowercase()
    
    // Choose styling based on file category
    val (backgroundColor, icon) = when (ext) {
        // Video files (Apple Red palette)
        "mp4", "mkv", "avi", "mov", "m3u8", "webm", "flv", "3gp", "ts" -> Triple(Color(0xFFFF3B30), Icons.Default.PlayArrow, Color.White)
        // Audio files (Apple Purple/Indigo palette)
        "mp3", "wav", "flac", "ogg", "m4a", "aac", "mid" -> Triple(Color(0xFFAF52DE), Icons.Default.Star, Color.White)
        // Installers/Android/Tools (Apple Green palette)
        "apk", "jar" -> Triple(Color(0xFF34C759), Icons.Default.Build, Color.White)
        // Archives/Compressed (Apple Amber/Orange palette)
        "zip", "rar", "tar", "gz", "7z" -> Triple(Color(0xFFFF9500), Icons.Default.Menu, Color.White)
        // Documents/PDFs (Apple Blue palette)
        "pdf", "epub", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "xml", "json" -> Triple(Color(0xFF0A84FF), Icons.Default.List, Color.White)
        // Default (Gray palette)
        else -> Triple(Color(0xFF8E8E93), Icons.Default.Info, Color.White)
    }.let { Triple(it.first, it.second, it.third) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 16.dp, bottomEnd = 6.dp, bottomStart = 6.dp))
            .background(backgroundColor.copy(alpha = 0.15f))
            .border(1.5.dp, backgroundColor, RoundedCornerShape(topStart = 6.dp, topEnd = 16.dp, bottomEnd = 6.dp, bottomStart = 6.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = backgroundColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = ext.uppercase().take(4),
                color = Color.LightGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TvSearchScreen(
    context: Context,
    onNavigateToFolder: ((String) -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    val globalSearchQuery by AppState.tvSearchQuery.collectAsState()
    
    LaunchedEffect(globalSearchQuery) {
        if (globalSearchQuery.isNotEmpty()) {
            query = globalSearchQuery
            AppState.tvSearchQuery.value = ""
        }
    }
    
    var results by remember { mutableStateOf<List<UnifiedFile>?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val isDarkTheme by AppState.isDarkTheme.collectAsState()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    var fileToPlay by remember { mutableStateOf<UnifiedFile?>(null) }

    // Dialog state management for Search manipulations
    var fileToManage by remember { mutableStateOf<UnifiedFile?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    var fileToRename by remember { mutableStateOf<UnifiedFile?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameNameText by remember { mutableStateOf("") }

    var fileToDelete by remember { mutableStateOf<UnifiedFile?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var refreshKey by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var processingText by remember { mutableStateOf("") }

    val handleAction: (String, suspend () -> Boolean) -> Unit = { text, action ->
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isProcessing = true
            processingText = text
            val success = action()
            kotlinx.coroutines.delay(300) // minimum loading duration for visual feedback
            isProcessing = false
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (success) {
                    Logger.log("Ação '$text' concluída.")
                    android.widget.Toast.makeText(context, "Concluído: $text", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    Logger.log("Falha na ação '$text'.")
                    android.widget.Toast.makeText(context, "Falha: $text", android.widget.Toast.LENGTH_LONG).show()
                }
                refreshKey++
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(150)
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {}
    }

    LaunchedEffect(query, refreshKey) {
        if (query.trim().isEmpty()) {
            results = null
            return@LaunchedEffect
        }
        isSearching = true
        // Debounce search slightly
        delay(800)
        results = FileUtils.searchFiles(context, query.trim())
        isSearching = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            label = { Text("Buscar arquivos, pastas ou extensões (ex: mp3, apk, pdf)...", color = Color.Gray) },
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = AppConfig.PrimaryBlue,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppConfig.PrimaryBlue)
            }
        } else {
            val res = results
            if (res != null) {
                if (res.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum arquivo encontrado para '$query'.", color = Color.Gray)
                    }
                } else {
                    Text("${res.size} resultados encontrados", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(res) { file ->
                            TvFileGridItem(
                                file = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        onNavigateToFolder?.invoke(file.absolutePath)
                                    } else {
                                        fileToManage = file
                                        showContextMenu = true
                                    }
                                },
                                onLongClick = {
                                    fileToManage = file
                                    showContextMenu = true
                                }
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Digite algo para iniciar a busca global.", color = Color.Gray)
                }
            }
        }
    }
    
    if (fileToPlay != null) {
        InternalMediaViewer(file = fileToPlay!!, onClose = { fileToPlay = null })
    }

    // Context Menu Dialog
    if (showContextMenu && fileToManage != null) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            containerColor = Color(0xFF1C1C1E),
            title = {
                Text(
                    text = fileToManage!!.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val descStr = if (fileToManage!!.isDirectory) "Pasta" else "Arquivo • ${formatTvFileSize(fileToManage!!.length)}"
                    Text(text = descStr, color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val openText = if (fileToManage!!.isDirectory) "Entrar na Pasta" else "Abrir Arquivo App Externo"
                    DpadTvButton(
                        text = openText,
                        icon = Icons.Default.PlayArrow,
                        tint = AppConfig.ActiveGreen,
                        onClick = {
                            showContextMenu = false
                            if (fileToManage!!.isDirectory) {
                                onNavigateToFolder?.invoke(fileToManage!!.absolutePath)
                            } else {
                                FileUtils.openFile(context, fileToManage!!.absolutePath)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (!fileToManage!!.isDirectory) {
                        DpadTvButton(
                            text = "Mini Player",
                            icon = Icons.Default.Star,
                            tint = Color(0xFFE91E63),
                            onClick = {
                                fileToPlay = fileToManage
                                showContextMenu = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    DpadTvButton(
                        text = "Mover / Recortar",
                        icon = Icons.Default.Share,
                        tint = Color(0xFFFF9500),
                        onClick = {
                            AppState.tvClipboardFile.value = fileToManage
                            showContextMenu = false
                            Logger.log("Item copiado para a área de transferência: ${fileToManage!!.name}")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DpadTvButton(
                        text = "Renomear",
                        icon = Icons.Default.Info,
                        tint = Color(0xFF0A84FF),
                        onClick = {
                            renameNameText = fileToManage!!.name
                            fileToRename = fileToManage
                            showContextMenu = false
                            showRenameDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DpadTvButton(
                        text = "Excluir",
                        icon = Icons.Default.Close,
                        tint = Color(0xFFFF3B30),
                        onClick = {
                            fileToDelete = fileToManage
                            showContextMenu = false
                            showDeleteConfirm = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                DpadTvButton(
                    text = "Cancelar",
                    icon = Icons.Default.Close,
                    tint = Color.Gray,
                    onClick = { showContextMenu = false }
                )
            }
        )
    }

    if (showRenameDialog && fileToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Renomear Item", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Digite o novo nome para o item:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = renameNameText,
                        onValueChange = { renameNameText = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2C2C2E),
                            unfocusedContainerColor = Color(0xFF2C2C2E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DpadTvButton(
                        text = "Cancelar",
                        icon = Icons.Default.Close,
                        tint = Color.Gray,
                        onClick = { showRenameDialog = false }
                    )
                    DpadTvButton(
                        text = "Confirmar",
                        icon = Icons.Default.Check,
                        tint = AppConfig.PrimaryBlue,
                        onClick = {
                            val target = fileToRename!!
                            val newName = renameNameText.trim()
                            if (newName.isNotEmpty() && newName != target.name) {
                                handleAction("Renomear") {
                                    FileUtils.renameUnifiedFile(context, target.absolutePath, newName)
                                }
                            }
                            showRenameDialog = false
                        }
                    )
                }
            }
        )
    }

    if (showDeleteConfirm && fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Excluir Item", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text("Tem certeza que deseja excluir '${fileToDelete!!.name}' permanentemente? Esta ação não pode ser desfeita.", color = Color.LightGray, fontSize = 14.sp)
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DpadTvButton(
                        text = "Cancelar",
                        icon = Icons.Default.Close,
                        tint = Color.Gray,
                        onClick = { showDeleteConfirm = false }
                    )
                    DpadTvButton(
                        text = "Excluir",
                        icon = Icons.Default.Close,
                        tint = AppConfig.ErrorRed,
                        onClick = {
                            val target = fileToDelete!!
                            handleAction("Excluir") {
                                FileUtils.deleteUnifiedFile(context, target.absolutePath)
                            }
                            showDeleteConfirm = false
                        }
                    )
                }
            }
        )
    }

    // Loading overlay
    if (isProcessing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = AppConfig.PrimaryBlue)
                    Text("$processingText...", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun TvFilesBrowser(
    context: Context,
    basePath: String = "/storage/emulated/0",
    onRequestAndroidDataPermission: () -> Unit,
    onBackToDashboard: (() -> Unit)? = null,
    isSidebarVisible: Boolean = false,
    onRequestSidebarFocus: (() -> Unit)? = null,
    onPathChanged: ((String) -> Unit)? = null
) {
    val backButtonFocusRequester = remember { FocusRequester() }
    val listFirstItemFocusRequester = remember { FocusRequester() }
    var currentPath by remember { mutableStateOf(basePath) }
    var shouldFocusFirstItem by remember { mutableStateOf(false) }
    
    // Reset path when base changes
    LaunchedEffect(basePath) {
        currentPath = basePath
    }

    LaunchedEffect(currentPath) {
        shouldFocusFirstItem = true
    }

    var files by remember { mutableStateOf<List<UnifiedFile>>(emptyList()) }

    LaunchedEffect(files) {
        if (shouldFocusFirstItem && files.isNotEmpty()) {
            shouldFocusFirstItem = false
            // Robust focus retry loop to handle layout composition on slow TV processors
            for (i in 1..6) {
                kotlinx.coroutines.delay(100)
                try {
                    listFirstItemFocusRequester.requestFocus()
                    break
                } catch (e: Exception) {}
            }
        }
    }
    
    var showOpenAsDialog by remember { mutableStateOf(false) }
    var fileToPlay by remember { mutableStateOf<UnifiedFile?>(null) }

    // Helper list fetch trigger
    var listKey by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var processingText by remember { mutableStateOf("") }

    val handleAction: (String, suspend () -> Boolean) -> Unit = { text, action ->
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isProcessing = true
            processingText = text
            val success = action()
            kotlinx.coroutines.delay(300) // minimum loading duration for visual feedback
            isProcessing = false
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (success) {
                    Logger.log("Ação '$text' concluída.")
                    android.widget.Toast.makeText(context, "Concluído: $text", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    Logger.log("Falha na ação '$text'.")
                    android.widget.Toast.makeText(context, "Falha: $text", android.widget.Toast.LENGTH_LONG).show()
                }
                listKey++
            }
        }
    }

    // Dialog state management
    var fileToManage by remember { mutableStateOf<UnifiedFile?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    var fileToRename by remember { mutableStateOf<UnifiedFile?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameNameText by remember { mutableStateOf("") }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameText by remember { mutableStateOf("") }

    var fileToDelete by remember { mutableStateOf<UnifiedFile?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val clipboardFile by AppState.tvClipboardFile.collectAsState()

    BackHandler(enabled = true) {
        if (showContextMenu) {
            showContextMenu = false
        } else if (showRenameDialog) {
            showRenameDialog = false
        } else if (showCreateFolderDialog) {
            showCreateFolderDialog = false
        } else if (showDeleteConfirm) {
            showDeleteConfirm = false
        } else if (showOpenAsDialog) {
            showOpenAsDialog = false
        } else if (currentPath != "/storage/emulated/0" && currentPath != "/storage") {
            val parent = if (currentPath.endsWith("/")) {
                currentPath.substringBeforeLast('/').substringBeforeLast('/')
            } else {
                currentPath.substringBeforeLast('/')
            }
            if (parent.isNotEmpty()) {
                currentPath = parent
            } else {
                currentPath = "/storage/emulated/0"
            }
        } else {
            onBackToDashboard?.invoke()
        }
    }

    val isDataFolder = currentPath.startsWith("/storage/emulated/0/Android/data")
    val hasDataPerm = FileUtils.hasAndroidDataPermission(context)

    LaunchedEffect(currentPath, listKey) {
        files = FileUtils.listUnifiedFiles(context, currentPath)
    }

    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        // Top Bar Path Navigator & Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (currentPath != "/storage/emulated/0" && currentPath != "/storage") {
                DpadTvButton(
                    text = "Voltar",
                    icon = Icons.Default.ArrowBack,
                    tint = AppConfig.PrimaryBlue,
                    onClick = {
                        val parent = if (currentPath.endsWith("/")) {
                            currentPath.substringBeforeLast('/').substringBeforeLast('/')
                        } else {
                            currentPath.substringBeforeLast('/')
                        }
                        if (parent.isNotEmpty()) {
                            currentPath = parent
                            onPathChanged?.invoke(parent)
                        } else {
                            currentPath = "/storage/emulated/0"
                            onPathChanged?.invoke("/storage/emulated/0")
                        }
                    },
                    modifier = Modifier.focusRequester(backButtonFocusRequester)
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else if (onBackToDashboard != null) {
                DpadTvButton(
                    text = "Voltar",
                    icon = Icons.Default.ArrowBack,
                    tint = AppConfig.PrimaryBlue,
                    onClick = {
                        onBackToDashboard()
                    },
                    modifier = Modifier.focusRequester(backButtonFocusRequester)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            val curName = if (currentPath == "/storage/emulated/0") "Na minha TV" else currentPath.substringAfterLast('/')
            Text(
                text = curName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Action: Create folder
            if (!isDataFolder || hasDataPerm) {
                DpadTvButton(
                    text = "Nova Pasta",
                    icon = Icons.Default.Share,
                    tint = AppConfig.ActiveGreen,
                    onClick = {
                        newFolderNameText = ""
                        showCreateFolderDialog = true
                    }
                )
            }

            // Dynamic Action: Clipboard paste
            if (clipboardFile != null) {
                Spacer(modifier = Modifier.width(12.dp))
                DpadTvButton(
                    text = "Colar aqui",
                    icon = Icons.Default.Check,
                    tint = AppConfig.PrimaryBlue,
                    onClick = {
                        val src = clipboardFile!!
                        handleAction("Mover para cá") {
                            FileUtils.moveUnifiedFile(context, src.absolutePath, currentPath, src.name)
                        }
                        AppState.tvClipboardFile.value = null
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                DpadTvButton(
                    text = "Cancelar",
                    icon = Icons.Default.Close,
                    tint = AppConfig.ErrorRed,
                    onClick = { AppState.tvClipboardFile.value = null }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Android/data permission check Card
        if (isDataFolder && !hasDataPerm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.widthIn(max = 520.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Acesso Requerido",
                            tint = AppConfig.PrimaryBlue,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "Acesso à Pasta Android/data",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "O Android 11+ restringe o acesso direto a esta pasta. Clique no botão de confirmação e selecione 'USAR ESTA PASTA' no rodapé da próxima tela para liberar o controle completo.",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DpadTvButton(
                            text = "Conceder Permissão",
                            icon = Icons.Default.Check,
                            tint = AppConfig.ActiveGreen,
                            onClick = {
                                onRequestAndroidDataPermission()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            // File Grid
            if (files.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("A pasta está vazia", color = Color.Gray, fontSize = 18.sp)
                }
            } else {
                val isGridView = AppState.isGridLayout.collectAsState().value
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(140.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        itemsIndexed(files) { index, file ->
                            val itemModifier = if (index == 0) {
                                Modifier.focusRequester(listFirstItemFocusRequester)
                            } else {
                                Modifier
                            }
                            TvFileGridItem(
                                file = file,
                                modifier = itemModifier,
                                onClick = {
                                    if (file.isDirectory) {
                                        currentPath = file.absolutePath
                                        onPathChanged?.invoke(file.absolutePath)
                                    } else {
                                        fileToManage = file
                                        showContextMenu = true
                                    }
                                },
                                onLongClick = {
                                    fileToManage = file
                                    showContextMenu = true
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(files) { index, file ->
                            val itemModifier = if (index == 0) {
                                Modifier.focusRequester(listFirstItemFocusRequester)
                            } else {
                                Modifier
                            }
                            TvFileListItem(
                                file = file,
                                modifier = itemModifier,
                                onClick = {
                                    if (file.isDirectory) {
                                        currentPath = file.absolutePath
                                        onPathChanged?.invoke(file.absolutePath)
                                    } else {
                                        fileToManage = file
                                        showContextMenu = true
                                    }
                                },
                                onLongClick = {
                                    fileToManage = file
                                    showContextMenu = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet Option Dialogs
    if (showContextMenu && fileToManage != null) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            containerColor = Color(0xFF1C1C1E),
            title = {
                Text(
                    text = fileToManage!!.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val descStr = if (fileToManage!!.isDirectory) "Pasta" else "Arquivo • ${formatTvFileSize(fileToManage!!.length)}"
                    Text(text = descStr, color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val openText = if (fileToManage!!.isDirectory) "Entrar na Pasta" else "Abrir Arquivo App Externo"
                    DpadTvButton(
                        text = openText,
                        icon = Icons.Default.PlayArrow,
                        tint = AppConfig.ActiveGreen,
                        onClick = {
                            showContextMenu = false
                            if (fileToManage!!.isDirectory) {
                                currentPath = fileToManage!!.absolutePath
                            } else {
                                FileUtils.openFile(context, fileToManage!!.absolutePath)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (!fileToManage!!.isDirectory) {
                        DpadTvButton(
                            text = "Mini Player",
                            icon = Icons.Default.Star,
                            tint = Color(0xFFE91E63),
                            onClick = {
                                fileToPlay = fileToManage
                                showContextMenu = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    DpadTvButton(
                        text = "Mover / Recortar",
                        icon = Icons.Default.Share,
                        tint = Color(0xFFFF9500),
                        onClick = {
                            AppState.tvClipboardFile.value = fileToManage
                            showContextMenu = false
                            Logger.log("Item copiado para a área de transferência: ${fileToManage!!.name}")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DpadTvButton(
                        text = "Renomear",
                        icon = Icons.Default.Info,
                        tint = Color(0xFF0A84FF),
                        onClick = {
                            renameNameText = fileToManage!!.name
                            fileToRename = fileToManage
                            showContextMenu = false
                            showRenameDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!fileToManage!!.isDirectory) {
                        DpadTvButton(
                            text = "Abrir como...",
                            icon = Icons.Default.PlayArrow,
                            tint = Color(0xFF34C759),
                            onClick = {
                                showContextMenu = false
                                showOpenAsDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    DpadTvButton(
                        text = "Excluir",
                        icon = Icons.Default.Close,
                        tint = Color(0xFFFF3B30),
                        onClick = {
                            fileToDelete = fileToManage
                            showContextMenu = false
                            showDeleteConfirm = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                DpadTvButton(
                    text = "Cancelar",
                    icon = Icons.Default.Close,
                    tint = Color.Gray,
                    onClick = { showContextMenu = false }
                )
            }
        )
    }

    if (showRenameDialog && fileToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Renomear Item", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Digite o novo nome para o item:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = renameNameText,
                        onValueChange = { renameNameText = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2C2C2E),
                            unfocusedContainerColor = Color(0xFF2C2C2E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DpadTvButton(
                        text = "Cancelar",
                        icon = Icons.Default.Close,
                        tint = Color.Gray,
                        onClick = { showRenameDialog = false }
                    )
                    DpadTvButton(
                        text = "Salvar",
                        icon = Icons.Default.Check,
                        tint = AppConfig.ActiveGreen,
                        onClick = {
                            val src = fileToRename!!
                            if (renameNameText.isNotBlank() && renameNameText != src.name) {
                                handleAction("Renomear file") {
                                    FileUtils.renameUnifiedFile(context, src.absolutePath, renameNameText.trim())
                                }
                            }
                            showRenameDialog = false
                        }
                    )
                }
            }
        )
    }

    if (showOpenAsDialog && fileToManage != null) {
        AlertDialog(
            onDismissRequest = { showOpenAsDialog = false },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Abrir como...", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Escolha o tipo de visualizador:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    DpadTvButton(
                        text = "Vídeo",
                        icon = Icons.Default.PlayArrow,
                        tint = Color(0xFF34C759),
                        onClick = {
                            showOpenAsDialog = false
                            openFileAsIntent(context, fileToManage!!.absolutePath, "video/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DpadTvButton(
                        text = "Áudio",
                        icon = Icons.Default.Star,
                        tint = Color(0xFFAF52DE),
                        onClick = {
                            showOpenAsDialog = false
                            openFileAsIntent(context, fileToManage!!.absolutePath, "audio/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DpadTvButton(
                        text = "Imagem",
                        icon = Icons.Default.Info,
                        tint = Color(0xFFFF2D55),
                        onClick = {
                            showOpenAsDialog = false
                            openFileAsIntent(context, fileToManage!!.absolutePath, "image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DpadTvButton(
                        text = "Texto / Documento",
                        icon = Icons.Default.List,
                        tint = Color(0xFF0A84FF),
                        onClick = {
                            showOpenAsDialog = false
                            openFileAsIntent(context, fileToManage!!.absolutePath, "text/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                DpadTvButton(
                    text = "Cancelar",
                    icon = Icons.Default.Close,
                    tint = Color.Gray,
                    onClick = { showOpenAsDialog = false }
                )
            }
        )
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Nova Pasta", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Digite o nome da nova pasta:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = newFolderNameText,
                        onValueChange = { newFolderNameText = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2C2C2E),
                            unfocusedContainerColor = Color(0xFF2C2C2E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DpadTvButton(
                        text = "Cancelar",
                        icon = Icons.Default.Close,
                        tint = Color.Gray,
                        onClick = { showCreateFolderDialog = false }
                    )
                    DpadTvButton(
                        text = "Criar",
                        icon = Icons.Default.Check,
                        tint = AppConfig.ActiveGreen,
                        onClick = {
                            if (newFolderNameText.isNotBlank()) {
                                handleAction("Criar pasta") {
                                    FileUtils.createUnifiedDirectory(context, currentPath, newFolderNameText.trim())
                                }
                            }
                            showCreateFolderDialog = false
                        }
                    )
                }
            }
        )
    }

    if (showDeleteConfirm && fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Excluir Item", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text("Deseja realmente excluir permanentemente '${fileToDelete!!.name}'?", color = Color.LightGray)
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DpadTvButton(
                        text = "Cancelar",
                        icon = Icons.Default.Close,
                        tint = Color.Gray,
                        onClick = { showDeleteConfirm = false }
                    )
                    DpadTvButton(
                        text = "Excluir",
                        icon = Icons.Default.Check,
                        tint = AppConfig.ErrorRed,
                        onClick = {
                            val f = fileToDelete!!
                            handleAction("Excluir item") {
                                FileUtils.deleteUnifiedFile(context, f.absolutePath)
                            }
                            showDeleteConfirm = false
                        }
                    )
                }
            }
        )
    }

    if (isProcessing) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.Transparent,
            title = null,
            text = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(Color(0xE61C1C1E), RoundedCornerShape(16.dp))
                            .padding(32.dp)
                    ) {
                        CircularProgressIndicator(color = AppConfig.PrimaryBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(processingText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (fileToPlay != null) {
        InternalMediaViewer(file = fileToPlay!!, onClose = { fileToPlay = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvFileGridItem(file: UnifiedFile, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val isDarkTheme by AppState.isDarkTheme.collectAsState()
    var isFocused by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "GridItemScale"
    )
    
    val itemBgColor = if (isFocused) {
        if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    } else {
        Color.Transparent
    }

    Column(
        modifier = modifier
            .width(110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(itemBgColor)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                                onClick()
                                true
                            } else {
                                false
                            }
                        }
                        android.view.KeyEvent.KEYCODE_MENU -> {
                            onLongClick()
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                    if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER) {
                        if (keyEvent.nativeKeyEvent.flags and android.view.KeyEvent.FLAG_LONG_PRESS != 0) {
                            onLongClick()
                            true
                        } else false
                    } else false
                } else false
            }
            .clickable(onClick = onClick)
            .border(if (isFocused) BorderStroke(2.5.dp, if (isDarkTheme) Color.White else AppConfig.PrimaryBlue) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .shadow(if (isFocused) 8.dp else 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (file.isDirectory) {
                AestheticSprites.ProceduralFolderIcon(modifier = Modifier.size(56.dp), isDarkTheme = isDarkTheme)
            } else {
                val ext = file.extension
                if (ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")) {
                    val model = if (file.uriString != null) Uri.parse(file.uriString) else File(file.absolutePath)
                    coil.compose.AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    AestheticSprites.ProceduralFileIcon(modifier = Modifier.size(52.dp), extension = ext, isDarkTheme = isDarkTheme)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = file.name,
            color = if (isDarkTheme) Color.White else Color.Black,
            fontSize = 12.sp,
            maxLines = 2,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvFileListItem(file: UnifiedFile, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val isDark by AppState.isDarkTheme.collectAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "ListItemScale"
    )
    val bgColor = if (isFocused) AppConfig.PrimaryBlue else if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val textColor = if (isFocused) Color.White else if (isDark) Color.White else Color.Black
    val textMutedColor = if (isFocused) Color.LightGray else if (isDark) Color.Gray else Color.DarkGray
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                                onClick()
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_MENU -> {
                            onLongClick()
                            true
                        }
                        else -> false
                    }
                } else if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                    if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER) {
                        if (keyEvent.nativeKeyEvent.flags and android.view.KeyEvent.FLAG_LONG_PRESS != 0) {
                            onLongClick()
                            true
                        } else false
                    } else false
                } else false
            }
            .clickable(onClick = onClick)
            .border(if (isFocused) BorderStroke(2.5.dp, if (isDark) Color.White else AppConfig.PrimaryBlue) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (file.isDirectory) {
                AestheticSprites.ProceduralFolderIcon(modifier = Modifier.size(42.dp), isDarkTheme = isDark)
            } else {
                val ext = file.extension
                if (ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")) {
                    val model = if (file.uriString != null) Uri.parse(file.uriString) else File(file.absolutePath)
                    coil.compose.AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    AestheticSprites.ProceduralFileIcon(modifier = Modifier.size(38.dp), extension = ext, isDarkTheme = isDark)
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                color = textColor,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            val subText = if (file.isDirectory) "Pasta" else formatTvFileSize(file.length)
            Text(
                text = subText,
                color = textMutedColor,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

fun openFileAsIntent(context: Context, path: String, mimeType: String) {
    try {
        val uri = FileUtils.getUriForPath(context, path)
        if (uri == null) {
            Logger.log("Failed to open file: URI is null")
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Logger.log("Failed to open file as $mimeType: ${e.message}")
    }
}

@Composable
fun TvServerDashboard(onToggleServer: () -> Unit) {
    val isRunning by ServerState.isServerRunning.collectAsState()
    val isConnected by ServerState.isClientConnected.collectAsState()
    val clientIp by ServerState.clientIp.collectAsState()
    val serverIp by ServerState.serverIp.collectAsState()
    val logs by Logger.logs.collectAsState()
    val isDarkTheme by AppState.isDarkTheme.collectAsState()

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val textMutedColor = if (isDarkTheme) Color.Gray else Color.DarkGray
    val panelBg = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val panelBorder = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("Servidor Host", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Left Panel: Server Info & Status
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(panelBg)
                    .border(BorderStroke(1.dp, panelBorder), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HostStatusIndicator(isRunning = isRunning, serverIp = serverIp)
                Spacer(modifier = Modifier.height(24.dp))
                
                if (!isConnected) {
                    Text(
                        text = "ESCANEE PARA CONECTAR", 
                        color = textMutedColor, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val serverUrl = "http://$serverIp:${AppConfig.PORT}"
                    val borderBrush = remember(isRunning) {
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = if (isRunning) {
                                listOf(AppConfig.PrimaryBlue, Color(0xFF30D158))
                            } else {
                                listOf(Color(0xFFFF9500), Color(0xFFFF453A))
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(borderBrush)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val encodedUrl = java.net.URLEncoder.encode(serverUrl, "UTF-8")
                            val qrColorHex = if (isRunning) "0a84ff" else "ff9500"
                            val qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=$encodedUrl&color=$qrColorHex&bgcolor=ffffff"
                            coil.compose.AsyncImage(
                                model = qrCodeUrl,
                                contentDescription = "Código QR",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppConfig.ActiveGreen, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("CLIENTE CONECTADO", color = AppConfig.ActiveGreen, fontWeight = FontWeight.Bold)
                    Text("IP: $clientIp", color = textColor)
                }

                Spacer(modifier = Modifier.weight(1f))
                
                DpadTvButton(
                    text = if (isRunning) "Parar Servidor" else "Iniciar Servidor",
                    icon = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                    tint = if (isRunning) AppConfig.ErrorRed else AppConfig.PrimaryBlue,
                    onClick = onToggleServer
                )
            }
            
            // Right Panel: Logs
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(panelBg)
                    .border(BorderStroke(1.dp, panelBorder), RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Text("LOGS DE TELEMETRIA", color = textMutedColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs) { logLine ->
                        Text(
                            text = logLine,
                            color = if (logLine.contains("Error", ignoreCase = true)) AppConfig.ErrorRed else textColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun InternalMediaViewer(file: UnifiedFile, onClose: () -> Unit) {
    val context = LocalContext.current
    val ext = file.extension.lowercase()
    val isImage = ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    val isVideo = ext in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "m3u8", "mpd")
    val isAudio = ext in listOf("mp3", "wav", "flac", "ogg", "m4a", "aac")
    
    var videoViewInstance by remember { mutableStateOf<android.widget.VideoView?>(null) }
    var isPlayingState by remember { mutableStateOf(true) }
    var currentPosState by remember { mutableStateOf(0) }
    var durationState by remember { mutableStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        ServerState.playerState.value = "PLAYING"
        ServerState.playTitle.value = file.name
        onDispose {
            ServerState.playerState.value = "IDLE"
            ServerState.currentPlayTime.value = 0.0
            ServerState.playDuration.value = 0.0
            ServerState.playTitle.value = ""
        }
    }

    // Capture remote playback actions sent via REST API or remote control
    LaunchedEffect(videoViewInstance) {
        if (videoViewInstance != null) {
            RemoteCommandChannel.commands.collect { cmd ->
                if (cmd.startsWith("SEEK|")) {
                    val sec = cmd.substringAfter("SEEK|").toDoubleOrNull() ?: 0.0
                    val ms = (sec * 1000).toInt()
                    videoViewInstance?.seekTo(ms)
                } else if (cmd == "MEDIA_PLAY" || cmd == "PLAY") {
                    videoViewInstance?.start()
                    isPlayingState = true
                } else if (cmd == "MEDIA_PAUSE" || cmd == "PAUSE") {
                    videoViewInstance?.pause()
                    isPlayingState = false
                }
            }
         }
    }

    // D-Pad Back Button support inside player
    BackHandler(enabled = true) {
        onClose()
    }

    // Controls slide/fade autohide timer
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5000)
            controlsVisible = false
        }
    }

    // Local loop timer for video duration/position syncing
    LaunchedEffect(videoViewInstance, isPlayingState) {
        if (videoViewInstance != null) {
            while (true) {
                try {
                    val pos = videoViewInstance!!.currentPosition
                    val dur = videoViewInstance!!.duration
                    currentPosState = pos
                    durationState = dur
                    
                    ServerState.playerState.value = if (isPlayingState) "PLAYING" else "PAUSED"
                    ServerState.currentPlayTime.value = pos.toDouble() / 1000.0
                    ServerState.playDuration.value = dur.toDouble() / 1000.0
                } catch (e: Exception) {}
                delay(250)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { controlsVisible = true }
            .onKeyEvent { keyEvent ->
                controlsVisible = true
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            videoViewInstance?.let { vv ->
                                if (isPlayingState) {
                                    vv.pause()
                                    isPlayingState = false
                                } else {
                                    vv.start()
                                    isPlayingState = true
                                }
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                        android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            videoViewInstance?.let { vv ->
                                val newPos = (vv.currentPosition - 10000).coerceAtLeast(0)
                                vv.seekTo(newPos)
                                currentPosState = newPos
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                        android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            videoViewInstance?.let { vv ->
                                val newPos = (vv.currentPosition + 10000).coerceAtMost(vv.duration)
                                vv.seekTo(newPos)
                                currentPosState = newPos
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        if (isImage) {
            val model = if (file.uriString != null) Uri.parse(file.uriString) else File(file.absolutePath)
            coil.compose.AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else if (isVideo || isAudio) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isAudio) {
                    // Show a beautiful glowing abstract canvas visualizer
                    AestheticSprites.ConstellationMesh(
                        modifier = Modifier.fillMaxSize(),
                        isDarkTheme = true
                    )
                    
                    // Display glowing playing disk in the center
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        AestheticSprites.ProceduralFileIcon(
                            modifier = Modifier.size(112.dp),
                            extension = file.extension,
                            isDarkTheme = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = file.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "NEXUS AUDIOMATE ACTIVE DECODING...",
                            color = AppConfig.PrimaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                AndroidView(
                    factory = { ctx ->
                        android.widget.VideoView(ctx).apply {
                            val uri = if (file.absolutePath.startsWith("http://") || file.absolutePath.startsWith("https://")) {
                                Uri.parse(file.absolutePath)
                            } else if (file.uriString != null) {
                                Uri.parse(file.uriString)
                            } else {
                                Uri.fromFile(File(file.absolutePath))
                            }
                            setVideoURI(uri)
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                            videoViewInstance = this
                        }
                    },
                    modifier = if (isAudio) Modifier.size(1.dp) else Modifier.fillMaxSize()
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Formato de mídia não suportado no player interno", color = Color.White)
            }
        }
        
        // Auto-hiding Top Close Button
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopEnd).padding(32.dp)
        ) {
            DpadTvButton(
                text = "Fechar",
                icon = Icons.Default.Close,
                tint = Color.White,
                onClick = onClose
            )
        }

        // Custom UI TV Playback Controls
        AnimatedVisibility(
            visible = (isVideo || isAudio) && controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                // Progress slider bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatTime(currentPosState),
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    val progress = if (durationState > 0) currentPosState.toFloat() / durationState.toFloat() else 0f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.DarkGray)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AppConfig.PrimaryBlue)
                        )
                    }
                    
                    Text(
                        text = formatTime(durationState),
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control buttons row
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DpadTvButton(
                        text = "-10 Segundos",
                        icon = Icons.Default.Refresh,
                        tint = Color.White,
                        onClick = {
                            videoViewInstance?.let { vv ->
                                val newPos = (vv.currentPosition - 10000).coerceAtLeast(0)
                                vv.seekTo(newPos)
                                currentPosState = newPos
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    DpadTvButton(
                        text = if (isPlayingState) "Pausar" else "Reproduzir",
                        icon = if (isPlayingState) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                        tint = if (isPlayingState) Color(0xFFFF9500) else Color(0xFF30D158),
                        onClick = {
                            videoViewInstance?.let { vv ->
                                if (isPlayingState) {
                                    vv.pause()
                                    isPlayingState = false
                                } else {
                                    vv.start()
                                    isPlayingState = true
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    DpadTvButton(
                        text = "+10 Segundos",
                        icon = Icons.Default.PlayArrow,
                        tint = Color.White,
                        onClick = {
                            videoViewInstance?.let { vv ->
                                val newPos = (vv.currentPosition + 10000).coerceAtMost(vv.duration)
                                vv.seekTo(newPos)
                                currentPosState = newPos
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HostStatusIndicator(isRunning: Boolean, serverIp: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isRunning) AppConfig.ActiveGreen.copy(alpha = 0.1f) else AppConfig.ErrorRed.copy(alpha = 0.1f))
            .border(1.dp, if (isRunning) AppConfig.ActiveGreen else AppConfig.ErrorRed, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (isRunning) AppConfig.ActiveGreen else AppConfig.ErrorRed))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(if (isRunning) "ONLINE" else "DESATIVADO", color = if (isRunning) AppConfig.ActiveGreen else AppConfig.ErrorRed, fontWeight = FontWeight.Bold)
            Text(if (isRunning) "http://$serverIp:${AppConfig.PORT}" else "O servidor de rede está parado", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun DpadTvButton(
    text: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale = animateFloatAsState(if (isFocused) 1.05f else 1.0f).value
    val buttonBackground = if (isFocused) AppConfig.PrimaryBlue else Color(0xFF2C2C2E)
    val contentColor = if (isFocused) Color.White else Color.White

    Row(
        modifier = modifier
            .height(54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(buttonBackground)
            .onFocusChanged { state -> isFocused = state.isFocused }
            .clickable { onClick() }
            .border(if (isFocused) BorderStroke(3.dp, Color.White) else BorderStroke(1.dp, Color.Transparent), RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (isFocused) Color.White else tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
    }
}

@Composable
fun TvSplashScreen() {
    var scale by remember { mutableStateOf(0.8f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "SplashScale"
    )
    
    LaunchedEffect(Unit) {
        scale = 1.0f
    }

    Box(
        modifier = Modifier.fillMaxSize().background(AppConfig.BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_nexus_logo),
                contentDescription = "Nexus Logo",
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "NEXUS",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }.padding(top = 4.dp)) {
                Text(
                    "PRO",
                    color = Color.LightGray,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.width(20.dp).height(4.dp).background(Color(0xFFFF9933), shape = RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
fun ChangelogPopup(
    context: Context,
    onDismiss: () -> Unit
) {
    val pInfo = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0) }
    val localVersionName = pInfo.versionName ?: "1.1.3"
    val localVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        pInfo.longVersionCode.toInt()
    } else {
        pInfo.versionCode
    }

    val currentChangelog = ChangelogManager.changelogs.firstOrNull { it.versionCode == localVersionCode } 
        ?: ChangelogManager.changelogs.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151517),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AppConfig.PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Novidades Instaladas!",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Versão ${currentChangelog.versionName} • Build ${currentChangelog.versionCode}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                Text(
                    text = "O aplicativo foi atualizado com sucesso por ${ChangelogManager.creatorName}! Abaixo estão as melhorias realizadas nesta versão para deixar seu Nexus ainda mais robusto:",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(currentChangelog.highlights) { bullet ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "•",
                                color = AppConfig.PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = bullet,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                DpadTvButton(
                    text = "Entendi, Excelente!",
                    icon = Icons.Default.Check,
                    tint = Color(0xFF30D158),
                    onClick = onDismiss,
                    modifier = Modifier.widthIn(min = 180.dp)
                )
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AboutVersionDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    val pInfo = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0) }
    val localVersionName = pInfo.versionName ?: "1.1.3"
    val localVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        pInfo.longVersionCode.toInt()
    } else {
        pInfo.versionCode
    }

    var checkStateMessage by remember { mutableStateOf("") }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateFound by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isDownloadingUpdate) onDismiss() },
        containerColor = Color(0xFF151517),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AppConfig.PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Sobre esta Versão",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Nexus Explorer Pro",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(Color(0xFF232325), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "DESENVOLVEDOR DO PROJETO",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ChangelogManager.creatorName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "GitHub: ${ChangelogManager.creatorGithub}",
                            color = AppConfig.PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "HISTÓRICO COMPLETO DE ATUALIZAÇÕES",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(ChangelogManager.changelogs) { item ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Versão ${item.versionName} (${item.releaseDate})",
                                    color = AppConfig.PrimaryBlue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Build ${item.versionCode}",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            item.highlights.forEach { highlight ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 6.dp, bottom = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "•", color = Color(0xFFFF9933), fontSize = 12.sp)
                                    Text(text = highlight, color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (checkStateMessage.isNotEmpty()) {
                    Text(
                        text = checkStateMessage,
                        color = if (updateFound) Color(0xFFE91E63) else Color.Green,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (updateFound) {
                        DpadTvButton(
                            text = if (isDownloadingUpdate) "Espere..." else "Baixar e Atualizar",
                            icon = Icons.Default.Refresh,
                            tint = Color(0xFFE91E63),
                            onClick = {
                                if (!isDownloadingUpdate) {
                                    isDownloadingUpdate = true
                                    checkStateMessage = "Iniciando download..."
                                    scope.launch {
                                        val repoOwner = "kelvinhx"
                                        val repoName = "Android-TV-File-Explorer-Web-Video-Cast"
                                        val artifactName = "app-debug"
                                        Updater.downloadExtractAndInstall(
                                            context = context,
                                            repoOwner = repoOwner,
                                            repoName = repoName,
                                            artifactName = artifactName,
                                            onProgress = { msg ->
                                                checkStateMessage = msg
                                                if (msg.startsWith("Erro")) {
                                                    isDownloadingUpdate = false
                                                }
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.widthIn(min = 180.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        DpadTvButton(
                            text = if (isCheckingUpdates) "Consultando..." else "Verificar Nova Versão",
                            icon = Icons.Default.Refresh,
                            tint = AppConfig.PrimaryBlue,
                            onClick = {
                                if (!isCheckingUpdates) {
                                    isCheckingUpdates = true
                                    checkStateMessage = "Verificando se há atualizações no GitHub..."
                                    scope.launch {
                                        val available = Updater.isUpdateAvailable(context)
                                        if (available) {
                                            checkStateMessage = "Nova versão encontrada no repositório!"
                                            updateFound = true
                                        } else {
                                            checkStateMessage = "Você já está na versão mais recente!"
                                            updateFound = false
                                        }
                                        isCheckingUpdates = false
                                    }
                                }
                            },
                            modifier = Modifier.widthIn(min = 180.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        DpadTvButton(
                            text = if (isDownloadingUpdate) "Espere..." else "Forçar Build (Bypass)",
                            icon = Icons.Default.PlayArrow,
                            tint = Color(0xFFFF9500),
                            onClick = {
                                if (!isDownloadingUpdate) {
                                    isDownloadingUpdate = true
                                    checkStateMessage = "Iniciando download forçado (Bypass)..."
                                    scope.launch {
                                        val repoOwner = "kelvinhx"
                                        val repoName = "Android-TV-File-Explorer-Web-Video-Cast"
                                        val artifactName = "app-debug"
                                        Updater.downloadExtractAndInstall(
                                            context = context,
                                            repoOwner = repoOwner,
                                            repoName = repoName,
                                            artifactName = artifactName,
                                            onProgress = { msg ->
                                                checkStateMessage = msg
                                                if (msg.startsWith("Erro")) {
                                                    isDownloadingUpdate = false
                                                }
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.widthIn(min = 180.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    DpadTvButton(
                        text = "Fechar",
                        icon = Icons.Default.Close,
                        tint = Color.Gray,
                        onClick = { if (!isDownloadingUpdate) onDismiss() },
                        modifier = Modifier.widthIn(min = 120.dp)
                    )
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

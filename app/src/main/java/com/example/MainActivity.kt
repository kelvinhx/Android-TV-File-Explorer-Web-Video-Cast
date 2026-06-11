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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.input.key.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

object AppState {
    val browserUrl = MutableStateFlow<String?>(null)
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
            } catch (e: Exception) {
                Logger.log("Failed to persist Android/data folder permission: ${e.message}")
            }
        }
    }

    fun triggerAndroidDataPermissionRequest() {
        try {
            val documentUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata")
            requestDocumentTree.launch(documentUri)
        } catch (e: Exception) {
            Logger.log("Failed to target document URI, launching normal folder selector: ${e.message}")
            try {
                requestDocumentTree.launch(null)
            } catch (e2: Exception) {
                Logger.log("Fatal: could not request folder tree.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Write initial log
        Logger.log("Nexus Explorer Pro launched.")
        
        // Auto start background Server
        startNexusService()

        setContent {
            val browserUrl by AppState.browserUrl.collectAsState()

            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = AppConfig.BackgroundDark
                ) { innerPadding ->
                    if (browserUrl != null) {
                        BrowserScreen(
                            initialUrl = browserUrl!!,
                            onClose = { AppState.browserUrl.value = null }
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

    var selectedSidebarItem by remember { mutableStateOf("On My TV") }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // --- Sidebar (Locations) ---
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(Color(0xFF1C1C1E))
                .padding(20.dp)
        ) {
            Text(
                text = "Locais",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
            )

            TvSidebarItem(
                text = "Na minha TV",
                icon = Icons.Default.Home,
                isSelected = selectedSidebarItem == "On My TV",
                onClick = { selectedSidebarItem = "On My TV" }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TvSidebarItem(
                text = "Servidor Host",
                icon = Icons.Default.Share,
                isSelected = selectedSidebarItem == "Host Server",
                onClick = { selectedSidebarItem = "Host Server" }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            TvSidebarItem(
                text = "Configurações",
                icon = Icons.Default.Settings,
                isSelected = selectedSidebarItem == "Settings",
                onClick = { selectedSidebarItem = "Settings" }
            )
        }

        // --- Main Content Area ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF000000))
        ) {
            when (selectedSidebarItem) {
                "On My TV" -> {
                    if (hasStoragePermission) {
                        TvFilesBrowser(
                            context = context,
                            onRequestAndroidDataPermission = onRequestAndroidDataPermission
                        )
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
                "Host Server" -> {
                    TvServerDashboard(onToggleServer = onToggleServer)
                }
                "Settings" -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Configurações", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(30.dp))
                            
                            DpadTvButton(
                                text = "Configurações do Android TV",
                                icon = Icons.Default.Settings,
                                tint = AppConfig.PrimaryBlue,
                                onClick = { 
                                    try {
                                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    } catch (e: Exception) {}
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            DpadTvButton(
                                text = "Gerenciar Permissões Padrão",
                                icon = Icons.Default.Lock,
                                tint = AppConfig.AccentGold,
                                onClick = onRequestPermissions
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            DpadTvButton(
                                text = "Permitir Acesso Completo ao Android/data",
                                icon = Icons.Default.Info,
                                tint = AppConfig.ActiveGreen,
                                onClick = onRequestAndroidDataPermission
                            )
                        }
                    }
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
    var isFocused by remember { mutableStateOf(false) }
    
    val backgroundColor = when {
        isFocused -> AppConfig.PrimaryBlue
        isSelected -> Color(0xFF2C2C2E)
        else -> Color.Transparent
    }
    
    val contentColor = when {
        isFocused -> Color.White
        isSelected -> AppConfig.PrimaryBlue
        else -> Color.Gray
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
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
fun TvFilesBrowser(
    context: Context,
    onRequestAndroidDataPermission: () -> Unit
) {
    var currentPath by remember { mutableStateOf("/storage/emulated/0") }
    var files by remember { mutableStateOf<List<UnifiedFile>>(emptyList()) }
    var showOpenAsDialog by remember { mutableStateOf(false) }

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

    var clipboardFile by remember { mutableStateOf<UnifiedFile?>(null) }

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
                        } else {
                            currentPath = "/storage/emulated/0"
                        }
                    }
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
                        clipboardFile = null
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                DpadTvButton(
                    text = "Cancelar",
                    icon = Icons.Default.Close,
                    tint = AppConfig.ErrorRed,
                    onClick = { clipboardFile = null }
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
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(files) { file ->
                        TvFileGridItem(
                            file = file,
                            onClick = {
                                fileToManage = file
                                showContextMenu = true
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
                    
                    val openText = if (fileToManage!!.isDirectory) "Entrar na Pasta" else "Abrir Arquivo"
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
                    
                    DpadTvButton(
                        text = "Mover / Recortar",
                        icon = Icons.Default.Share,
                        tint = Color(0xFFFF9500),
                        onClick = {
                            clipboardFile = fileToManage
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvFileGridItem(file: UnifiedFile, onClick: () -> Unit, onLongClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    
    val scale = animateFloatAsState(targetValue = if (isFocused) 1.05f else 1.0f).value
    
    Column(
        modifier = Modifier
            .width(100.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) Color(0xFF2C2C2E) else Color.Transparent)
            .onFocusChanged { isFocused = it.isFocused }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(if (isFocused) BorderStroke(2.dp, Color.White) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
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
                IosFolderIcon(modifier = Modifier.size(54.dp))
            } else {
                val ext = file.extension
                if (ext in listOf("jpg", "jpeg", "png", "webp", "gif")) {
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
                    IosFileIcon(modifier = Modifier.size(50.dp), extension = ext)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = file.name,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
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

    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("Servidor Host", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Left Panel: Server Info & Status
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HostStatusIndicator(isRunning = isRunning, serverIp = serverIp)
                Spacer(modifier = Modifier.height(24.dp))
                
                if (!isConnected) {
                    Text("ESCANEE PARA CONECTAR", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val serverUrl = "http://$serverIp:${AppConfig.PORT}"
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val encodedUrl = java.net.URLEncoder.encode(serverUrl, "UTF-8")
                        val qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=$encodedUrl&color=000000&bgcolor=ffffff"
                        coil.compose.AsyncImage(
                            model = qrCodeUrl,
                            contentDescription = "Código QR",
                            modifier = Modifier.size(144.dp)
                        )
                    }
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppConfig.ActiveGreen, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("CLIENTE CONECTADO", color = AppConfig.ActiveGreen, fontWeight = FontWeight.Bold)
                    Text("IP: $clientIp", color = Color.White)
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
                    .background(Color(0xFF1C1C1E))
                    .padding(24.dp)
            ) {
                Text("LOGS DE TELEMETRIA", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs) { logLine ->
                        Text(
                            text = logLine,
                            color = if (logLine.contains("Error", ignoreCase = true)) AppConfig.ErrorRed else Color.White,
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

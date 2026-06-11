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

object AppState {
    val browserUrl = MutableStateFlow<String?>(null)
}

class MainActivity : ComponentActivity() {

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
                            onRequestPermissions = { triggerPermissionRequest() }
                        )
                    }
                }
            }
        }

        // Setup real-time keypress simulator or overlay logger
        lifecycleScope.launch {
            RemoteCommandChannel.commands.collect { cmd ->
                Logger.log("Activity captured command event: $cmd")
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
    onRequestPermissions: () -> Unit
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
                text = "Locations",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
            )

            TvSidebarItem(
                text = "On My TV",
                icon = Icons.Default.Home,
                isSelected = selectedSidebarItem == "On My TV",
                onClick = { selectedSidebarItem = "On My TV" }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TvSidebarItem(
                text = "Host Server",
                icon = Icons.Default.Share,
                isSelected = selectedSidebarItem == "Host Server",
                onClick = { selectedSidebarItem = "Host Server" }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            TvSidebarItem(
                text = "Settings",
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
                        TvFilesBrowser(context = context)
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Storage Permission Required", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Please grant access in Settings to view files.", color = Color.Gray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(24.dp))
                                DpadTvButton(
                                    text = "Grant Permission",
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
                            Text("Settings", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(20.dp))
                            DpadTvButton(
                                text = "Manage Permissions",
                                icon = Icons.Default.Lock,
                                tint = AppConfig.AccentGold,
                                onClick = onRequestPermissions
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
fun TvSidebarItem(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
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
        modifier = Modifier
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

@Composable
fun TvFilesBrowser(context: Context) {
    var currentPath by remember { mutableStateOf("/storage/emulated/0") }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(currentPath) {
        val dir = File(currentPath)
        if (dir.exists() && dir.isDirectory) {
            files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        // Top Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (currentPath != "/storage/emulated/0" && currentPath != "/storage") {
                DpadTvButton(
                    text = "Back",
                    icon = Icons.Default.ArrowBack,
                    tint = AppConfig.PrimaryBlue,
                    onClick = {
                        val parent = File(currentPath).parent
                        if (parent != null) {
                            currentPath = parent
                        }
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = File(currentPath).name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // File Grid
        if (files.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("Folder is empty", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(files) { file ->
                    TvFileGridItem(file = file, onClick = {
                        if (file.isDirectory) {
                            currentPath = file.absolutePath
                        } else {
                            openFileIntent(context, file)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun TvFileGridItem(file: File, onClick: () -> Unit) {
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
            .border(if (isFocused) BorderStroke(2.dp, Color.White) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = if (file.isDirectory) Icons.Default.List else Icons.Default.Info
        val iconTint = if (file.isDirectory) Color(0xFF007AFF) else Color(0xFF8E8E93)

        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1C1C1E))
                .shadow(if (isFocused) 8.dp else 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(36.dp))
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

fun openFileIntent(context: Context, file: File) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )
        // Basic MIME type guessing
        val extension = file.extension.lowercase()
        val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Logger.log("Failed to open file on TV: ${e.message}")
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
        Text("Network Host", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
                    Text("SCAN TO CONNECT", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val serverUrl = "http://$serverIp:${AppConfig.PORT}"
                    Box(modifier = Modifier.size(160.dp).background(Color.White).padding(8.dp), contentAlignment = Alignment.Center) {
                        Text(text = "QR\n$serverUrl", color = Color.Black, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppConfig.ActiveGreen, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("CLIENT CONNECTED", color = AppConfig.ActiveGreen, fontWeight = FontWeight.Bold)
                    Text("IP: $clientIp", color = Color.White)
                }

                Spacer(modifier = Modifier.weight(1f))
                
                DpadTvButton(
                    text = if (isRunning) "Stop Server" else "Start Server",
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
                Text("TELEMETRY LOGS", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            Text(if (isRunning) "ONLINE" else "OFFLINE", color = if (isRunning) AppConfig.ActiveGreen else AppConfig.ErrorRed, fontWeight = FontWeight.Bold)
            Text(if (isRunning) "http://$serverIp:${AppConfig.PORT}" else "Server is stopped", color = Color.Gray, fontSize = 12.sp)
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
            .border(if (isFocused) BorderStroke(3.dp, Color.White) else BorderStroke(1.dp, Color.Transparent), RoundedCornerShape(14.dp))
            .onFocusChanged { state -> isFocused = state.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (isFocused) Color.White else tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
    }
}

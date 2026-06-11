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
                        TvFilesBrowser(context = context)
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
                            Spacer(modifier = Modifier.height(20.dp))
                            DpadTvButton(
                                text = "Gerenciar Permissões",
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 4.dp, bottomStart = 4.dp))
            .background(Color(0xFF1C1C1E))
            .border(1.5.dp, Color(0xFF3A3A3C), RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 4.dp, bottomStart = 4.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = extension.uppercase().take(4),
                color = Color.LightGray,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TvFilesBrowser(context: Context) {
    var currentPath by remember { mutableStateOf("/storage/emulated/0") }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var isManageMode by remember { mutableStateOf(false) }

    // Dialog state management
    var fileToManage by remember { mutableStateOf<File?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    var fileToRename by remember { mutableStateOf<File?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameNameText by remember { mutableStateOf("") }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameText by remember { mutableStateOf("") }

    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var clipboardFile by remember { mutableStateOf<File?>(null) }

    // Helper list fetch trigger
    var listKey by remember { mutableStateOf(0) }

    LaunchedEffect(currentPath, listKey) {
        val dir = File(currentPath)
        if (dir.exists() && dir.isDirectory) {
            files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        }
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
                        val parent = File(currentPath).parent
                        if (parent != null) {
                            currentPath = parent
                        }
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            val curName = if (currentPath == "/storage/emulated/0") "Na minha TV" else File(currentPath).name
            Text(
                text = curName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Action: Toggle Manage Mode
            DpadTvButton(
                text = if (isManageMode) "Modo: Gerenciar" else "Modo: Navegar",
                icon = if (isManageMode) Icons.Default.Settings else Icons.Default.Home,
                tint = if (isManageMode) AppConfig.AccentGold else AppConfig.PrimaryBlue,
                onClick = { isManageMode = !isManageMode }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Action: Create folder
            DpadTvButton(
                text = "Nova Pasta",
                icon = Icons.Default.Share,
                tint = AppConfig.ActiveGreen,
                onClick = {
                    newFolderNameText = ""
                    showCreateFolderDialog = true
                }
            )

            // Dynamic Action: Clipboard paste
            if (clipboardFile != null) {
                Spacer(modifier = Modifier.width(12.dp))
                DpadTvButton(
                    text = "Colar aqui",
                    icon = Icons.Default.Check,
                    tint = AppConfig.PrimaryBlue,
                    onClick = {
                        val src = clipboardFile!!
                        val success = moveFileOrDirectory(src, File(currentPath))
                        if (success) {
                            Logger.log("Item movido com sucesso para: $currentPath")
                            clipboardFile = null
                            listKey++
                        } else {
                            Logger.log("Erro ao mover item.")
                        }
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
                            if (isManageMode) {
                                fileToManage = file
                                showContextMenu = true
                            } else {
                                if (file.isDirectory) {
                                    currentPath = file.absolutePath
                                } else {
                                    openFileIntent(context, file)
                                }
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
                    val descStr = if (fileToManage!!.isDirectory) "Pasta" else "Arquivo • ${formatTvFileSize(fileToManage!!.length())}"
                    Text(text = descStr, color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
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
                                val dest = File(src.parentFile, renameNameText.trim())
                                val success = src.renameTo(dest)
                                if (success) {
                                    Logger.log("Item renomeado para: ${dest.name}")
                                    listKey++
                                } else {
                                    Logger.log("Falha ao renomear item.")
                                }
                            }
                            showRenameDialog = false
                        }
                    )
                }
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
                                val newDir = File(currentPath, newFolderNameText.trim())
                                val success = newDir.mkdirs()
                                if (success) {
                                    Logger.log("Pasta criada com sucesso: ${newDir.name}")
                                    listKey++
                                } else {
                                    Logger.log("Erro ao criar pasta.")
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
                            val success = f.deleteRecursively()
                            if (success) {
                                Logger.log("Item excluído com sucesso: ${f.name}")
                                listKey++
                            } else {
                                Logger.log("Erro ao excluir item.")
                            }
                            showDeleteConfirm = false
                        }
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvFileGridItem(file: File, onClick: () -> Unit, onLongClick: () -> Unit) {
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
                val ext = file.extension.lowercase()
                if (ext in listOf("jpg", "jpeg", "png", "webp", "gif")) {
                    coil.compose.AsyncImage(
                        model = file,
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

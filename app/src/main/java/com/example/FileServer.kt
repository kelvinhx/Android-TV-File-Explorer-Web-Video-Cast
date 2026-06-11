package com.example

import android.content.Context
import android.media.AudioManager
import android.content.Intent
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class FileServer(private val context: Context) {
    private var server: NettyApplicationEngine? = null

    fun start() {
        if (server != null) return
        Logger.log("Initializing Ktor Netty core on port ${AppConfig.PORT}...")
        
        try {
            server = embeddedServer(Netty, port = AppConfig.PORT) {
                routing {
                    // Serve Web PWA dashboard
                    get("/") {
                        val host = try { 
                            call.request.origin.remoteHost 
                        } catch (e: Exception) { 
                            "Client Mobile" 
                        }
                        ServerState.setClientConnected(true, host)
                        Logger.log("Connection established with client IP: $host")
                        call.respondText(WebInterface.getHtml(), ContentType.Text.Html)
                    }

                    // Directory Discovery
                    get("/api/files") {
                        val rootPath = "/storage/emulated/0"
                        val pathParam = call.request.queryParameters["path"] ?: rootPath
                        val dir = File(pathParam)

                        if (!dir.exists() || !dir.isDirectory) {
                            call.respondText(
                                JSONObject().put("status", "error").put("message", "Requested path can't be found").toString(),
                                ContentType.Application.Json
                            )
                            return@get
                        }

                        val isRoot = dir.absolutePath == rootPath || dir.absolutePath == "/"
                        val filesList = dir.listFiles() ?: emptyArray()
                        
                        val fileArray = JSONArray()
                        for (f in filesList) {
                            // Skip hidden system files to keep UI clean
                            if (f.name.startsWith(".")) continue
                            
                            val obj = JSONObject()
                            obj.put("name", f.name)
                            obj.put("absolutePath", f.absolutePath)
                            obj.put("isDirectory", f.isDirectory)
                            
                            if (f.isDirectory) {
                                val sizeBytes = FileUtils.getFolderSize(f)
                                val subfiles = f.listFiles()
                                val count = subfiles?.size ?: 0
                                obj.put("sizeFormatted", "$count items (${FileUtils.formatSize(sizeBytes)})")
                            } else {
                                obj.put("sizeFormatted", FileUtils.formatSize(f.length()))
                            }
                            fileArray.put(obj)
                        }

                        // Order list: Folders first, alphabetical
                        val sortedList = mutableListOf<JSONObject>()
                        for (i in 0 until fileArray.length()) {
                            sortedList.add(fileArray.getJSONObject(i))
                        }
                        sortedList.sortWith(compareBy({ !it.getBoolean("isDirectory") }, { it.getString("name").lowercase() }))

                        val sortedJsonArray = JSONArray(sortedList)

                        val stat = try {
                            android.os.StatFs("/storage/emulated/0")
                        } catch (e: Exception) {
                            null
                        }
                        val bytesAvailable = stat?.let { it.blockSizeLong * it.availableBlocksLong } ?: 0L
                        val bytesTotal = stat?.let { it.blockSizeLong * it.blockCountLong } ?: 0L
                        val bytesUsed = bytesTotal - bytesAvailable
                        val storagePercent = if (bytesTotal > 0) ((bytesUsed.toDouble() / bytesTotal.toDouble()) * 100).toInt() else 0

                        val response = JSONObject()
                        response.put("currentPath", dir.absolutePath)
                        response.put("isRoot", isRoot)
                        response.put("files", sortedJsonArray)
                        response.put("ramPercent", Logger.getRamTelemetry().percentageUsed)
                        response.put("storageFreeFormatted", FileUtils.formatSize(bytesAvailable))
                        response.put("storageTotalFormatted", FileUtils.formatSize(bytesTotal))
                        response.put("storagePercent", storagePercent)

                        call.respondText(response.toString(), ContentType.Application.Json)
                    }

                    // File Downloading/Streaming
                    get("/api/download") {
                        val path = call.request.queryParameters["path"]
                        if (path.isNullOrEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "Path parameter is required.")
                            return@get
                        }

                        val file = File(path)
                        if (!file.exists() || file.isDirectory) {
                            call.respond(HttpStatusCode.NotFound, "File not found.")
                            return@get
                        }

                        Logger.log("Streaming file: ${file.name}")
                        call.respondOutputStream(ContentType.parse(FileUtils.getMimeType(file)), HttpStatusCode.OK) {
                            file.inputStream().use { input ->
                                val buffer = ByteArray(64 * 1024) // 64KB buffer matching rules
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    write(buffer, 0, bytesRead)
                                }
                            }
                        }
                    }

                    // Remote Trigger File Viewer
                    post("/api/open") {
                        val path = call.request.queryParameters["path"]
                        if (path.isNullOrEmpty()) {
                            call.respondText(JSONObject().put("status", "error").put("message", "Path is missing").toString(), ContentType.Application.Json)
                            return@post
                        }

                        val file = File(path)
                        if (!file.exists()) {
                            call.respondText(JSONObject().put("status", "error").put("message", "File does not exist").toString(), ContentType.Application.Json)
                            return@post
                        }

                        val success = FileUtils.openFile(this@FileServer.context, file)
                        if (success) {
                            call.respondText(JSONObject().put("status", "success").toString(), ContentType.Application.Json)
                        } else {
                            call.respondText(JSONObject().put("status", "error").put("message", "Could not execute view Action").toString(), ContentType.Application.Json)
                        }
                    }

                    // Open Browser to cast video or webpage
                    post("/api/cast") {
                        val requestBody = call.receiveText()
                        val json = JSONObject(requestBody)
                        val url = json.optString("url")
                        if (url.isNotEmpty()) {
                            Logger.log("Casting URL to TV browser: $url")
                            AppState.browserUrl.value = url
                            call.respondText(JSONObject().put("status", "success").toString(), ContentType.Application.Json)
                        } else {
                            call.respondText(JSONObject().put("status", "error").put("message", "URL missing").toString(), ContentType.Application.Json)
                        }
                    }

                    // Browse proxy for sniffing videos
                    get("/api/proxy") {
                        val urlStr = call.request.queryParameters["url"] ?: ""
                        if (urlStr.isEmpty()) {
                            call.respondText("Missing URL", ContentType.Text.Plain)
                            return@get
                        }
                        
                        try {
                            val url = java.net.URL(urlStr)
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.requestMethod = "GET"
                            // Pretend to be a mobile browser
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1")
                            
                            // Headers to avoid some blocks, though sophisticated sites will still block
                            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                            
                            conn.connect()
                            
                            val contentType = conn.contentType ?: "text/html"
                            
                            if (contentType.startsWith("text/html")) {
                                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                                var html = stream?.bufferedReader()?.readText() ?: ""
                                
                                val baseUrl = "${url.protocol}://${url.host}"
                                val path = url.path
                                val basePath = if (path.substringAfterLast("/").contains(".")) 
                                    baseUrl + path.substringBeforeLast("/") + "/" 
                                else 
                                    baseUrl + path + (if(path.endsWith("/")) "" else "/")
                                
                                val inject = """
                                    <script>
                                    // Internal AdBlock / Video Sniffer
                                    document.addEventListener('DOMContentLoaded', () => {
                                        // Remove some typical ad containers
                                        const ads = document.querySelectorAll('.ad, .ads, [id^=google_ads], ins');
                                        ads.forEach(ad => ad.style.display = 'none');

                                        const notifyParent = (src) => {
                                            if(src && (src.endsWith('.mp4') || src.endsWith('.m3u8') || src.includes('video'))) {
                                                window.parent.postMessage({type: 'video_found', url: src}, '*');
                                            }
                                        };

                                        const videos = document.querySelectorAll('video');
                                        videos.forEach(v => {
                                            notifyParent(v.src || v.querySelector('source')?.src);
                                        });

                                        const obs = new MutationObserver(mutations => {
                                            mutations.forEach(m => {
                                                m.addedNodes.forEach(n => {
                                                    if(n.tagName === 'VIDEO') {
                                                        const src = n.src || n.querySelector('source')?.src;
                                                        notifyParent(src);
                                                    } else if (n.querySelectorAll) {
                                                        const vids = n.querySelectorAll('video');
                                                        vids.forEach(v => notifyParent(v.src || v.querySelector('source')?.src));
                                                        
                                                        // Hide injected ads dynamically
                                                        const injAds = n.querySelectorAll('.ad, .ads');
                                                        injAds.forEach(ad => ad.style.display = 'none');
                                                    }
                                                });
                                            });
                                        });
                                        obs.observe(document.body, {childList: true, subtree: true});
                                    });

                                    // Intercept links to keep them inside proxy
                                    window.addEventListener('click', e => {
                                        const a = e.target.closest('a');
                                        if (a && a.href && a.href.startsWith('http')) {
                                            e.preventDefault();
                                            window.parent.postMessage({type: 'navigate', url: a.href}, '*');
                                        }
                                    });
                                    </script>
                                """.trimIndent()

                                html = html.replace("<head>", "<head><base href=\"$basePath\">$inject")
                                call.respondText(html, ContentType.Text.Html)
                            } else {
                                // If not HTML (e.g. image/json/etc), stream directly
                                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                                call.respondOutputStream(ContentType.parse(contentType), HttpStatusCode.OK) {
                                    stream?.use { input ->
                                        val buffer = ByteArray(8 * 1024)
                                        var bytesRead: Int
                                        while (input.read(buffer).also { bytesRead = it } != -1) {
                                            write(buffer, 0, bytesRead)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            call.respondText("Browser Proxy Error: ${e.message}", ContentType.Text.Plain)
                        }
                    }

                    // Native operations actions (MKDIR, DELETE, RENAME)
                    post("/api/action") {
                        val requestBody = call.receiveText()
                        val json = JSONObject(requestBody)
                        val action = json.optString("action")
                        val path = json.optString("path")
                        val argument = json.optString("argument")

                        if (path.isEmpty()) {
                            call.respondText(JSONObject().put("status", "error").put("message", "Path is missing").toString(), ContentType.Application.Json)
                            return@post
                        }

                        val file = File(path)
                        var success = false
                        var message = ""

                        try {
                            when (action) {
                                "MKDIR" -> {
                                    success = file.mkdirs()
                                    if (success) Logger.log("Created folder: $path")
                                    else message = "Permission denied or folder exists"
                                }
                                "DELETE" -> {
                                    success = if (file.isDirectory) file.deleteRecursively() else file.delete()
                                    if (success) Logger.log("Deleted item: $path")
                                    else message = "Error deleting item"
                                }
                                "RENAME" -> {
                                    if (argument.isNotEmpty()) {
                                        val target = File(file.parentFile, argument)
                                        success = file.renameTo(target)
                                        if (success) Logger.log("Renamed item to: ${target.name}")
                                        else message = "Name collision or folder missing"
                                    } else {
                                        message = "Target name must be provided"
                                    }
                                }
                                "COPY" -> {
                                    if (argument.isNotEmpty()) {
                                        val srcFile = File(argument)
                                        val destFile = File(file, srcFile.name)
                                        if (srcFile.exists()) {
                                            success = if (srcFile.isDirectory) {
                                                srcFile.copyRecursively(destFile, overwrite = true)
                                            } else {
                                                srcFile.copyTo(destFile, overwrite = true)
                                                true
                                            }
                                            if (success) Logger.log("Copied: ${srcFile.name} to ${destFile.absolutePath}")
                                            else message = "Copy failed"
                                        } else {
                                            message = "Source file does not exist"
                                        }
                                    } else {
                                        message = "Source path must be provided"
                                    }
                                }
                                "MOVE" -> {
                                    if (argument.isNotEmpty()) {
                                        val srcFile = File(argument)
                                        val destFile = File(file, srcFile.name)
                                        if (srcFile.exists()) {
                                            success = srcFile.renameTo(destFile)
                                            if (!success) {
                                                try {
                                                    if (srcFile.isDirectory) {
                                                        srcFile.copyRecursively(destFile, overwrite = true)
                                                        srcFile.deleteRecursively()
                                                    } else {
                                                        srcFile.copyTo(destFile, overwrite = true)
                                                        srcFile.delete()
                                                    }
                                                    success = true
                                                } catch (e: Exception) {
                                                    success = false
                                                    message = "Move failed: ${e.message}"
                                                }
                                            }
                                            if (success) Logger.log("Moved: ${srcFile.name} to ${destFile.absolutePath}")
                                        } else {
                                            message = "Source file does not exist"
                                        }
                                    } else {
                                        message = "Source path must be provided"
                                    }
                                }
                                else -> {
                                    message = "Unsupported target action: $action"
                                }
                            }
                        } catch (e: Exception) {
                            success = false
                            message = e.message ?: "Action runtime error"
                        }

                        val response = JSONObject()
                        if (success) {
                            response.put("status", "success")
                        } else {
                            response.put("status", "error")
                            response.put("message", message)
                        }
                        call.respondText(response.toString(), ContentType.Application.Json)
                    }

                    // Browser RC events
                    post("/api/remote") {
                        val requestBody = call.receiveText()
                        val json = JSONObject(requestBody)
                        val command = json.optString("command")

                        Logger.log("Remote signal received: $command")
                        handleRemoteCommand(command)

                        call.respondText(JSONObject().put("status", "success").toString(), ContentType.Application.Json)
                    }

                    // Multipart file uploads using 64KB streams mapping
                    post("/api/upload") {
                        val pathParam = call.request.queryParameters["path"] ?: "/storage/emulated/0"
                        val uploadDir = File(pathParam)

                        if (!uploadDir.exists()) {
                            uploadDir.mkdirs()
                        }

                        val multipart = call.receiveMultipart()
                        var count = 0

                        try {
                            multipart.forEachPart { part ->
                                if (part is PartData.FileItem) {
                                    val originalName = part.originalFileName ?: "uploaded_file_${System.currentTimeMillis()}"
                                    val dst = File(uploadDir, originalName)
                                    
                                    Logger.log("Receiving: $originalName")
                                    part.streamProvider().use { input ->
                                        dst.outputStream().use { output ->
                                            val buffer = ByteArray(64 * 1024) // 64KB buffer
                                            var bytesRead: Int
                                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                                output.write(buffer, 0, bytesRead)
                                            }
                                        }
                                    }
                                    Logger.log("Stored file: ${dst.name} (${FileUtils.formatSize(dst.length())})")
                                    ServerState.postUploadNotification("Received file '${dst.name}' from your iPhone successfully!")
                                    count++
                                }
                                part.dispose()
                            }
                            call.respondText(JSONObject().put("status", "success").put("count", count).toString(), ContentType.Application.Json)
                        } catch (e: Exception) {
                            Logger.log("Upload failed: ${e.message}")
                            call.respondText(JSONObject().put("status", "error").put("message", e.message).toString(), ContentType.Application.Json)
                        }
                    }
                }
            }.apply {
                start(wait = false)
            }

            ServerState.setServerRunning(true)
            val localIp = NetworkManager.getLocalIpAddress()
            ServerState.setServerIp(localIp)
            Logger.log("Nexus server online: http://$localIp:${AppConfig.PORT}")
        } catch (e: Exception) {
            Logger.log("Server failed to start: ${e.message}")
            ServerState.setServerRunning(false)
        }
    }

    private fun handleRemoteCommand(command: String) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (command) {
            "VOL_UP" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                Logger.log("Music volume raised")
            }
            "VOL_DOWN" -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                Logger.log("Music volume lowered")
            }
            "SETTINGS" -> {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Logger.log("Dispatched Android Settings panel intent")
                } catch (e: Exception) {
                    Logger.log("Intending System Settings failed: ${e.message}")
                }
            }
            else -> {
                // Relay keys (UP, DOWN, LEFT, RIGHT, OK, BACK, HOME, MEDIA_PLAY, MEDIA_PREV, etc.) to flow channel listeners
                RemoteCommandChannel.postCommand(command)
            }
        }
    }

    fun stop() {
        try {
            server?.stop(1000, 2000)
            server = null
            ServerState.setServerRunning(false)
            ServerState.setClientConnected(false)
            Logger.log("Ktor file server shutdown.")
        } catch (e: Exception) {
            Logger.log("Error during server shutdown: ${e.message}")
        }
    }
}

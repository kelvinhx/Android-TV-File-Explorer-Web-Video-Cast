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
                                    <style>
                                    /* Premium iOS 18 / 26 Inspired Style Injection to force hide ads and banners */
                                    .ad, .ads, .ad-box, .advertisement, #ad-container, [id^=google_ads_], [class*=ad-], ins,
                                    iframe[src*="doubleclick.net"], iframe[src*="adservice"], iframe[src*="popads"], iframe[src*="propeller"],
                                    .popunder, .popup, div[class*="popup"], div[id*="popup"], .widget-ad, .banner-ad,
                                    div[style*="z-index: 2147483647"], div[style*="z-index:99999"], div[style*="z-index: 999999"] {
                                        display: none !important;
                                        opacity: 0 !important;
                                        pointer-events: none !important;
                                        height: 0 !important;
                                        width: 0 !important;
                                        visibility: hidden !important;
                                    }
                                    </style>
                                    <script>
                                    // 1. Popup & Popunder Blocker (Force block window.open)
                                    window.open = function() {
                                        console.log("Interactive popup blocked!");
                                        return null;
                                    };

                                    // 2. Video link patterns detector
                                    const isVideoUrl = (url) => {
                                        if (!url || typeof url !== 'string') return false;
                                        const lower = url.toLowerCase();
                                        
                                        // Specific extensions with or without query params
                                        if (/\.(mp4|m3u8|mkv|mov|avi|webm|flv|3gp|ts|mpd)(\?|$)/.test(lower)) return true;
                                        
                                        // Video streaming specialized domains and endpoints
                                        if (lower.includes('streamtape.com/get_video') ||
                                            lower.includes('dood') || lower.includes('/pass_md5/') ||
                                            lower.includes('mixdrop') || lower.includes('/delivery/') ||
                                            lower.includes('filemoon') || lower.includes('byse') ||
                                            lower.includes('mcloud') || lower.includes('vizcloud') ||
                                            lower.includes('fembed') || lower.includes('streamwis') ||
                                            lower.includes('/api/source/') || lower.includes('manifest.mpd') ||
                                            lower.includes('master.m3u8')
                                        ) {
                                            return true;
                                        }
                                        return false;
                                    };

                                    const notifyParent = (src) => {
                                        if(src && isVideoUrl(src)) {
                                            console.log("Sniffed Video URL:", src);
                                            window.parent.postMessage({type: 'video_found', url: src}, '*');
                                        }
                                    };

                                    // 3. Dynamic Property Hooking (Instantly catches javascript-set sources)
                                    try {
                                        const originalSrcDescriptor = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'src');
                                        if (originalSrcDescriptor) {
                                            Object.defineProperty(HTMLMediaElement.prototype, 'src', {
                                                get: function() { return originalSrcDescriptor.get.call(this); },
                                                set: function(val) {
                                                    originalSrcDescriptor.set.call(this, val);
                                                    notifyParent(val);
                                                },
                                                configurable: true
                                            });
                                        }
                                    } catch(e) { console.error("Media hook error", e); }

                                    try {
                                        const originalSourceSrcDescriptor = Object.getOwnPropertyDescriptor(HTMLSourceElement.prototype, 'src');
                                        if (originalSourceSrcDescriptor) {
                                            Object.defineProperty(HTMLSourceElement.prototype, 'src', {
                                                get: function() { return originalSourceSrcDescriptor.get.call(this); },
                                                set: function(val) {
                                                    originalSourceSrcDescriptor.set.call(this, val);
                                                    notifyParent(val);
                                                },
                                                configurable: true
                                            });
                                        }
                                    } catch(e) { console.error("Source hook error", e); }

                                    // Hook Fetch requests
                                    const originalFetch = window.fetch;
                                    window.fetch = function(...args) {
                                        const url = args[0];
                                        if (typeof url === 'string') {
                                            notifyParent(url);
                                        } else if (url && url.url) {
                                            notifyParent(url.url);
                                        }
                                        return originalFetch.apply(this, args);
                                    };

                                    // Hook XMLHttpRequest
                                    const originalOpen = XMLHttpRequest.prototype.open;
                                    XMLHttpRequest.prototype.open = function(method, url, ...args) {
                                        if (typeof url === 'string') {
                                            notifyParent(url);
                                        }
                                        return originalOpen.apply(this, [method, url, ...args]);
                                    };

                                    // Recursive Iframe Hijacking
                                    const hijackIframes = () => {
                                        document.querySelectorAll('iframe').forEach(iframe => {
                                            try {
                                                const src = iframe.src || iframe.getAttribute('src');
                                                if (src && src.startsWith('http') && !src.includes('/api/proxy') && !src.includes(window.location.origin)) {
                                                    const proxiedSrc = window.location.origin + '/api/proxy?url=' + encodeURIComponent(src);
                                                    iframe.src = proxiedSrc;
                                                    iframe.setAttribute('src', proxiedSrc);
                                                }
                                            } catch(e) {}
                                        });
                                    };

                                    // Main Initialization & Observation
                                    document.addEventListener('DOMContentLoaded', () => {
                                        // Static check
                                        hijackIframes();
                                        const ads = document.querySelectorAll('.ad, .ads, [id^=google_ads], ins');
                                        ads.forEach(ad => ad.style.display = 'none');

                                        const videos = document.querySelectorAll('video');
                                        videos.forEach(v => {
                                            notifyParent(v.src || v.querySelector('source')?.src);
                                        });

                                        // Dynamic check via MutationObserver
                                        const obs = new MutationObserver(mutations => {
                                            hijackIframes(); // Keep sub-players inside proxy!
                                            mutations.forEach(m => {
                                                m.addedNodes.forEach(n => {
                                                    if(n.tagName === 'VIDEO') {
                                                        notifyParent(n.src || n.querySelector('source')?.src);
                                                    } else if (n.tagName === 'IFRAME') {
                                                        // Instantly hijack new iframe
                                                        const src = n.src || n.getAttribute('src');
                                                        if (src && src.startsWith('http') && !src.includes('/api/proxy') && !src.includes(window.location.origin)) {
                                                            const proxied = window.location.origin + '/api/proxy?url=' + encodeURIComponent(src);
                                                            n.src = proxied;
                                                            n.setAttribute('src', proxied);
                                                        }
                                                    } else if (n.querySelectorAll) {
                                                        const vids = n.querySelectorAll('video');
                                                        vids.forEach(v => notifyParent(v.src || v.querySelector('source')?.src));
                                                        
                                                        const injAds = n.querySelectorAll('.ad, .ads, [id^=google_ads], ins');
                                                        injAds.forEach(ad => ad.style.display = 'none');
                                                    }
                                                });
                                            });
                                        });
                                        obs.observe(document.body, {childList: true, subtree: true});
                                        
                                        // Regular interval running to catch dynamic changes
                                        setInterval(() => {
                                            hijackIframes();
                                        }, 1500);
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

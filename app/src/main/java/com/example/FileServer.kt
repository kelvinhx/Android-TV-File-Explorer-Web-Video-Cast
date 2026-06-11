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

    private fun isPathAllowed(path: String): Boolean {
        return try {
            val canonical = File(path).canonicalPath
            canonical.startsWith("/storage/") || canonical.startsWith("/sdcard/") || canonical.startsWith(context.cacheDir.absolutePath)
        } catch(e: Exception) {
            false
        }
    }

    private fun sanitizePath(path: String?, defaultPath: String): String {
        val resolvedPath = path ?: defaultPath
        return try { File(resolvedPath).canonicalPath } catch(e: Exception) { resolvedPath }.replace("//", "/")
    }

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

                    get("/api/update_available") {
                        val repoOwner = call.request.queryParameters["owner"] ?: "kelvinhx"
                        val repoName = call.request.queryParameters["repo"] ?: "Android-TV-File-Explorer-Web-Video-Cast"
                        
                        val pInfo = this@FileServer.context.packageManager.getPackageInfo(this@FileServer.context.packageName, 0)
                        val localVersion = pInfo.versionName ?: "1.0"
                        val localVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            pInfo.longVersionCode
                        } else {
                            pInfo.versionCode.toLong()
                        }
                        
                        var updateAvailable = false
                        var remoteVersionCode: Long? = null
                        var latestVersion = localVersion
                        
                        try {
                            val url = java.net.URL("https://raw.githubusercontent.com/$repoOwner/$repoName/main/app/build.gradle.kts?t=${System.currentTimeMillis()}")
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.connect()
                            
                            if (conn.responseCode in 200..299) {
                                val gradleContent = conn.inputStream.bufferedReader().readText()
                                
                                val versionCodeMatch = Regex("versionCode\\s*=\\s*(\\d+)").find(gradleContent)
                                remoteVersionCode = versionCodeMatch?.groupValues?.get(1)?.toLongOrNull()
                                
                                val versionNameMatch = Regex("versionName\\s*=\\s*\"([^\"]+)\"").find(gradleContent)
                                latestVersion = versionNameMatch?.groupValues?.get(1) ?: localVersion
                                
                                if (remoteVersionCode != null && remoteVersionCode >= localVersionCode) {
                                    updateAvailable = true
                                }
                            }
                        } catch(e: Exception) {
                            e.printStackTrace()
                        }
                        
                        val response = JSONObject()
                        response.put("updateAvailable", updateAvailable)
                        response.put("latestVersion", latestVersion)
                        response.put("localVersion", localVersion)
                        response.put("remoteVersionCode", remoteVersionCode)
                        response.put("localVersionCode", localVersionCode)
                        
                        call.respondText(response.toString(), ContentType.Application.Json)
                    }

                    // Directory Discovery
                    get("/api/search") {
                        val query = call.request.queryParameters["q"] ?: ""
                        if (query.isEmpty()) {
                            call.respondText("{\"status\":\"error\", \"message\":\"Query was empty\"}", ContentType.Application.Json)
                            return@get
                        }
                        
                        val searchResults = FileUtils.searchFiles(this@FileServer.context, query)
                        
                        val jsonArray = JSONArray()
                        searchResults.forEach { f ->
                            val obj = JSONObject()
                            obj.put("name", f.name)
                            obj.put("absolutePath", f.absolutePath)
                            obj.put("path", f.absolutePath)
                            obj.put("isDirectory", f.isDirectory)
                            if (f.isDirectory) {
                                obj.put("sizeFormatted", "Pasta")
                            } else {
                                obj.put("sizeFormatted", FileUtils.formatSize(f.length))
                            }
                            jsonArray.put(obj)
                        }
                        
                        val response = JSONObject()
                        response.put("status", "success")
                        response.put("files", jsonArray)
                        call.respondText(response.toString(), ContentType.Application.Json)
                    }

                    get("/api/files") {
                        val rootPath = "/storage/emulated/0"
                        val pathParam = sanitizePath(call.request.queryParameters["path"], rootPath)
                        
                        if (!isPathAllowed(pathParam)) {
                            call.respondText(JSONObject().put("status", "error").put("message", "Acesso negado para o caminho fornecido.").toString(), ContentType.Application.Json, HttpStatusCode.Forbidden)
                            return@get
                        }
                        
                        // Check Android/data permission if navigating there
                        val isDataFolder = pathParam.startsWith("/storage/emulated/0/Android/data")
                        val hasDataPerm = FileUtils.hasAndroidDataPermission(this@FileServer.context)
                        
                        if (isDataFolder && !hasDataPerm) {
                            call.respondText(
                                JSONObject()
                                    .put("status", "error")
                                    .put("message", "Acesso negado à pasta Android/data. Conceda permissão na TV.")
                                    .put("permission_required", "ANDROID_DATA")
                                    .toString(),
                                ContentType.Application.Json
                            )
                            return@get
                        }

                        val unifiedList = FileUtils.listUnifiedFiles(this@FileServer.context, pathParam)
                        val isRoot = pathParam == rootPath || pathParam == "/"
                        
                        val fileArray = JSONArray()
                        for (f in unifiedList) {
                            if (f.name.startsWith(".")) continue
                            
                            val obj = JSONObject()
                            obj.put("name", f.name)
                            obj.put("absolutePath", f.absolutePath)
                            obj.put("isDirectory", f.isDirectory)
                            
                            if (f.isDirectory) {
                                if (f.absolutePath.startsWith("/storage/emulated/0/Android/data")) {
                                    val subItems = FileUtils.listUnifiedFiles(this@FileServer.context, f.absolutePath)
                                    obj.put("sizeFormatted", "${subItems.size} itens")
                                } else {
                                    val subfiles = File(f.absolutePath).listFiles()
                                    val count = subfiles?.size ?: 0
                                    obj.put("sizeFormatted", "$count itens")
                                }
                            } else {
                                obj.put("sizeFormatted", FileUtils.formatSize(f.length))
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
                        response.put("currentPath", pathParam)
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
                        val isDownload = call.request.queryParameters["download"] == "1"
                        if (path.isNullOrEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "Path parameter is required.")
                            return@get
                        }

                        val targetPath = sanitizePath(path, path)
                        if (!isPathAllowed(targetPath)) {
                            call.respondText(JSONObject().put("status", "error").put("message", "Acesso negado para o diretório alvo.").toString(), ContentType.Application.Json, HttpStatusCode.Forbidden)
                            return@get
                        }
                        
                        Logger.log("Streaming file via download api: $targetPath")

                        if (isDownload) {
                            val fileName = targetPath.substringAfterLast("/")
                            call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"$fileName\"")
                        }

                        if (targetPath.startsWith("/storage/emulated/0/Android/data") && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            val uri = FileUtils.getUriForPath(this@FileServer.context, targetPath)
                            if (uri != null) {
                                val mime = FileUtils.getMimeType(targetPath)
                                call.respondOutputStream(ContentType.parse(mime), HttpStatusCode.OK) {
                                    this@FileServer.context.contentResolver.openInputStream(uri)?.use { input ->
                                        val buffer = ByteArray(64 * 1024)
                                        var bytesRead: Int
                                        while (input.read(buffer).also { bytesRead = it } != -1) {
                                            write(buffer, 0, bytesRead)
                                        }
                                    }
                                }
                                return@get
                            } else {
                                call.respond(HttpStatusCode.NotFound, "File not found or permission denied in Android/data.")
                                return@get
                            }
                        }

                        val file = File(targetPath)
                        if (!file.exists() || file.isDirectory) {
                            call.respond(HttpStatusCode.NotFound, "File not found.")
                            return@get
                        }

                        call.respondOutputStream(ContentType.parse(FileUtils.getMimeType(file.name)), HttpStatusCode.OK) {
                            file.inputStream().use { input ->
                                val buffer = ByteArray(64 * 1024)
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

                        val targetPath = sanitizePath(path, path)
                        if (!isPathAllowed(targetPath)) {
                            call.respondText(JSONObject().put("status", "error").put("message", "Acesso negado").toString(), ContentType.Application.Json, HttpStatusCode.Forbidden)
                            return@post
                        }
                        
                        val success = FileUtils.openFile(this@FileServer.context, targetPath)
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
                        
                        val reqReferer = call.request.header(HttpHeaders.Referrer) ?: ""
                        var proxyReferer = urlStr
                        if (reqReferer.contains("/api/proxy?url=")) {
                            val originalUrlStr = reqReferer.substringAfter("/api/proxy?url=").substringBefore("&")
                            try {
                                proxyReferer = java.net.URLDecoder.decode(originalUrlStr, "UTF-8")
                            } catch (e: Exception) {}
                        }
                        
                        try {
                            var redirectCount = 0
                            var currentUrlStr = urlStr
                            var conn: java.net.HttpURLConnection? = null
                            var currentUrl: java.net.URL = java.net.URL(currentUrlStr)
                            
                            while (redirectCount < 10) {
                                currentUrl = java.net.URL(currentUrlStr)
                                conn = currentUrl.openConnection() as java.net.HttpURLConnection
                                conn.requestMethod = "GET"
                                conn.instanceFollowRedirects = false
                                // Pretend to be a modern Android Chrome browser
                                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                                
                                conn.setRequestProperty("Referer", proxyReferer)
                                
                                // Headers to avoid some blocks, though sophisticated sites will still block
                                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                conn.setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                                
                                val host = currentUrl.host
                                val domainCookies = ServerState.getCookies(host)
                                if (domainCookies.isNotEmpty()) {
                                    val cookieStr = domainCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                                    conn.setRequestProperty("Cookie", cookieStr)
                                }
                                
                                conn.connect()
                                
                                // Extract cookies
                                conn.headerFields["Set-Cookie"]?.forEach { cookieHeader ->
                                    val parts = cookieHeader.split(";").first().split("=", limit = 2)
                                    if (parts.size == 2) {
                                        ServerState.setCookie(host, parts[0], parts[1])
                                    }
                                }
                                
                                val status = conn.responseCode
                                if (status == java.net.HttpURLConnection.HTTP_MOVED_TEMP
                                    || status == java.net.HttpURLConnection.HTTP_MOVED_PERM
                                    || status == java.net.HttpURLConnection.HTTP_SEE_OTHER
                                    || status == 307 || status == 308) {
                                    val newUrl = conn.getHeaderField("Location")
                                    val nextUrl = if (newUrl.startsWith("http")) newUrl else java.net.URL(currentUrl, newUrl).toString()
                                    currentUrlStr = nextUrl
                                    redirectCount++
                                } else {
                                    break
                                }
                            }
                            
                            if (conn == null) {
                                call.respondText("Failed to proxy", ContentType.Text.Plain)
                                return@get
                            }
                            
                            val contentType = conn.contentType ?: "text/html"
                            
                            if (contentType.startsWith("text/html")) {
                                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                                var html = stream?.bufferedReader()?.readText() ?: ""
                                
                                val baseUrl = "${currentUrl.protocol}://${currentUrl.host}"
                                val path = currentUrl.path
                                val basePath = if (path.substringAfterLast("/").contains(".")) 
                                    baseUrl + path.substringBeforeLast("/") + "/" 
                                else 
                                    baseUrl + path + (if(path.endsWith("/")) "" else "/")
                                
                                val inject = """
                                    <style>
                                    /* Premium Liquid Glass Inspired Style Injection to force hide ads and banners */
                                    .ad, .ads, .ad-box, .advertisement, #ad-container, [id^=google_ads_],
                                    iframe[src*="doubleclick.net"], iframe[src*="adservice"], iframe[src*="popads"], iframe[src*="propeller"],
                                    .popunder, .widget-ad, .banner-ad, .taboola, .outbrain, .adsbygoogle,
                                    div[class*="Sponsored"], div[class*="sponsored"],
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
                                    window.open = function() { console.log("Popup blocked"); return null; };
                                    const isVideoUrl = (url) => {
                                        if (!url || typeof url !== 'string') return false;
                                        const lower = url.toLowerCase();
                                        if (/\.(mp4|m3u8|mkv|mov|avi|webm|flv|3gp|ts|mpd)(\?|$)/.test(lower)) return true;
                                        if (lower.includes('streamtape') || lower.includes('dood') || lower.includes('mixdrop') || 
                                            lower.includes('filemoon') || lower.includes('mcloud') || lower.includes('vizcloud') ||
                                            lower.includes('vidsrc') || lower.includes('rabbitstream') || lower.includes('vidplay') ||
                                            lower.includes('supervideo') || lower.includes('pobreflix') || lower.includes('megacloud') ||
                                            lower.includes('gdriveplayer') || lower.includes('uqload') || lower.includes('voe.sx') ||
                                            lower.includes('/api/source/') || lower.includes('manifest.mpd') || lower.includes('master.m3u8') ||
                                            lower.includes('/get_video') || lower.includes('/playlist.m3u8') || lower.includes('blob:http')
                                        ) {
                                            return true;
                                        }
                                        return false;
                                    };

                                    const notifyParent = (src) => {
                                        if (src && isVideoUrl(src)) {
                                            console.log("Sniffed Video URL:", src);
                                            try {
                                                window.top.postMessage({type: 'video_found', url: src}, '*');
                                            } catch(e) {
                                                window.parent.postMessage({type: 'video_found', url: src}, '*');
                                            }
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
                                        // Removed: hijacking iframes breaks Cloudflare protected embeds like PobreFlix.
                                        // document.querySelectorAll('iframe').forEach(...)
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
                                                        // We no longer hijack iframes dynamically to prevent breaking external secure players
                                                        // const src = n.src || n.getAttribute('src');
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
                                        if (a) {
                                            if (a.getAttribute('target')) {
                                                a.removeAttribute('target');
                                            }
                                            if (a.href && a.href.startsWith('http')) {
                                                e.preventDefault();
                                                try {
                                                    window.top.postMessage({type: 'navigate', url: a.href}, '*');
                                                } catch(err) {
                                                    window.parent.postMessage({type: 'navigate', url: a.href}, '*');
                                                }
                                            }
                                        }
                                    }, true);

                                    window.addEventListener('submit', e => {
                                        const form = e.target;
                                        if (form && form.action) {
                                            e.preventDefault();
                                            let action = form.action.startsWith('http') ? form.action : new URL(form.action, document.baseURI || window.location.href).href;
                                            const formData = new FormData(form);
                                            const params = new URLSearchParams(formData);
                                            
                                            if (form.method && form.method.toLowerCase() === 'get') {
                                                let urlObj = new URL(action);
                                                for (const [key, value] of params.entries()) {
                                                    urlObj.searchParams.append(key, value);
                                                }
                                                action = urlObj.href;
                                            }
                                            
                                            try {
                                                window.top.postMessage({type: 'navigate', url: action}, '*');
                                            } catch(err) {
                                                window.parent.postMessage({type: 'navigate', url: action}, '*');
                                            }
                                        }
                                    });
                                    </script>
                                """.trimIndent()

                                val headMatch = Regex("(?i)<head[^>]*>").find(html)
                                if (headMatch != null) {
                                    html = html.replaceFirst(Regex("(?i)<head[^>]*>"), "${headMatch.value}\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n<base href=\"$basePath\">\n$inject")
                                } else {
                                    val htmlMatch = Regex("(?i)<html[^>]*>").find(html)
                                    if (htmlMatch != null) {
                                        html = html.replaceFirst(Regex("(?i)<html[^>]*>"), "${htmlMatch.value}\n<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n<base href=\"$basePath\">\n$inject</head>")
                                    } else {
                                        html = "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n<base href=\"$basePath\">\n$inject</head>\n$html"
                                    }
                                }
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

                    // Native operations actions (MKDIR, DELETE, RENAME, COPY, MOVE)
                    post("/api/action") {
                        val requestBody = call.receiveText()
                        val json = JSONObject(requestBody)
                        val action = json.optString("action")
                        val rawPath = json.optString("path")
                        val argument = json.optString("argument")

                        if (rawPath.isEmpty()) {
                            call.respondText(JSONObject().put("status", "error").put("message", "Path is missing").toString(), ContentType.Application.Json)
                            return@post
                        }

                        val path = sanitizePath(rawPath, rawPath)
                        if (!isPathAllowed(path)) {
                            call.respondText(JSONObject().put("status", "error").put("message", "Acesso ao diretório negado").toString(), ContentType.Application.Json)
                            return@post
                        }

                        val file = File(path)
                        var success = false
                        var message = ""

                        try {
                            when (action) {
                                "MKDIR" -> {
                                    val parent = file.parent ?: "/storage/emulated/0"
                                    val name = file.name
                                    success = FileUtils.createUnifiedDirectory(this@FileServer.context, parent, name)
                                    if (success) Logger.log("Created folder: $path")
                                    else message = "Permission denied or folder exists"
                                }
                                "DELETE" -> {
                                    success = FileUtils.deleteUnifiedFile(this@FileServer.context, path)
                                    if (success) Logger.log("Deleted item: $path")
                                    else message = "Error deleting item"
                                }
                                "RENAME" -> {
                                    if (argument.isNotEmpty()) {
                                        success = FileUtils.renameUnifiedFile(this@FileServer.context, path, argument)
                                        if (success) Logger.log("Renamed item to: $argument")
                                        else message = "Rename failed or item exists"
                                    } else {
                                        message = "Target name must be provided"
                                    }
                                }
                                "COPY" -> {
                                    if (argument.isNotEmpty()) {
                                        val srcFile = File(argument)
                                        val destParent = file.absolutePath
                                        val name = srcFile.name
                                        success = FileUtils.copyUnifiedFile(this@FileServer.context, argument, destParent, name)
                                        if (success) Logger.log("Copied: $argument to $destParent")
                                        else message = "Copy failed"
                                    } else {
                                        message = "Source path must be provided"
                                    }
                                }
                                "MOVE" -> {
                                    if (argument.isNotEmpty()) {
                                        val srcFile = File(argument)
                                        val destParent = file.absolutePath
                                        val name = srcFile.name
                                        success = FileUtils.moveUnifiedFile(this@FileServer.context, argument, destParent, name)
                                        if (success) Logger.log("Moved: $argument to $destParent")
                                        else message = "Move failed"
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
                        val rawPath = call.request.queryParameters["path"] ?: "/storage/emulated/0"
                        val pathParam = sanitizePath(rawPath, rawPath)
                        
                        if (!isPathAllowed(pathParam)) {
                            call.respondText(JSONObject().put("status", "error").put("message", "Upload restrito nesse caminho.").toString(), ContentType.Application.Json, HttpStatusCode.Forbidden)
                            return@post
                        }
                        
                        val multipart = call.receiveMultipart()
                        var count = 0

                        try {
                            multipart.forEachPart { part ->
                                if (part is PartData.FileItem) {
                                    val originalName = part.originalFileName ?: "uploaded_file_${System.currentTimeMillis()}"
                                    val outputStream = FileUtils.getOutputStreamForPath(this@FileServer.context, pathParam, originalName)
                                    
                                    if (outputStream != null) {
                                        Logger.log("Receiving and writing upload: $originalName to $pathParam")
                                        part.streamProvider().use { input ->
                                            outputStream.use { output ->
                                                val buffer = ByteArray(64 * 1024) // 64KB buffer
                                                var bytesRead: Int
                                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                                    output.write(buffer, 0, bytesRead)
                                                }
                                            }
                                        }
                                        Logger.log("Stored file uploaded successfully: $originalName")
                                        ServerState.postUploadNotification("Received file '$originalName' from your iPhone successfully!")
                                        count++
                                    } else {
                                        Logger.log("Failed to create output stream for file: $originalName")
                                    }
                                }
                                part.dispose()
                            }
                            call.respondText(JSONObject().put("status", "success").put("count", count).toString(), ContentType.Application.Json)
                        } catch (e: Exception) {
                            Logger.log("Upload failed: ${e.message}")
                            call.respondText(JSONObject().put("status", "error").put("message", e.message).toString(), ContentType.Application.Json)
                        }
                    }

                    get("/{...}") {
                        val path = call.request.path()
                        val referer = call.request.header(HttpHeaders.Referrer) ?: ""
                        if (referer.contains("/api/proxy?url=")) {
                            val originalUrlStr = referer.substringAfter("/api/proxy?url=").substringBefore("&")
                            try {
                                val decodedUrl = java.net.URLDecoder.decode(originalUrlStr, "UTF-8")
                                val baseUri = java.net.URI(decodedUrl)
                                val targetUri = baseUri.resolve(path).toString()
                                call.respondRedirect("/api/proxy?url=" + java.net.URLEncoder.encode(targetUri, "UTF-8"))
                            } catch (e: Exception) {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    post("/{...}") {
                        val path = call.request.path()
                        val referer = call.request.header(HttpHeaders.Referrer) ?: ""
                        if (referer.contains("/api/proxy?url=")) {
                            val originalUrlStr = referer.substringAfter("/api/proxy?url=").substringBefore("&")
                            try {
                                val decodedUrl = java.net.URLDecoder.decode(originalUrlStr, "UTF-8")
                                val baseUri = java.net.URI(decodedUrl)
                                val targetUri = baseUri.resolve(path).toString()
                                call.respondRedirect("/api/proxy?url=" + java.net.URLEncoder.encode(targetUri, "UTF-8")) // fallback to GET
                            } catch (e: Exception) {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        } else {
                            call.respond(HttpStatusCode.NotFound)
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
            ErrorTracker.logError("FileServer", "Server failed to start", e)
            ErrorTracker.attemptAutoCorrection("FileServer", "server_start_failed")
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

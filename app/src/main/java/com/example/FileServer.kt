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

                        val response = JSONObject()
                        response.put("currentPath", dir.absolutePath)
                        response.put("isRoot", isRoot)
                        response.put("files", sortedJsonArray)

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

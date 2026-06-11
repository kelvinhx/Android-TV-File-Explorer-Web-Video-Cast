package com.example

import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ErrorTracker {
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun logError(context: String, errorMessage: String, exception: Throwable? = null) {
        val timestamp = timeFormat.format(Date())
        val logLine = "[$timestamp] [$context] $errorMessage ${exception?.stackTraceToString() ?: ""}\n"
        
        // Log to internal logger
        Logger.log("ERROR: [$context] $errorMessage")

        // Try to write to file
        try {
            if (Environment.isExternalStorageManager()) {
                val nexusFolder = File(Environment.getExternalStorageDirectory(), "Nexus Explorer/Logs")
                if (!nexusFolder.exists()) {
                    nexusFolder.mkdirs()
                }
                
                val logFile = File(nexusFolder, "error_log.txt")
                if (!logFile.exists()) {
                    logFile.createNewFile()
                }
                
                FileWriter(logFile, true).use { writer ->
                    writer.append(logLine)
                }
            }
        } catch (e: Exception) {
            Logger.log("Failed to write to external error log: ${e.message}")
        }
    }
    
    // Auto-correction tools
    fun attemptAutoCorrection(context: String, errorType: String) {
        logError("AutoCorrection", "Attempting auto correction for $errorType in $context")
        when (errorType) {
            "web_fetch_failed" -> {
                Logger.log("Web interface fetch failed, advising to check network connection.")
            }
            "open_apk_failed" -> {
                Logger.log("Failed to open APK via default view intent. Consider root/system installer or check MANAGE_EXTERNAL_STORAGE.")
            }
            "saf_permission_failed" -> {
                Logger.log("SAF Permission failed on TV. Some TVs lack DocumentsUI. Android/Data might be inaccessible without root.")
            }
            "server_start_failed" -> {
                Logger.log("Server failed to start, attempting clean restart...")
            }
            else -> {
                Logger.log("No specific auto-correction available for $errorType.")
            }
        }
    }
}

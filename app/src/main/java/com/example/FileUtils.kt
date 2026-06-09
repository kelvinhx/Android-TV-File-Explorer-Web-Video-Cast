package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

object FileUtils {

    fun getFolderSize(dir: File): Long {
        var size: Long = 0
        if (dir.exists() && dir.isDirectory) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    size += if (file.isDirectory) {
                        getFolderSize(file)
                    } else {
                        file.length()
                    }
                }
            }
        } else if (dir.exists()) {
            size = dir.length()
        }
        return size
    }

    fun formatSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = if (digitGroups < units.size) digitGroups else units.size - 1
        return String.format(Locale.US, "%.2f %s", sizeInBytes / Math.pow(1024.0, index.toDouble()), units[index])
    }

    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase(Locale.getDefault())
        if (extension.isEmpty()) return "*/*"
        
        when (extension) {
            "apk" -> return "application/vnd.android.package-archive"
            "mp4", "mkv", "webm", "avi", "mov" -> return "video/*"
            "mp3", "wav", "ogg", "flac", "aac" -> return "audio/*"
            "jpg", "jpeg", "png", "webp", "gif" -> return "image/*"
            "txt", "log", "json", "html", "xml", "css", "js" -> return "text/plain"
            "pdf" -> return "application/pdf"
            "zip", "rar", "tar", "gz" -> return "application/compress"
        }

        val map = MimeTypeMap.getSingleton()
        return map.getMimeTypeFromExtension(extension) ?: "*/*"
    }

    fun openFile(context: Context, file: File): Boolean {
        return try {
            Logger.log("Requesting system to view or open: ${file.name}")
            val mime = getMimeType(file)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Logger.log("Successfully dispatched View Intent for: ${file.name}")
            true
        } catch (e: Exception) {
            Logger.log("Failed to open file: ${e.message}")
            false
        }
    }
}

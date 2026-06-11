package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale

data class UnifiedFile(
    val name: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val length: Long,
    val extension: String,
    val uriString: String? = null
)

object FileUtils {

    fun hasAndroidDataPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        val treeUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata")
        return context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission && it.isWritePermission
        }
    }

    fun getDocumentFileForPath(context: Context, path: String): DocumentFile? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val targetPath = path.replace("//", "/")
        if (!targetPath.startsWith("/storage/emulated/0/Android/data")) return null
        
        val relativePath = targetPath.substringAfter("/storage/emulated/0/Android/data").trim('/')
        val treeUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata")
        var currentDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        
        if (relativePath.isEmpty()) {
            return currentDoc
        }
        
        val parts = relativePath.split('/')
        for (part in parts) {
            if (part.isEmpty()) continue
            val nextDoc = currentDoc.findFile(part)
            if (nextDoc != null) {
                currentDoc = nextDoc
            } else {
                return null
            }
        }
        return currentDoc
    }

    fun listUnifiedFiles(context: Context, path: String): List<UnifiedFile> {
        val targetPath = path.replace("//", "/")
        if (targetPath.startsWith("/storage/emulated/0/Android/data") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (hasAndroidDataPermission(context)) {
                val doc = getDocumentFileForPath(context, targetPath)
                if (doc != null && doc.isDirectory) {
                    val list = doc.listFiles()
                    return list.map { subDoc ->
                        val subName = subDoc.name ?: ""
                        val subPath = if (targetPath.endsWith("/")) targetPath + subName else "$targetPath/$subName"
                        UnifiedFile(
                            name = subName,
                            absolutePath = subPath,
                            isDirectory = subDoc.isDirectory,
                            length = if (subDoc.isDirectory) 0L else subDoc.length(),
                            extension = subName.substringAfterLast('.', "").lowercase(),
                            uriString = subDoc.uri.toString()
                        )
                    }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                }
            }
        }

        // Standard File Fallback
        val dir = File(targetPath)
        if (dir.exists() && dir.isDirectory) {
            val list = dir.listFiles() ?: emptyArray()
            return list.map { f ->
                UnifiedFile(
                    name = f.name,
                    absolutePath = f.absolutePath,
                    isDirectory = f.isDirectory,
                    length = if (f.isDirectory) 0L else f.length(),
                    extension = f.extension.lowercase(),
                    uriString = null
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        }
        return emptyList()
    }

    fun deleteUnifiedFile(context: Context, path: String): Boolean {
        val targetPath = path.replace("//", "/")
        if (targetPath.startsWith("/storage/emulated/0/Android/data") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val doc = getDocumentFileForPath(context, targetPath)
            if (doc != null) {
                return doc.delete()
            }
            return false
        }
        val file = File(targetPath)
        return if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    fun renameUnifiedFile(context: Context, path: String, newName: String): Boolean {
        val targetPath = path.replace("//", "/")
        if (targetPath.startsWith("/storage/emulated/0/Android/data") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val doc = getDocumentFileForPath(context, targetPath)
            if (doc != null) {
                return doc.renameTo(newName)
            }
            return false
        }
        val file = File(targetPath)
        val target = File(file.parentFile, newName)
        return file.renameTo(target)
    }

    fun createUnifiedDirectory(context: Context, parentPath: String, folderName: String): Boolean {
        val targetPath = parentPath.replace("//", "/")
        if (targetPath.startsWith("/storage/emulated/0/Android/data") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val doc = getDocumentFileForPath(context, targetPath)
            if (doc != null && doc.isDirectory) {
                val exist = doc.findFile(folderName)
                if (exist != null && exist.isDirectory) return true
                return doc.createDirectory(folderName) != null
            }
            return false
        }
        val dir = File(targetPath, folderName)
        return dir.mkdirs()
    }

    fun getOutputStreamForPath(context: Context, parentPath: String, fileName: String): OutputStream? {
        val targetPath = parentPath.replace("//", "/")
        if (targetPath.startsWith("/storage/emulated/0/Android/data") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val docDir = getDocumentFileForPath(context, targetPath)
            if (docDir != null && docDir.isDirectory) {
                val existing = docDir.findFile(fileName)
                val fileToUse = existing ?: docDir.createFile("*/*", fileName)
                if (fileToUse != null) {
                    return context.contentResolver.openOutputStream(fileToUse.uri)
                }
            }
            return null
        }
        val uploadDir = File(targetPath)
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }
        val dst = File(uploadDir, fileName)
        return dst.outputStream()
    }

    fun getUriForPath(context: Context, path: String): Uri? {
        val targetPath = path.replace("//", "/")
        if (targetPath.startsWith("/storage/emulated/0/Android/data") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getDocumentFileForPath(context, targetPath)?.uri
        }
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(targetPath)
            )
        } catch (e: Exception) {
            Uri.fromFile(File(targetPath))
        }
    }

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

    fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
        if (extension.isEmpty()) return "*/*"
        
        when (extension) {
            "apk" -> return "application/vnd.android.package-archive"
            "mp4", "mkv", "webm", "avi", "mov", "ts", "m3u8" -> return "video/*"
            "mp3", "wav", "ogg", "flac", "aac", "m4a" -> return "audio/*"
            "jpg", "jpeg", "png", "webp", "gif" -> return "image/*"
            "txt", "log", "json", "html", "xml", "css", "js", "kt" -> return "text/plain"
            "pdf" -> return "application/pdf"
            "zip", "rar", "tar", "gz", "7z" -> return "application/compress"
        }

        val map = MimeTypeMap.getSingleton()
        return map.getMimeTypeFromExtension(extension) ?: "*/*"
    }

    fun copyUnifiedFile(context: Context, srcPath: String, destParentPath: String, destName: String): Boolean {
        return try {
            val srcTarget = srcPath.replace("//", "/")
            val destTarget = destParentPath.replace("//", "/")
            val input = if (srcTarget.startsWith("/storage/emulated/0/Android/data") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uri = getUriForPath(context, srcTarget) ?: return false
                context.contentResolver.openInputStream(uri) ?: return false
            } else {
                val f = File(srcTarget)
                if (!f.exists()) return false
                if (f.isDirectory) {
                    val destDir = File(destTarget, destName)
                    return f.copyRecursively(destDir, overwrite = true)
                }
                f.inputStream()
            }
            
            val output = getOutputStreamForPath(context, destTarget, destName) ?: return false
            
            input.use { inp ->
                output.use { out ->
                    inp.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            Logger.log("Copy failed: ${e.message}")
            false
        }
    }

    fun moveUnifiedFile(context: Context, srcPath: String, destParentPath: String, destName: String): Boolean {
        val srcTarget = srcPath.replace("//", "/")
        val destTarget = destParentPath.replace("//", "/")
        // Try rapid rename first
        val success = renameUnifiedFile(context, srcTarget, destName)
        if (success) {
            val srcFile = File(srcTarget)
            val correctDestFile = File(destTarget, destName)
            if (srcFile.parent == correctDestFile.parent) {
                return true
            }
        }
        
        // Force copy and delete channel
        val copied = copyUnifiedFile(context, srcTarget, destTarget, destName)
        if (copied) {
            deleteUnifiedFile(context, srcTarget)
            return true
        }
        return false
    }

    fun openFile(context: Context, path: String): Boolean {
        return try {
            Logger.log("Requesting system to view or open: $path")
            val mime = getMimeType(path)
            val uri = getUriForPath(context, path) ?: return false

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Logger.log("Successfully dispatched View Intent for: $path")
            true
        } catch (e: Exception) {
            Logger.log("Failed to open file: ${e.message}")
            false
        }
    }
}

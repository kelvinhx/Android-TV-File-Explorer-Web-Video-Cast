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

    private fun createSampleFiles(root: File) {
        try {
            root.mkdirs()
            val movies = File(root, "Filmes").apply { mkdirs() }
            val music = File(root, "Músicas").apply { mkdirs() }
            val photos = File(root, "Fotos").apply { mkdirs() }
            val docs = File(root, "Documentos").apply { mkdirs() }
            
            val welcomeFile = File(docs, "Bem_vindo_ao_Nexus.txt")
            if (!welcomeFile.exists()) {
                welcomeFile.writeText(
                    "===============================================\n" +
                    "   BEM-VINDO AO NEXUS EXPLORER PRO (WEB CAST)  \n" +
                    "===============================================\n\n" +
                    "Este é um sistema unificado de gerenciamento de arquivos\n" +
                    "para sua TV de alta performance.\n\n" +
                    "Recursos da versão web:\n" +
                    "1. Upload inteligente: clique no botão '+' no topo para enviar arquivos\n" +
                    "2. Transmissão em tempo real (Cast): transmita links de vídeo para a TV\n" +
                    "3. Controle remoto integrado para reproduzir mídias\n" +
                    "4. Baixar e visualizar arquivos diretamente no navegador do seu celular\n\n" +
                    "Organização de pastas:\n" +
                    "- /Filmes : Coloque seus vídeos (MP4, MKV)\n" +
                    "- /Músicas : Suas faixas de áudio (MP3, WAV)\n" +
                    "- /Fotos : Imagens (PNG, JPG, WEBP)\n" +
                    "- /Documentos : Documentos diversos\n\n" +
                    "Desenvolvido com carinho para oferecer a melhor experiência."
                )
            }
            
            val audioFile = File(music, "Som_Ambiente.mp3")
            if (!audioFile.exists()) {
                val tinyMp3Bytes = byteArrayOf(
                    0xFF.toByte(), 0xE3.toByte(), 0x18.toByte(), 0xC4.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x03.toByte(),
                    0x48.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x4C.toByte(), 0x41.toByte(), 0x4D.toByte(),
                    0x45.toByte(), 0x33.toByte(), 0x2E.toByte(), 0x39.toByte(), 0x38.toByte(), 0x2E.toByte(), 0x32.toByte(), 0x00.toByte()
                )
                audioFile.writeBytes(tinyMp3Bytes)
            }

            val videoFile = File(movies, "Vídeo_Demonstrativo.mp4")
            if (!videoFile.exists()) {
                val tinyMp4Bytes = byteArrayOf(
                    0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x6D, 0x70, 0x34, 0x32, 0x00, 0x00, 0x00, 0x00,
                    0x6D, 0x70, 0x34, 0x32, 0x69, 0x73, 0x6F, 0x6D, 0x00, 0x00, 0x00, 0x08, 0x66, 0x72, 0x65, 0x65
                )
                videoFile.writeBytes(tinyMp4Bytes)
            }

            val imgFile = File(photos, "Logo_Nexus_Glass.png")
            if (!imgFile.exists()) {
                val tinyPngBytes = byteArrayOf(
                    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte(),
                    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4.toByte(),
                    0x89.toByte(), 0x00, 0x00, 0x00, 0x0D, 0x49.toByte(), 0x44.toByte(), 0x41.toByte(), 0x54.toByte(), 0x78, 0xDA.toByte(), 0x63, 0x60, 0x60,
                    0x60, 0x60, 0x00, 0x00, 0x00, 0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4.toByte(), 0x00, 0x00, 0x00, 0x00,
                    0x49.toByte(), 0x45.toByte(), 0x4E.toByte(), 0x44.toByte(), 0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
                )
                imgFile.writeBytes(tinyPngBytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getResolvedFile(context: Context, path: String): File {
        val targetPath = path.replace("//", "/")
        if (targetPath.startsWith("/storage/emulated/0")) {
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (hasPermission) {
                val extFile = File(targetPath)
                try {
                    if (extFile.exists() && extFile.canRead()) {
                        // On Android, check if listing is actually permitted
                        val listCount = extFile.listFiles()?.size ?: -1
                        if (listCount >= 0 || targetPath != "/storage/emulated/0") {
                            return extFile
                        }
                    }
                } catch(e: Exception) {}
            }
            
            val relative = targetPath.substringAfter("/storage/emulated/0").trimStart('/')
            val virtualRoot = File(context.filesDir, "virtual_storage")
            if (!virtualRoot.exists()) {
                virtualRoot.mkdirs()
                createSampleFiles(virtualRoot)
            }
            return if (relative.isEmpty()) virtualRoot else File(virtualRoot, relative)
        }
        return File(targetPath)
    }

    fun toVirtualPath(context: Context, physicalPath: String): String {
        val virtualRootPath = File(context.filesDir, "virtual_storage").absolutePath
        val normalized = physicalPath.replace("//", "/")
        if (normalized.startsWith(virtualRootPath)) {
            val relative = normalized.substringAfter(virtualRootPath).trimStart('/')
            return if (relative.isEmpty()) "/storage/emulated/0" else "/storage/emulated/0/$relative"
        }
        return normalized
    }

    fun getExternalStorageRoots(context: Context): List<String> {
        val roots = mutableListOf<String>()
        val externalDirs = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
        for (dir in externalDirs) {
            if (dir != null) {
                // Get the root of the storage volume. E.g., /storage/emulated/0
                val path = dir.absolutePath
                val rootPath = path.substringBefore("/Android/data")
                if (!roots.contains(rootPath)) {
                    roots.add(rootPath)
                }
            }
        }
        // Fallback for /storage
        try {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.isDirectory) {
                storageDir.listFiles()?.forEach { file ->
                    if (file.isDirectory && file.canRead() && !roots.contains(file.absolutePath)) {
                        roots.add(file.absolutePath)
                    }
                }
            }
        } catch (e: Exception) {}
        return roots.distinct()
    }

    suspend fun searchFiles(context: Context, query: String): List<UnifiedFile> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val results = mutableListOf<UnifiedFile>()
        val q = query.lowercase().trim()
        if (q.isEmpty()) return@withContext results

        val audioExtensions = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "mid", "wma", "opus")
        val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "m3u8", "webm", "flv", "3gp", "ts")
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
        val documentExtensions = setOf("pdf", "epub", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "xml", "json", "html", "css", "js", "kt")

        val targetAudio = q == "category_audio" || q == "musicas" || q == "músicas" || q == "audio" || q == "áudio"
        val targetVideo = q == "category_videos" || q == "videos" || q == "vídeos" || q == "video"
        val targetImage = q == "category_images" || q == "fotos" || q == "foto" || q == "imagens" || q == "imagem"
        val targetDocs = q == "category_documents" || q == "documentos" || q == "documento" || q == "texto" || q == "text" || q == "docs"

        val isExtensionSearch = q.startsWith(".") || listOf("mp3", "apk", "pdf", "mp4", "jpg", "png", "zip", "rar", "mkv").contains(q)
        val extQuery = if (q.startsWith(".")) q.substring(1) else q

        val roots = getExternalStorageRoots(context)
        
        for (root in roots) {
            val rootFile = getResolvedFile(context, root)
            if (!rootFile.exists() || !rootFile.canRead()) continue
            
            try {
                rootFile.walkTopDown()
                    .onEnter { it.name != "Android" && !it.isHidden && it.canRead() } // skip Android/data to save time and hidden folders
                    .forEach { f ->
                        if (!f.isDirectory) {
                            val fName = f.name.lowercase()
                            val fExt = f.extension.lowercase()
                            
                            var matches = fName.contains(q)
                            if (!matches) {
                                if (targetAudio && audioExtensions.contains(fExt)) matches = true
                                else if (targetVideo && videoExtensions.contains(fExt)) matches = true
                                else if (targetImage && imageExtensions.contains(fExt)) matches = true
                                else if (targetDocs && documentExtensions.contains(fExt)) matches = true
                                else if (isExtensionSearch && fExt == extQuery) matches = true
                            }
                            
                            if (matches) {
                                results.add(UnifiedFile(f.name, toVirtualPath(context, f.absolutePath), f.isDirectory, f.length(), f.extension))
                            }
                            if (results.size >= 150) return@withContext results
                        }
                    }
            } catch (e: Exception) {}
        }
        return@withContext results
    }

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
        val dir = getResolvedFile(context, targetPath)
        if (dir.exists() && dir.isDirectory) {
            val list = dir.listFiles() ?: emptyArray()
            return list.map { f ->
                UnifiedFile(
                    name = f.name,
                    absolutePath = toVirtualPath(context, f.absolutePath),
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
        val file = getResolvedFile(context, targetPath)
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
        val file = getResolvedFile(context, targetPath)
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
        val parent = getResolvedFile(context, targetPath)
        val dir = File(parent, folderName)
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
        val uploadDir = getResolvedFile(context, targetPath)
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
                getResolvedFile(context, targetPath)
            )
        } catch (e: Exception) {
            Uri.fromFile(getResolvedFile(context, targetPath))
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
                val f = getResolvedFile(context, srcTarget)
                if (!f.exists()) return false
                if (f.isDirectory) {
                    val destDir = File(getResolvedFile(context, destTarget), destName)
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
            val srcFile = getResolvedFile(context, srcTarget)
            val correctDestFile = File(getResolvedFile(context, destTarget), destName)
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
            try {
                context.startActivity(intent)
                Logger.log("Successfully dispatched View Intent for: $path")
                true
            } catch (e: android.content.ActivityNotFoundException) {
                Logger.log("No default app found for $mime. Falling back to chooser.")
                ErrorTracker.logError("FileUtils", "No default app found for opening $mime at $path", e)
                try {
                    val chooser = Intent.createChooser(intent, "Abrir com").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                    true
                } catch (e2: Exception) {
                    ErrorTracker.logError("FileUtils", "Failed to launch chooser", e2)
                    ErrorTracker.attemptAutoCorrection("FileUtils", "open_apk_failed")
                    android.widget.Toast.makeText(context, "Erro ao abrir: Nenhum app suportado encontrado.", android.widget.Toast.LENGTH_LONG).show()
                    false
                }
            }
        } catch (e: Exception) {
            Logger.log("Failed to open file: ${e.message}")
            android.widget.Toast.makeText(context, "Erro ao abrir: Nenhum app suportado encontrado.", android.widget.Toast.LENGTH_LONG).show()
            false
        }
    }
}

package com.example

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object Updater {
    
    suspend fun downloadExtractAndInstall(context: Context, zipUrl: String, onProgress: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                onProgress("Conectando ao GitHub...")
                val cacheDir = context.cacheDir
                val zipFile = File(cacheDir, "update.zip")
                if (zipFile.exists()) zipFile.delete()
                
                val connection = URL(zipUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connect()
                
                if (connection.responseCode !in 200..299) {
                    onProgress("Erro: Falha no download (Código ${connection.responseCode})")
                    return@withContext
                }

                val contentLength = connection.contentLength
                val input = connection.inputStream
                val output = FileOutputStream(zipFile)
                
                val data = ByteArray(8192)
                var count: Int
                var totalDownloaded: Long = 0
                
                while (input.read(data).also { count = it } != -1) {
                    output.write(data, 0, count)
                    totalDownloaded += count
                    if (contentLength > 0) {
                        val progress = (totalDownloaded * 100) / contentLength
                        onProgress("Baixando atualização... $progress%")
                    } else {
                        onProgress("Baixando atualização...")
                    }
                }
                output.flush()
                output.close()
                input.close()
                
                onProgress("Extraindo atualização do arquivo ZIP...")
                val apkFile = extractApkFromZip(zipFile, cacheDir)
                
                // Excluir arquivo zip extraído
                zipFile.delete()
                
                if (apkFile != null) {
                    onProgress("Iniciando instalação...")
                    installApk(context, apkFile)
                    onProgress("")
                } else {
                    onProgress("Erro: Nenhum formato .apk encontrado no diretório ZIP.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onProgress("Erro na atualização: ${e.message}")
            }
        }
    }
    
    private fun extractApkFromZip(zipFile: File, outputDir: File): File? {
        var apkFile: File? = null
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var zipEntry: ZipEntry? = zis.nextEntry
            while (zipEntry != null) {
                if (!zipEntry.isDirectory && zipEntry.name.endsWith(".apk")) {
                    val extractedFile = File(outputDir, "update.apk")
                    if (extractedFile.exists()) extractedFile.delete()
                    
                    BufferedOutputStream(FileOutputStream(extractedFile)).use { bos ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (zis.read(buffer).also { read = it } != -1) {
                            bos.write(buffer, 0, read)
                        }
                    }
                    apkFile = extractedFile
                    break // Instalaremos o primeiro APK encontrado
                }
                zipEntry = zis.nextEntry
            }
            zis.closeEntry()
        }
        return apkFile
    }

    private fun installApk(context: Context, apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        context.startActivity(intent)
    }
}

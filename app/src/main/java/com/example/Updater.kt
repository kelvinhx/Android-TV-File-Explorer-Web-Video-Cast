package com.example

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object Updater {

    suspend fun isUpdateAvailable(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/rebeijar/NexusTv/actions/runs?branch=main&status=success")
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                if (connection.responseCode in 200..299) {
                    val jsonStr = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(jsonStr)
                    val runs = json.optJSONArray("workflow_runs")
                    if (runs != null && runs.length() > 0) {
                        val latestRun = runs.optJSONObject(0)
                        val runId = latestRun.optLong("id")
                        
                        val prefs = context.getSharedPreferences("updater_prefs", Context.MODE_PRIVATE)
                        val lastRunId = prefs.getLong("last_installed_run_id", 0L)
                        
                        if (runId > lastRunId) {
                            prefs.edit().putLong("latest_available_run_id", runId).apply()
                            return@withContext true
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext false
        }
    }

    fun cleanUpOldUpdates() {
        try {
            val nexusDir = File(Environment.getExternalStorageDirectory(), "Nexus")
            if (nexusDir.exists() && nexusDir.isDirectory) {
                nexusDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".apk") || file.name.endsWith(".zip")) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun downloadExtractAndInstall(context: Context, zipUrl: String, onProgress: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                onProgress("Conectando ao GitHub...")
                val nexusDir = File(Environment.getExternalStorageDirectory(), "Nexus")
                if (!nexusDir.exists()) nexusDir.mkdirs()
                
                val zipFile = File(nexusDir, "update.zip")
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
                val apkFile = extractApkFromZip(zipFile, nexusDir)
                
                // Excluir arquivo zip extraído
                zipFile.delete()
                
                if (apkFile != null) {
                    onProgress("Iniciando instalação...")
                    
                    // Mark as installed before firing intent
                    val prefs = context.getSharedPreferences("updater_prefs", Context.MODE_PRIVATE)
                    val latest = prefs.getLong("latest_available_run_id", 0L)
                    prefs.edit().putLong("last_installed_run_id", latest).apply()

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

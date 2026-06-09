package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    private const val MAX_LOG_LINES = 100
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun log(message: String) {
        val timestamp = timeFormat.format(Date())
        val loggedLine = "[$timestamp] $message"
        android.util.Log.d("NexusExplorer", message)
        
        val currentList = _logs.value.toMutableList()
        currentList.add(0, loggedLine) // Insert at top (most recent first)
        if (currentList.size > MAX_LOG_LINES) {
            currentList.removeAt(currentList.size - 1)
        }
        _logs.value = currentList
    }

    fun getRamTelemetry(): RamStats {
        val runtime = Runtime.getRuntime()
        val usedMem = runtime.totalMemory() - runtime.freeMemory()
        val maxMem = runtime.maxMemory()
        val freeMem = maxMem - usedMem
        return RamStats(
            usedBytes = usedMem,
            freeBytes = freeMem,
            maxBytes = maxMem,
            percentageUsed = if (maxMem > 0) ((usedMem.toDouble() / maxMem.toDouble()) * 100).toInt() else 0
        )
    }

    data class RamStats(
        val usedBytes: Long,
        val freeBytes: Long,
        val maxBytes: Long,
        val percentageUsed: Int
    )
}

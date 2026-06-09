package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ServerState {
    private val _isClientConnected = MutableStateFlow(false)
    val isClientConnected: StateFlow<Boolean> = _isClientConnected

    private val _clientIp = MutableStateFlow<String?>(null)
    val clientIp: StateFlow<String?> = _clientIp

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning

    private val _serverIp = MutableStateFlow("127.0.0.1")
    val serverIp: StateFlow<String> = _serverIp

    fun setClientConnected(connected: Boolean, ip: String? = null) {
        _isClientConnected.value = connected
        _clientIp.value = ip
    }

    fun setServerRunning(running: Boolean) {
        _isServerRunning.value = running
    }

    fun setServerIp(ip: String) {
        _serverIp.value = ip
    }
}

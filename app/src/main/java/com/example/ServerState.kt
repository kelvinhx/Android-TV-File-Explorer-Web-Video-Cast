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

    private val globalCookies = mutableMapOf<String, MutableMap<String, String>>()
    
    fun getCookies(host: String): Map<String, String> {
        val matchingCookies = mutableMapOf<String, String>()
        globalCookies.forEach { (domain, domainCookies) ->
            if (host.endsWith(domain)) {
                matchingCookies.putAll(domainCookies)
            }
        }
        return matchingCookies
    }
    
    fun setCookie(host: String, name: String, value: String) {
        val domainCookies = globalCookies.getOrPut(host) { mutableMapOf() }
        domainCookies[name] = value
    }

    val uploadNotification = MutableStateFlow<String?>(null)

    fun postUploadNotification(message: String) {
        uploadNotification.value = message
    }

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

package com.example

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NsdHelper(context: Context) {
    private val nsdManager: NsdManager? = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val serviceType = "_http._tcp."
    private val serviceName = "NexusTV"

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val _discoveredIp = MutableStateFlow<String?>(null)
    val discoveredIp: StateFlow<String?> = _discoveredIp

    fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = this@NsdHelper.serviceName
            this.serviceType = this@NsdHelper.serviceType
            this.port = port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Logger.log("mDNS Service registered: ${NsdServiceInfo.serviceName}")
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Logger.log("mDNS Registration failed: $errorCode")
            }
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }
        try {
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Logger.log("Error registering NSD: ${e.message}")
        }
    }

    fun discoverServices() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Logger.log("mDNS Discovery started")
            }
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == serviceType && service.serviceName.contains(serviceName)) {
                    nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val ip = serviceInfo.host.hostAddress
                            if (ip != null) {
                                _discoveredIp.value = ip
                                Logger.log("mDNS Discovered TV at: $ip")
                                stopDiscovery()
                            }
                        }
                    })
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                if (service.serviceName.contains(serviceName)) {
                    _discoveredIp.value = null
                }
            }
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { stopDiscovery() }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        try {
            nsdManager?.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Logger.log("Error starting NSD discovery: ${e.message}")
        }
    }

    fun stopRegistration() {
        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
        } catch(e: Exception){}
    }

    fun stopDiscovery() {
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch(e: Exception){}
    }
}

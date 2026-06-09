package com.example

import java.net.NetworkInterface
import java.util.Collections

object NetworkManager {

    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                // Ignore loopback and virtual/inactive interfaces if possible
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress) {
                        val ip = address.hostAddress ?: continue
                        // Filter for IPv4 addresses
                        if (ip.indexOf(':') < 0) {
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.log("Error finding local network IP: ${e.message}")
        }
        return "127.0.0.1"
    }
}

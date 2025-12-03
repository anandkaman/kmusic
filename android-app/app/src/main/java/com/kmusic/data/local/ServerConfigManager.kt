package com.kmusic.data.local

import android.content.Context
import android.content.SharedPreferences

class ServerConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "server_config",
        Context.MODE_PRIVATE
    )

    fun saveServerConfig(address: String, port: Int, useHttps: Boolean) {
        prefs.edit().apply {
            putString(KEY_SERVER_ADDRESS, address)
            putInt(KEY_PORT, port)
            putBoolean(KEY_USE_HTTPS, useHttps)
            apply()
        }
    }

    fun getServerAddress(): String {
        return prefs.getString(KEY_SERVER_ADDRESS, DEFAULT_ADDRESS) ?: DEFAULT_ADDRESS
    }

    fun getPort(): Int {
        return prefs.getInt(KEY_PORT, DEFAULT_PORT)
    }

    fun useHttps(): Boolean {
        return prefs.getBoolean(KEY_USE_HTTPS, false)
    }

    fun getBaseUrl(): String {
        val protocol = if (useHttps()) "https" else "http"
        val address = getServerAddress()
        val port = getPort()
        return "$protocol://$address:$port/"
    }

    fun isConfigured(): Boolean {
        return prefs.contains(KEY_SERVER_ADDRESS)
    }

    companion object {
        private const val KEY_SERVER_ADDRESS = "server_address"
        private const val KEY_PORT = "port"
        private const val KEY_USE_HTTPS = "use_https"

        private const val DEFAULT_ADDRESS = "192.168.1.100"
        private const val DEFAULT_PORT = 8080
    }
}

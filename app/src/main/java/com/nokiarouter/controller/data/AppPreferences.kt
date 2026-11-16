package com.nokiarouter.controller.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("nokia_router_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_ROUTER_IP = "key_router_ip"
        private const val KEY_USERNAME = "key_username"
        private const val KEY_PASSWORD = "key_password"
        private const val KEY_REMEMBER_CREDENTIALS = "key_remember_credentials"
        private const val KEY_OPEN_SSID = "key_open_ssid"
        private const val KEY_OPEN_ENABLED = "key_open_enabled"
        private const val KEY_MAC_ENTRIES = "key_mac_entries"
        private const val KEY_WIFI_NETWORKS = "key_wifi_networks"
    }

    var routerIp: String
        get() = prefs.getString(KEY_ROUTER_IP, "192.168.1.254") ?: "192.168.1.254"
        set(value) = prefs.edit().putString(KEY_ROUTER_IP, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "admin") ?: "admin"
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var password: String
        get() = prefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASSWORD, value).apply()

    var rememberCredentials: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_CREDENTIALS, true)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_CREDENTIALS, value).apply()

    var openNetworkSsid: String
        get() = prefs.getString(KEY_OPEN_SSID, "Nokia_VIP_Open") ?: "Nokia_VIP_Open"
        set(value) = prefs.edit().putString(KEY_OPEN_SSID, value).apply()

    var openNetworkEnabled: Boolean
        get() = prefs.getBoolean(KEY_OPEN_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_OPEN_ENABLED, value).apply()

    fun saveMacEntries(entries: List<MacFilterEntry>) {
        val json = gson.toJson(entries)
        prefs.edit().putString(KEY_MAC_ENTRIES, json).apply()
    }

    fun getMacEntries(): List<MacFilterEntry> {
        val json = prefs.getString(KEY_MAC_ENTRIES, null) ?: return listOf(
            MacFilterEntry("1C:69:7A:B4:92:11", "هاتفي (Samsung)", "Nokia_VIP_Open", "Allowed"),
            MacFilterEntry("E8:9F:80:C2:55:40", "لابتوب (ThinkPad)", "Nokia_VIP_Open", "Allowed")
        )
        return try {
            val type = object : TypeToken<List<MacFilterEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveWifiNetworks(networks: List<WifiNetworkItem>) {
        val json = gson.toJson(networks)
        prefs.edit().putString(KEY_WIFI_NETWORKS, json).apply()
    }

    fun getWifiNetworks(): List<WifiNetworkItem> {
        val json = prefs.getString(KEY_WIFI_NETWORKS, null) ?: return listOf(
            WifiNetworkItem(1, "Nokia-Home-2.4G", "2.4 GHz", true, true, "WPA2-PSK", "••••••••", "Auto", false),
            WifiNetworkItem(5, "Nokia-Home-5G", "5 GHz", true, true, "WPA2/WPA3 Personal", "••••••••", "Auto", false),
            WifiNetworkItem(2, "Nokia_VIP_Open", "2.4 GHz (مقيدة)", true, true, "مفتوحة بدون باسورد (Open)", "", "Auto", true)
        )
        return try {
            val type = object : TypeToken<List<WifiNetworkItem>>() {}.type
            val list = gson.fromJson<List<WifiNetworkItem>>(json, type) ?: emptyList()
            list.map { net ->
                val isTrulyOpen = net.password.isBlank() &&
                    !net.password.contains("•") &&
                    (net.isGuestOrOpen || net.securityMode.contains("None", ignoreCase = true) || net.securityMode.contains("Open", ignoreCase = true)) &&
                    !net.securityMode.contains("WPA", ignoreCase = true) &&
                    !net.securityMode.contains("11i", ignoreCase = true)
                net.copy(
                    isGuestOrOpen = isTrulyOpen,
                    securityMode = if (isTrulyOpen) "مفتوحة بدون باسورد (Open)" else if (net.securityMode.contains("None", ignoreCase = true)) "WPA2-PSK" else net.securityMode
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_PASSWORD)
            .apply()
    }

    fun setDeviceNickname(mac: String, nickname: String) {
        val nicknames = getDeviceNicknames().toMutableMap()
        if (nickname.isBlank()) {
            nicknames.remove(mac.lowercase())
        } else {
            nicknames[mac.lowercase()] = nickname.trim()
        }
        prefs.edit().putString("key_device_nicknames", gson.toJson(nicknames)).apply()
    }

    fun getDeviceNicknames(): Map<String, String> {
        val json = prefs.getString("key_device_nicknames", null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun addBlockedMac(mac: String) {
        val set = getBlockedMacs().toMutableSet()
        set.add(mac.uppercase())
        prefs.edit().putStringSet("key_blocked_macs", set).apply()
    }

    fun removeBlockedMac(mac: String) {
        val set = getBlockedMacs().toMutableSet()
        set.remove(mac.uppercase())
        prefs.edit().putStringSet("key_blocked_macs", set).apply()
    }

    fun getBlockedMacs(): Set<String> {
        return prefs.getStringSet("key_blocked_macs", emptySet()) ?: emptySet()
    }
}

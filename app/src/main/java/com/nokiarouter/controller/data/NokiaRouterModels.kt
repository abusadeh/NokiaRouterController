package com.nokiarouter.controller.data

import com.google.gson.annotations.SerializedName

data class NonceResponse(
    @SerializedName("nonce") val nonce: String? = null,
    @SerializedName("randomKey") val randomKey: String? = null,
    @SerializedName("pubkey") val pubkey: String? = null
)

data class LoginResponse(
    @SerializedName("result") val result: Int = -1,
    @SerializedName("failurecount") val failureCount: Int? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("sid") val sid: String? = null,
    @SerializedName("error_msg") val errorMsg: String? = null
)

data class MainRouterInfo(
    @SerializedName("gwmodel") val gwModel: String? = null,
    @SerializedName("g_operatorId") val operatorId: String? = null,
    @SerializedName("g_curr_weblang") val webLang: String? = null
)

data class ConnectedDeviceItem(
    val ip: String,
    val mac: String,
    val hostName: String,
    val connectionType: String, // "Wi-Fi 2.4G", "Wi-Fi 5G", "LAN Cable"
    val signalDbm: String? = null,
    val isOnline: Boolean = true,
    val isWhitelisted: Boolean = false,
    val isBlocked: Boolean = false,
    val oid: Int? = null
)

data class MacFilterEntry(
    val mac: String,
    val deviceName: String = "",
    val ssidName: String = "Open Protected Network",
    val status: String = "Allowed"
)

data class WifiNetworkItem(
    val ssidIndex: Int = 1,              // 1, 2, 3, 4 (2.4G) or 5, 6, 7, 8 (5G)
    val ssid: String,                    // Network name
    val band: String,                    // "2.4 GHz" or "5 GHz"
    val isEnabled: Boolean = true,       // On / Off
    val isBroadcast: Boolean = true,     // Broadcast SSID
    val securityMode: String = "WPA2-PSK", // "None" (Open), "WPA2-PSK", "WPA3-SAE"
    val password: String = "",           // Cleartext password
    val channel: String = "Auto",
    val isGuestOrOpen: Boolean = false,
    val bandwidth: String = "Auto",      // "Auto", "20 MHz", "40 MHz", "80 MHz", "160 MHz"
    val txPower: String = "100%",        // "100%", "75%", "50%", "25%"
    val muMimo: Boolean = true,          // Multi-user MIMO
    val wmm: Boolean = true,             // Wi-Fi Multimedia QoS
    val maxUsers: Int = 32               // Max clients limit
)

data class WanStatusInfo(
    val externalIp: String = "غير متصل",
    val status: String = "Connected",
    val uptimeFormatted: String = "0 دقيقة",
    val bytesReceivedFormatted: String = "0 MB",
    val bytesSentFormatted: String = "0 MB",
    val dnsServers: String = "",
    val ponStatus: String = "Up",
    val maxBitRate: String = "2.5 Gbps"
)

data class PingDiagnosticResult(
    val target: String = "8.8.8.8",
    val isSuccess: Boolean = false,
    val latencyMs: Long = -1L,
    val details: String = ""
)

data class FirewallSettings(
    val level: String = "Low",            // "High", "Low", "Off"
    val attackProtection: Boolean = true, // DoS protection
    val pingFromWan: Boolean = false      // Block WAN Ping
)

data class LanDhcpSettings(
    val routerIp: String = "192.168.1.254",
    val subnetMask: String = "255.255.255.0",
    val dhcpStartIp: String = "192.168.1.2",
    val dhcpEndIp: String = "192.168.1.253",
    val leaseTimeHours: Int = 24,
    val primaryDns: String = "192.168.1.254",
    val secondaryDns: String = "8.8.8.8"
)

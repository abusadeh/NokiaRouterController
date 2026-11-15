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
    val isWhitelisted: Boolean = false
)

data class MacFilterEntry(
    val mac: String,
    val deviceName: String = "",
    val ssidName: String = "Open Protected Network",
    val status: String = "Allowed"
)

data class WifiBandConfig(
    val bandName: String, // "2.4 GHz", "5 GHz", "Guest / Open"
    val ssid: String,
    val isEnabled: Boolean,
    val security: String, // "Open (No Password)", "WPA2-PSK", etc.
    val password: String = "",
    val macFilterEnabled: Boolean = false
)

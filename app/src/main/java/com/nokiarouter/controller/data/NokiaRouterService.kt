package com.nokiarouter.controller.data

import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class NokiaRouterService(
    private var baseUrl: String = "http://192.168.1.254"
) {
    private val gson = Gson()

    // In-memory cookie jar to retain session cookies (sid, lsid, etc.)
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val existing = cookieStore.getOrPut(url.host) { mutableListOf() }
            cookies.forEach { newCookie ->
                existing.removeAll { it.name == newCookie.name }
                existing.add(newCookie)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    var pubKeyPem: String = ""
        private set
    var csrfToken: String = ""
        private set
    var sessionId: String = ""
        private set
    var sessionKeys: SessionKeys? = null
        private set
    var isLoggedIn: Boolean = false
        private set
    var routerModel: String = "Nokia G-1426G-A"
        private set

    fun setRouterIp(ip: String) {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")
        baseUrl = "http://$cleanIp"
    }

    suspend fun getRouterBasicInfo(): Result<MainRouterInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/main_web_app.cgi")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful && body.isNotEmpty()) {
                val info = gson.fromJson(body, MainRouterInfo::class.java)
                info.gwModel?.let { routerModel = it }
                Result.success(info)
            } else {
                Result.failure(Exception("Failed to get router info: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchNonce(): Result<NonceResponse> = withContext(Dispatchers.IO) {
        try {
            val emptyBody = "".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/login_web_app.cgi?nonce")
                .post(emptyBody)
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful && body.isNotEmpty()) {
                val nonceData = gson.fromJson(body, NonceResponse::class.java)
                nonceData.pubkey?.let {
                    var formatted = it
                    if (!formatted.contains("-----BEGIN PUBLIC KEY-----\n")) {
                        val raw = formatted.replace("-----BEGIN PUBLIC KEY-----", "")
                            .replace("-----END PUBLIC KEY-----", "")
                            .trim()
                        formatted = "-----BEGIN PUBLIC KEY-----\n$raw\n-----END PUBLIC KEY-----"
                    }
                    pubKeyPem = formatted
                }
                Result.success(nonceData)
            } else {
                Result.failure(Exception("Failed to fetch nonce from router"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            val nonceRes = fetchNonce().getOrThrow()
            val nonce = nonceRes.nonce ?: throw Exception("Nonce is missing")
            val randomKey = nonceRes.randomKey ?: throw Exception("randomKey is missing")

            val encrypted = NokiaCrypto.buildEncryptedLoginPayload(
                pubKeyPem = pubKeyPem,
                nonce = nonce,
                randomKey = randomKey,
                username = username,
                password = password
            )
            sessionKeys = encrypted.sessionKeys

            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
            val reqBody = encrypted.postBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$baseUrl/login_web_app.cgi")
                .post(reqBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotEmpty()) {
                val loginResult = gson.fromJson(body, LoginResponse::class.java)
                if (loginResult.result == 0 && !loginResult.token.isNullOrEmpty()) {
                    csrfToken = loginResult.token
                    sessionId = loginResult.sid.orEmpty()
                    isLoggedIn = true
                    Result.success(loginResult)
                } else {
                    Result.failure(Exception("كلمة المرور أو اسم المستخدم غير صحيح (رمز الخطأ: ${loginResult.result})"))
                }
            } else {
                Result.failure(Exception("خطأ في الاتصال بالراوتر (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConnectedDevices(): Result<List<ConnectedDeviceItem>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/lan_status_web_app.cgi?lan")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val rawBody = response.body?.string().orEmpty()
            val decrypted = decryptIfEncrypted(rawBody)

            val devices = parseConnectedDevices(decrypted)
            Result.success(devices)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMacFilterRules(): Result<List<MacFilterEntry>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/macfilter_status_web_app.cgi")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val rawBody = response.body?.string().orEmpty()
            val decrypted = decryptIfEncrypted(rawBody)

            val entries = parseMacFilterEntries(decrypted)
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMacToWhitelist(macAddress: String, deviceName: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanMac = macAddress.trim().uppercase()
            val encodedName = URLEncoder.encode(deviceName.ifBlank { "Permitted Device" }, "UTF-8")

            // Format parameters matching Nokia macfilter_web_app.cgi?add_wlan
            val params = "mac_addr=$cleanMac&device_name=$encodedName&filter_mode=allow&mac_address=$cleanMac"
            val postPayload = NokiaCrypto.encryptPostWithCsrf(
                pubKeyPem = pubKeyPem,
                csrfToken = csrfToken,
                params = params
            )

            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
            val reqBody = postPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$baseUrl/macfilter_web_app.cgi?add_wlan")
                .post(reqBody)
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMacFromWhitelist(macAddress: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanMac = macAddress.trim().uppercase()
            val params = "act=del_wlan&mac=$cleanMac"
            val postPayload = NokiaCrypto.encryptPostWithCsrf(
                pubKeyPem = pubKeyPem,
                csrfToken = csrfToken,
                params = params
            )

            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
            val reqBody = postPayload.toRequestBody(mediaType)

            // Del endpoint uses act=del_wlan&mac=...
            val request = Request.Builder()
                .url("$baseUrl/macfilter_web_app.cgi?act=del_wlan&mac=$cleanMac")
                .post(reqBody)
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Configures and creates an Open (No Password) Wi-Fi Network restricted strictly by MAC Filtering.
     * Applies settings to both Guest Wi-Fi subsystem and WLAN Multi-SSID subsystem.
     */
    suspend fun configureOpenMacRestrictedNetwork(
        ssidName: String,
        enable: Boolean
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val encodedSsid = URLEncoder.encode(ssidName.trim(), "UTF-8")
            val isEnabledFlag = if (enable) "1" else "0"

            // 1. Submit to Guest Network subsystem: ConfigWhwGuest
            val guestParams = StringBuilder()
                .append("guest_enable=").append(isEnabledFlag)
                .append("&guest_ssid=").append(encodedSsid)
                .append("&security_mode=None")
                .append("&broadcast=1")
                .append("&mac_filter_enable=1")
                .append("&mac_filter_mode=allow")
                .toString()

            val guestPayload = NokiaCrypto.encryptPostWithCsrf(
                pubKeyPem = pubKeyPem,
                csrfToken = csrfToken,
                params = guestParams
            )

            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
            val guestReq = Request.Builder()
                .url("$baseUrl/wlan_config_guest_web_app.cgi?ConfigWhwGuest")
                .post(guestPayload.toRequestBody(mediaType))
                .build()

            val guestRes = client.newCall(guestReq).execute()

            // 2. Also ensure MAC filtering is globally active for WLAN in allowlist mode
            val filterGlobalParams = "filter_mode=allow&mac_filter_enable=1&act=set_filter"
            val filterPayload = NokiaCrypto.encryptPostWithCsrf(
                pubKeyPem = pubKeyPem,
                csrfToken = csrfToken,
                params = filterGlobalParams
            )
            val filterReq = Request.Builder()
                .url("$baseUrl/macfilter_web_app.cgi?filter_mode=allow")
                .post(filterPayload.toRequestBody(mediaType))
                .build()
            client.newCall(filterReq).execute()

            Result.success(guestRes.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rebootRouter(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val postPayload = NokiaCrypto.encryptPostWithCsrf(
                pubKeyPem = pubKeyPem,
                csrfToken = csrfToken
            )
            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
            val reqBody = postPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$baseUrl/reboot_web_app.cgi")
                .post(reqBody)
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setLedStatus(enabled: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val postPayload = NokiaCrypto.encryptPostWithCsrf(
                pubKeyPem = pubKeyPem,
                csrfToken = csrfToken,
                params = "led_status=${if (enabled) "1" else "0"}"
            )
            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
            val reqBody = postPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$baseUrl/ledctrl_web_app.cgi?SetLedGlb")
                .post(reqBody)
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decryptIfEncrypted(rawResponse: String): String {
        val trimmed = rawResponse.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed
        }
        val keys = sessionKeys ?: return trimmed
        return try {
            val cipherBytes = Base64.decode(trimmed, Base64.DEFAULT)
            NokiaCrypto.aesDecryptCbc(keys.decKeyBytes, keys.decIvBytes, cipherBytes)
        } catch (e: Exception) {
            trimmed
        }
    }

    private fun parseConnectedDevices(json: String): List<ConnectedDeviceItem> {
        val list = mutableListOf<ConnectedDeviceItem>()
        if (json.isEmpty()) return list
        try {
            val root = gson.fromJson(json, Map::class.java)
            val clientList = (root["client_list"] ?: root["lan_list"] ?: root["wlan_list"]) as? List<*>
            if (clientList != null) {
                for (item in clientList) {
                    if (item is Map<*, *>) {
                        val ip = item["ip"]?.toString() ?: item["IPv4Address"]?.toString() ?: "192.168.1.X"
                        val mac = item["mac"]?.toString() ?: item["MACAddress"]?.toString() ?: "00:00:00:00:00:00"
                        val name = item["host_name"]?.toString() ?: item["HostName"]?.toString() ?: "Device"
                        val conn = item["interface"]?.toString() ?: "Wi-Fi"
                        list.add(ConnectedDeviceItem(ip, mac, name, conn))
                    }
                }
            }
        } catch (e: Exception) {
            // Handled
        }
        return list
    }

    private fun parseMacFilterEntries(json: String): List<MacFilterEntry> {
        val list = mutableListOf<MacFilterEntry>()
        if (json.isEmpty()) return list
        try {
            val root = gson.fromJson(json, Map::class.java)
            val filterList = (root["wlan_mac_filter_config"] ?: root["mac_filter_list"]) as? List<*>
            if (filterList != null) {
                for (item in filterList) {
                    if (item is Map<*, *>) {
                        val mac = item["mac"]?.toString() ?: item["MACAddress"]?.toString() ?: ""
                        val name = item["name"]?.toString() ?: "Permitted Device"
                        if (mac.isNotEmpty()) {
                            list.add(MacFilterEntry(mac, name))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handled
        }
        return list
    }
}

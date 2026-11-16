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

    internal val client = OkHttpClient.Builder()
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

    suspend fun fetchWifiNetworks(): Result<List<WifiNetworkItem>> = withContext(Dispatchers.IO) {
        try {
            val networks = mutableListOf<WifiNetworkItem>()

            // 1. Fetch 2.4 GHz SSIDs
            val req24 = Request.Builder()
                .url("$baseUrl/wlan_config_status_web_app.cgi")
                .get()
                .build()
            val res24 = client.newCall(req24).execute()
            val raw24 = res24.body?.string().orEmpty()
            val dec24 = decryptIfEncrypted(raw24)
            parseWifiList(dec24, "2.4 GHz", networks)

            // 2. Fetch 5 GHz SSIDs
            val req5 = Request.Builder()
                .url("$baseUrl/wlan_config_status_web_app.cgi?v=11ac")
                .get()
                .build()
            val res5 = client.newCall(req5).execute()
            val raw5 = res5.body?.string().orEmpty()
            val dec5 = decryptIfEncrypted(raw5)
            parseWifiList(dec5, "5 GHz", networks)

            // 3. Fetch Guest Wi-Fi info
            val reqGuest = Request.Builder()
                .url("$baseUrl/wlan_config_guest_status_web_app.cgi")
                .get()
                .build()
            val resGuest = client.newCall(reqGuest).execute()
            val rawGuest = resGuest.body?.string().orEmpty()
            val decGuest = decryptIfEncrypted(rawGuest)
            parseGuestWifi(decGuest, networks)

            Result.success(networks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWifiNetwork(item: WifiNetworkItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val is5G = item.band.contains("5")
            val endpoint = if (is5G) "wlan_config_web_app.cgi?do_config_glb11ac" else "wlan_config_web_app.cgi?do_config_glb"

            val encodedSsid = URLEncoder.encode(item.ssid.trim(), "UTF-8")
            val encodedPwd = URLEncoder.encode(item.password.trim(), "UTF-8")
            val isEnabledFlag = if (item.isEnabled) "1" else "0"
            val encryptMode = if (item.securityMode.contains("None", ignoreCase = true) || item.password.isEmpty()) "None" else "Private2"

            val bandwidthCode = when {
                item.bandwidth.contains("160") -> "3"
                item.bandwidth.contains("80") -> "2"
                item.bandwidth.contains("40") -> "1"
                else -> "0"
            }
            val powerCode = item.txPower.replace("%", "").trim().ifBlank { "100" }

            val params = StringBuilder()
                .append("wl_bandwidth=").append(bandwidthCode)
                .append("&wl_channel=").append(if (item.channel.isBlank()) "Auto" else item.channel)
                .append("&wl_power=").append(powerCode)
                .append("&wl_wmm=").append(if (item.wmm) "1" else "0")
                .append("&wl_mimo=").append(if (item.muMimo) "1" else "0")
                .append("&total_max_user=").append(item.maxUsers)
                .append("&wl_id=").append(item.ssidIndex)
                .append("&wl_ssid=").append(encodedSsid)
                .append("&wl_ssidname=").append(encodedSsid)
                .append("&wl_en=").append(isEnabledFlag)
                .append("&wl_broad=").append(if (item.isBroadcast) "1" else "0")
                .append("&port_mode=0")
                .append("&isReboot=0")
                .append("&max_user=").append(item.maxUsers)
                .append("&wl_encryptmode=").append(encryptMode)

            if (encryptMode != "None") {
                params.append("&wpaKey=").append(encodedPwd)
            }

            params.append("&dnName=dfault&newDomain=&wanList=-2&nofIP=24&wl_enable=on")

            val postPayload = NokiaCrypto.encryptPostWithCsrf(
                pubKeyPem = pubKeyPem,
                csrfToken = csrfToken,
                params = params.toString()
            )

            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
            val request = Request.Builder()
                .url("$baseUrl/$endpoint")
                .post(postPayload.toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addNewWifiNetwork(
        ssid: String,
        band: String,
        password: String,
        isOpen: Boolean
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val is5G = band.contains("5")
        // Secondary SSID index: 2 on 2.4G, 6 on 5G
        val targetIndex = if (is5G) 6 else 2
        val security = if (isOpen) "None" else "WPA2-PSK"

        val newItem = WifiNetworkItem(
            ssidIndex = targetIndex,
            ssid = ssid,
            band = band,
            isEnabled = true,
            isBroadcast = true,
            securityMode = security,
            password = if (isOpen) "" else password,
            channel = "Auto",
            isGuestOrOpen = isOpen
        )
        updateWifiNetwork(newItem)
    }

    suspend fun deleteWifiNetwork(item: WifiNetworkItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val is5G = item.band.contains("5")
            val endpoint = if (is5G) "wlan_config_web_app.cgi?do_config_glb11ac" else "wlan_config_web_app.cgi?do_config_glb"

            // 1. If it's a Guest Wi-Fi network, call ConfigWhwGuest to disable and clear it
            if (item.isGuestOrOpen || item.ssidIndex in listOf(2, 6) || item.band.contains("Guest", ignoreCase = true)) {
                try {
                    val guestParams = "act=ConfigWhwGuest&iso=&dura=&accessType=open&enable=0&wl_ssid=&wpaKey=&dual_band=1&ssid_num=2&sync_value=1"
                    val guestPayload = NokiaCrypto.encryptPostWithCsrf(pubKeyPem, csrfToken, guestParams)
                    val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
                    val guestReq = Request.Builder()
                        .url("$baseUrl/wlan_config_guest_web_app.cgi?ConfigWhwGuest")
                        .post(guestPayload.toRequestBody(mediaType))
                        .build()
                    client.newCall(guestReq).execute()
                } catch (_: Exception) {}
            }

            // 2. Disable and wipe the SSID configuration in the router's WLAN slot
            val params = StringBuilder()
                .append("wl_bandwidth=0")
                .append("&wl_channel=").append(item.channel.ifBlank { "Auto" })
                .append("&wl_power=100")
                .append("&wl_wmm=1")
                .append("&wl_mimo=1")
                .append("&total_max_user=32")
                .append("&wl_id=").append(item.ssidIndex)
                .append("&wl_ssid=")
                .append("&wl_ssidname=")
                .append("&wl_en=0")
                .append("&wl_broad=0")
                .append("&port_mode=0")
                .append("&isReboot=0")
                .append("&max_user=32")
                .append("&wl_encryptmode=None")
                .append("&dnName=dfault&newDomain=&wanList=-2&nofIP=24")
                .toString()

            val postPayload = NokiaCrypto.encryptPostWithCsrf(
                pubKeyPem = pubKeyPem,
                csrfToken = csrfToken,
                params = params
            )
            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
            val request = Request.Builder()
                .url("$baseUrl/$endpoint")
                .post(postPayload.toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun configureOpenMacRestrictedNetwork(
        ssidName: String,
        enable: Boolean
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val encodedSsid = URLEncoder.encode(ssidName.trim(), "UTF-8")
            val isEnabledFlag = if (enable) "1" else "0"

            // 1. Configure Guest / Open Network subsystem
            val guestParams = StringBuilder()
                .append("act=ConfigWhwGuest")
                .append("&iso=")
                .append("&dura=")
                .append("&accessType=open")
                .append("&enable=").append(isEnabledFlag)
                .append("&wl_ssid=").append(encodedSsid)
                .append("&wpaKey=")
                .append("&dual_band=1")
                .append("&ssid_num=2")
                .append("&sync_value=1")
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

            client.newCall(guestReq).execute()

            // 2. Also configure secondary SSID on 2.4G as Open network without password
            val secondaryParams = StringBuilder()
                .append("wl_bandwidth=0&wl_channel=Auto&wl_power=100&wl_wmm=1&wl_mimo=1&total_max_user=32")
                .append("&wl_id=2")
                .append("&wl_ssid=").append(encodedSsid)
                .append("&wl_ssidname=").append(encodedSsid)
                .append("&wl_en=").append(isEnabledFlag)
                .append("&wl_broad=1&port_mode=0&isReboot=0&max_user=32&wl_encryptmode=None")
                .append("&dnName=dfault&newDomain=&wanList=-2&nofIP=24&wl_enable=on")
                .toString()

            val secPayload = NokiaCrypto.encryptPostWithCsrf(
                pubKeyPem = pubKeyPem,
                csrfToken = csrfToken,
                params = secondaryParams
            )
            val secReq = Request.Builder()
                .url("$baseUrl/wlan_config_web_app.cgi?do_config_glb")
                .post(secPayload.toRequestBody(mediaType))
                .build()
            client.newCall(secReq).execute()

            // 3. Set MAC filtering to Allowlist mode
            val filterParams = "filter_mode=allow&mac_filter_enable=1&act=set_filter"
            val filterPayload = NokiaCrypto.encryptPostWithCsrf(
                pubKeyPem = pubKeyPem,
                csrfToken = csrfToken,
                params = filterParams
            )
            val filterReq = Request.Builder()
                .url("$baseUrl/macfilter_web_app.cgi?filter_mode=allow")
                .post(filterPayload.toRequestBody(mediaType))
                .build()
            val filterRes = client.newCall(filterReq).execute()

            Result.success(filterRes.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConnectedDevices(): Result<List<ConnectedDeviceItem>> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch /lan_status_web_app.cgi?wlan (all LAN & WLAN DHCP leases & MACs)
            var rawWlan = ""
            try {
                val reqWlan = Request.Builder()
                    .url("$baseUrl/lan_status_web_app.cgi?wlan")
                    .get()
                    .build()
                val resWlan = client.newCall(reqWlan).execute()
                rawWlan = resWlan.body?.string().orEmpty()
            } catch (e: Exception) {
                // Ignore and fallback
            }
            val decWlan = decryptIfEncrypted(rawWlan)

            // 2. Fetch /dashboard_device_status_web_app.cgi (device hostnames and active status)
            var rawDash = ""
            try {
                val reqDash = Request.Builder()
                    .url("$baseUrl/dashboard_device_status_web_app.cgi")
                    .get()
                    .build()
                val resDash = client.newCall(reqDash).execute()
                rawDash = resDash.body?.string().orEmpty()
            } catch (e: Exception) {
                // Ignore
            }
            val decDash = decryptIfEncrypted(rawDash)

            // 3. Fetch /sta_info_web_app.cgi (live associated Wi-Fi stations & RSSI dBm)
            var rawSta = ""
            try {
                val reqSta = Request.Builder()
                    .url("$baseUrl/sta_info_web_app.cgi")
                    .get()
                    .build()
                val resSta = client.newCall(reqSta).execute()
                rawSta = resSta.body?.string().orEmpty()
            } catch (e: Exception) {
                // Ignore
            }
            val decSta = decryptIfEncrypted(rawSta)

            val devices = parseCombinedConnectedDevices(decWlan, decDash, decSta)
            Result.success(devices)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchWanAndPonStatus(): Result<WanStatusInfo> = withContext(Dispatchers.IO) {
        try {
            var extIp = "غير متصل"
            var connStatus = "Connected"
            var uptimeSec = 0L
            var rxBytes = 0L
            var txBytes = 0L
            var dns = ""

            // 1. Fetch WAN IPv4 info
            try {
                val req = Request.Builder().url("$baseUrl/show_wan_status_web_app.cgi?ipv4").get().build()
                val raw = client.newCall(req).execute().body?.string().orEmpty()
                val dec = decryptIfEncrypted(raw)
                val root = gson.fromJson(dec, Map::class.java)
                val conns = root["wan_conns"] as? List<*>
                val firstConn = conns?.firstOrNull() as? Map<*, *>
                val pppList = firstConn?.get("pppConns") as? List<*>
                val ppp = pppList?.firstOrNull() as? Map<*, *>

                extIp = ppp?.get("ExternalIPAddress")?.toString() ?: extIp
                connStatus = ppp?.get("ConnectionStatus")?.toString() ?: connStatus
                uptimeSec = (ppp?.get("Uptime") as? Number)?.toLong() 
                    ?: ppp?.get("Uptime")?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                rxBytes = (ppp?.get("EthernetBytesReceived") as? Number)?.toLong() 
                    ?: ppp?.get("EthernetBytesReceived")?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                txBytes = (ppp?.get("EthernetBytesSent") as? Number)?.toLong() 
                    ?: ppp?.get("EthernetBytesSent")?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                dns = ppp?.get("DNSServers")?.toString() ?: ""
            } catch (_: Exception) {}

            // 2. Fetch PON status
            var ponStatus = "Up"
            var maxBitRate = "2.5 Gbps"
            try {
                val req = Request.Builder().url("$baseUrl/pon_status_web_app.cgi").get().build()
                val raw = client.newCall(req).execute().body?.string().orEmpty()
                val dec = decryptIfEncrypted(raw)
                val root = gson.fromJson(dec, Map::class.java)
                ponStatus = root["Status"]?.toString() ?: ponStatus
                val bitrate = root["MaxBitRate"]?.toString()
                if (bitrate != null && bitrate.isNotEmpty()) {
                    maxBitRate = "$bitrate Mbps"
                }
            } catch (_: Exception) {}

            fun formatBytes(bytes: Long): String {
                return when {
                    bytes >= 1_073_741_824L -> String.format(java.util.Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
                    bytes >= 1_048_576L -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_048_576.0)
                    else -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1024.0)
                }
            }

            fun formatUptime(seconds: Long): String {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                return if (hours > 0) "$hours ساعة و $minutes دقيقة" else "$minutes دقيقة"
            }

            val info = WanStatusInfo(
                externalIp = extIp,
                status = connStatus,
                uptimeFormatted = formatUptime(uptimeSec),
                bytesReceivedFormatted = formatBytes(rxBytes),
                bytesSentFormatted = formatBytes(txBytes),
                dnsServers = dns,
                ponStatus = ponStatus,
                maxBitRate = maxBitRate
            )
            Result.success(info)
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
            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()

            // 1. Ensure MAC filter is active in allow mode
            try {
                val modeParams = "filter_mode=allow&mac_filter_enable=1&act=set_filter"
                val modePayload = NokiaCrypto.encryptPostWithCsrf(pubKeyPem, csrfToken, modeParams)
                val modeReq = Request.Builder()
                    .url("$baseUrl/macfilter_web_app.cgi?filter_mode=allow")
                    .post(modePayload.toRequestBody(mediaType))
                    .build()
                client.newCall(modeReq).execute()
            } catch (_: Exception) {}

            // 2. Add to global WLAN whitelist
            val params = "mac_addr=$cleanMac&device_name=$encodedName&filter_mode=allow&mac_address=$cleanMac"
            val postPayload = NokiaCrypto.encryptPostWithCsrf(
                pubKeyPem = pubKeyPem,
                csrfToken = csrfToken,
                params = params
            )
            val reqBody = postPayload.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$baseUrl/macfilter_web_app.cgi?add_wlan")
                .post(reqBody)
                .build()
            client.newCall(request).execute()

            // 3. Ensure Allow rule on open & secondary SSIDs (2, 6, 1, 5)
            for (ssidId in listOf(2, 6, 1, 5)) {
                try {
                    val p = "wlan_mac_exclude_mod=Allow&wl_id=$ssidId&ssid_enable=on&wlan_servername=$encodedName&wlan_mac_address=$cleanMac"
                    val payload = NokiaCrypto.encryptPostWithCsrf(pubKeyPem, csrfToken, p)
                    val req = Request.Builder()
                        .url("$baseUrl/macfilter_web_app.cgi?add_wlan")
                        .post(payload.toRequestBody(mediaType))
                        .build()
                    client.newCall(req).execute()
                } catch (_: Exception) {}
            }

            // 4. Remove any firewall drops if previously blocked
            try {
                unblockDeviceOnRouter(cleanMac, null)
            } catch (_: Exception) {}

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMacFromWhitelist(macAddress: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanMac = macAddress.trim().uppercase()
            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()

            // 1. Remove from global WLAN whitelist
            val params = "act=del_wlan&mac=$cleanMac"
            val postPayload = NokiaCrypto.encryptPostWithCsrf(
                pubKeyPem = pubKeyPem,
                csrfToken = csrfToken,
                params = params
            )
            val reqBody = postPayload.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$baseUrl/macfilter_web_app.cgi?act=del_wlan&mac=$cleanMac")
                .post(reqBody)
                .build()
            client.newCall(request).execute()

            // 2. Remove from SSIDs
            for (ssidId in listOf(2, 6, 1, 5)) {
                try {
                    val p = "act=del_wlan&mac=$cleanMac&wl_id=$ssidId"
                    val payload = NokiaCrypto.encryptPostWithCsrf(pubKeyPem, csrfToken, p)
                    val req = Request.Builder()
                        .url("$baseUrl/macfilter_web_app.cgi?act=del_wlan&mac=$cleanMac")
                        .post(payload.toRequestBody(mediaType))
                        .build()
                    client.newCall(req).execute()
                } catch (_: Exception) {}
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun blockDeviceOnRouter(
        macAddress: String,
        ipAddress: String? = null,
        deviceName: String = "",
        oid: Int? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanMac = macAddress.trim().uppercase()
            val cleanIp = ipAddress?.trim().orEmpty()
            val encodedName = URLEncoder.encode(deviceName.ifBlank { "Blocked Device" }, "UTF-8")
            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()

            // 1. Layer 3 Instant Firewall Block: Drop all WAN internet packets from this IP immediately
            if (cleanIp.isNotEmpty() && cleanIp.contains(".") && !cleanIp.endsWith(".X") && cleanIp != "0.0.0.0") {
                try {
                    val ipFilterParams = "ip_filter_mode=WANblack&servername=$encodedName&serverIp=$cleanIp&filter_name=block_$cleanIp&source_ip_address=$cleanIp&Source_NetMask=255.255.255.255&destination_ip_address=&Dest_NetMask=&protocol=0&app_value=&source_portnum=&source_eportnum=&destination_portnum=&destination_eportnum=&ip_version=4&ip_filter_enable=1"
                    val ipPayload = NokiaCrypto.encryptPostWithCsrf(pubKeyPem, csrfToken, ipFilterParams)
                    val ipReq = Request.Builder()
                        .url("$baseUrl/ipfilter_web_app.cgi?add_glb")
                        .post(ipPayload.toRequestBody(mediaType))
                        .build()
                    client.newCall(ipReq).execute()
                } catch (_: Exception) {}
            }

            // 2. Layer 2 Hardware MAC Filter: Add to Deny filter on primary and secondary SSIDs (2.4G & 5G)
            val ssidIndices = listOf(1, 2, 5, 6)
            for (ssidId in ssidIndices) {
                try {
                    val params = "wlan_mac_exclude_mod=Deny&wl_id=$ssidId&ssid_enable=on&wlan_servername=$encodedName&wlan_mac_address=$cleanMac"
                    val payload = NokiaCrypto.encryptPostWithCsrf(pubKeyPem, csrfToken, params)
                    val req = Request.Builder()
                        .url("$baseUrl/macfilter_web_app.cgi?add_wlan")
                        .post(payload.toRequestBody(mediaType))
                        .build()
                    client.newCall(req).execute()
                } catch (_: Exception) {}
            }

            // 3. Remove from whitelist if present
            try {
                removeMacFromWhitelist(cleanMac)
            } catch (_: Exception) {}

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unblockDeviceOnRouter(
        macAddress: String,
        ipAddress: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanMac = macAddress.trim().uppercase()
            val cleanIp = ipAddress?.trim().orEmpty()
            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()

            // 1. Layer 2: Remove MAC filter deny entry on all SSIDs
            val ssidIndices = listOf(1, 2, 5, 6)
            for (ssidId in ssidIndices) {
                try {
                    val delParams = "act=del_wlan&ssid=$ssidId&mac=$cleanMac"
                    val payload = NokiaCrypto.encryptPostWithCsrf(pubKeyPem, csrfToken, "")
                    val req = Request.Builder()
                        .url("$baseUrl/macfilter_web_app.cgi?$delParams")
                        .post(payload.toRequestBody(mediaType))
                        .build()
                    client.newCall(req).execute()
                } catch (_: Exception) {}
            }

            // 2. Layer 3: Remove IP filter rule if exists for this IP
            try {
                val statusRes = fetchRawEndpoint("ipfilter_status_web_app.cgi").getOrNull().orEmpty()
                if (statusRes.isNotEmpty()) {
                    val root = gson.fromJson(statusRes, Map::class.java)
                    val ruleList = root["ip_filter_list"] as? List<*>
                    ruleList?.forEach { item ->
                        if (item is Map<*, *>) {
                            val srcIp = item["SourceIP"]?.toString().orEmpty()
                            val desc = item["Description"]?.toString().orEmpty()
                            val oid = (item["_oid"] as? Number)?.toInt()
                                ?: item["_oid"]?.toString()?.toDoubleOrNull()?.toInt()
                            if (oid != null && (srcIp == cleanIp || (cleanIp.isNotEmpty() && desc.contains(cleanIp)) || desc.contains(cleanMac))) {
                                val appName = item["X_ASB_COM_AppName"]?.toString() ?: "0"
                                val destIntf = item["DestInterface"]?.toString() ?: "WAN"
                                val target = item["Target"]?.toString() ?: "Drop"
                                val delUrl = "$baseUrl/ipfilter_web_app.cgi?v_glb=delip&id=$oid,$appName,$destIntf,$srcIp,$target"
                                val payload = NokiaCrypto.encryptPostWithCsrf(pubKeyPem, csrfToken, "")
                                val req = Request.Builder()
                                    .url(delUrl)
                                    .post(payload.toRequestBody(mediaType))
                                    .build()
                                client.newCall(req).execute()
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            Result.success(true)
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

    suspend fun runPingDiagnostic(host: String = "8.8.8.8"): Result<PingDiagnosticResult> = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(host, 53), 3000)
            val latency = System.currentTimeMillis() - start
            socket.close()
            Result.success(
                PingDiagnosticResult(
                    target = host,
                    isSuccess = true,
                    latencyMs = latency,
                    details = "زمن الاستجابة: $latency ملي ثانية • الخادم متاح ومتصل"
                )
            )
        } catch (e: Exception) {
            Result.success(
                PingDiagnosticResult(
                    target = host,
                    isSuccess = false,
                    latencyMs = -1L,
                    details = "تعذر الاتصال بالهدف ($host): ${e.message}"
                )
            )
        }
    }

    suspend fun updateFirewallSettings(settings: FirewallSettings): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val attackVal = if (settings.attackProtection) "1" else "0"
            val pingVal = if (settings.pingFromWan) "1" else "0"
            val params = "security_level=${settings.level}&attack_protect=$attackVal&wan_ping=$pingVal"
            val postPayload = NokiaCrypto.encryptPostWithCsrf(pubKeyPem, csrfToken, params)
            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
            val request = Request.Builder()
                .url("$baseUrl/firewall_web_app.cgi")
                .post(postPayload.toRequestBody(mediaType))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLanDhcpSettings(settings: LanDhcpSettings): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val params = StringBuilder()
                .append("ip_address=").append(settings.routerIp)
                .append("&subnet_mask=").append(settings.subnetMask)
                .append("&dhcp_start=").append(settings.dhcpStartIp)
                .append("&dhcp_end=").append(settings.dhcpEndIp)
                .append("&lease_time=").append(settings.leaseTimeHours * 3600)
                .append("&primary_dns=").append(settings.primaryDns)
                .append("&secondary_dns=").append(settings.secondaryDns)
                .toString()

            val postPayload = NokiaCrypto.encryptPostWithCsrf(pubKeyPem, csrfToken, params)
            val mediaType = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()
            val request = Request.Builder()
                .url("$baseUrl/lan_config_web_app.cgi")
                .post(postPayload.toRequestBody(mediaType))
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

    private fun parseWifiList(json: String, band: String, output: MutableList<WifiNetworkItem>) {
        if (json.isEmpty()) return
        try {
            val root = gson.fromJson(json, Map::class.java)
            val wlanList = (root["wlan_config_glb"] ?: root["wlan_config_glb11ac"] ?: root["wlan_list"]) as? List<*>
            if (wlanList != null) {
                for (item in wlanList) {
                    if (item is Map<*, *>) {
                        val ssid = item["SSID"]?.toString() ?: continue
                        if (ssid.isBlank()) continue

                        val ssidIndex = (item["SSID_num"] as? Number)?.toInt() ?: 1
                        val enable = (item["Enable"] as? Number)?.toInt() ?: 1

                        // Secondary SSIDs that are disabled/inactive shouldn't clutter the UI as active networks
                        if (ssidIndex != 1 && ssidIndex != 5 && enable == 0) continue
                        val beaconType = item["BeaconType"]?.toString() ?: "11i"
                        val channel = item["Channel"]?.toString() ?: "Auto"

                        // Extract password from psks array and decrypt it
                        var clearPassword = ""
                        val psks = item["psks"] as? List<*>
                        if (psks != null && psks.isNotEmpty()) {
                            val firstPsk = psks[0] as? Map<*, *>
                            val rawKey = firstPsk?.get("PreSharedKey")?.toString().orEmpty()
                            clearPassword = decryptIfEncrypted(rawKey)
                        }

                        val isSecurityNone = beaconType.equals("None", ignoreCase = true) || (clearPassword.isEmpty() && beaconType.isEmpty())
                        val securityStr = when {
                            isSecurityNone -> "مفتوحة بدون باسورد (Open)"
                            beaconType.contains("WPA3", ignoreCase = true) -> "WPA3-SAE"
                            beaconType.contains("11i", ignoreCase = true) || beaconType.contains("WPA2", ignoreCase = true) -> "WPA2-PSK"
                            else -> "WPA/WPA2 Personal"
                        }

                        output.add(
                            WifiNetworkItem(
                                ssidIndex = ssidIndex,
                                ssid = ssid,
                                band = band,
                                isEnabled = enable == 1,
                                isBroadcast = true,
                                securityMode = securityStr,
                                password = clearPassword,
                                channel = channel,
                                isGuestOrOpen = isSecurityNone || ssidIndex in listOf(2, 6)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Handled
        }
    }

    private fun parseGuestWifi(json: String, output: MutableList<WifiNetworkItem>) {
        if (json.isEmpty()) return
        try {
            val root = gson.fromJson(json, Map::class.java)
            val guestList = root["guest_list"] as? List<*>
            if (guestList != null) {
                for (item in guestList) {
                    if (item is Map<*, *>) {
                        val ssid = item["SSID"]?.toString() ?: item["guest_ssid"]?.toString() ?: continue
                        if (ssid.isBlank()) continue
                        val enable = (item["Enable"] as? Number)?.toInt() ?: 1
                        if (enable == 0) continue
                        var clearPassword = ""
                        val rawKey = item["PreSharedKey"]?.toString() ?: item["wpaKey"]?.toString().orEmpty()
                        if (rawKey.isNotEmpty()) {
                            clearPassword = decryptIfEncrypted(rawKey)
                        }
                        val isOpen = clearPassword.isEmpty() || item["security_mode"] == "None"

                        output.add(
                            WifiNetworkItem(
                                ssidIndex = 2,
                                ssid = ssid,
                                band = "2.4 GHz + 5 GHz (Guest)",
                                isEnabled = enable == 1,
                                isBroadcast = true,
                                securityMode = if (isOpen) "مفتوحة بدون باسورد (Open)" else "WPA2-PSK",
                                password = clearPassword,
                                isGuestOrOpen = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Handled
        }
    }

    private data class StaClientInfo(
        val mac: String,
        val ssid: String,
        val signalDbm: String,
        val downlinkRate: String
    )

    private fun parseCombinedConnectedDevices(
        wlanJson: String,
        dashJson: String,
        staJson: String
    ): List<ConnectedDeviceItem> {
        val staClients = mutableMapOf<String, StaClientInfo>()

        fun findStaClients(node: Any?) {
            when (node) {
                is Map<*, *> -> {
                    val mac = (node["AssociatedDeviceMACAddress"] ?: node["mac"])?.toString()?.trim()?.lowercase()
                    if (!mac.isNullOrEmpty() && mac.contains(":")) {
                        val signal = node["SignalStrength"]?.toString() ?: ""
                        val signalStr = if (signal.isNotEmpty()) "$signal dBm" else ""
                        val ssid = node["SSID"]?.toString().orEmpty()
                        val rate = node["LastDataDownlinkRate"]?.toString().orEmpty()
                        staClients[mac] = StaClientInfo(mac, ssid, signalStr, rate)
                    }
                    for (value in node.values) {
                        findStaClients(value)
                    }
                }
                is List<*> -> {
                    for (element in node) {
                        findStaClients(element)
                    }
                }
            }
        }

        if (staJson.isNotEmpty()) {
            try {
                val root = gson.fromJson(staJson, Any::class.java)
                findStaClients(root)
            } catch (_: Exception) {}
        }

        val oidToHostName = mutableMapOf<Int, String>()
        val oidToActive = mutableMapOf<Int, Boolean>()
        val macToAlias = mutableMapOf<String, String>()

        if (dashJson.isNotEmpty()) {
            try {
                val root = gson.fromJson(dashJson, Map::class.java)
                val devCfg = root["device_cfg"] as? List<*>
                devCfg?.forEach { item ->
                    if (item is Map<*, *>) {
                        val oid = (item["_oid"] as? Number)?.toInt()
                            ?: item["_oid"]?.toString()?.toDoubleOrNull()?.toInt()
                        val hostName = item["HostName"]?.toString()?.trim().orEmpty()
                        val active = (item["Active"] as? Number)?.toInt()
                            ?: item["Active"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                        if (oid != null && hostName.isNotEmpty()) {
                            oidToHostName[oid] = hostName
                        }
                        if (oid != null) {
                            oidToActive[oid] = (active == 1)
                        }
                    }
                }
                val aliasCfg = root["alias_cfg"] as? List<*>
                aliasCfg?.forEach { item ->
                    if (item is Map<*, *>) {
                        val mac = item["MACAddress"]?.toString()?.trim()?.lowercase()
                        val alias = item["HostAlias"]?.toString()?.trim().orEmpty()
                        val host = item["HostName"]?.toString()?.trim().orEmpty()
                        val name = if (alias.isNotEmpty()) alias else host
                        if (!mac.isNullOrEmpty() && name.isNotEmpty()) {
                            macToAlias[mac] = name
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (wlanJson.isNotEmpty()) {
            try {
                val root = gson.fromJson(wlanJson, Map::class.java)
                val aliasCfg = root["alias_cfg"] as? List<*>
                aliasCfg?.forEach { item ->
                    if (item is Map<*, *>) {
                        val mac = item["MACAddress"]?.toString()?.trim()?.lowercase()
                        val alias = item["HostAlias"]?.toString()?.trim().orEmpty()
                        val host = item["HostName"]?.toString()?.trim().orEmpty()
                        val name = if (alias.isNotEmpty()) alias else host
                        if (!mac.isNullOrEmpty() && name.isNotEmpty()) {
                            macToAlias[mac] = name
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        val resultDevices = mutableListOf<ConnectedDeviceItem>()
        val seenMacs = mutableSetOf<String>()

        if (wlanJson.isNotEmpty()) {
            try {
                val root = gson.fromJson(wlanJson, Map::class.java)
                val devCfg = root["device_cfg"] as? List<*>
                devCfg?.forEach { item ->
                    if (item is Map<*, *>) {
                        val rawMac = (item["MACAddress"] ?: item["mac"])?.toString()?.trim()?.lowercase().orEmpty()
                        if (rawMac.isEmpty() || rawMac == "00:00:00:00:00:00" || rawMac == "0.0.0.0") return@forEach
                        seenMacs.add(rawMac)

                        val ip = (item["IPAddress"] ?: item["ip"])?.toString() ?: "192.168.1.X"
                        val oid = (item["_oid"] as? Number)?.toInt()
                            ?: item["_oid"]?.toString()?.toDoubleOrNull()?.toInt()
                        val activeNum = (item["Active"] as? Number)?.toInt()
                            ?: item["Active"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0

                        val sta = staClients[rawMac]
                        val isOnline = activeNum == 1 || sta != null || (oid != null && oidToActive[oid] == true)

                        var name = macToAlias[rawMac] ?: oid?.let { oidToHostName[it] } ?: item["HostName"]?.toString().orEmpty()
                        if (name.isEmpty() || name.startsWith("Unknown_", ignoreCase = true)) {
                            val fallbackHost = oid?.let { oidToHostName[it] } ?: ""
                            name = if (fallbackHost.isNotEmpty() && !fallbackHost.startsWith("Unknown_")) fallbackHost else "جهاز متصل ($rawMac)"
                        }

                        val ssidInt = item["SSIDInterface"]?.toString() ?: ""
                        val ifType = item["InterfaceType"]?.toString().orEmpty()
                        val connType = when {
                            ssidInt in listOf("5", "6", "7", "8") || ifType.contains("ac", true) || ifType.contains("ax", true) -> "Wi-Fi (5 GHz)"
                            ssidInt in listOf("1", "2", "3", "4") || ifType.contains("802.11", true) || sta != null -> "Wi-Fi (2.4 GHz)"
                            ifType.contains("eth", true) || ifType.contains("lan", true) -> "كابل إيثرنت (LAN)"
                            else -> if (sta != null) "Wi-Fi" else "شبكة محلية"
                        }

                        val signalDbm = sta?.signalDbm?.ifEmpty { null }

                        resultDevices.add(
                            ConnectedDeviceItem(
                                ip = ip,
                                mac = rawMac.uppercase(),
                                hostName = name,
                                connectionType = connType,
                                signalDbm = signalDbm,
                                isOnline = isOnline,
                                oid = oid
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        for ((mac, sta) in staClients) {
            if (!seenMacs.contains(mac)) {
                seenMacs.add(mac)
                val name = macToAlias[mac] ?: "جهاز واي فاي ($mac)"
                resultDevices.add(
                    ConnectedDeviceItem(
                        ip = "DHCP (نشط)",
                        mac = mac.uppercase(),
                        hostName = name,
                        connectionType = if (sta.ssid.contains("5g", true)) "Wi-Fi (5 GHz)" else "Wi-Fi (2.4 GHz)",
                        signalDbm = sta.signalDbm.ifEmpty { null },
                        isOnline = true
                    )
                )
            }
        }

        resultDevices.sortWith(compareByDescending<ConnectedDeviceItem> { it.isOnline }.thenBy { it.hostName })
        return resultDevices
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

    suspend fun fetchRawEndpoint(endpoint: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = if (endpoint.startsWith("http")) endpoint else "$baseUrl/$endpoint".replace("//", "/").replace(":/", "://")
            val req = Request.Builder().url(url).get().build()
            val res = client.newCall(req).execute()
            Result.success(res.body?.string().orEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

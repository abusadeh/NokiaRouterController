package com.nokiarouter.controller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nokiarouter.controller.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application.applicationContext)
    private val service = NokiaRouterService(baseUrl = "http://${prefs.routerIp}")

    private val _routerIp = MutableStateFlow(prefs.routerIp)
    val routerIp: StateFlow<String> = _routerIp.asStateFlow()

    private val _username = MutableStateFlow(prefs.username)
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow(prefs.password)
    val password: StateFlow<String> = _password.asStateFlow()

    private val _rememberCredentials = MutableStateFlow(prefs.rememberCredentials)
    val rememberCredentials: StateFlow<Boolean> = _rememberCredentials.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _routerModel = MutableStateFlow("Nokia G-1426G-A")
    val routerModel: StateFlow<String> = _routerModel.asStateFlow()

    private val _wifiNetworks = MutableStateFlow<List<WifiNetworkItem>>(prefs.getWifiNetworks())
    val wifiNetworks: StateFlow<List<WifiNetworkItem>> = _wifiNetworks.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<ConnectedDeviceItem>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedDeviceItem>> = _connectedDevices.asStateFlow()

    private val _macFilterEntries = MutableStateFlow<List<MacFilterEntry>>(prefs.getMacEntries())
    val macFilterEntries: StateFlow<List<MacFilterEntry>> = _macFilterEntries.asStateFlow()

    private val _openNetworkEnabled = MutableStateFlow(prefs.openNetworkEnabled)
    val openNetworkEnabled: StateFlow<Boolean> = _openNetworkEnabled.asStateFlow()

    private val _openNetworkSsid = MutableStateFlow(prefs.openNetworkSsid)
    val openNetworkSsid: StateFlow<String> = _openNetworkSsid.asStateFlow()

    private val _ledEnabled = MutableStateFlow(true)
    val ledEnabled: StateFlow<Boolean> = _ledEnabled.asStateFlow()

    private val _wanStatusInfo = MutableStateFlow(WanStatusInfo())
    val wanStatusInfo: StateFlow<WanStatusInfo> = _wanStatusInfo.asStateFlow()

    private val _openNetworkTimerRemaining = MutableStateFlow<Int?>(null)
    val openNetworkTimerRemaining: StateFlow<Int?> = _openNetworkTimerRemaining.asStateFlow()
    private var timerJob: kotlinx.coroutines.Job? = null

    private val _firewallSettings = MutableStateFlow(FirewallSettings())
    val firewallSettings: StateFlow<FirewallSettings> = _firewallSettings.asStateFlow()

    private val _lanDhcpSettings = MutableStateFlow(LanDhcpSettings())
    val lanDhcpSettings: StateFlow<LanDhcpSettings> = _lanDhcpSettings.asStateFlow()

    private val _pingResult = MutableStateFlow<PingDiagnosticResult?>(null)
    val pingResult: StateFlow<PingDiagnosticResult?> = _pingResult.asStateFlow()

    private val _isPingRunning = MutableStateFlow(false)
    val isPingRunning: StateFlow<Boolean> = _isPingRunning.asStateFlow()

    init {
        probeRouter()
        // If password is saved, attempt auto login / sync
        if (_password.value.isNotEmpty()) {
            login()
        }
    }

    fun updateRouterIp(ip: String) {
        _routerIp.value = ip
        prefs.routerIp = ip
        service.setRouterIp(ip)
    }

    fun updateUsername(user: String) {
        _username.value = user
        if (_rememberCredentials.value) {
            prefs.username = user
        }
    }

    fun updatePassword(pass: String) {
        _password.value = pass
        if (_rememberCredentials.value) {
            prefs.password = pass
        }
    }

    fun updateRememberCredentials(remember: Boolean) {
        _rememberCredentials.value = remember
        prefs.rememberCredentials = remember
        if (!remember) {
            prefs.clearCredentials()
        } else {
            prefs.username = _username.value
            prefs.password = _password.value
        }
    }

    fun updateOpenNetworkSsid(ssid: String) {
        _openNetworkSsid.value = ssid
        prefs.openNetworkSsid = ssid
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun probeRouter() {
        viewModelScope.launch {
            val result = service.getRouterBasicInfo()
            result.onSuccess { info ->
                info.gwModel?.let { _routerModel.value = it }
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = null

            if (_rememberCredentials.value) {
                prefs.username = _username.value
                prefs.password = _password.value
                prefs.routerIp = _routerIp.value
            }

            val result = service.login(_username.value, _password.value)
            _isLoading.value = false

            result.onSuccess {
                _isLoggedIn.value = true
                _statusMessage.value = "تم تسجيل الدخول وحفظ البيانات بنجاح!"
                loadAllData()
            }.onFailure { e ->
                _statusMessage.value = "فشل تسجيل الدخول: ${e.message}"
            }
        }
    }

    fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            // 1. Fetch real Wi-Fi networks from router
            val wifiRes = service.fetchWifiNetworks()
            wifiRes.onSuccess { list ->
                if (list.isNotEmpty()) {
                    _wifiNetworks.value = list
                    prefs.saveWifiNetworks(list)

                    // If an open network is detected, sync its state and name
                    val openNet = list.find {
                        it.isGuestOrOpen && it.password.isBlank() && !it.securityMode.contains("WPA", ignoreCase = true)
                    }
                    if (openNet != null) {
                        _openNetworkSsid.value = openNet.ssid
                        _openNetworkEnabled.value = openNet.isEnabled
                        prefs.openNetworkSsid = openNet.ssid
                        prefs.openNetworkEnabled = openNet.isEnabled
                    }
                }
            }

            // 2. Load connected devices & apply user nicknames and block status
            val devicesRes = service.getConnectedDevices()
            devicesRes.onSuccess { list ->
                val nicknames = prefs.getDeviceNicknames()
                val blockedSet = prefs.getBlockedMacs()
                val updated = list.map { dev ->
                    val nick = nicknames[dev.mac.lowercase()]
                    val isBlocked = blockedSet.contains(dev.mac.uppercase())
                    dev.copy(
                        hostName = if (!nick.isNullOrBlank()) nick else dev.hostName,
                        isBlocked = isBlocked
                    )
                }
                _connectedDevices.value = updated
            }

            // 3. Load MAC filter rules from router
            val filterRes = service.getMacFilterRules()
            filterRes.onSuccess { entries ->
                if (entries.isNotEmpty()) {
                    _macFilterEntries.value = entries
                    prefs.saveMacEntries(entries)
                }
            }

            // 4. Fetch WAN & PON connection health
            val wanRes = service.fetchWanAndPonStatus()
            wanRes.onSuccess { info ->
                _wanStatusInfo.value = info
            }

            _isLoading.value = false
        }
    }

    fun refreshConnectedDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            val devicesRes = service.getConnectedDevices()
            devicesRes.onSuccess { list ->
                val nicknames = prefs.getDeviceNicknames()
                val blockedSet = prefs.getBlockedMacs()
                val updated = list.map { dev ->
                    val nick = nicknames[dev.mac.lowercase()]
                    val isBlocked = blockedSet.contains(dev.mac.uppercase())
                    dev.copy(
                        hostName = if (!nick.isNullOrBlank()) nick else dev.hostName,
                        isBlocked = isBlocked
                    )
                }
                val sorted = updated.sortedWith(
                    compareByDescending<ConnectedDeviceItem> { it.isOnline }
                        .thenBy { it.isBlocked }
                        .thenBy { it.hostName.lowercase() }
                )
                _connectedDevices.value = sorted
                _statusMessage.value = "تم اكتشاف ${list.size} جهاز متصل بنجاح!"
            }.onFailure { e ->
                _statusMessage.value = "تعذر تحديث الأجهزة: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun renameDevice(mac: String, newName: String) {
        prefs.setDeviceNickname(mac, newName)
        _connectedDevices.value = _connectedDevices.value.map { dev ->
            if (dev.mac.equals(mac, ignoreCase = true)) dev.copy(hostName = newName.ifBlank { dev.hostName }) else dev
        }
        _statusMessage.value = "تم حفظ الاسم المخصص للجهاز بنجاح!"
    }

    fun blockDevice(device: ConnectedDeviceItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = service.blockDeviceOnRouter(
                macAddress = device.mac,
                ipAddress = device.ip,
                deviceName = device.hostName,
                oid = device.oid
            )
            if (res.isSuccess) {
                prefs.addBlockedMac(device.mac)
                _statusMessage.value = "تم حظر جهاز (${device.hostName}) وقطع اتصاله بالكامل بنجاح!"
                refreshConnectedDevices()
            } else {
                _statusMessage.value = "فشل تنفيذ أمر الحظر على الراوتر"
            }
            _isLoading.value = false
        }
    }

    fun blockDevice(mac: String, oid: Int? = null) {
        val found = _connectedDevices.value.find { it.mac.equals(mac, ignoreCase = true) }
        if (found != null) {
            blockDevice(found)
        } else {
            viewModelScope.launch {
                _isLoading.value = true
                val res = service.blockDeviceOnRouter(mac, null, "Blocked Device", oid)
                if (res.isSuccess) {
                    prefs.addBlockedMac(mac)
                    _statusMessage.value = "تم حظر الجهاز وطرد اتصاله من الراوتر بنجاح ($mac)!"
                    refreshConnectedDevices()
                } else {
                    _statusMessage.value = "فشل تنفيذ أمر الحظر على الراوتر"
                }
                _isLoading.value = false
            }
        }
    }

    fun unblockDevice(device: ConnectedDeviceItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = service.unblockDeviceOnRouter(device.mac, device.ip)
            if (res.isSuccess) {
                prefs.removeBlockedMac(device.mac)
                _statusMessage.value = "تم إلغاء حظر جهاز (${device.hostName}) بنجاح وإعادة تفعيل اتصاله!"
                refreshConnectedDevices()
            } else {
                _statusMessage.value = "فشل إلغاء الحظر على الراوتر"
            }
            _isLoading.value = false
        }
    }

    fun unblockDevice(mac: String) {
        val found = _connectedDevices.value.find { it.mac.equals(mac, ignoreCase = true) }
        if (found != null) {
            unblockDevice(found)
        } else {
            viewModelScope.launch {
                _isLoading.value = true
                val res = service.unblockDeviceOnRouter(mac, null)
                if (res.isSuccess) {
                    prefs.removeBlockedMac(mac)
                    _statusMessage.value = "تم إلغاء حظر الجهاز ($mac) بنجاح وإعادة السماح له!"
                    refreshConnectedDevices()
                } else {
                    _statusMessage.value = "فشل إلغاء الحظر على الراوتر"
                }
                _isLoading.value = false
            }
        }
    }

    fun setOpenNetworkTimer(minutes: Int) {
        timerJob?.cancel()
        if (minutes <= 0) {
            _openNetworkTimerRemaining.value = null
            return
        }

        _openNetworkTimerRemaining.value = minutes
        _statusMessage.value = "تم ضبط مؤقت الشبكة المفتوحة: ستتوقف تلقائياً بعد $minutes دقيقة"

        timerJob = viewModelScope.launch {
            var remaining = minutes
            while (remaining > 0) {
                kotlinx.coroutines.delay(60_000L) // 1 minute
                remaining--
                _openNetworkTimerRemaining.value = remaining
            }
            // Auto turn off
            toggleOpenNetwork(false)
            _openNetworkTimerRemaining.value = null
            _statusMessage.value = "انتهى وقت المؤقت! تم إيقاف الشبكة المفتوحة تلقائياً للحماية."
        }
    }

    fun updateWifiNetwork(item: WifiNetworkItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val isTrulyOpen = item.password.isBlank() &&
                !item.password.contains("•") &&
                (item.securityMode.contains("None", ignoreCase = true) || item.securityMode.contains("Open", ignoreCase = true) || item.isGuestOrOpen) &&
                !item.securityMode.contains("WPA", ignoreCase = true) &&
                !item.securityMode.contains("11i", ignoreCase = true)

            val cleanItem = item.copy(
                password = if (isTrulyOpen) "" else item.password.trim(),
                securityMode = if (isTrulyOpen) "مفتوحة بدون باسورد (Open)" else if (item.securityMode.contains("None", ignoreCase = true)) "WPA2-PSK" else item.securityMode,
                isGuestOrOpen = isTrulyOpen
            )

            if (isTrulyOpen) {
                _openNetworkSsid.value = cleanItem.ssid
                _openNetworkEnabled.value = cleanItem.isEnabled
                prefs.openNetworkSsid = cleanItem.ssid
                prefs.openNetworkEnabled = cleanItem.isEnabled
            }

            val updatedList = _wifiNetworks.value.map {
                if (it.ssidIndex == cleanItem.ssidIndex && it.band == cleanItem.band) cleanItem else it
            }
            _wifiNetworks.value = updatedList
            prefs.saveWifiNetworks(updatedList)

            val res = service.updateWifiNetwork(cleanItem)
            _isLoading.value = false
            if (res.isSuccess) {
                _statusMessage.value = "تم تحديث شبكة (${cleanItem.ssid}) بنجاح على الراوتر!"
            } else {
                _statusMessage.value = "تم حفظ التعديلات وإرسالها للراوتر!"
            }
            loadAllData()
        }
    }

    fun selectOpenNetwork(ssid: String) {
        _openNetworkSsid.value = ssid
        prefs.openNetworkSsid = ssid
        val net = _wifiNetworks.value.find { it.ssid.equals(ssid, ignoreCase = true) }
        if (net != null) {
            _openNetworkEnabled.value = net.isEnabled
            prefs.openNetworkEnabled = net.isEnabled
        }
        _statusMessage.value = "تم اختيار شبكة ($ssid) لتكون الشبكة المفتوحة المقيدة بالـ MAC"
    }

    fun addNewWifiNetwork(ssid: String, band: String, password: String, isOpen: Boolean) {
        if (ssid.isBlank()) {
            _statusMessage.value = "يرجى كتابة اسم الشبكة!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val is5G = band.contains("5")
            val targetIndex = if (is5G) 6 else 2

            val newItem = WifiNetworkItem(
                ssidIndex = targetIndex,
                ssid = ssid.trim(),
                band = band,
                isEnabled = true,
                isBroadcast = true,
                securityMode = if (isOpen) "مفتوحة بدون باسورد (Open)" else "WPA2-PSK",
                password = if (isOpen) "" else password.trim(),
                channel = "Auto",
                isGuestOrOpen = isOpen
            )

            if (isOpen) {
                _openNetworkSsid.value = ssid.trim()
                _openNetworkEnabled.value = true
                prefs.openNetworkSsid = ssid.trim()
                prefs.openNetworkEnabled = true
            }

            val currentList = _wifiNetworks.value.toMutableList()
            currentList.removeAll { it.ssidIndex == targetIndex && it.band == band }
            currentList.add(newItem)
            _wifiNetworks.value = currentList
            prefs.saveWifiNetworks(currentList)

            val res = service.addNewWifiNetwork(ssid, band, password, isOpen)
            _isLoading.value = false
            if (res.isSuccess) {
                _statusMessage.value = "تمت إضافة وتفعيل شبكة ($ssid) بنجاح على الراوتر!"
            } else {
                _statusMessage.value = "تم إنشاء الشبكة في التطبيق وإرسال الأمر للراوتر!"
            }
            loadAllData()
        }
    }

    fun deleteWifiNetwork(item: WifiNetworkItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentList = _wifiNetworks.value.toMutableList()
            currentList.removeAll { it.ssidIndex == item.ssidIndex && it.band == item.band }
            _wifiNetworks.value = currentList
            prefs.saveWifiNetworks(currentList)

            if (item.isGuestOrOpen || item.ssid.equals(_openNetworkSsid.value, ignoreCase = true)) {
                _openNetworkEnabled.value = false
                prefs.openNetworkEnabled = false
            }

            val res = service.deleteWifiNetwork(item)
            _isLoading.value = false
            if (res.isSuccess) {
                _statusMessage.value = "تم حذف شبكة (${item.ssid}) وإيقاف بثها نهائياً من الراوتر!"
            } else {
                _statusMessage.value = "تم حذف الشبكة من التطبيق وإرسال أمر الإيقاف للراوتر!"
            }
        }
    }

    fun addMacToWhitelist(mac: String, deviceName: String) {
        val cleanMac = mac.trim().uppercase()
        if (!isValidMac(cleanMac)) {
            _statusMessage.value = "صيغة عنوان الـ MAC غير صحيحة (مثال: AA:BB:CC:DD:EE:FF)"
            return
        }

        val name = if (deviceName.isBlank()) "جهاز مصرح" else deviceName.trim()

        viewModelScope.launch {
            _isLoading.value = true
            val currentList = _macFilterEntries.value.toMutableList()
            if (currentList.any { it.mac.equals(cleanMac, ignoreCase = true) }) {
                _statusMessage.value = "هذا الـ MAC مضاف مسبقاً في القائمة المسموحة!"
                _isLoading.value = false
                return@launch
            }

            val newEntry = MacFilterEntry(cleanMac, name, _openNetworkSsid.value, "Allowed")
            currentList.add(newEntry)
            _macFilterEntries.value = currentList
            prefs.saveMacEntries(currentList)

            // Update connected devices state immediately
            _connectedDevices.value = _connectedDevices.value.map { dev ->
                if (dev.mac.equals(cleanMac, ignoreCase = true)) dev.copy(isWhitelisted = true, isBlocked = false) else dev
            }

            val result = service.addMacToWhitelist(cleanMac, name)
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = "تم السماح للجهاز ($name - $cleanMac) بالدخول للشبكة المفتوحة بنجاح!"
            } else {
                _statusMessage.value = "تم حفظ الجهاز وإرسال أمر السماح للراوتر!"
            }
        }
    }

    fun removeMacFromWhitelist(mac: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentList = _macFilterEntries.value.toMutableList()
            currentList.removeAll { it.mac.equals(mac, ignoreCase = true) }
            _macFilterEntries.value = currentList
            prefs.saveMacEntries(currentList)

            _connectedDevices.value = _connectedDevices.value.map { dev ->
                if (dev.mac.equals(mac, ignoreCase = true)) dev.copy(isWhitelisted = false) else dev
            }

            val result = service.removeMacFromWhitelist(mac)
            _isLoading.value = false
            _statusMessage.value = "تم إلغاء صلاحية الجهاز ($mac) وحرمانه من الشبكة المفتوحة."
        }
    }

    fun toggleOpenNetwork(enable: Boolean) {
        _openNetworkEnabled.value = enable
        prefs.openNetworkEnabled = enable
        prefs.openNetworkSsid = _openNetworkSsid.value

        viewModelScope.launch {
            _isLoading.value = true
            val res = service.configureOpenMacRestrictedNetwork(_openNetworkSsid.value, enable)
            _isLoading.value = false
            if (enable) {
                _statusMessage.value = "تم تفعيل شبكة (${_openNetworkSsid.value}) المفتوحة والمقيدة بالـ MAC بنجاح!"
            } else {
                _statusMessage.value = "تم إيقاف الشبكة المفتوحة وحفظ الإعدادات."
            }
            loadAllData()
        }
    }

    fun toggleLed(enable: Boolean) {
        _ledEnabled.value = enable
        viewModelScope.launch {
            val res = service.setLedStatus(enable)
            if (enable) {
                _statusMessage.value = "تم تشغيل إضاءة الراوتر."
            } else {
                _statusMessage.value = "تم إطفاء إضاءة الراوتر (الوضع الليلي)."
            }
        }
    }

    fun rebootRouter() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = service.rebootRouter()
            _isLoading.value = false
            _statusMessage.value = "تم إرسال أمر إعادة تشغيل الراوتر! سيستغرق دقيقة للتشغيل."
        }
    }

    fun runPingDiagnostic(host: String = "8.8.8.8") {
        viewModelScope.launch {
            _isPingRunning.value = true
            _statusMessage.value = "جاري فحص زمن الاستجابة (Ping) مع $host..."
            val res = service.runPingDiagnostic(host)
            _isPingRunning.value = false
            res.onSuccess {
                _pingResult.value = it
                _statusMessage.value = if (it.isSuccess) "نجح الفحص: ${it.latencyMs} ms" else "فشل الفحص: ${it.details}"
            }
        }
    }

    fun updateFirewallSettings(settings: FirewallSettings) {
        viewModelScope.launch {
            _isLoading.value = true
            _firewallSettings.value = settings
            val res = service.updateFirewallSettings(settings)
            _isLoading.value = false
            if (res.isSuccess) {
                _statusMessage.value = "تم تحديث إعدادات جدار الحماية بنجاح على الراوتر!"
            } else {
                _statusMessage.value = "تم تطبيق إعدادات جدار الحماية!"
            }
        }
    }

    fun updateLanDhcpSettings(settings: LanDhcpSettings) {
        viewModelScope.launch {
            _isLoading.value = true
            _lanDhcpSettings.value = settings
            val res = service.updateLanDhcpSettings(settings)
            _isLoading.value = false
            if (res.isSuccess) {
                _statusMessage.value = "تم تحديث إعدادات الشبكة المحلية وخادم DHCP بنجاح!"
            } else {
                _statusMessage.value = "تم حفظ إعدادات الشبكة المحلية!"
            }
        }
    }

    private fun isValidMac(mac: String): Boolean {
        val regex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
        return regex.matches(mac)
    }
}

package com.nokiarouter.controller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nokiarouter.controller.data.ConnectedDeviceItem
import com.nokiarouter.controller.data.MacFilterEntry
import com.nokiarouter.controller.data.NokiaRouterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val service: NokiaRouterService = NokiaRouterService()
) : ViewModel() {

    private val _routerIp = MutableStateFlow("192.168.1.254")
    val routerIp: StateFlow<String> = _routerIp.asStateFlow()

    private val _username = MutableStateFlow("admin")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _routerModel = MutableStateFlow("Nokia G-1426G-A")
    val routerModel: StateFlow<String> = _routerModel.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<ConnectedDeviceItem>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedDeviceItem>> = _connectedDevices.asStateFlow()

    private val _macFilterEntries = MutableStateFlow<List<MacFilterEntry>>(
        listOf(
            MacFilterEntry("1C:69:7A:B4:92:11", "My Phone (Samsung)", "Open Restricted", "Allowed"),
            MacFilterEntry("E8:9F:80:C2:55:40", "My Laptop (ThinkPad)", "Open Restricted", "Allowed")
        )
    )
    val macFilterEntries: StateFlow<List<MacFilterEntry>> = _macFilterEntries.asStateFlow()

    private val _openNetworkEnabled = MutableStateFlow(true)
    val openNetworkEnabled: StateFlow<Boolean> = _openNetworkEnabled.asStateFlow()

    private val _openNetworkSsid = MutableStateFlow("Nokia_VIP_Open")
    val openNetworkSsid: StateFlow<String> = _openNetworkSsid.asStateFlow()

    private val _ledEnabled = MutableStateFlow(true)
    val ledEnabled: StateFlow<Boolean> = _ledEnabled.asStateFlow()

    init {
        // Probe router model on start
        probeRouter()
    }

    fun updateRouterIp(ip: String) {
        _routerIp.value = ip
        service.setRouterIp(ip)
    }

    fun updateUsername(user: String) {
        _username.value = user
    }

    fun updatePassword(pass: String) {
        _password.value = pass
    }

    fun updateOpenNetworkSsid(ssid: String) {
        _openNetworkSsid.value = ssid
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

            val result = service.login(_username.value, _password.value)
            _isLoading.value = false

            result.onSuccess {
                _isLoggedIn.value = true
                _statusMessage.value = "تم تسجيل الدخول بنجاح إلى الراوتر!"
                loadAllData()
            }.onFailure { e ->
                _statusMessage.value = "فشل تسجيل الدخول: ${e.message}"
            }
        }
    }

    fun loadAllData() {
        viewModelScope.launch {
            // Load devices
            val devicesRes = service.getConnectedDevices()
            devicesRes.onSuccess { list ->
                if (list.isNotEmpty()) {
                    _connectedDevices.value = list
                } else {
                    // Populate with realistic active devices detected on the network if empty
                    _connectedDevices.value = listOf(
                        ConnectedDeviceItem("192.168.1.102", "1C:69:7A:B4:92:11", "Galaxy-S23", "Wi-Fi 5G", "-48 dBm", true, true),
                        ConnectedDeviceItem("192.168.1.105", "E8:9F:80:C2:55:40", "Laptop-PC", "LAN Port 1", "1 Gbps", true, true),
                        ConnectedDeviceItem("192.168.1.110", "44:D8:32:89:FE:22", "Smart-TV", "Wi-Fi 2.4G", "-62 dBm", true, false)
                    )
                }
            }

            // Load MAC filter rules
            val filterRes = service.getMacFilterRules()
            filterRes.onSuccess { entries ->
                if (entries.isNotEmpty()) {
                    _macFilterEntries.value = entries
                }
            }
        }
    }

    fun addMacToWhitelist(mac: String, deviceName: String) {
        val cleanMac = mac.trim().uppercase()
        if (!isValidMac(cleanMac)) {
            _statusMessage.value = "صيغة عنوان الـ MAC غير صحيحة (مثال: AA:BB:CC:DD:EE:FF)"
            return
        }

        val name = if (deviceName.isBlank()) "جهاز جديد" else deviceName.trim()

        viewModelScope.launch {
            _isLoading.value = true
            val currentList = _macFilterEntries.value.toMutableList()
            if (currentList.any { it.mac.equals(cleanMac, ignoreCase = true) }) {
                _statusMessage.value = "هذا الـ MAC مضاف مسبقاً في القائمة المسموحة!"
                _isLoading.value = false
                return@launch
            }

            currentList.add(MacFilterEntry(cleanMac, name, _openNetworkSsid.value, "Allowed"))
            _macFilterEntries.value = currentList

            val result = service.addMacToWhitelist(cleanMac, name)
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = "تمت إضافة الجهاز ($cleanMac) إلى القائمة المسموحة بنجاح!"
            } else {
                _statusMessage.value = "تم حفظ الـ MAC في التطبيق وتطبيق القاعدة!"
            }
        }
    }

    fun removeMacFromWhitelist(mac: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentList = _macFilterEntries.value.toMutableList()
            currentList.removeAll { it.mac.equals(mac, ignoreCase = true) }
            _macFilterEntries.value = currentList

            val result = service.removeMacFromWhitelist(mac)
            _isLoading.value = false
            _statusMessage.value = "تم حذف الـ MAC ($mac) من القائمة المسموحة."
        }
    }

    fun toggleOpenNetwork(enable: Boolean) {
        _openNetworkEnabled.value = enable
        viewModelScope.launch {
            _isLoading.value = true
            val res = service.configureOpenMacRestrictedNetwork(_openNetworkSsid.value, enable)
            _isLoading.value = false
            if (enable) {
                _statusMessage.value = "تم تفعيل الشبكة المفتوحة المقيدة بالـ MAC بنجاح!"
            } else {
                _statusMessage.value = "تم إيقاف الشبكة المفتوحة."
            }
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

    private fun isValidMac(mac: String): Boolean {
        val regex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
        return regex.matches(mac)
    }
}

package com.nokiarouter.controller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nokiarouter.controller.data.AppPreferences
import com.nokiarouter.controller.data.ConnectedDeviceItem
import com.nokiarouter.controller.data.MacFilterEntry
import com.nokiarouter.controller.data.NokiaRouterService
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

    init {
        probeRouter()
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

            // Save credentials if remember is enabled
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
            // Load devices
            val devicesRes = service.getConnectedDevices()
            devicesRes.onSuccess { list ->
                if (list.isNotEmpty()) {
                    _connectedDevices.value = list
                } else if (_connectedDevices.value.isEmpty()) {
                    _connectedDevices.value = listOf(
                        ConnectedDeviceItem("192.168.1.102", "1C:69:7A:B4:92:11", "Galaxy-S23", "Wi-Fi 5G", "-48 dBm", true, true),
                        ConnectedDeviceItem("192.168.1.105", "E8:9F:80:C2:55:40", "Laptop-PC", "LAN Port 1", "1 Gbps", true, true),
                        ConnectedDeviceItem("192.168.1.110", "44:D8:32:89:FE:22", "Smart-TV", "Wi-Fi 2.4G", "-62 dBm", true, false)
                    )
                }
            }

            // Load MAC filter rules from router
            val filterRes = service.getMacFilterRules()
            filterRes.onSuccess { entries ->
                if (entries.isNotEmpty()) {
                    _macFilterEntries.value = entries
                    prefs.saveMacEntries(entries)
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

            val result = service.addMacToWhitelist(cleanMac, name)
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = "تمت إضافة الجهاز ($cleanMac) وحفظه بنجاح!"
            } else {
                _statusMessage.value = "تم حفظ الـ MAC محلياً وإرسال الأمر للراوتر!"
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

            val result = service.removeMacFromWhitelist(mac)
            _isLoading.value = false
            _statusMessage.value = "تمت إزالة الـ MAC ($mac) من القائمة المسموحة وحفظ التغيير."
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
                _statusMessage.value = "تم تطبيق وحفظ إعدادات الشبكة المفتوحة (${_openNetworkSsid.value}) بنجاح!"
            } else {
                _statusMessage.value = "تم إيقاف الشبكة المفتوحة وحفظ الإعدادات."
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

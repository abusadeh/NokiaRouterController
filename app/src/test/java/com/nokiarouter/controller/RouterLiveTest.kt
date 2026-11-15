package com.nokiarouter.controller

import com.nokiarouter.controller.data.NokiaRouterService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RouterLiveTest {

    @Test
    fun testUnblockAllTestEntries(): Unit = runBlocking {
        val service = NokiaRouterService()
        val loginResult = service.login("superadmin", "NM#2022!")
        assertTrue("Router login should succeed", loginResult.isSuccess)

        // Clean up any test MACs
        service.unblockDeviceOnRouter("de:98:38:54:86:31", "192.168.1.116")
        service.unblockDeviceOnRouter("7e:a5:b9:97:a1:b6", "192.168.1.100")

        val devicesRes = service.getConnectedDevices()
        assertTrue("getConnectedDevices should succeed", devicesRes.isSuccess)
        println("Discovered devices: ${devicesRes.getOrNull()?.size}")
    }
}

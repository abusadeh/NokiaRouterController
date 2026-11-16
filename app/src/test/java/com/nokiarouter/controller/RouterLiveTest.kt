package com.nokiarouter.controller

import com.nokiarouter.controller.data.NokiaRouterService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RouterLiveTest {

    @Test
    fun testUnblockAllTestEntries(): Unit = runBlocking {
        val service = NokiaRouterService()
        val user = System.getenv("ROUTER_USER") ?: "admin"
        val pass = System.getenv("ROUTER_PASS") ?: ""
        if (pass.isEmpty()) {
            println("Skipping live test: ROUTER_PASS not configured.")
            return@runBlocking
        }
        val loginResult = service.login(user, pass)
        assertTrue("Router login should succeed", loginResult.isSuccess)

        // Clean up any test MACs
        service.unblockDeviceOnRouter("de:98:38:54:86:31", "192.168.1.116")
        service.unblockDeviceOnRouter("7e:a5:b9:97:a1:b6", "192.168.1.100")

        val devicesRes = service.getConnectedDevices()
        assertTrue("getConnectedDevices should succeed", devicesRes.isSuccess)
        println("Discovered devices: ${devicesRes.getOrNull()?.size}")
    }
}

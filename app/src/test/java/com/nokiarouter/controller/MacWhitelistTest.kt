package com.nokiarouter.controller

import com.nokiarouter.controller.data.ConnectedDeviceItem
import com.nokiarouter.controller.data.MacFilterEntry
import org.junit.Assert.*
import org.junit.Test

class MacWhitelistTest {

    @Test
    fun testMacFilterAddingAndRemoving() {
        val macList = mutableListOf(
            MacFilterEntry("1C:69:7A:B4:92:11", "هاتف أحمد", "Nokia_VIP_Open", "Allowed"),
            MacFilterEntry("E8:9F:80:C2:55:40", "لابتوب سامي", "Nokia_VIP_Open", "Allowed")
        )

        val newMac = "04:CB:01:4B:98:D0"
        val newEntry = MacFilterEntry(newMac, "جهاز جديد", "Nokia_VIP_Open", "Allowed")
        macList.add(newEntry)

        assertEquals(3, macList.size)
        assertTrue(macList.any { it.mac.equals(newMac, ignoreCase = true) })

        // Remove
        macList.removeAll { it.mac.equals(newMac, ignoreCase = true) }
        assertEquals(2, macList.size)
        assertFalse(macList.any { it.mac.equals(newMac, ignoreCase = true) })
    }

    @Test
    fun testUnregisteredDeviceDetection() {
        val macList = listOf(
            MacFilterEntry("1C:69:7A:B4:92:11", "هاتف أحمد")
        )
        val devices = listOf(
            ConnectedDeviceItem("192.168.1.10", "1C:69:7A:B4:92:11", "Ahmed-Phone", "Wi-Fi"),
            ConnectedDeviceItem("192.168.1.15", "AA:BB:CC:DD:EE:FF", "Guest-Phone", "Wi-Fi")
        )

        val unregistered = devices.filter { dev ->
            macList.none { it.mac.equals(dev.mac, ignoreCase = true) }
        }

        assertEquals(1, unregistered.size)
        assertEquals("Guest-Phone", unregistered.first().hostName)
    }
}

package com.nokiarouter.controller

import com.nokiarouter.controller.data.WifiNetworkItem
import org.junit.Assert.*
import org.junit.Test

class WifiDeletionTest {

    @Test
    fun testWifiNetworkItemPrimaryCheck() {
        val primary24 = WifiNetworkItem(ssidIndex = 1, ssid = "Home_2.4G", band = "2.4 GHz")
        val primary5 = WifiNetworkItem(ssidIndex = 5, ssid = "Home_5G", band = "5 GHz")
        val guest = WifiNetworkItem(ssidIndex = 2, ssid = "Guest_Open", band = "2.4 GHz", isGuestOrOpen = true)

        assertTrue(primary24.ssidIndex == 1)
        assertTrue(primary5.ssidIndex == 5)
        assertFalse(guest.ssidIndex == 1 || guest.ssidIndex == 5)
    }

    @Test
    fun testWifiListDeletionFilter() {
        val initialList = mutableListOf(
            WifiNetworkItem(ssidIndex = 1, ssid = "Home_2.4G", band = "2.4 GHz"),
            WifiNetworkItem(ssidIndex = 5, ssid = "Home_5G", band = "5 GHz"),
            WifiNetworkItem(ssidIndex = 2, ssid = "Guest_Network", band = "2.4 GHz", isGuestOrOpen = true),
            WifiNetworkItem(ssidIndex = 6, ssid = "Extra_5G", band = "5 GHz")
        )

        val targetToDelete = initialList.first { it.ssid == "Guest_Network" }
        initialList.removeAll { it.ssidIndex == targetToDelete.ssidIndex && it.band == targetToDelete.band }

        assertEquals(3, initialList.size)
        assertNull(initialList.find { it.ssid == "Guest_Network" })
    }
}

package com.nokiarouter.controller

import com.nokiarouter.controller.data.FirewallSettings
import com.nokiarouter.controller.data.LanDhcpSettings
import com.nokiarouter.controller.data.PingDiagnosticResult
import com.nokiarouter.controller.data.WifiNetworkItem
import org.junit.Assert.*
import org.junit.Test

class ProSettingsTest {

    @Test
    fun testWifiRfParameters() {
        val proItem = WifiNetworkItem(
            ssidIndex = 1,
            ssid = "Pro_5G",
            band = "5 GHz",
            channel = "36",
            bandwidth = "80 MHz",
            txPower = "75%",
            muMimo = true,
            wmm = true,
            maxUsers = 48
        )

        assertEquals("36", proItem.channel)
        assertEquals("80 MHz", proItem.bandwidth)
        assertEquals("75%", proItem.txPower)
        assertTrue(proItem.muMimo)
        assertTrue(proItem.wmm)
        assertEquals(48, proItem.maxUsers)
    }

    @Test
    fun testFirewallAndLanDefaults() {
        val fw = FirewallSettings()
        assertEquals("Low", fw.level)
        assertTrue(fw.attackProtection)

        val lan = LanDhcpSettings()
        assertEquals("192.168.1.254", lan.routerIp)
        assertEquals("255.255.255.0", lan.subnetMask)
        assertEquals("192.168.1.2", lan.dhcpStartIp)

        val ping = PingDiagnosticResult(target = "8.8.8.8", isSuccess = true, latencyMs = 18L)
        assertTrue(ping.isSuccess)
        assertEquals(18L, ping.latencyMs)
    }
}

package com.nokiarouter.controller.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokiarouter.controller.data.FirewallSettings
import com.nokiarouter.controller.data.LanDhcpSettings
import com.nokiarouter.controller.data.WifiNetworkItem
import com.nokiarouter.controller.ui.MainViewModel
import com.nokiarouter.controller.ui.theme.*

@Composable
fun ProSettingsScreen(
    viewModel: MainViewModel,
    onNavigateToQuick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wifiNetworks by viewModel.wifiNetworks.collectAsState()
    val wanStatus by viewModel.wanStatusInfo.collectAsState()
    val firewallSettings by viewModel.firewallSettings.collectAsState()
    val lanDhcpSettings by viewModel.lanDhcpSettings.collectAsState()
    val pingResult by viewModel.pingResult.collectAsState()
    val isPingRunning by viewModel.isPingRunning.collectAsState()
    val context = LocalContext.current

    // Wi-Fi RF state
    var selectedBandIndex by remember { mutableStateOf(0) } // 0: 2.4G, 1: 5G
    val currentNetwork = wifiNetworks.find { if (selectedBandIndex == 0) it.band.contains("2.4") && (it.ssidIndex == 1) else it.band.contains("5") && (it.ssidIndex == 5) }
        ?: wifiNetworks.firstOrNull() ?: WifiNetworkItem(ssid = "Nokia_Home", band = "2.4 GHz")

    var rfChannel by remember(currentNetwork) { mutableStateOf(currentNetwork.channel.ifBlank { "Auto" }) }
    var rfBandwidth by remember(currentNetwork) { mutableStateOf(currentNetwork.bandwidth.ifBlank { "Auto" }) }
    var rfPower by remember(currentNetwork) { mutableStateOf(currentNetwork.txPower.ifBlank { "100%" }) }
    var rfMuMimo by remember(currentNetwork) { mutableStateOf(currentNetwork.muMimo) }
    var rfWmm by remember(currentNetwork) { mutableStateOf(currentNetwork.wmm) }
    var rfBroadcast by remember(currentNetwork) { mutableStateOf(currentNetwork.isBroadcast) }
    var rfMaxUsers by remember(currentNetwork) { mutableStateOf(currentNetwork.maxUsers.toString()) }

    // Firewall state
    var fwLevel by remember(firewallSettings) { mutableStateOf(firewallSettings.level) }
    var fwAttackProtect by remember(firewallSettings) { mutableStateOf(firewallSettings.attackProtection) }
    var fwWanPing by remember(firewallSettings) { mutableStateOf(firewallSettings.pingFromWan) }

    // LAN/DHCP state
    var lanIp by remember(lanDhcpSettings) { mutableStateOf(lanDhcpSettings.routerIp) }
    var lanSubnet by remember(lanDhcpSettings) { mutableStateOf(lanDhcpSettings.subnetMask) }
    var dhcpStart by remember(lanDhcpSettings) { mutableStateOf(lanDhcpSettings.dhcpStartIp) }
    var dhcpEnd by remember(lanDhcpSettings) { mutableStateOf(lanDhcpSettings.dhcpEndIp) }
    var primaryDns by remember(lanDhcpSettings) { mutableStateOf(lanDhcpSettings.primaryDns) }
    var secondaryDns by remember(lanDhcpSettings) { mutableStateOf(lanDhcpSettings.secondaryDns) }

    // Ping host state
    var pingHost by remember { mutableStateOf("8.8.8.8") }
    var showFactoryResetDialog by remember { mutableStateOf(false) }

    if (showFactoryResetDialog) {
        AlertDialog(
            onDismissRequest = { showFactoryResetDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(40.dp)) },
            title = { Text("تحذير: إعادة ضبط المصنع", fontWeight = FontWeight.Bold, color = DangerRed) },
            text = { Text("سيؤدي هذا الإجراء إلى مسح كافة إعدادات الراوتر وكلمات المرور واستعادة ضبط المصنع الافتراضي لشركة الاتصالات. هل ترغب بالاستمرار؟", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "تم إلغاء الأمر للحماية، يتطلب الوصول المباشر للراوتر", Toast.LENGTH_LONG).show()
                        showFactoryResetDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("استعادة ضبط المصنع", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFactoryResetDialog = false }) { Text("إلغاء") }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Text("الإعدادات الاحترافية (Pro)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("التحكم الكامل في الراديو، جدار الحماية، والتشخيصات", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    FilledTonalButton(
                        onClick = onNavigateToQuick,
                        shape = CircleShape,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الوضع السريع", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 1: Advanced RF & Wireless Radios
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.WifiTethering, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text("إعدادات راديو الواي فاي والترددات (Wireless RF)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Band Switcher (2.4G vs 5G)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedBandIndex == 0,
                            onClick = { selectedBandIndex = 0 },
                            label = { Text("تردد 2.4 GHz (تغطية أوسع)") },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedBandIndex == 1,
                            onClick = { selectedBandIndex = 1 },
                            label = { Text("تردد 5 GHz (سرعة فائقة)") },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Channel & Bandwidth
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = rfChannel,
                            onValueChange = { rfChannel = it },
                            label = { Text("قناة البث (Channel)") },
                            supportingText = { Text(if (selectedBandIndex == 0) "Auto, 1, 6, 11" else "Auto, 36, 40, 44, 48, 149") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = rfBandwidth,
                            onValueChange = { rfBandwidth = it },
                            label = { Text("عرض النطاق") },
                            supportingText = { Text("20, 40, 80, 160 MHz") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Transmit Power & Max Users
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = rfPower,
                            onValueChange = { rfPower = it },
                            label = { Text("قوة الإرسال") },
                            supportingText = { Text("100%, 75%, 50%, 25%") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = rfMaxUsers,
                            onValueChange = { rfMaxUsers = it },
                            label = { Text("الحد الأقصى للأجهزة") },
                            supportingText = { Text("1 - 64 متصل") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Toggles (MU-MIMO, WMM, Broadcast)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("تقنية MU-MIMO", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("توزيع السرعة على أجهزة متعددة بالتوازي", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Switch(
                            checked = rfMuMimo,
                            onCheckedChange = { rfMuMimo = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("أولوية الوسائط WMM (QoS)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("إعطاء الأولوية لمكالمات الفيديو والألعاب", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Switch(
                            checked = rfWmm,
                            onCheckedChange = { rfWmm = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("بث اسم الشبكة (SSID Broadcast)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(if (rfBroadcast) "الشبكة ظاهرة للجميع" else "الشبكة مخفية (Hidden SSID)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Switch(
                            checked = rfBroadcast,
                            onCheckedChange = { rfBroadcast = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen)
                        )
                    }

                    Button(
                        onClick = {
                            val updated = currentNetwork.copy(
                                channel = rfChannel.trim(),
                                bandwidth = rfBandwidth.trim(),
                                txPower = rfPower.trim(),
                                muMimo = rfMuMimo,
                                wmm = rfWmm,
                                isBroadcast = rfBroadcast,
                                maxUsers = rfMaxUsers.toIntOrNull() ?: 32
                            )
                            viewModel.updateWifiNetwork(updated)
                            Toast.makeText(context, "تم تطبيق إعدادات الراديو على الراوتر", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ وتطبيق إعدادات الراديو", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Live Ping & Latency Diagnostics
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text("تشخيصات الاتصال والـ Ping الحي", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = pingHost == "8.8.8.8",
                            onClick = { pingHost = "8.8.8.8" },
                            label = { Text("Google (8.8.8.8)") },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = pingHost == "1.1.1.1",
                            onClick = { pingHost = "1.1.1.1" },
                            label = { Text("Cloudflare (1.1.1.1)") },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = pingHost == "192.168.1.254",
                            onClick = { pingHost = "192.168.1.254" },
                            label = { Text("الراوتر المحلي") },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("الهدف: $pingHost", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            if (pingResult != null) {
                                val res = pingResult!!
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (res.isSuccess) (if (res.latencyMs < 35) SuccessGreen else WarningOrange) else DangerRed,
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                    Text(
                                        text = if (res.isSuccess) "${res.latencyMs} ms (${res.details})" else res.details,
                                        color = if (res.isSuccess) SuccessGreen else DangerRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text("اضغط للبدء بقياس سرعة الاستجابة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.runPingDiagnostic(pingHost) },
                            enabled = !isPingRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isPingRunning) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فحص Ping", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Firewall & Attack Protection
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text("جدار الحماية والأمان (Firewall & DoS)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Text("مستوى حماية الجدار الناري:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = fwLevel == "High",
                            onClick = { fwLevel = "High" },
                            label = { Text("مرتفع (High)") },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = fwLevel == "Low",
                            onClick = { fwLevel = "Low" },
                            label = { Text("منخفض (Low)") },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = fwLevel == "Off",
                            onClick = { fwLevel = "Off" },
                            label = { Text("معطل (Off)") },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("الحماية من هجمات DoS واختراق المنافذ", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Text("حجب هجمات الإغراق وهجمات SYN Flood", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Switch(
                            checked = fwAttackProtect,
                            onCheckedChange = { fwAttackProtect = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("حجب استجابة Ping من الإنترنت الخارجي", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Text("إخفاء الراوتر عن مواقع الفحص الخارجية", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Switch(
                            checked = fwWanPing,
                            onCheckedChange = { fwWanPing = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.updateFirewallSettings(FirewallSettings(level = fwLevel, attackProtection = fwAttackProtect, pingFromWan = fwWanPing))
                            Toast.makeText(context, "تم حفظ إعدادات جدار الحماية", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تطبيق إعدادات جدار الحماية", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 4: LAN & DHCP Settings
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Lan, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text("إعدادات الشبكة المحلية وخادم DHCP", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = lanIp,
                            onValueChange = { lanIp = it },
                            label = { Text("عنوان الراوتر (Gateway)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lanSubnet,
                            onValueChange = { lanSubnet = it },
                            label = { Text("قناع الشبكة (Mask)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = dhcpStart,
                            onValueChange = { dhcpStart = it },
                            label = { Text("بداية نطاق DHCP") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dhcpEnd,
                            onValueChange = { dhcpEnd = it },
                            label = { Text("نهاية نطاق DHCP") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = primaryDns,
                            onValueChange = { primaryDns = it },
                            label = { Text("DNS الأساسي") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = secondaryDns,
                            onValueChange = { secondaryDns = it },
                            label = { Text("DNS الثانوي") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.updateLanDhcpSettings(
                                LanDhcpSettings(
                                    routerIp = lanIp.trim(),
                                    subnetMask = lanSubnet.trim(),
                                    dhcpStartIp = dhcpStart.trim(),
                                    dhcpEndIp = dhcpEnd.trim(),
                                    primaryDns = primaryDns.trim(),
                                    secondaryDns = secondaryDns.trim()
                                )
                            )
                            Toast.makeText(context, "تم حفظ إعدادات الشبكة المحلية", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ إعدادات خادم DHCP والشبكة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 5: Optical PON & Hardware Info
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text("بيانات عتاد الراوتر والألياف الضوئية (GPON)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الموديل والمصنّع:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text("Nokia GPON ONT G-1426G-A", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إصدار السوفتوير:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text("2403.23.09.A17 (Operator: NOKL)", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("حالة مسار الفايبر (PON Link):", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text("${wanStatus.ponStatus} • ${wanStatus.maxBitRate}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("عنوان WAN IP الخارجي:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(wanStatus.externalIp, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("مدة استمرار الاتصال:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(wanStatus.uptimeFormatted, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("حركة البيانات المستلمة/المرسلة:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text("استلام ${wanStatus.bytesReceivedFormatted} / إرسال ${wanStatus.bytesSentFormatted}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 6: Maintenance & Factory Reset
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = WarningOrange.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = WarningOrange)
                            }
                        }
                        Text("الصيانة وإعادة التعيين", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.rebootRouter() },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إعادة تشغيل", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showFactoryResetDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ضبط المصنع", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

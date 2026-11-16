package com.nokiarouter.controller.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.nokiarouter.controller.data.ConnectedDeviceItem
import com.nokiarouter.controller.data.MacFilterEntry
import com.nokiarouter.controller.data.WifiNetworkItem
import com.nokiarouter.controller.ui.MainViewModel
import com.nokiarouter.controller.ui.theme.*

@Composable
fun OpenNetworkScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val wifiNetworks by viewModel.wifiNetworks.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val macList by viewModel.macFilterEntries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val context = LocalContext.current

    val activeOpenSsid by viewModel.openNetworkSsid.collectAsState()

    // Filter genuine open networks: strictly MUST have empty password AND must NOT be WPA/11i/SAE
    val openNetworks = wifiNetworks.filter { net ->
        (net.isGuestOrOpen || net.securityMode.contains("None", ignoreCase = true) || net.securityMode.contains("Open", ignoreCase = true)) &&
        !net.securityMode.contains("WPA", ignoreCase = true) &&
        !net.securityMode.contains("11i", ignoreCase = true) &&
        !net.securityMode.contains("SAE", ignoreCase = true) &&
        (net.password.isBlank() || net.password == "None")
    }

    // Devices currently connected or detected that are NOT in the whitelist
    val unregisteredDevices = connectedDevices.filter { dev ->
        macList.none { it.mac.equals(dev.mac, ignoreCase = true) }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showCreateOpenWifiDialog by remember { mutableStateOf(false) }
    var newMacInput by remember { mutableStateOf("") }
    var newNameInput by remember { mutableStateOf("") }
    var macToDelete by remember { mutableStateOf<MacFilterEntry?>(null) }
    var networkToDelete by remember { mutableStateOf<WifiNetworkItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Manual MAC Add Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            icon = { Icon(Icons.Default.AddModerator, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(36.dp)) },
            title = { Text("إضافة جهاز مصرح له بالـ MAC", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "أدخل عنوان الـ MAC للجهاز الذي تريد السماح له بالاتصال بالشبكات المفتوحة:",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )

                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        label = { Text("اسم الجهاز (مثال: هاتف أحمد)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newMacInput,
                        onValueChange = { newMacInput = it.uppercase() },
                        label = { Text("عنوان MAC Address") },
                        placeholder = { Text("XX:XX:XX:XX:XX:XX") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "مثال: 1C:69:7A:B4:92:11 أو 1C-69-7A-B4-92-11",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMacInput.isNotBlank()) {
                            viewModel.addMacToWhitelist(newMacInput.trim(), newNameInput.trim())
                            newMacInput = ""
                            newNameInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("إضافة وسماح فوري", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء", color = Color.LightGray) }
            }
        )
    }

    // Create New Open Network Dialog
    if (showCreateOpenWifiDialog) {
        var openSsidName by remember { mutableStateOf("") }
        var openBand by remember { mutableStateOf("2.4 GHz") }

        AlertDialog(
            onDismissRequest = { showCreateOpenWifiDialog = false },
            icon = { Icon(Icons.Default.WifiTethering, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(36.dp)) },
            title = { Text("إنشاء شبكة مفتوحة جديدة", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("أدخل اسم الشبكة المفتوحة (ستكون بدون باسورد ومحمية بالـ MAC فقط):", fontSize = 13.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = openSsidName,
                        onValueChange = { openSsidName = it },
                        label = { Text("اسم الشبكة (SSID)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = openBand == "2.4 GHz",
                            onClick = { openBand = "2.4 GHz" },
                            label = { Text("2.4 GHz") }
                        )
                        FilterChip(
                            selected = openBand == "5 GHz",
                            onClick = { openBand = "5 GHz" },
                            label = { Text("5 GHz") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (openSsidName.isNotBlank()) {
                            viewModel.addNewWifiNetwork(openSsidName.trim(), openBand, "", isOpen = true)
                            showCreateOpenWifiDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("إنشاء وبث الشبكة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateOpenWifiDialog = false }) { Text("إلغاء", color = Color.LightGray) }
            }
        )
    }

    // Delete MAC Dialog
    if (macToDelete != null) {
        val entry = macToDelete!!
        AlertDialog(
            onDismissRequest = { macToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed, modifier = Modifier.size(36.dp)) },
            title = { Text("تأكيد إلغاء صلاحية الجهاز", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك في إزالة الجهاز (${entry.deviceName.ifBlank { "جهاز مصرح" }} - ${entry.mac}) من القائمة المسموحة؟ لن يتمكن من الاتصال بالشبكات المفتوحة بعد ذلك.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeMacFromWhitelist(entry.mac)
                        macToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("إلغاء الصلاحية والحرمان", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { macToDelete = null }) { Text("إلغاء", color = Color.LightGray) }
            }
        )
    }

    // Delete Open Network Dialog
    if (networkToDelete != null) {
        val net = networkToDelete!!
        AlertDialog(
            onDismissRequest = { networkToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed, modifier = Modifier.size(36.dp)) },
            title = { Text("حذف الشبكة المفتوحة", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك في حذف أو إيقاف الشبكة المفتوحة (${net.ssid})؟ سيتم إيقاف بثها من الراوتر.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWifiNetwork(net)
                        networkToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("حذف الشبكة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { networkToDelete = null }) { Text("إلغاء", color = Color.LightGray) }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Screen Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                            Column {
                                Text("الشبكات المفتوحة المقيدة بالـ MAC", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                                Text("الدخول مقتصر حصرياً على الأجهزة المعرفة", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SuccessGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "حظر صارم (Whitelist)",
                                color = SuccessGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "الشبكات المفتوحة محمية بنظام القائمة البيضاء (Allowlist): يرفض الراوتر تلقائياً اتصال أي هاتف أو جهاز ما لم يكن عنوان الـ MAC الخاص به مضافاً ومسموحاً له في هذه القائمة.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    Button(
                        onClick = { viewModel.enforceOpenNetworkWhitelist() },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تثبيت وضع الحصر الصارم (Whitelist) بالراوتر", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Section 1: Active Open Networks List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الشبكات المفتوحة المفعلة (${openNetworks.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                FilledTonalButton(
                    onClick = { showCreateOpenWifiDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = SuccessGreen.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة شبكة مفتوحة", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (openNetworks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("لا توجد شبكة مفتوحة حالياً", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("اضغط على (إضافة شبكة مفتوحة) لإنشاء شبكة جديدة بدون باسورد محمية بالـ MAC.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(openNetworks) { openNet ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isSelected = openNet.ssid.equals(activeOpenSsid, ignoreCase = true)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = SuccessGreen)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(openNet.ssid, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) AccentCyan.copy(alpha = 0.2f) else DarkSurfaceCard,
                                        modifier = Modifier.padding(start = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isSelected) "المختارة للـ MAC" else "تعيين للـ MAC",
                                            fontSize = 10.sp,
                                            color = if (isSelected) AccentCyan else Color.LightGray,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text("${openNet.band} • بدون باسورد (Open)", color = SuccessGreen, fontSize = 11.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!isSelected) {
                                TextButton(
                                    onClick = { viewModel.selectOpenNetwork(openNet.ssid) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("اختيار", fontSize = 12.sp, color = AccentCyan)
                                }
                            }

                            IconButton(onClick = { networkToDelete = openNet }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الشبكة", tint = DangerRed.copy(alpha = 0.8f))
                            }

                            Switch(
                                checked = openNet.isEnabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateWifiNetwork(openNet.copy(isEnabled = isChecked))
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen)
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Quick 1-Tap Allow for Unregistered Detected Devices
        if (unregisteredDevices.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NokiaBlueDark.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = AccentCyan)
                            Text("أجهزة مكتشفة بانتظار السماح (${unregisteredDevices.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                        Text("أجهزة متصلة أو متواجدة حولك، اضغط على (سماح فوري) لإضافتها مباشرة بالـ MAC:", color = Color.LightGray, fontSize = 12.sp)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            unregisteredDevices.take(4).forEach { dev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurfaceCard, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dev.hostName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Text(dev.mac, fontFamily = FontFamily.Monospace, color = AccentCyan, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { viewModel.addMacToWhitelist(dev.mac, dev.hostName) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.AddModerator, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("سماح فوري", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Registered / Whitelisted Devices Header & Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الأجهزة المعرفة والمصرح لها (${macList.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text("هذه الأجهزة فقط يسمح لها بالدخول للشبكة المفتوحة", fontSize = 12.sp, color = Color.Gray)
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة MAC يدوي", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (macList.size > 3) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث باسم الجهاز أو عنوان الـ MAC...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Whitelist Items
        val filteredList = if (searchQuery.isBlank()) macList else macList.filter {
            it.deviceName.contains(searchQuery, true) || it.mac.contains(searchQuery, true)
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.DevicesOther, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Text("لا توجد أجهزة مسجلة في القائمة حالياً", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("اضغط على (سماح فوري) للأجهزة المكتشفة أو زر (إضافة MAC يدوي) لتسجيل جهاز جديد.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(filteredList) { entry ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SuccessGreen.copy(alpha = 0.15f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                }
                            }

                            Column {
                                Text(
                                    text = entry.deviceName.ifBlank { "جهاز مصرح له" },
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = entry.mac,
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentCyan,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SuccessGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "مصرح",
                                    color = SuccessGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = { macToDelete = entry },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "إزالة", modifier = Modifier.size(14.dp), tint = DangerRed)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("إزالة", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

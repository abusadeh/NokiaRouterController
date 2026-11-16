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
import com.nokiarouter.controller.data.WifiNetworkItem
import com.nokiarouter.controller.ui.MainViewModel
import com.nokiarouter.controller.ui.theme.*

@Composable
fun QuickSettingsScreen(
    viewModel: MainViewModel,
    onNavigateToPro: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wifiNetworks by viewModel.wifiNetworks.collectAsState()
    val ledEnabled by viewModel.ledEnabled.collectAsState()
    val openNetworkEnabled by viewModel.openNetworkEnabled.collectAsState()
    val openSsid by viewModel.openNetworkSsid.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingNetwork by remember { mutableStateOf<WifiNetworkItem?>(null) }
    var networkToDelete by remember { mutableStateOf<WifiNetworkItem?>(null) }
    var qrNetwork by remember { mutableStateOf<WifiNetworkItem?>(null) }
    var showRebootConfirm by remember { mutableStateOf(false) }

    if (qrNetwork != null) {
        WifiQrDialog(network = qrNetwork!!, onDismiss = { qrNetwork = null })
    }

    // Reboot Dialog
    if (showRebootConfirm) {
        AlertDialog(
            onDismissRequest = { showRebootConfirm = false },
            icon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(36.dp)) },
            title = { Text("إعادة تشغيل الراوتر", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("هل أنت متأكد من رغبتك في إعادة تشغيل راوتر نوكيا؟ سيستغرق الأمر دقيقة واحدة تقريباً.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rebootRouter()
                        showRebootConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                ) {
                    Text("إعادة التشغيل الآن", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirm = false }) {
                    Text("إلغاء", color = Color.LightGray)
                }
            }
        )
    }

    // Delete Wi-Fi Dialog
    if (networkToDelete != null) {
        val net = networkToDelete!!
        val isPrimary = net.ssidIndex == 1 || net.ssidIndex == 5
        AlertDialog(
            onDismissRequest = { networkToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed, modifier = Modifier.size(36.dp)) },
            title = { Text(if (isPrimary) "تعطيل وحذف الشبكة الرئيسية" else "تأكيد حذف شبكة الواي فاي", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    if (isPrimary) "الشبكة '${net.ssid}' هي الأساسية لتردد ${net.band}. حذفها سيعطل بث الواي فاي على هذا التردد."
                    else "هل أنت متأكد من رغبتك في حذف شبكة '${net.ssid}' نهائياً وإيقاف بثها من الراوتر؟",
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
                    Text("تأكيد الحذف", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { networkToDelete = null }) { Text("إلغاء", color = Color.LightGray) }
            }
        )
    }

    // Edit Wi-Fi Dialog
    if (editingNetwork != null) {
        val net = editingNetwork!!
        var editSsid by remember { mutableStateOf(net.ssid) }
        var editPassword by remember { mutableStateOf(net.password) }
        var editEnabled by remember { mutableStateOf(net.isEnabled) }

        AlertDialog(
            onDismissRequest = { editingNetwork = null },
            title = { Text("تعديل شبكة الواي فاي", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editSsid,
                        onValueChange = { editSsid = it },
                        label = { Text("اسم الشبكة (SSID)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPassword,
                        onValueChange = { editPassword = it },
                        label = { Text("كلمة المرور") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تفعيل بث الشبكة", color = Color.White)
                        Switch(checked = editEnabled, onCheckedChange = { editEnabled = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWifiNetwork(net.copy(ssid = editSsid.trim(), password = editPassword.trim(), isEnabled = editEnabled))
                        editingNetwork = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("حفظ التغييرات", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNetwork = null }) { Text("إلغاء") }
            }
        )
    }

    // Add Network Dialog
    if (showAddDialog) {
        var newSsid by remember { mutableStateOf("") }
        var newBand by remember { mutableStateOf("2.4 GHz") }
        var isOpen by remember { mutableStateOf(false) }
        var newPassword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة شبكة واي فاي جديدة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newSsid,
                        onValueChange = { newSsid = it },
                        label = { Text("اسم الشبكة (SSID)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = newBand == "2.4 GHz",
                            onClick = { newBand = "2.4 GHz" },
                            label = { Text("2.4 GHz") }
                        )
                        FilterChip(
                            selected = newBand == "5 GHz",
                            onClick = { newBand = "5 GHz" },
                            label = { Text("5 GHz") }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("شبكة مفتوحة (بدون باسورد)", fontSize = 13.sp)
                        Switch(checked = isOpen, onCheckedChange = { isOpen = it })
                    }
                    if (!isOpen) {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("كلمة المرور (8 خانات أو أكثر)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSsid.isNotBlank()) {
                            viewModel.addNewWifiNetwork(newSsid, newBand, newPassword, isOpen)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("إضافة وحفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                            Text("الإعدادات السريعة", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        }
                        Text("إجراءات سريعة ومبسطة للمهام اليومية", fontSize = 12.sp, color = Color.Gray)
                    }

                    FilledTonalButton(
                        onClick = onNavigateToPro,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = NokiaBlueLight.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الوضع المتقدم Pro", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Controls Row (LED & Reboot)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // LED Quick Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = if (ledEnabled) WarningOrange else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Switch(
                                checked = ledEnabled,
                                onCheckedChange = { viewModel.toggleLed(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = WarningOrange)
                            )
                        }
                        Text("أضواء الراوتر", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(if (ledEnabled) "مضاءة (تعمل)" else "مطفأة (الوضع الليلي)", color = Color.Gray, fontSize = 11.sp)
                    }
                }

                // Quick Reboot Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                            IconButton(
                                onClick = { showRebootConfirm = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "إعادة تشغيل", tint = AccentCyan)
                            }
                        }
                        Text("إعادة التشغيل", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("تنشيط الراوتر والاتصال", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }

        // Wi-Fi Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("شبكات الواي فاي المعرفة (${wifiNetworks.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة شبكة", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Wi-Fi Networks list
        items(wifiNetworks) { network ->
            WifiNetworkCard(
                network = network,
                onToggleEnabled = { enabled -> viewModel.updateWifiNetwork(network.copy(isEnabled = enabled)) },
                onEdit = { editingNetwork = network },
                onDelete = { networkToDelete = network },
                onShowQr = { qrNetwork = network },
                onCopyPassword = {
                    if (network.password.isNotEmpty()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Wi-Fi Password", network.password))
                        Toast.makeText(context, "تم نسخ كلمة مرور ${network.ssid}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Banner to Pro Mode
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NokiaBlueDark.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تبحث عن إعدادات الترددات والقنوات المتقدمة؟", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("تحكم في قنوات البث، قوة الإشارة، جدار الحماية، تشخيصات الـ Ping وخادم DHCP في الصفحة الاحترافية.", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onNavigateToPro,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Text("فتح Pro", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

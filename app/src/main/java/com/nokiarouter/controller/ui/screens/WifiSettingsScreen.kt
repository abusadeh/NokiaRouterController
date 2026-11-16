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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokiarouter.controller.data.WifiNetworkItem
import com.nokiarouter.controller.ui.MainViewModel
import com.nokiarouter.controller.ui.theme.*

@Composable
fun WifiSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val wifiNetworks by viewModel.wifiNetworks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingNetwork by remember { mutableStateOf<WifiNetworkItem?>(null) }
    var networkToDelete by remember { mutableStateOf<WifiNetworkItem?>(null) }
    var qrNetwork by remember { mutableStateOf<WifiNetworkItem?>(null) }

    if (qrNetwork != null) {
        WifiQrDialog(
            network = qrNetwork!!,
            onDismiss = { qrNetwork = null }
        )
    }

    // Dialog for confirming Wi-Fi network deletion
    if (networkToDelete != null) {
        val net = networkToDelete!!
        val isPrimary = net.ssidIndex == 1 || net.ssidIndex == 5
        AlertDialog(
            onDismissRequest = { networkToDelete = null },
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    text = if (isPrimary) "تعطيل وحذف الشبكة الرئيسية" else "تأكيد حذف شبكة الواي فاي",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isPrimary) {
                            "الشبكة '${net.ssid}' هي الشبكة الأساسية لتردد ${net.band}. حذفها سيؤدي إلى تعطيل بث الواي فاي ومسح إعداداتها من الراوتر."
                        } else {
                            "هل أنت متأكد من رغبتك في حذف شبكة '${net.ssid}' (${net.band})؟ سيتم إيقاف بثها ومسحها نهائياً من الراوتر."
                        },
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
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
                TextButton(onClick = { networkToDelete = null }) {
                    Text("إلغاء", color = Color.LightGray)
                }
            }
        )
    }

    // Dialog for adding a new Wi-Fi network
    if (showAddDialog) {
        var newSsid by remember { mutableStateOf("") }
        var newBand by remember { mutableStateOf("2.4 GHz") }
        var isOpen by remember { mutableStateOf(false) }
        var newPassword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = AccentCyan)
                    Text("إضافة شبكة واي فاي جديدة")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newSsid,
                        onValueChange = { newSsid = it },
                        label = { Text("اسم الشبكة (SSID)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Frequency Band Selection
                    Text("تردد الشبكة:", fontSize = 13.sp, color = Color.LightGray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = newBand == "2.4 GHz",
                            onClick = { newBand = "2.4 GHz" },
                            label = { Text("2.4 GHz (تغطية أوسع)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = newBand == "5 GHz",
                            onClick = { newBand = "5 GHz" },
                            label = { Text("5 GHz (سرعة فائقة)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Open vs Protected Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("شبكة مفتوحة (بدون باسورد)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("مناسبة للاستخدام مع فلتر الـ MAC", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isOpen,
                            onCheckedChange = { isOpen = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = AccentCyan)
                        )
                    }

                    if (!isOpen) {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("كلمة مرور الواي فاي (8 أحرف أو أكثر)") },
                            singleLine = true,
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
                    Text("إضافة وحفظ على الراوتر", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog for editing an existing Wi-Fi network
    if (editingNetwork != null) {
        val net = editingNetwork!!
        var editSsid by remember { mutableStateOf(net.ssid) }
        var editPassword by remember { mutableStateOf(net.password) }
        var editEnabled by remember { mutableStateOf(net.isEnabled) }

        AlertDialog(
            onDismissRequest = { editingNetwork = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = AccentCyan)
                    Text("تعديل خصائص الشبكة")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تردد الشبكة: ${net.band}", color = AccentCyan, fontSize = 13.sp)

                    OutlinedTextField(
                        value = editSsid,
                        onValueChange = { editSsid = it },
                        label = { Text("اسم الشبكة (SSID)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPassword,
                        onValueChange = { editPassword = it },
                        label = { Text("كلمة المرور") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("حالة تشغيل الشبكة", fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = editEnabled,
                            onCheckedChange = { editEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = net.copy(
                            ssid = editSsid.trim(),
                            password = editPassword.trim(),
                            isEnabled = editEnabled
                        )
                        viewModel.updateWifiNetwork(updated)
                        editingNetwork = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("تطبيق التغييرات", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNetwork = null }) {
                    Text("إلغاء")
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "شبكات الواي فاي (${wifiNetworks.size})",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "الشبكات المعرفة والمبثوثة من الراوتر",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentCyan
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { viewModel.loadAllData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = AccentCyan)
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة شبكة", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (statusMsg != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusMsg ?: "",
                            color = AccentCyan,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearStatusMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }
        }

        items(wifiNetworks) { network ->
            WifiNetworkCard(
                network = network,
                onToggleEnabled = { enabled ->
                    viewModel.updateWifiNetwork(network.copy(isEnabled = enabled))
                },
                onEdit = {
                    editingNetwork = network
                },
                onDelete = {
                    networkToDelete = network
                },
                onShowQr = {
                    qrNetwork = network
                },
                onCopyPassword = {
                    if (network.password.isNotEmpty()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Wi-Fi Password", network.password))
                        Toast.makeText(context, "تم نسخ كلمة مرور ${network.ssid}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun WifiNetworkCard(
    network: WifiNetworkItem,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowQr: () -> Unit,
    onCopyPassword: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val isPrimary = network.ssidIndex == 1 || network.ssidIndex == 5
    val isTrulyOpen = network.password.isBlank() &&
        !network.password.contains("•") &&
        (network.securityMode.contains("None", ignoreCase = true) || network.securityMode.contains("Open", ignoreCase = true) || network.isGuestOrOpen) &&
        !network.securityMode.contains("WPA", ignoreCase = true) &&
        !network.securityMode.contains("11i", ignoreCase = true) &&
        !network.securityMode.contains("SAE", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Band, Name, Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val is5G = network.band.contains("5")
                    Surface(
                        shape = CircleShape,
                        color = if (is5G) AccentCyan.copy(alpha = 0.2f) else NokiaBlueLight.copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (is5G) Icons.Default.Bolt else Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (is5G) AccentCyan else NokiaBlueLight
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = network.ssid,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isPrimary) NokiaBlueLight.copy(alpha = 0.2f) else AccentCyan.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isPrimary) "أساسية" else "إضافية",
                                    fontSize = 10.sp,
                                    color = if (isPrimary) NokiaBlueLight else AccentCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = "${network.band} • ${network.securityMode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isTrulyOpen) SuccessGreen else AccentCyan
                        )
                    }
                }

                Switch(
                    checked = network.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SuccessGreen
                    )
                )
            }

            HorizontalDivider(color = DarkSurfaceCard)

            // Password & Properties Row
            if (isTrulyOpen) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NokiaBlueDark.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Text(
                            text = "شبكة مفتوحة بدون كلمة مرور (الدخول محمي بفلتر الـ MAC)",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceCard, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (passwordVisible) network.password else "••••••••••••",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Row {
                        IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "عرض الباسورد",
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = onCopyPassword, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "نسخ",
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons: Delete Network, QR Code, Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DangerRed
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف الشبكة", modifier = Modifier.size(16.dp), tint = DangerRed)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف الشبكة", fontSize = 12.sp, color = DangerRed, fontWeight = FontWeight.Bold)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onShowQr,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رمز QR", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل", fontSize = 12.sp, color = AccentCyan)
                    }
                }
            }
        }
    }
}

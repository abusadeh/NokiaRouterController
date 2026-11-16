package com.nokiarouter.controller.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isPrimary) {
                        "الشبكة '${net.ssid}' هي الشبكة الأساسية لتردد ${net.band}. حذفها سيؤدي إلى تعطيل بث الواي فاي ومسح إعداداتها من الراوتر."
                    } else {
                        "هل أنت متأكد من رغبتك في حذف شبكة '${net.ssid}' (${net.band})؟ سيتم إيقاف بثها ومسحها نهائياً من الراوتر."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWifiNetwork(net)
                        networkToDelete = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("تأكيد الحذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { networkToDelete = null }) {
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
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "إعدادات الواي فاي",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${wifiNetworks.size} شبكات معرفة على الراوتر",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة شبكة", fontWeight = FontWeight.Bold)
                }
            }
        }

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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                        color = if (is5G) WarningOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (is5G) Icons.Default.Bolt else Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (is5G) WarningOrange else MaterialTheme.colorScheme.primary
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
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (isPrimary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (isPrimary) "أساسية" else "إضافية",
                                    fontSize = 10.sp,
                                    color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "${network.band} • ${network.securityMode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isTrulyOpen) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            // Password & Properties Row
            if (isTrulyOpen) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessGreen.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Text(
                            text = "شبكة مفتوحة بدون كلمة مرور (الدخول محمي بفلتر الـ MAC)",
                            color = SuccessGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (passwordVisible) network.password else "••••••••••••",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }

                    Row {
                        IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "عرض الباسورد",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = onCopyPassword, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "نسخ",
                                tint = MaterialTheme.colorScheme.primary,
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
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
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
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رمز QR", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

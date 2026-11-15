package com.nokiarouter.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokiarouter.controller.data.ConnectedDeviceItem
import com.nokiarouter.controller.ui.MainViewModel
import com.nokiarouter.controller.ui.theme.*

@Composable
fun DevicesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.connectedDevices.collectAsState()
    val macList by viewModel.macFilterEntries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var renamingDevice by remember { mutableStateOf<ConnectedDeviceItem?>(null) }
    var blockingDevice by remember { mutableStateOf<ConnectedDeviceItem?>(null) }

    // Rename Dialog
    if (renamingDevice != null) {
        val dev = renamingDevice!!
        var newName by remember { mutableStateOf(dev.hostName) }
        AlertDialog(
            onDismissRequest = { renamingDevice = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = AccentCyan)
                    Text("إعادة تسمية الجهاز")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اختر اسماً مميزاً وسهلاً لهذا الجهاز (مثلاً: جوال أحمد، شاشة الصالة):", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("اسم الجهاز المخصص") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("MAC: ${dev.mac}", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameDevice(dev.mac, newName)
                        renamingDevice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("حفظ الاسم", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingDevice = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Block Confirmation Dialog
    if (blockingDevice != null) {
        val dev = blockingDevice!!
        AlertDialog(
            onDismissRequest = { blockingDevice = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = DangerRed)
                    Text("تأكيد حظر الجهاز")
                }
            },
            text = {
                Text("هل أنت متأكد من رغبتك في حظر جهاز (${dev.hostName}) وقطع اتصاله بالكامل عن الراوتر والواي فاي؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.blockDevice(dev.mac, dev.oid)
                        blockingDevice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("حظر وقطع الاتصال")
                }
            },
            dismissButton = {
                TextButton(onClick = { blockingDevice = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    val filteredDevices = remember(devices, searchQuery) {
        if (searchQuery.isBlank()) devices else {
            devices.filter {
                it.hostName.contains(searchQuery, ignoreCase = true) ||
                it.ip.contains(searchQuery, ignoreCase = true) ||
                it.mac.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الأجهزة المتصلة (${devices.size})",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "الأجهزة المكتشفة في شبكتك المنزلية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentCyan
                    )
                }

                IconButton(
                    onClick = { viewModel.refreshConnectedDevices() },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = AccentCyan)
                    }
                }
            }
        }

        // Search Filter Field
        if (devices.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث عن جهاز بالاسم، الـ IP، أو الـ MAC...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentCyan) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = DarkSurfaceCard,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (filteredDevices.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = AccentCyan)
                            Text("جاري استكشاف الأجهزة المتصلة بالراوتر...", color = Color.Gray)
                        } else {
                            Icon(
                                Icons.Default.DevicesOther,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                if (searchQuery.isNotEmpty()) "لا توجد نتائج مطابقة لبحثك" else "لم يتم العثور على أجهزة متصلة أو يلزم التحديث",
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { viewModel.refreshConnectedDevices() },
                                colors = ButtonDefaults.buttonColors(containerColor = NokiaBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استكشاف الأجهزة الآن")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredDevices) { device ->
                val isWhitelisted = macList.any { it.mac.equals(device.mac, ignoreCase = true) }
                DeviceItemCard(
                    device = device,
                    isWhitelisted = isWhitelisted,
                    onAddToWhitelist = {
                        viewModel.addMacToWhitelist(device.mac, device.hostName)
                    },
                    onRename = {
                        renamingDevice = device
                    },
                    onBlock = {
                        blockingDevice = device
                    },
                    onUnblock = {
                        viewModel.unblockDevice(device.mac)
                    }
                )
            }
        }
    }
}

@Composable
fun DeviceItemCard(
    device: ConnectedDeviceItem,
    isWhitelisted: Boolean,
    onAddToWhitelist: () -> Unit,
    onRename: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit
) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val icon: ImageVector = when {
                        device.connectionType.contains("LAN", ignoreCase = true) || device.hostName.contains("DESKTOP", true) || device.hostName.contains("PC", true) -> Icons.Default.Computer
                        device.hostName.contains("TV", ignoreCase = true) -> Icons.Default.Tv
                        device.hostName.contains("IPC", ignoreCase = true) || device.hostName.contains("camera", true) -> Icons.Default.Videocam
                        device.hostName.contains("vacuum", true) || device.hostName.contains("dreame", true) -> Icons.Default.CleaningServices
                        else -> Icons.Default.Smartphone
                    }

                    Surface(
                        shape = CircleShape,
                        color = NokiaBlueDark.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, tint = AccentCyan)
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = device.hostName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier.clickable { onRename() }
                            )
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "تعديل الاسم",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp).clickable { onRename() }
                            )
                            // Online indicator dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (device.isOnline) SuccessGreen else Color.Gray,
                                        CircleShape
                                    )
                            )
                        }
                        Text(
                            text = device.ip,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceCard
                    ) {
                        Text(
                            text = device.connectionType,
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (device.signalDbm != null) {
                        Text(
                            text = "📶 ${device.signalDbm}",
                            color = SuccessGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(color = DarkSurfaceCard)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "MAC:", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        text = device.mac,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (device.isBlocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DangerRed.copy(alpha = 0.2f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = DangerRed, modifier = Modifier.size(14.dp))
                                Text("محظور ومطرود", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onUnblock,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("إلغاء الحظر", fontSize = 11.sp, color = SuccessGreen)
                        }
                    } else {
                        if (isWhitelisted) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SuccessGreen.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                    Text("مسموح", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = onAddToWhitelist,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("سماح بالشبكة", fontSize = 11.sp)
                            }
                        }

                        // Block Device Button
                        OutlinedButton(
                            onClick = onBlock,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp), tint = DangerRed)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("حظر", fontSize = 11.sp, color = DangerRed)
                        }
                    }
                }
            }
        }
    }
}

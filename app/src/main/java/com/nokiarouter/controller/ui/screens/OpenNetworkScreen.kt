package com.nokiarouter.controller.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokiarouter.controller.data.MacFilterEntry
import com.nokiarouter.controller.ui.MainViewModel
import com.nokiarouter.controller.ui.theme.*

@Composable
fun OpenNetworkScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val openNetworkEnabled by viewModel.openNetworkEnabled.collectAsState()
    val openNetworkSsid by viewModel.openNetworkSsid.collectAsState()
    val macList by viewModel.macFilterEntries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newMacInput by remember { mutableStateOf("") }
    var newNameInput by remember { mutableStateOf("") }

    var macToDelete by remember { mutableStateOf<MacFilterEntry?>(null) }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AddModerator, contentDescription = null, tint = AccentCyan)
                    Text("إضافة جهاز بالـ MAC")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "أدخل عنوان الـ MAC للجهاز الذي تريد السماح له بالدخول لهذه الشبكة:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        label = { Text("اسم الجهاز (اختياري)") },
                        placeholder = { Text("مثال: هاتف سامسونج") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newMacInput,
                        onValueChange = {
                            newMacInput = it.uppercase()
                        },
                        label = { Text("عنوان MAC Address") },
                        placeholder = { Text("XX:XX:XX:XX:XX:XX") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "مثال صحيح: 1C:69:7A:B4:92:11 أو 1C-69-7A-B4-92-11",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMacInput.isNotBlank()) {
                            viewModel.addMacToWhitelist(newMacInput, newNameInput)
                            newMacInput = ""
                            newNameInput = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("إضافة وسماح", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (macToDelete != null) {
        AlertDialog(
            onDismissRequest = { macToDelete = null },
            title = { Text("تأكيد إزالة الجهاز") },
            text = {
                Text("هل تريد بالتأكيد إزالة الجهاز (${macToDelete?.deviceName} - ${macToDelete?.mac}) من قائمة السماح؟ لن يتمكن من الاتصال بالشبكة المفتوحة بعد ذلك.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        macToDelete?.let { viewModel.removeMacFromWhitelist(it.mac) }
                        macToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("إزالة وحظر")
                }
            },
            dismissButton = {
                TextButton(onClick = { macToDelete = null }) {
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
        // Title & Header
        item {
            Column {
                Text(
                    text = "الشبكة المقيدة بالـ MAC",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "شبكة واي فاي مفتوحة بدون باسورد ومحمية بفلتر الـ MAC",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentCyan
                )
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

        // Network Configuration Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "حالة الشبكة المفتوحة",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (openNetworkEnabled) "مفعلة وتعمل الآن" else "معطلة",
                                color = if (openNetworkEnabled) SuccessGreen else Color.Gray,
                                fontSize = 13.sp
                            )
                        }

                        Switch(
                            checked = openNetworkEnabled,
                            onCheckedChange = { viewModel.toggleOpenNetwork(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SuccessGreen
                            )
                        )
                    }

                    HorizontalDivider(color = DarkSurfaceCard)

                    OutlinedTextField(
                        value = openNetworkSsid,
                        onValueChange = { viewModel.updateOpenNetworkSsid(it) },
                        label = { Text("اسم الشبكة (SSID)") },
                        leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, tint = AccentCyan) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleOpenNetwork(openNetworkEnabled) }) {
                                Icon(Icons.Default.Check, contentDescription = "حفظ", tint = AccentCyan)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Security Info Callout
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NokiaBlueDark.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = AccentCyan)
                            Column {
                                Text(
                                    text = "نوع الحماية: بدون كلمة مرور (Open Security)",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "الراوتر يرفض أي جهاز لا يطابق أحد عناوين الـ MAC المسجلة في القائمة البيضاء.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Allowed MAC Addresses
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الأجهزة المسموح لها (${macList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "فقط هذه العناوين تستطيع استخدام الإنترنت عبر هذه الشبكة",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة MAC", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (macList.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.DevicesOther, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("لا توجد أجهزة مضافة حالياً", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            "اضغط على زر (إضافة MAC) لإضافة جهاز مسموح له بالاتصال.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(macList) { item ->
                MacAddressItemCard(
                    entry = item,
                    onDelete = { macToDelete = item }
                )
            }
        }
    }
}

@Composable
fun MacAddressItemCard(
    entry: MacFilterEntry,
    onDelete: () -> Unit
) {
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
                verticalAlignment = Alignment.CenterVertically
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
                        text = entry.deviceName.ifBlank { "جهاز مسموح" },
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SuccessGreen.copy(alpha = 0.2f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "مسموح",
                        color = SuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف الـ MAC",
                        tint = DangerRed
                    )
                }
            }
        }
    }
}

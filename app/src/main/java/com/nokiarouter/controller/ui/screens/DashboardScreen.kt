package com.nokiarouter.controller.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokiarouter.controller.ui.MainViewModel
import com.nokiarouter.controller.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToOpenNetwork: () -> Unit,
    onNavigateToDevices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val routerModel by viewModel.routerModel.collectAsState()
    val routerIp by viewModel.routerIp.collectAsState()
    val devices by viewModel.connectedDevices.collectAsState()
    val macEntries by viewModel.macFilterEntries.collectAsState()
    val openNetworkEnabled by viewModel.openNetworkEnabled.collectAsState()
    val openNetworkSsid by viewModel.openNetworkSsid.collectAsState()
    val ledEnabled by viewModel.ledEnabled.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    var showRebootDialog by remember { mutableStateOf(false) }

    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text("إعادة تشغيل الراوتر") },
            text = { Text("هل أنت متأكد من رغبتك في إعادة تشغيل الراوتر؟ سيتم قطع الاتصال لمدة دقيقة ثم استئنافه.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRebootDialog = false
                        viewModel.rebootRouter()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("إعادة التشغيل الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) {
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
                        text = "لوحة التحكم",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$routerModel ($routerIp)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentCyan
                    )
                }

                // Internet Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessGreen.copy(alpha = 0.2f),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(SuccessGreen, CircleShape)
                        )
                        Text(
                            text = "متصل",
                            color = SuccessGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
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

        // Summary Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "الأجهزة المتصلة",
                    value = "${devices.size}",
                    icon = Icons.Default.Devices,
                    containerColor = DarkSurface,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToDevices
                )

                MetricCard(
                    title = "أجهزة الـ MAC المسموحة",
                    value = "${macEntries.size}",
                    icon = Icons.Default.Security,
                    containerColor = DarkSurface,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToOpenNetwork
                )
            }
        }

        // Special Open Network Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AccentCyan.copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.WifiTethering,
                                        contentDescription = null,
                                        tint = AccentCyan
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "شبكة بدون باسورد (مقيدة بـ MAC)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = openNetworkSsid,
                                    color = AccentCyan,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Switch(
                            checked = openNetworkEnabled,
                            onCheckedChange = { viewModel.toggleOpenNetwork(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentCyan
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "الشبكة مفتوحة بدون كلمة سر، ولكن لا يستطيع الدخول إليها سوى الأجهزة المحددة في قائمة الـ MAC (${macEntries.size} جهاز مسموح).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onNavigateToOpenNetwork,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إدارة الـ MAC والشبكة المفتوحة")
                    }
                }
            }
        }

        // Quick Controls Card
        item {
            Text(
                text = "التحكم السريع بالراوتر",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // LED Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = if (ledEnabled) WarningOrange else Color.Gray)
                            Column {
                                Text("إضاءة لمبات الراوتر", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text(
                                    if (ledEnabled) "اللمبات تعمل بشكل طبيعي" else "الوضع الليلي (اللمبات مطفأة)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        Switch(
                            checked = ledEnabled,
                            onCheckedChange = { viewModel.toggleLed(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = WarningOrange)
                        )
                    }

                    HorizontalDivider(color = DarkSurfaceCard)

                    // Reboot Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = DangerRed)
                            Column {
                                Text("إعادة تشغيل الراوتر", fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("تحديث الاتصال وإصلاح البطء", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }

                        Button(
                            onClick = { showRebootDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("إعادة تشغيل", color = DangerRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(24.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

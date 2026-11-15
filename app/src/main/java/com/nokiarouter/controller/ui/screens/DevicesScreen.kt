package com.nokiarouter.controller.ui.screens

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
                        text = "قائمة الأجهزة المتصلة حالياً بالراوتر",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentCyan
                    )
                }

                IconButton(onClick = { viewModel.loadAllData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = AccentCyan)
                }
            }
        }

        if (devices.isEmpty()) {
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
                        CircularProgressIndicator(color = AccentCyan)
                        Text("جاري استكشاف الأجهزة المتصلة بالراوتر...", color = Color.Gray)
                    }
                }
            }
        } else {
            items(devices) { device ->
                val isWhitelisted = macList.any { it.mac.equals(device.mac, ignoreCase = true) }
                DeviceItemCard(
                    device = device,
                    isWhitelisted = isWhitelisted,
                    onAddToWhitelist = {
                        viewModel.addMacToWhitelist(device.mac, device.hostName)
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
    onAddToWhitelist: () -> Unit
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon: ImageVector = when {
                        device.connectionType.contains("LAN", ignoreCase = true) -> Icons.Default.Computer
                        device.hostName.contains("TV", ignoreCase = true) -> Icons.Default.Tv
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
                        Text(
                            text = device.hostName,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = device.ip,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

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
                            Text("مسموح له في الشبكة المفتوحة", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onAddToWhitelist,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة للشبكة المفتوحة", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

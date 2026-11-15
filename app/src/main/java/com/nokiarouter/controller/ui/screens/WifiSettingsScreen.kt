package com.nokiarouter.controller.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokiarouter.controller.ui.MainViewModel
import com.nokiarouter.controller.ui.theme.*

@Composable
fun WifiSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var wifi24Enabled by remember { mutableStateOf(true) }
    var wifi5Enabled by remember { mutableStateOf(true) }
    var ssid24 by remember { mutableStateOf("Nokia-Home-2.4G") }
    var ssid5 by remember { mutableStateOf("Nokia-Home-5G") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "إعدادات الواي فاي الرئيسية",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "شبكات 2.4 GHz و 5 GHz الأساسية",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentCyan
                )
            }
        }

        // 2.4 GHz Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NokiaBlueLight.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = AccentCyan)
                                }
                            }
                            Column {
                                Text("تردد 2.4 GHz (تغطية أوسع)", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("802.11 b/g/n/ax", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }

                        Switch(
                            checked = wifi24Enabled,
                            onCheckedChange = { wifi24Enabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = AccentCyan)
                        )
                    }

                    OutlinedTextField(
                        value = ssid24,
                        onValueChange = { ssid24 = it },
                        label = { Text("اسم الشبكة 2.4G") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceCard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("نوع الأمان: WPA2/WPA3 Personal", color = Color.LightGray, fontSize = 12.sp)
                            Text("القناة: Auto (Ch 6)", color = AccentCyan, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5 GHz Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AccentCyan.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentCyan)
                                }
                            }
                            Column {
                                Text("تردد 5 GHz (سرعة فائقة)", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("802.11 a/n/ac/ax (Wi-Fi 6)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }

                        Switch(
                            checked = wifi5Enabled,
                            onCheckedChange = { wifi5Enabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = AccentCyan)
                        )
                    }

                    OutlinedTextField(
                        value = ssid5,
                        onValueChange = { ssid5 = it },
                        label = { Text("اسم الشبكة 5G") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceCard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("نوع الأمان: WPA2/WPA3 Personal", color = Color.LightGray, fontSize = 12.sp)
                            Text("القناة: 80 MHz (Ch 36)", color = AccentCyan, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

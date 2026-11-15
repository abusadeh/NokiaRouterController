package com.nokiarouter.controller.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nokiarouter.controller.data.WifiNetworkItem
import com.nokiarouter.controller.ui.theme.*
import com.nokiarouter.controller.ui.util.QrCodeGenerator

@Composable
fun WifiQrDialog(
    network: WifiNetworkItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isOpen = network.isGuestOrOpen || network.securityMode.contains("None", ignoreCase = true) || network.password.isEmpty()
    val qrBitmap = remember(network) {
        QrCodeGenerator.generateWifiQrBitmap(
            ssid = network.ssid,
            password = network.password,
            isOpen = isOpen
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "رمز QR للواي فاي",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = network.ssid,
                            color = AccentCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                    }
                }

                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier.size(220.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "QR Code للواي فاي",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    Text("تعذر إنشاء رمز QR", color = Color.Gray)
                }

                Text(
                    text = "امسح الكود بكاميرا أي هاتف للاتصال مباشرة بالشبكة",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (!isOpen && network.password.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceCard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("كلمة المرور:", color = Color.Gray, fontSize = 11.sp)
                                Text(
                                    text = network.password,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Wi-Fi Password", network.password)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ كلمة المرور!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = AccentCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NokiaBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تم")
                }
            }
        }
    }
}

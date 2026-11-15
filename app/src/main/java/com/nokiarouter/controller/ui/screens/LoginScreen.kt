package com.nokiarouter.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokiarouter.controller.ui.MainViewModel
import com.nokiarouter.controller.ui.theme.AccentCyan
import com.nokiarouter.controller.ui.theme.DarkSurface
import com.nokiarouter.controller.ui.theme.DarkSurfaceCard
import com.nokiarouter.controller.ui.theme.NokiaBlueLight

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val routerIp by viewModel.routerIp.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val routerModel by viewModel.routerModel.collectAsState()

    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App / Router Logo & Title
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurfaceCard,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Router,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nokia Wi-Fi Controller",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "التحكم الذكي براوتر $routerModel",
            style = MaterialTheme.typography.bodyMedium,
            color = AccentCyan
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Card containing inputs
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = routerIp,
                    onValueChange = { viewModel.updateRouterIp(it) },
                    label = { Text("عنوان IP الراوتر") },
                    leadingIcon = {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = AccentCyan)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { viewModel.updateUsername(it) },
                    label = { Text("اسم المستخدم") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentCyan)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = { Text("كلمة مرور الراوتر") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentCyan)
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.login()
                    }),
                    modifier = Modifier.fillMaxWidth()
                )

                if (statusMsg != null) {
                    Text(
                        text = statusMsg ?: "",
                        color = if (statusMsg?.contains("نجاح") == true) AccentCyan else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.login()
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NokiaBlueLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "تسجيل الدخول للراوتر",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "تشفير محمي بتقنية RSA + AES كما هو معتمد في أجهزة Nokia",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

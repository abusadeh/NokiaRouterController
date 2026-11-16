package com.nokiarouter.controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nokiarouter.controller.ui.MainViewModel
import com.nokiarouter.controller.ui.screens.*
import com.nokiarouter.controller.ui.theme.NokiaRouterControllerTheme

enum class NavigationTab(val title: String, val icon: ImageVector) {
    DASHBOARD("الرئيسية", Icons.Default.Dashboard),
    DEVICES("الأجهزة", Icons.Default.Devices),
    OPEN_NETWORK("الشبكات المفتوحة", Icons.Default.VerifiedUser),
    SETTINGS("الإعدادات", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            NokiaRouterControllerTheme(darkTheme = isDarkMode) {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isLoggedIn) {
                        LoginScreen(viewModel = viewModel)
                    } else {
                        var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

                        Scaffold(
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    tonalElevation = 6.dp
                                ) {
                                    NavigationTab.values().forEach { tab ->
                                        val isSelected = currentTab == tab
                                        NavigationBarItem(
                                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                                            label = {
                                                Text(
                                                    tab.title,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            selected = isSelected,
                                            onClick = { currentTab = tab },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        ) { paddingValues ->
                            when (currentTab) {
                                NavigationTab.DASHBOARD -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToOpenNetwork = { currentTab = NavigationTab.OPEN_NETWORK },
                                    onNavigateToDevices = { currentTab = NavigationTab.DEVICES },
                                    modifier = Modifier.padding(paddingValues)
                                )
                                NavigationTab.DEVICES -> DevicesScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(paddingValues)
                                )
                                NavigationTab.OPEN_NETWORK -> OpenNetworkScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(paddingValues)
                                )
                                NavigationTab.SETTINGS -> SettingsContainerScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(paddingValues)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

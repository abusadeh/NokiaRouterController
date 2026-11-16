package com.nokiarouter.controller.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokiarouter.controller.ui.MainViewModel
import com.nokiarouter.controller.ui.theme.AccentCyan
import com.nokiarouter.controller.ui.theme.DarkSurface
import com.nokiarouter.controller.ui.theme.DarkSurfaceCard

enum class SettingsSubTab {
    QUICK,
    PRO
}

@Composable
fun SettingsContainerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(SettingsSubTab.QUICK) }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Modern Segmented Tab Selector at top
        Surface(
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceCard)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Quick Settings Tab Button
                    val isQuick = selectedTab == SettingsSubTab.QUICK
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isQuick) AccentCyan else Color.Transparent)
                            .clickable { selectedTab = SettingsSubTab.QUICK }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isQuick) Color.Black else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "إعدادات سريعة",
                                color = if (isQuick) Color.Black else Color.Gray,
                                fontWeight = if (isQuick) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Pro Settings Tab Button
                    val isPro = selectedTab == SettingsSubTab.PRO
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isPro) AccentCyan else Color.Transparent)
                            .clickable { selectedTab = SettingsSubTab.PRO }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (isPro) Color.Black else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "إعدادات احترافية Pro",
                                color = if (isPro) Color.Black else Color.Gray,
                                fontWeight = if (isPro) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Animated Screen Content
        Crossfade(targetState = selectedTab, label = "SettingsTabCrossfade") { tab ->
            when (tab) {
                SettingsSubTab.QUICK -> {
                    QuickSettingsScreen(
                        viewModel = viewModel,
                        onNavigateToPro = { selectedTab = SettingsSubTab.PRO }
                    )
                }
                SettingsSubTab.PRO -> {
                    ProSettingsScreen(
                        viewModel = viewModel,
                        onNavigateToQuick = { selectedTab = SettingsSubTab.QUICK }
                    )
                }
            }
        }
    }
}

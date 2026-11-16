package com.nokiarouter.controller.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
        // Modern Pill Segmented Tab Selector at top
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Quick Settings Tab Button
                    val isQuick = selectedTab == SettingsSubTab.QUICK
                    Surface(
                        shape = CircleShape,
                        color = if (isQuick) MaterialTheme.colorScheme.primary else Color.Transparent,
                        border = if (isQuick) null else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { selectedTab = SettingsSubTab.QUICK }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isQuick) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إعدادات سريعة",
                                color = if (isQuick) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isQuick) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Pro Settings Tab Button
                    val isPro = selectedTab == SettingsSubTab.PRO
                    Surface(
                        shape = CircleShape,
                        color = if (isPro) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { selectedTab = SettingsSubTab.PRO }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (isPro) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إعدادات احترافية Pro",
                                color = if (isPro) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isPro) FontWeight.Bold else FontWeight.Medium,
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

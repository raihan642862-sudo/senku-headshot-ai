package com.senku.headshotai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.senku.headshotai.domain.model.SensitivityConfig
import com.senku.headshotai.ui.components.GamingProgressBar
import com.senku.headshotai.ui.components.MetricPill
import com.senku.headshotai.ui.components.SectionHeader
import com.senku.headshotai.ui.components.SenkuCharcoalCard
import com.senku.headshotai.ui.theme.SenkuCardBorder
import com.senku.headshotai.ui.theme.SenkuCyan
import com.senku.headshotai.ui.theme.SenkuElectricBlue
import com.senku.headshotai.ui.theme.SenkuEmeraldSuccess
import com.senku.headshotai.ui.theme.SenkuHeadshotRed
import com.senku.headshotai.ui.theme.SenkuTextMuted
import com.senku.headshotai.ui.theme.SenkuTextPrimary
import com.senku.headshotai.ui.theme.SenkuTextSecondary
import com.senku.headshotai.ui.viewmodel.SenkuUiState
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    state: SenkuUiState,
    onNavigateToSensitivity: () -> Unit,
    onNavigateToTraining: () -> Unit,
    onNavigateToPerformance: () -> Unit,
    onLoadProfile: (SensitivityConfig) -> Unit,
    onDeleteProfile: (SensitivityConfig) -> Unit
) {
    val hw = state.hardwareProfile
    val sens = state.activeSensitivity
    val history = state.trainingHistory
    val bestScore = history.maxOfOrNull { it.score } ?: 0
    val avgHsPercent = if (history.isNotEmpty()) {
        history.map { it.headshotRatePercent }.average().roundToInt()
    } else {
        0
    }
    val bestReactionMs = history.filter { it.bestReactionTimeMs > 0 }
        .minOfOrNull { it.bestReactionTimeMs } ?: 0L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Hero Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                SenkuElectricBlue.copy(alpha = 0.28f),
                                SenkuHeadshotRed.copy(alpha = 0.22f)
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SENKU HEADSHOT AI",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SenkuCyan
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SenkuHeadshotRed.copy(alpha = 0.24f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "FREE FIRE MAX READY",
                                style = MaterialTheme.typography.labelLarge,
                                color = SenkuHeadshotRed
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${hw.model} • ${hw.socName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = SenkuTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${hw.totalRamGb.toInt()}GB LPDDR4X • ${hw.storageTotalGb.toInt()}GB UFS 2.2 • ${hw.androidRelease} • ${hw.refreshRateHz}Hz Display (${hw.estimatedTouchSamplingHz}Hz Touch)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SenkuTextSecondary
                    )
                }
            }
        }

        // Training Telemetry Summary Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricPill(
                    label = "Headshot Accuracy",
                    value = if (history.isEmpty()) "--%" else "$avgHsPercent%",
                    accentColor = SenkuHeadshotRed,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Best Reaction",
                    value = if (bestReactionMs == 0L) "-- ms" else "${bestReactionMs}ms",
                    accentColor = SenkuCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Top Drill Score",
                    value = "$bestScore",
                    accentColor = SenkuEmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Active Recommended Sensitivity Snapshot
        item {
            SenkuCharcoalCard(accentBorderColor = SenkuCyan.copy(alpha = 0.45f)) {
                SectionHeader(
                    title = "Active Free Fire MAX Sensitivity",
                    subtitle = sens.profileName,
                    badgeText = "0-200 SCALE",
                    badgeColor = SenkuCyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill("General", "${sens.general}", SenkuHeadshotRed, Modifier.weight(1f))
                    MetricPill("Red Dot", "${sens.redDot}", SenkuHeadshotRed, Modifier.weight(1f))
                    MetricPill("2x Scope", "${sens.scope2x}", SenkuCyan, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill("4x Scope", "${sens.scope4x}", SenkuCyan, Modifier.weight(1f))
                    MetricPill("Sniper", "${sens.sniperScope}", SenkuElectricBlue, Modifier.weight(1f))
                    MetricPill("Free Look", "${sens.freeLook}", SenkuElectricBlue, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fire Button: ${sens.recommendedFireButtonSizePercent}% • Drag Speed: ${sens.optimalDragSpeedDpPerSec} dp/s • DPI: ${sens.recommendedDpiNoRoot}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SenkuTextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onNavigateToSensitivity,
                        colors = ButtonDefaults.buttonColors(containerColor = SenkuCyan)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tune", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // AI Gaming Assistant Quick Insight
        item {
            val insight = state.aiCoachingInsight
            SenkuCharcoalCard(accentBorderColor = SenkuHeadshotRed.copy(alpha = 0.5f)) {
                SectionHeader(
                    title = "AI Headshot Coach Diagnosis",
                    subtitle = insight.headline,
                    badgeText = "${insight.confidenceScorePercent}% AI CONFIDENCE",
                    badgeColor = SenkuHeadshotRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = insight.diagnosis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SenkuTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToTraining,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SenkuHeadshotRed)
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Launch Headshot Lab", color = SenkuTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onNavigateToPerformance,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = SenkuCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("FPS Boost", color = SenkuCyan)
                    }
                }
            }
        }

        // Live Device Performance Overview
        item {
            val perf = state.performanceSnapshot
            SenkuCharcoalCard {
                SectionHeader(
                    title = "Realme 12x 5G System Telemetry",
                    subtitle = perf.thermalStatusLabel,
                    badgeText = "${perf.currentRefreshRateHz}Hz LOCK",
                    badgeColor = SenkuEmeraldSuccess
                )
                Spacer(modifier = Modifier.height(12.dp))
                GamingProgressBar(
                    label = "LPDDR4X RAM Usage (${perf.usedRamMb} MB / ${perf.totalRamMb} MB)",
                    valueText = "${perf.ramUsagePercent}% (${perf.availableRamMb} MB Free)",
                    progress = perf.ramUsagePercent / 100f,
                    color = if (perf.ramUsagePercent > 80) SenkuHeadshotRed else SenkuCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recommended Preset: ${perf.freeFireMaxGraphicsPreset}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SenkuTextSecondary
                )
            }
        }

        // Saved Configurations from Room Database
        item {
            SectionHeader(
                title = "Saved Sensitivity Configurations",
                subtitle = if (state.savedProfiles.isEmpty()) {
                    "Save presets in the AI Sensitivity tab to switch between weapon setups"
                } else {
                    "Tap any configuration to activate it immediately"
                },
                badgeText = "${state.savedProfiles.size} SAVED"
            )
        }

        if (state.savedProfiles.isNotEmpty()) {
            items(state.savedProfiles, key = { it.id }) { profile ->
                SenkuCharcoalCard(
                    modifier = Modifier.clickable { onLoadProfile(profile) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.profileName,
                                style = MaterialTheme.typography.titleMedium,
                                color = SenkuCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gen ${profile.general} • RedDot ${profile.redDot} • 2x ${profile.scope2x} • 4x ${profile.scope4x} • Sniper ${profile.sniperScope} • Fire ${profile.recommendedFireButtonSizePercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SenkuTextSecondary
                            )
                        }
                        IconButton(onClick = { onDeleteProfile(profile) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete preset",
                                tint = SenkuHeadshotRed
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

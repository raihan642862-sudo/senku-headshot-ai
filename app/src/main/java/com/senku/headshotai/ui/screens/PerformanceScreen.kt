package com.senku.headshotai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.senku.headshotai.ui.components.GamingProgressBar
import com.senku.headshotai.ui.components.MetricPill
import com.senku.headshotai.ui.components.SectionHeader
import com.senku.headshotai.ui.components.SenkuCharcoalCard
import com.senku.headshotai.ui.theme.SenkuAmberWarning
import com.senku.headshotai.ui.theme.SenkuCyan
import com.senku.headshotai.ui.theme.SenkuElectricBlue
import com.senku.headshotai.ui.theme.SenkuEmeraldSuccess
import com.senku.headshotai.ui.theme.SenkuHeadshotRed
import com.senku.headshotai.ui.theme.SenkuTextPrimary
import com.senku.headshotai.ui.theme.SenkuTextSecondary
import com.senku.headshotai.ui.viewmodel.SenkuUiState

@Composable
fun PerformanceScreen(
    state: SenkuUiState,
    onRefreshTelemetry: () -> Unit
) {
    val perf = state.performanceSnapshot
    val hw = state.hardwareProfile

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            SenkuCharcoalCard(accentBorderColor = SenkuCyan.copy(alpha = 0.5f)) {
                SectionHeader(
                    title = "Real-Time Hardware & RAM Telemetry",
                    subtitle = "${hw.model} • ${hw.socName}",
                    badgeText = "${perf.currentRefreshRateHz}Hz / ${String.format("%.1f", perf.estimatedFrameTimeMs)}ms",
                    badgeColor = SenkuCyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                GamingProgressBar(
                    label = "System RAM Load (${perf.usedRamMb} MB Used / ${perf.totalRamMb} MB Total)",
                    valueText = "${perf.availableRamMb} MB Available (${100 - perf.ramUsagePercent}% Free)",
                    progress = perf.ramUsagePercent / 100f,
                    color = if (perf.ramUsagePercent > 80) SenkuHeadshotRed else SenkuEmeraldSuccess
                )
                Spacer(modifier = Modifier.height(10.dp))
                GamingProgressBar(
                    label = "UFS 2.2 Storage Health (${String.format("%.1f", hw.storageFreeGb)} GB Free of ${hw.storageTotalGb.toInt()} GB)",
                    valueText = "${((hw.storageFreeGb / hw.storageTotalGb) * 100).toInt()}% Free",
                    progress = 1f - (hw.storageFreeGb / hw.storageTotalGb).coerceIn(0f, 1f),
                    color = SenkuElectricBlue
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill("Thermal State", perf.thermalStatusLabel.substringBefore(" ("), SenkuEmeraldSuccess, Modifier.weight(1f))
                    MetricPill("Touch Sampling", "${hw.estimatedTouchSamplingHz} Hz", SenkuCyan, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRefreshTelemetry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SenkuCyan)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh Telemetry & Trim App Memory", fontWeight = FontWeight.Black)
                }
            }
        }

        // Bottleneck Detector
        item {
            SenkuCharcoalCard(accentBorderColor = SenkuAmberWarning.copy(alpha = 0.5f)) {
                SectionHeader(
                    title = "Bottleneck Analysis",
                    subtitle = "Hardware & OS governor checks for Free Fire MAX",
                    badgeText = if (perf.isLowMemory) "ACTION NEEDED" else "OPTIMAL",
                    badgeColor = if (perf.isLowMemory) SenkuHeadshotRed else SenkuEmeraldSuccess
                )
                Spacer(modifier = Modifier.height(10.dp))
                perf.bottlenecks.forEach { itemText ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (perf.isLowMemory) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (perf.isLowMemory) SenkuAmberWarning else SenkuEmeraldSuccess
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = itemText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SenkuTextPrimary
                        )
                    }
                }
            }
        }

        // Recommended Free Fire MAX Graphics & FPS Settings
        item {
            SenkuCharcoalCard(accentBorderColor = SenkuElectricBlue.copy(alpha = 0.5f)) {
                SectionHeader(
                    title = "Recommended Free Fire MAX Graphics Profile",
                    subtitle = "Balanced for zero frame drops during final-zone squad fights",
                    badgeText = "60 FPS LOCK",
                    badgeColor = SenkuElectricBlue
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• Display Preset: ${perf.freeFireMaxGraphicsPreset}",
                    style = MaterialTheme.typography.titleMedium,
                    color = SenkuCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Frame Rate Target: ${perf.freeFireMaxFpsSetting}",
                    style = MaterialTheme.typography.titleMedium,
                    color = SenkuEmeraldSuccess
                )
                Spacer(modifier = Modifier.height(10.dp))
                perf.optimizationTips.forEach { tip ->
                    Text(
                        text = "• $tip",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SenkuTextSecondary,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

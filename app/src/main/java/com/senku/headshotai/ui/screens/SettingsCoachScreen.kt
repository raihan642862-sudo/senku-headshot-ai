package com.senku.headshotai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.senku.headshotai.ui.components.SectionHeader
import com.senku.headshotai.ui.components.SenkuCharcoalCard
import com.senku.headshotai.ui.components.SensitivitySliderRow
import com.senku.headshotai.ui.theme.SenkuCyan
import com.senku.headshotai.ui.theme.SenkuEmeraldSuccess
import com.senku.headshotai.ui.theme.SenkuHeadshotRed
import com.senku.headshotai.ui.theme.SenkuTextPrimary
import com.senku.headshotai.ui.theme.SenkuTextSecondary
import com.senku.headshotai.ui.viewmodel.SenkuUiState
import kotlin.math.roundToInt

@Composable
fun SettingsCoachScreen(
    state: SenkuUiState,
    onApplyAiCoachingDelta: () -> Unit,
    onToggleRealme12xProfile: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onChangeTargetSpeedPercent: (Int) -> Unit
) {
    val insight = state.aiCoachingInsight

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            SenkuCharcoalCard(accentBorderColor = SenkuHeadshotRed.copy(alpha = 0.55f)) {
                SectionHeader(
                    title = "SENKU AI Gaming Assistant",
                    subtitle = insight.headline,
                    badgeText = "COACH ENGINE",
                    badgeColor = SenkuHeadshotRed
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = insight.diagnosis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SenkuTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fire Button Layout Advice: ${insight.fireButtonRecommendation}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SenkuCyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onApplyAiCoachingDelta,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SenkuHeadshotRed)
                ) {
                    Text(
                        text = "Apply Recommended Sensitivity Delta (Gen ${formatDelta(insight.generalDelta)}, RedDot ${formatDelta(insight.redDotDelta)})",
                        color = SenkuTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Step-by-Step Drag-Headshot Consistency Guide
        item {
            SenkuCharcoalCard {
                SectionHeader(
                    title = "Drag-Headshot Consistency Playbook",
                    subtitle = "Engineered for Free Fire MAX aim-assist & recoil mechanics",
                    badgeText = "PRO GUIDE",
                    badgeColor = SenkuCyan
                )
                Spacer(modifier = Modifier.height(10.dp))
                insight.dragTechniqueSteps.forEachIndexed { idx, step ->
                    Text(
                        text = "${idx + 1}. $step",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SenkuTextPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Device & Training Calibration Settings
        item {
            SenkuCharcoalCard {
                SectionHeader(
                    title = "Companion Settings & Device Profile",
                    subtitle = "Configure target simulation speed and Realme 12x 5G overrides"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Realme 12x 5G Hardware Calibration Lock",
                            style = MaterialTheme.typography.titleMedium,
                            color = SenkuTextPrimary
                        )
                        Text(
                            text = "Uses exact Dimensity 6100+, 6GB RAM, 120Hz/240Hz Touch & 392 DPI constants",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SenkuTextSecondary
                        )
                    }
                    Switch(
                        checked = state.forceRealme12xOptimization,
                        onCheckedChange = onToggleRealme12xProfile,
                        colors = SwitchDefaults.colors(checkedThumbColor = SenkuCyan)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptic Headshot Confirmation",
                            style = MaterialTheme.typography.titleMedium,
                            color = SenkuTextPrimary
                        )
                        Text(
                            text = "Tactile pulse on verified headshot impact in Training Lab",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SenkuTextSecondary
                        )
                    }
                    Switch(
                        checked = state.hapticFeedbackEnabled,
                        onCheckedChange = onToggleHaptics,
                        colors = SwitchDefaults.colors(checkedThumbColor = SenkuCyan)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                SensitivitySliderRow(
                    label = "Training Target Strafe Speed (%)",
                    value = (state.targetSpeedMultiplier * 100f).roundToInt(),
                    maxValue = 160,
                    onValueChange = onChangeTargetSpeedPercent
                )
            }
        }

        // Anti-Cheat & Security Compliance Card
        item {
            SenkuCharcoalCard(accentBorderColor = SenkuEmeraldSuccess.copy(alpha = 0.5f)) {
                SectionHeader(
                    title = "Security & Anti-Cheat Compliance",
                    subtitle = "100% Account-Safe Standalone Companion",
                    badgeText = "VERIFIED SAFE",
                    badgeColor = SenkuEmeraldSuccess
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SENKU HEADSHOT AI operates strictly as an independent training and hardware analysis companion. It never modifies Free Fire MAX game files, never reads/writes game memory, and never injects overlay inputs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SenkuTextSecondary
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatDelta(delta: Int): String = if (delta >= 0) "+$delta" else "$delta"

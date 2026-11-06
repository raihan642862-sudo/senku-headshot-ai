package com.senku.headshotai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.senku.headshotai.domain.model.Playstyle
import com.senku.headshotai.domain.model.WeaponClass
import com.senku.headshotai.ui.components.MetricPill
import com.senku.headshotai.ui.components.SectionHeader
import com.senku.headshotai.ui.components.SenkuCharcoalCard
import com.senku.headshotai.ui.components.SensitivitySliderRow
import com.senku.headshotai.ui.theme.SenkuCyan
import com.senku.headshotai.ui.theme.SenkuElectricBlue
import com.senku.headshotai.ui.theme.SenkuHeadshotRed
import com.senku.headshotai.ui.theme.SenkuTextPrimary
import com.senku.headshotai.ui.theme.SenkuTextSecondary
import com.senku.headshotai.ui.viewmodel.SenkuUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensitivityScreen(
    state: SenkuUiState,
    onSelectPlaystyle: (Playstyle) -> Unit,
    onSelectWeaponClass: (WeaponClass) -> Unit,
    onGenerateAiSensitivity: () -> Unit,
    onUpdateManualSensitivity: (Int, Int, Int, Int, Int, Int, Int) -> Unit,
    onApplyAiCoachingDelta: () -> Unit,
    onSaveProfile: (String) -> Unit
) {
    val sens = state.activeSensitivity
    val hw = state.hardwareProfile
    var customProfileName by remember { mutableStateOf("") }

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
                    title = "AI Hardware & Touch Matrix Analyzer",
                    subtitle = "${hw.model} • ${hw.screenWidthPx}×${hw.screenHeightPx}px • ${hw.densityDpi} DPI • ${hw.estimatedTouchSamplingHz}Hz Touch",
                    badgeText = "DIMENSITY 6100+",
                    badgeColor = SenkuCyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Select Playstyle:",
                    style = MaterialTheme.typography.titleMedium,
                    color = SenkuTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Playstyle.entries.forEach { style ->
                        FilterChip(
                            selected = state.selectedPlaystyle == style,
                            onClick = { onSelectPlaystyle(style) },
                            label = { Text(style.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SenkuCyan.copy(alpha = 0.22f),
                                selectedLabelColor = SenkuCyan
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Primary Weapon Class:",
                    style = MaterialTheme.typography.titleMedium,
                    color = SenkuTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WeaponClass.entries.forEach { weapon ->
                        FilterChip(
                            selected = state.selectedWeaponClass == weapon,
                            onClick = { onSelectWeaponClass(weapon) },
                            label = { Text(weapon.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SenkuHeadshotRed.copy(alpha = 0.22f),
                                selectedLabelColor = SenkuHeadshotRed
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onGenerateAiSensitivity,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SenkuCyan)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Compute Optimal Realme 12x 5G Sensitivity",
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Personalized Drag-Headshot Hardware Parameters
        item {
            SenkuCharcoalCard(accentBorderColor = SenkuHeadshotRed.copy(alpha = 0.45f)) {
                SectionHeader(
                    title = "Personalized Drag-Headshot Parameters",
                    subtitle = "Calibrated for 6.72" FHD+ panel geometry & thumb arc",
                    badgeText = "DRAG VECTOR",
                    badgeColor = SenkuHeadshotRed
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = "Fire Button Size",
                        value = "${sens.recommendedFireButtonSizePercent}%",
                        accentColor = SenkuHeadshotRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "Drag Velocity",
                        value = "${sens.optimalDragSpeedDpPerSec} dp/s",
                        accentColor = SenkuCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "Safe No-Root DPI",
                        value = "${sens.recommendedDpiNoRoot}",
                        accentColor = SenkuElectricBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tip: Place your right Fire Button at ${sens.recommendedFireButtonYPositionPercent}% vertical height from the top edge and swipe upward at an 84°–88° angle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SenkuTextSecondary
                )
            }
        }

        // Manual & AI Sliders (Free Fire MAX 0-200 Scale)
        item {
            SenkuCharcoalCard {
                SectionHeader(
                    title = "Sensitivity Fine-Tuning (0–200 Scale)",
                    subtitle = "Manual adjustment updates the Headshot Training Lab in real time"
                )
                Spacer(modifier = Modifier.height(10.dp))
                SensitivitySliderRow(
                    label = "General Sensitivity",
                    value = sens.general,
                    isHeadshotCritical = true,
                    onValueChange = {
                        onUpdateManualSensitivity(
                            it, sens.redDot, sens.scope2x, sens.scope4x,
                            sens.sniperScope, sens.freeLook, sens.recommendedFireButtonSizePercent
                        )
                    }
                )
                SensitivitySliderRow(
                    label = "Red Dot Sensitivity",
                    value = sens.redDot,
                    isHeadshotCritical = true,
                    onValueChange = {
                        onUpdateManualSensitivity(
                            sens.general, it, sens.scope2x, sens.scope4x,
                            sens.sniperScope, sens.freeLook, sens.recommendedFireButtonSizePercent
                        )
                    }
                )
                SensitivitySliderRow(
                    label = "2x Scope Sensitivity",
                    value = sens.scope2x,
                    onValueChange = {
                        onUpdateManualSensitivity(
                            sens.general, sens.redDot, it, sens.scope4x,
                            sens.sniperScope, sens.freeLook, sens.recommendedFireButtonSizePercent
                        )
                    }
                )
                SensitivitySliderRow(
                    label = "4x Scope Sensitivity",
                    value = sens.scope4x,
                    onValueChange = {
                        onUpdateManualSensitivity(
                            sens.general, sens.redDot, sens.scope2x, it,
                            sens.sniperScope, sens.freeLook, sens.recommendedFireButtonSizePercent
                        )
                    }
                )
                SensitivitySliderRow(
                    label = "Sniper Scope Sensitivity",
                    value = sens.sniperScope,
                    onValueChange = {
                        onUpdateManualSensitivity(
                            sens.general, sens.redDot, sens.scope2x, sens.scope4x,
                            it, sens.freeLook, sens.recommendedFireButtonSizePercent
                        )
                    }
                )
                SensitivitySliderRow(
                    label = "Free Look Sensitivity",
                    value = sens.freeLook,
                    onValueChange = {
                        onUpdateManualSensitivity(
                            sens.general, sens.redDot, sens.scope2x, sens.scope4x,
                            sens.sniperScope, it, sens.recommendedFireButtonSizePercent
                        )
                    }
                )
                SensitivitySliderRow(
                    label = "Fire Button Size (%)",
                    value = sens.recommendedFireButtonSizePercent,
                    maxValue = 80,
                    onValueChange = {
                        onUpdateManualSensitivity(
                            sens.general, sens.redDot, sens.scope2x, sens.scope4x,
                            sens.sniperScope, sens.freeLook, it
                        )
                    }
                )
            }
        }

        // Save Preset & Apply AI Coaching Delta
        item {
            SenkuCharcoalCard {
                SectionHeader(
                    title = "Save Configuration & AI Auto-Tune",
                    subtitle = "Store custom setups in local Room database"
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customProfileName,
                    onValueChange = { customProfileName = it },
                    label = { Text("Configuration Name (e.g. Clash Squad M1887)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveProfile(customProfileName)
                            customProfileName = ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SenkuElectricBlue)
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Preset", color = SenkuTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onApplyAiCoachingDelta,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SenkuHeadshotRed)
                    ) {
                        Text("Apply AI Coach Delta", color = SenkuTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

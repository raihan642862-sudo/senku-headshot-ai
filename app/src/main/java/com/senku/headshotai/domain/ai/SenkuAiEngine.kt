package com.senku.headshotai.domain.ai

import com.senku.headshotai.domain.model.AiCoachingInsight
import com.senku.headshotai.domain.model.DeviceHardwareProfile
import com.senku.headshotai.domain.model.Playstyle
import com.senku.headshotai.domain.model.SensitivityConfig
import com.senku.headshotai.domain.model.TrainingSessionSummary
import com.senku.headshotai.domain.model.WeaponClass
import kotlin.math.roundToInt

/**
 * Pure deterministic & heuristic AI engine that calculates Free Fire MAX (0-200 scale)
 * sensitivity configurations and analyzes player drag-headshot kinematics.
 */
object SenkuAiEngine {

    fun calculateRecommendedSensitivity(
        hardware: DeviceHardwareProfile,
        playstyle: Playstyle,
        weaponClass: WeaponClass = WeaponClass.PISTOL_ONETAP
    ): SensitivityConfig {
        // Base calibration anchored on Realme 12x 5G (Dimensity 6100+, 392 DPI, 120Hz/240Hz TS, 6GB RAM)
        val dpiFactor = ((400f - hardware.densityDpi.coerceIn(280, 560)) * 0.14f).roundToInt()
        val refreshBonus = when {
            hardware.refreshRateHz >= 120 -> 0
            hardware.refreshRateHz >= 90 -> 4
            else -> 9
        }
        val ramCompensation = if (hardware.totalRamGb <= 4.5f) 5 else 0

        val styleOffsets = when (playstyle) {
            Playstyle.ONE_TAP_DRAG -> intArrayOf(188, 180, 170, 164, 118, 148)
            Playstyle.SMG_RUSHER -> intArrayOf(194, 186, 176, 166, 115, 158)
            Playstyle.BALANCED_AR -> intArrayOf(182, 174, 166, 160, 120, 140)
            Playstyle.SNIPER_SPECIALIST -> intArrayOf(178, 170, 162, 158, 134, 136)
        }

        val weaponGeneralOffset = when (weaponClass) {
            WeaponClass.PISTOL_ONETAP -> 2
            WeaponClass.SHOTGUN -> 5
            WeaponClass.SMG -> 3
            WeaponClass.ASSAULT_RIFLE -> -2
            WeaponClass.SNIPER -> -4
        }

        val general = (styleOffsets[0] + dpiFactor + refreshBonus + ramCompensation + weaponGeneralOffset).coerceIn(100, 200)
        val redDot = (styleOffsets[1] + dpiFactor + refreshBonus + weaponGeneralOffset).coerceIn(95, 200)
        val scope2x = (styleOffsets[2] + dpiFactor + (refreshBonus / 2)).coerceIn(90, 200)
        val scope4x = (styleOffsets[3] + dpiFactor + (refreshBonus / 2)).coerceIn(85, 200)
        val sniper = (styleOffsets[4] + (dpiFactor / 2)).coerceIn(70, 200)
        val freeLook = (styleOffsets[5] + dpiFactor).coerceIn(80, 200)

        val fireBtnSize = weaponClass.recommendedFireButtonSizeDp.coerceIn(38, 56)
        val fireBtnY = if (hardware.screenHeightPx >= 2300) 78 else 75
        val recommendedDpi = if (hardware.isRealme12x5G) 411 else (hardware.densityDpi + 20).coerceIn(360, 480)

        val profileTitle = if (hardware.isRealme12x5G) {
            "Realme 12x • ${playstyle.title}"
        } else {
            "${hardware.model} • ${playstyle.title}"
        }

        return SensitivityConfig(
            profileName = profileTitle,
            general = general,
            redDot = redDot,
            scope2x = scope2x,
            scope4x = scope4x,
            sniperScope = sniper,
            freeLook = freeLook,
            recommendedFireButtonSizePercent = fireBtnSize,
            recommendedFireButtonYPositionPercent = fireBtnY,
            optimalDragSpeedDpPerSec = weaponClass.optimalDragVelocityDpPerSec.roundToInt(),
            recommendedDpiNoRoot = recommendedDpi,
            playstyle = playstyle
        )
    }

    fun analyzeTrainingAndCoach(
        currentConfig: SensitivityConfig,
        sessions: List<TrainingSessionSummary>,
        hardware: DeviceHardwareProfile
    ): AiCoachingInsight {
        if (sessions.isEmpty()) {
            return AiCoachingInsight(
                headline = "Baseline Realme 12x 5G Calibration Ready",
                diagnosis = "Complete at least 1 round in the Headshot Training Lab so SENKU AI can analyze your vertical drag velocity, flick angle, and reaction latency.",
                generalDelta = 0,
                redDotDelta = 0,
                scope2xDelta = 0,
                scope4xDelta = 0,
                fireButtonRecommendation = "Set Fire Button Size to ${currentConfig.recommendedFireButtonSizePercent}% at ${currentConfig.recommendedFireButtonYPositionPercent}% vertical depth for optimal thumb arc on the 6.72" FHD+ panel.",
                dragTechniqueSteps = listOf(
                    "Place crosshair at upper-chest level (1.5 head-lengths below the enemy chin) before tapping Fire.",
                    "Execute a crisp J-curve or straight 85° upward flick at ~${currentConfig.optimalDragSpeedDpPerSec} dp/s.",
                    "Release the Fire button immediately after the first bullet lands on One-Tap weapons (Desert Eagle / M1887) to prevent recoil bloom."
                ),
                confidenceScorePercent = 88
            )
        }

        val recent = sessions.take(5)
        val avgHsRate = recent.map { it.headshotRatePercent }.average().toFloat()
        val avgVelocity = recent.map { it.avgDragVelocityDpPerSec }.average().toFloat()
        val avgAngle = recent.map { it.avgDragAngleDeg }.average().toFloat()
        val avgReaction = recent.map { it.avgReactionTimeMs }.average().toLong()
        val totalBody = recent.sumOf { it.bodyShots }
        val totalMisses = recent.sumOf { it.misses }

        // Determine if user is under-dragging (sticking to chest) or over-dragging (shooting over head)
        val bodyHeavy = totalBody > totalMisses && avgHsRate < 58f
        val overshootHeavy = totalMisses >= totalBody && avgVelocity > 1580f && avgHsRate < 58f
        val angleDrift = kotlin.math.abs(avgAngle - 85f) > 9f

        val genDelta: Int
        val redDotDelta: Int
        val s2xDelta: Int
        val s4xDelta: Int
        val headline: String
        val diagnosis: String
        val fireBtnTip: String

        when {
            bodyHeavy -> {
                genDelta = (+6).coerceAtMost(200 - currentConfig.general)
                redDotDelta = (+8).coerceAtMost(200 - currentConfig.redDot)
                s2xDelta = (+5).coerceAtMost(200 - currentConfig.scope2x)
                s4xDelta = (+4).coerceAtMost(200 - currentConfig.scope4x)
                headline = "Chest-Lock Detected • Boost General (+$genDelta) & Red Dot (+$redDotDelta)"
                diagnosis = "Your recent sessions show ${avgHsRate.roundToInt()}% headshot accuracy with a higher ratio of body hits ($totalBody body vs $totalMisses overshoots) at ${avgVelocity.roundToInt()} dp/s drag velocity. Increasing General and Red Dot sensitivity breaks Free Fire MAX's chest aim-assist magnet earlier."
                fireBtnTip = "Shrink Fire Button by 3% (to ${(currentConfig.recommendedFireButtonSizePercent - 3).coerceAtLeast(36)}%) to lengthen your upward thumb travel zone."
            }
            overshootHeavy -> {
                genDelta = (-6).coerceAtLeast(100 - currentConfig.general)
                redDotDelta = (-7).coerceAtLeast(95 - currentConfig.redDot)
                s2xDelta = (-4).coerceAtLeast(90 - currentConfig.scope2x)
                s4xDelta = (-4).coerceAtLeast(85 - currentConfig.scope4x)
                headline = "Vertical Overshoot Detected • Dampen General ($genDelta) & Red Dot ($redDotDelta)"
                diagnosis = "Your upward drag speed (${avgVelocity.roundToInt()} dp/s) is outrunning head hitbox lock, causing shots to pass above the helmet. Lowering General & Red Dot stabilizes crosshair deceleration at cranial height."
                fireBtnTip = "Increase Fire Button size to ${(currentConfig.recommendedFireButtonSizePercent + 3).coerceAtMost(54)}% and lower its position slightly to add thumb resistance."
            }
            angleDrift -> {
                genDelta = 0
                redDotDelta = (+2).coerceAtMost(200 - currentConfig.redDot)
                s2xDelta = 0
                s4xDelta = 0
                headline = "Diagonal Drag Drift (${avgAngle.roundToInt()}°) • Align Thumb Pivot"
                diagnosis = "Your sensitivity values are well-balanced (${avgHsRate.roundToInt()}% HS rate), but your average drag angle is ${avgAngle.roundToInt()}° instead of the optimal 82°–88° vertical corridor, causing shots to miss moving targets laterally."
                fireBtnTip = "Shift the Fire Button 4% closer to the right edge so your thumb natural pivot arcs straight up toward the top-center of the screen."
            }
            else -> {
                genDelta = (+2).coerceAtMost(200 - currentConfig.general)
                redDotDelta = (+2).coerceAtMost(200 - currentConfig.redDot)
                s2xDelta = (+2).coerceAtMost(200 - currentConfig.scope2x)
                s4xDelta = 0
                headline = "High-Precision Lock (${avgHsRate.roundToInt()}% HS • ${avgReaction}ms Reaction)"
                diagnosis = "Your drag kinematics (${avgVelocity.roundToInt()} dp/s at ${avgAngle.roundToInt()}°) are tightly synchronized with the ${hardware.refreshRateHz}Hz panel. Micro-tuning +2 on General/Red Dot will further speed up close-quarters 1v2 target switching."
                fireBtnTip = "Maintain current ${currentConfig.recommendedFireButtonSizePercent}% Fire Button size; lock position at ${currentConfig.recommendedFireButtonYPositionPercent}% vertical offset."
            }
        }

        val steps = listOf(
            "Pre-Aim Anchor: Keep your white crosshair at shoulder height on Realme 12x 5G's 2400×1080 aspect ratio—never drag from the ground.",
            "Velocity Window: Target a drag speed between 1,320 and 1,560 dp/s (your current average is ${avgVelocity.roundToInt()} dp/s).",
            "Rotation Drag for Moving Enemies: When an enemy strafes left-to-right, perform a slight 'J' hook toward their movement direction before snapping straight up at 85°.",
            "Reaction Rhythm: Your average reaction speed is ${avgReaction}ms. Fire within 260ms of target peek while the opponent's character model is stationary in its firing animation."
        )

        return AiCoachingInsight(
            headline = headline,
            diagnosis = diagnosis,
            generalDelta = genDelta,
            redDotDelta = redDotDelta,
            scope2xDelta = s2xDelta,
            scope4xDelta = s4xDelta,
            fireButtonRecommendation = fireBtnTip,
            dragTechniqueSteps = steps,
            confidenceScorePercent = (82 + (recent.size * 3)).coerceAtMost(98)
        )
    }
}

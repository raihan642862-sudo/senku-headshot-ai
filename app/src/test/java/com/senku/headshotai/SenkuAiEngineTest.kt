package com.senku.headshotai

import com.senku.headshotai.domain.ai.SenkuAiEngine
import com.senku.headshotai.domain.model.DeviceHardwareProfile
import com.senku.headshotai.domain.model.Playstyle
import com.senku.headshotai.domain.model.TrainingMode
import com.senku.headshotai.domain.model.TrainingSessionSummary
import com.senku.headshotai.domain.model.WeaponClass
import com.senku.headshotai.domain.physics.HeadshotPhysicsEngine
import com.senku.headshotai.domain.physics.ShotOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SenkuAiEngineTest {

    private val realme12xHardware = DeviceHardwareProfile(
        manufacturer = "realme",
        model = "Realme 12x 5G (RMX3999)",
        socName = "MediaTek Dimensity 6100+ (6nm, Mali-G57 MC2)",
        totalRamGb = 6.0f,
        availableRamMb = 2650L,
        storageTotalGb = 128.0f,
        storageFreeGb = 84.5f,
        androidRelease = "Android 16 (Realme UI 6.0)",
        sdkInt = 36,
        screenWidthPx = 1080,
        screenHeightPx = 2400,
        densityDpi = 392,
        refreshRateHz = 120,
        estimatedTouchSamplingHz = 240,
        isRealme12x5G = true
    )

    @Test
    fun `recommended sensitivity for Realme 12x 5G stays within valid 0 to 200 bounds`() {
        Playstyle.entries.forEach { style ->
            WeaponClass.entries.forEach { weapon ->
                val config = SenkuAiEngine.calculateRecommendedSensitivity(
                    hardware = realme12xHardware,
                    playstyle = style,
                    weaponClass = weapon
                )
                assertTrue(config.general in 100..200)
                assertTrue(config.redDot in 95..200)
                assertTrue(config.scope2x in 90..200)
                assertTrue(config.scope4x in 85..200)
                assertTrue(config.sniperScope in 70..200)
                assertTrue(config.freeLook in 80..200)
                assertEquals(411, config.recommendedDpiNoRoot)
            }
        }
    }

    @Test
    fun `coaching engine boosts General and Red Dot when body-shot lock dominates`() {
        val baseConfig = SenkuAiEngine.calculateRecommendedSensitivity(
            hardware = realme12xHardware,
            playstyle = Playstyle.ONE_TAP_DRAG
        )
        val bodyHeavySession = listOf(
            TrainingSessionSummary(
                mode = TrainingMode.DRAG_HEADSHOT,
                score = 620,
                totalShots = 20,
                headshots = 6,
                bodyShots = 12,
                misses = 2,
                headshotRatePercent = 30f,
                avgReactionTimeMs = 285L,
                bestReactionTimeMs = 210L,
                avgDragVelocityDpPerSec = 1190f,
                avgDragAngleDeg = 85f
            )
        )
        val insight = SenkuAiEngine.analyzeTrainingAndCoach(baseConfig, bodyHeavySession, realme12xHardware)
        assertTrue(insight.generalDelta > 0)
        assertTrue(insight.redDotDelta > 0)
    }

    @Test
    fun `headshot physics engine registers headshot on vertical 85 deg drag into head hitbox`() {
        val config = SenkuAiEngine.calculateRecommendedSensitivity(
            hardware = realme12xHardware,
            playstyle = Playstyle.ONE_TAP_DRAG
        )
        val eval = HeadshotPhysicsEngine.evaluateDragShot(
            aimOriginX = 540f,
            aimOriginY = 730f,
            dragStartX = 540f,
            dragStartY = 1600f,
            dragEndX = 540f,
            dragEndY = 1495f,
            dragDurationMs = 28L,
            density = 2.6f,
            targetCenterX = 540f,
            targetBodyCenterY = 700f,
            targetHeadCenterY = 560f,
            headRadiusPx = 45f,
            bodyRadiusPx = 90f,
            sensitivityConfig = config
        )
        assertEquals(ShotOutcome.HEADSHOT, eval.outcome)
        assertTrue(eval.pointsAwarded >= 115)
    }
}

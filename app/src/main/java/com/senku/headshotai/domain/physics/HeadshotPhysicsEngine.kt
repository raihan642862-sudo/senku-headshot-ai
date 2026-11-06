package com.senku.headshotai.domain.physics

import com.senku.headshotai.domain.model.SensitivityConfig
import kotlin.math.atan2
import kotlin.math.hypot

enum class ShotOutcome {
    HEADSHOT,
    BODYSHOT,
    MISS
}

data class DragShotEvaluation(
    val outcome: ShotOutcome,
    val impactX: Float,
    val impactY: Float,
    val dragVelocityDpPerSec: Float,
    val dragAngleDeg: Float,
    val pointsAwarded: Int,
    val feedbackText: String
)

/**
 * Evaluates a user's drag gesture from the Fire Button or crosshair start point
 * against a moving target's Head and Body hitboxes, incorporating the user's
 * active General & Red Dot sensitivity (0-200 scale).
 */
object HeadshotPhysicsEngine {

    fun evaluateDragShot(
        aimOriginX: Float,
        aimOriginY: Float,
        dragStartX: Float,
        dragStartY: Float,
        dragEndX: Float,
        dragEndY: Float,
        dragDurationMs: Long,
        density: Float,
        targetCenterX: Float,
        targetBodyCenterY: Float,
        targetHeadCenterY: Float,
        headRadiusPx: Float,
        bodyRadiusPx: Float,
        sensitivityConfig: SensitivityConfig
    ): DragShotEvaluation {
        val safeDurationSec = (dragDurationMs.coerceAtLeast(16L)) / 1000f
        val dxPx = dragEndX - dragStartX
        val dyPx = dragEndY - dragStartY // Negative when dragging upward
        val distancePx = hypot(dxPx, dyPx)
        val distanceDp = distancePx / density.coerceAtLeast(1f)
        val velocityDpPerSec = (distanceDp / safeDurationSec).coerceIn(100f, 4500f)

        // Upward angle in degrees where 90 deg is straight up
        val rawAngleRad = atan2(-dyPx, dxPx)
        val rawAngleDeg = Math.toDegrees(rawAngleRad.toDouble()).toFloat()
        val normalizedAngleDeg = if (rawAngleDeg < 0f) rawAngleDeg + 360f else rawAngleDeg

        // Sensitivity multiplier maps Free Fire MAX 0-200 scale to crosshair travel gain
        val sensMultiplier = 0.55f + (sensitivityConfig.general * 0.0045f) + (sensitivityConfig.redDot * 0.0025f)

        val projectedX = aimOriginX + (dxPx * sensMultiplier * 0.85f)
        val projectedY = aimOriginY + (dyPx * sensMultiplier * 1.12f)

        val distToHead = hypot(projectedX - targetCenterX, projectedY - targetHeadCenterY)
        val distToBody = hypot(projectedX - targetCenterX, projectedY - targetBodyCenterY)

        // Ideal drag velocity window (1150 - 1850 dp/s) grants slight aim-lock magnetic snap similar to FF MAX mechanics
        val velocityInSweetSpot = velocityDpPerSec in 1100f..1900f && normalizedAngleDeg in 68f..112f
        val effectiveHeadRadius = if (velocityInSweetSpot) headRadiusPx * 1.22f else headRadiusPx

        return when {
            distToHead <= effectiveHeadRadius -> {
                val speedBonus = if (velocityInSweetSpot) 35 else 15
                DragShotEvaluation(
                    outcome = ShotOutcome.HEADSHOT,
                    impactX = projectedX,
                    impactY = projectedY,
                    dragVelocityDpPerSec = velocityDpPerSec,
                    dragAngleDeg = normalizedAngleDeg,
                    pointsAwarded = 100 + speedBonus,
                    feedbackText = "RED NUMBER HEADSHOT! (${velocityDpPerSec.toInt()} dp/s • ${normalizedAngleDeg.toInt()}°)"
                )
            }
            distToBody <= bodyRadiusPx -> {
                val reason = if (projectedY > targetHeadCenterY) {
                    "Chest Lock: Drag faster or +General Sens"
                } else {
                    "Shoulder Graze: Center crosshair before drag"
                }
                DragShotEvaluation(
                    outcome = ShotOutcome.BODYSHOT,
                    impactX = projectedX,
                    impactY = projectedY,
                    dragVelocityDpPerSec = velocityDpPerSec,
                    dragAngleDeg = normalizedAngleDeg,
                    pointsAwarded = 25,
                    feedbackText = reason
                )
            }
            else -> {
                val reason = when {
                    projectedY < targetHeadCenterY - headRadiusPx -> "Overshot Head: Drag lighter or lower Red Dot"
                    dyPx >= -15f -> "No Upward Drag: Swipe UP from Fire Button"
                    else -> "Off-Axis Flick (${normalizedAngleDeg.toInt()}°): Aim 85° vertical"
                }
                DragShotEvaluation(
                    outcome = ShotOutcome.MISS,
                    impactX = projectedX,
                    impactY = projectedY,
                    dragVelocityDpPerSec = velocityDpPerSec,
                    dragAngleDeg = normalizedAngleDeg,
                    pointsAwarded = 0,
                    feedbackText = reason
                )
            }
        }
    }
}

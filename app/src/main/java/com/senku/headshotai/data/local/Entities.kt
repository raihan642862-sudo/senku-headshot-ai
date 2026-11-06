package com.senku.headshotai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.senku.headshotai.domain.model.Playstyle
import com.senku.headshotai.domain.model.SensitivityConfig
import com.senku.headshotai.domain.model.TrainingMode
import com.senku.headshotai.domain.model.TrainingSessionSummary

@Entity(tableName = "sensitivity_profiles")
data class SensitivityProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileName: String,
    val general: Int,
    val redDot: Int,
    val scope2x: Int,
    val scope4x: Int,
    val sniperScope: Int,
    val freeLook: Int,
    val recommendedFireButtonSizePercent: Int,
    val recommendedFireButtonYPositionPercent: Int,
    val optimalDragSpeedDpPerSec: Int,
    val recommendedDpiNoRoot: Int,
    val playstyleName: String,
    val timestamp: Long
) {
    fun toDomain(): SensitivityConfig = SensitivityConfig(
        id = id,
        profileName = profileName,
        general = general,
        redDot = redDot,
        scope2x = scope2x,
        scope4x = scope4x,
        sniperScope = sniperScope,
        freeLook = freeLook,
        recommendedFireButtonSizePercent = recommendedFireButtonSizePercent,
        recommendedFireButtonYPositionPercent = recommendedFireButtonYPositionPercent,
        optimalDragSpeedDpPerSec = optimalDragSpeedDpPerSec,
        recommendedDpiNoRoot = recommendedDpiNoRoot,
        playstyle = runCatching { Playstyle.valueOf(playstyleName) }.getOrDefault(Playstyle.ONE_TAP_DRAG),
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(config: SensitivityConfig): SensitivityProfileEntity = SensitivityProfileEntity(
            id = config.id,
            profileName = config.profileName,
            general = config.general,
            redDot = config.redDot,
            scope2x = config.scope2x,
            scope4x = config.scope4x,
            sniperScope = config.sniperScope,
            freeLook = config.freeLook,
            recommendedFireButtonSizePercent = config.recommendedFireButtonSizePercent,
            recommendedFireButtonYPositionPercent = config.recommendedFireButtonYPositionPercent,
            optimalDragSpeedDpPerSec = config.optimalDragSpeedDpPerSec,
            recommendedDpiNoRoot = config.recommendedDpiNoRoot,
            playstyleName = config.playstyle.name,
            timestamp = config.timestamp
        )
    }
}

@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val modeName: String,
    val score: Int,
    val totalShots: Int,
    val headshots: Int,
    val bodyShots: Int,
    val misses: Int,
    val headshotRatePercent: Float,
    val avgReactionTimeMs: Long,
    val bestReactionTimeMs: Long,
    val avgDragVelocityDpPerSec: Float,
    val avgDragAngleDeg: Float,
    val timestamp: Long
) {
    fun toDomain(): TrainingSessionSummary = TrainingSessionSummary(
        id = id,
        mode = runCatching { TrainingMode.valueOf(modeName) }.getOrDefault(TrainingMode.DRAG_HEADSHOT),
        score = score,
        totalShots = totalShots,
        headshots = headshots,
        bodyShots = bodyShots,
        misses = misses,
        headshotRatePercent = headshotRatePercent,
        avgReactionTimeMs = avgReactionTimeMs,
        bestReactionTimeMs = bestReactionTimeMs,
        avgDragVelocityDpPerSec = avgDragVelocityDpPerSec,
        avgDragAngleDeg = avgDragAngleDeg,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(summary: TrainingSessionSummary): TrainingSessionEntity = TrainingSessionEntity(
            id = summary.id,
            modeName = summary.mode.name,
            score = summary.score,
            totalShots = summary.totalShots,
            headshots = summary.headshots,
            bodyShots = summary.bodyShots,
            misses = summary.misses,
            headshotRatePercent = summary.headshotRatePercent,
            avgReactionTimeMs = summary.avgReactionTimeMs,
            bestReactionTimeMs = summary.bestReactionTimeMs,
            avgDragVelocityDpPerSec = summary.avgDragVelocityDpPerSec,
            avgDragAngleDeg = summary.avgDragAngleDeg,
            timestamp = summary.timestamp
        )
    }
}

package com.senku.headshotai.domain.model

enum class Playstyle(val title: String, val description: String) {
    ONE_TAP_DRAG("One-Tap Drag", "Precision vertical flicks for Desert Eagle, M1887, Woodpecker & AC80"),
    SMG_RUSHER("Close-Range SMG Rush", "High-velocity tracking for MP40, UMP, MAC10 & Thompson"),
    BALANCED_AR("Balanced All-Rounder", "Consistent mid-range recoil control for AK47, Scar, Groza & M4A1"),
    SNIPER_SPECIALIST("Sniper & Marksman", "Micro-flick precision for AWM, M82B & Kar98k quick-switching")
}

enum class WeaponClass(
    val displayName: String,
    val optimalDragVelocityDpPerSec: Float,
    val optimalDragAngleMinDeg: Float,
    val optimalDragAngleMaxDeg: Float,
    val recommendedFireButtonSizeDp: Int
) {
    PISTOL_ONETAP("Desert Eagle / M500", 1420f, 76f, 94f, 44),
    SHOTGUN("M1887 / M1014", 1680f, 72f, 98f, 46),
    SMG("MP40 / UMP", 1290f, 74f, 96f, 48),
    ASSAULT_RIFLE("AK47 / SCAR / Woodpecker", 1180f, 78f, 92f, 50),
    SNIPER("AWM / M82B", 920f, 80f, 90f, 52)
}

enum class TrainingMode(val title: String, val subtitle: String, val durationSeconds: Int) {
    DRAG_HEADSHOT("Drag-Headshot Lab", "Swipe upward from the Fire Zone to lock onto moving target heads", 45),
    STRAFE_TRACKING("Strafe Head Tracking", "Track laterally dodging enemies at Free Fire MAX sprint speed", 45),
    REACTION_BLITZ("Reaction Speed Blitz", "Instant target acquisition to benchmark raw visual-motor latency", 30)
}

data class DeviceHardwareProfile(
    val manufacturer: String,
    val model: String,
    val socName: String,
    val totalRamGb: Float,
    val availableRamMb: Long,
    val storageTotalGb: Float,
    val storageFreeGb: Float,
    val androidRelease: String,
    val sdkInt: Int,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val densityDpi: Int,
    val refreshRateHz: Int,
    val estimatedTouchSamplingHz: Int,
    val isRealme12x5G: Boolean
)

data class SensitivityConfig(
    val id: Long = 0L,
    val profileName: String = "Realme 12x 5G Pro One-Tap",
    val general: Int = 186,        // Free Fire MAX 0-200 scale
    val redDot: Int = 178,         // 0-200 scale
    val scope2x: Int = 168,        // 0-200 scale
    val scope4x: Int = 162,        // 0-200 scale
    val sniperScope: Int = 118,    // 0-200 scale
    val freeLook: Int = 145,       // 0-200 scale
    val recommendedFireButtonSizePercent: Int = 45,
    val recommendedFireButtonYPositionPercent: Int = 78,
    val optimalDragSpeedDpPerSec: Int = 1450,
    val recommendedDpiNoRoot: Int = 411,
    val playstyle: Playstyle = Playstyle.ONE_TAP_DRAG,
    val timestamp: Long = System.currentTimeMillis()
)

data class TrainingSessionSummary(
    val id: Long = 0L,
    val mode: TrainingMode,
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
    val timestamp: Long = System.currentTimeMillis()
)

data class AiCoachingInsight(
    val headline: String,
    val diagnosis: String,
    val generalDelta: Int,
    val redDotDelta: Int,
    val scope2xDelta: Int,
    val scope4xDelta: Int,
    val fireButtonRecommendation: String,
    val dragTechniqueSteps: List<String>,
    val confidenceScorePercent: Int
)

data class PerformanceSnapshot(
    val usedRamMb: Long,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val ramUsagePercent: Int,
    val isLowMemory: Boolean,
    val currentRefreshRateHz: Int,
    val estimatedFrameTimeMs: Float,
    val thermalStatusLabel: String,
    val bottlenecks: List<String>,
    val freeFireMaxGraphicsPreset: String,
    val freeFireMaxFpsSetting: String,
    val optimizationTips: List<String>
)

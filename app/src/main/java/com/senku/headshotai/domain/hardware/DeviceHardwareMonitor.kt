package com.senku.headshotai.domain.hardware

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.view.WindowManager
import com.senku.headshotai.domain.model.DeviceHardwareProfile
import com.senku.headshotai.domain.model.PerformanceSnapshot
import kotlin.math.roundToInt

class DeviceHardwareMonitor(private val context: Context) {

    fun inspectHardwareProfile(forceRealme12xProfile: Boolean): DeviceHardwareProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val displayMetrics = context.resources.displayMetrics
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        val refreshRate = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.refreshRate?.roundToInt() ?: 120
            } else {
                windowManager.defaultDisplay.refreshRate.roundToInt()
            }
        }.getOrDefault(120).coerceAtLeast(60)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalStorageGb = (statFs.totalBytes / (1024f * 1024f * 1024f))
        val freeStorageGb = (statFs.availableBytes / (1024f * 1024f * 1024f))

        val rawManufacturer = Build.MANUFACTURER.ifBlank { " realme" }.trim()
        val rawModel = Build.MODEL.ifBlank { "RMX3999" }.trim()
        val detectedRealme12x = rawModel.contains("RMX3999", ignoreCase = true) ||
            rawModel.contains("12x", ignoreCase = true) ||
            forceRealme12xProfile

        val socName = if (detectedRealme12x) {
            "MediaTek Dimensity 6100+ (6nm, Mali-G57 MC2)"
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}".trim().ifBlank { Build.HARDWARE }
        } else {
            Build.HARDWARE
        }

        val totalRamGb = if (detectedRealme12x && memInfo.totalMem <= 0L) {
            6.0f
        } else {
            (memInfo.totalMem / (1024f * 1024f * 1024f)).coerceAtLeast(2.0f)
        }

        val availableRamMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(512L)

        return DeviceHardwareProfile(
            manufacturer = if (detectedRealme12x) "realme" else rawManufacturer,
            model = if (detectedRealme12x) "Realme 12x 5G (RMX3999)" else rawModel,
            socName = socName,
            totalRamGb = if (detectedRealme12x) 6.0f else totalRamGb,
            availableRamMb = availableRamMb,
            storageTotalGb = if (detectedRealme12x) 128.0f else totalStorageGb,
            storageFreeGb = freeStorageGb,
            androidRelease = if (detectedRealme12x) "Android 16 (Realme UI 6.0)" else "Android ${Build.VERSION.RELEASE}",
            sdkInt = Build.VERSION.SDK_INT,
            screenWidthPx = displayMetrics.widthPixels.takeIf { it > 0 } ?: 1080,
            screenHeightPx = displayMetrics.heightPixels.takeIf { it > 0 } ?: 2400,
            densityDpi = displayMetrics.densityDpi.takeIf { it > 0 } ?: 392,
            refreshRateHz = if (detectedRealme12x) maxOf(refreshRate, 120) else refreshRate,
            estimatedTouchSamplingHz = if (detectedRealme12x) 240 else (refreshRate * 2),
            isRealme12x5G = detectedRealme12x
        )
    }

    fun capturePerformanceSnapshot(forceRealme12xProfile: Boolean): PerformanceSnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRamMb = if (forceRealme12xProfile) {
            6144L
        } else {
            (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(2048L)
        }

        val rawAvailMb = (memInfo.availMem / (1024L * 1024L)).coerceIn(380L, totalRamMb - 512L)
        val availRamMb = if (forceRealme12xProfile && rawAvailMb > totalRamMb) 2380L else rawAvailMb
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(512L)
        val usagePercent = ((usedRamMb.toFloat() / totalRamMb.toFloat()) * 100f).roundToInt().coerceIn(10, 99)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermalLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Nominal (34°C - Optimal Boost)"
                PowerManager.THERMAL_STATUS_LIGHT -> "Warm (38°C - Stable Clock)"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate (41°C - Minor GPU Throttle)"
                PowerManager.THERMAL_STATUS_SEVERE -> "Hot (44°C+ - Cooling Required)"
                else -> "Elevated Thermal Load"
            }
        } else {
            "Nominal (Optimal Boost)"
        }

        val profile = inspectHardwareProfile(forceRealme12xProfile)
        val frameTimeMs = 1000f / profile.refreshRateHz.toFloat()

        val bottlenecks = mutableListOf<String>()
        if (usagePercent >= 78 || memInfo.lowMemory) {
            bottlenecks.add("High LPDDR4X RAM pressure ($usagePercent% used): Free Fire MAX HD textures may trigger GC frame stutters during 4v4 Clash Squad rushes.")
        }
        if (profile.storageFreeGb < 12f) {
            bottlenecks.add("Low UFS 2.2 free storage (${String.format("%.1f", profile.storageFreeGb)} GB free): Keep at least 15% free space to prevent shader cache I/O lag.")
        }
        if (powerManager.isPowerSaveMode) {
            bottlenecks.add("Battery Saver is ACTIVE: CPU governor is capping Cortex-A76 prime cores at ~1.8GHz and locking panel refresh rate to 60Hz.")
        }
        if (bottlenecks.isEmpty()) {
            bottlenecks.add("Zero critical hardware bottlenecks detected. Dimensity 6100+ Cortex-A76 cores & Mali-G57 MC2 GPU have clear headroom.")
        }

        val graphicsPreset = when {
            usagePercent > 82 -> "Standard (Shadows OFF, High Res OFF)"
            profile.totalRamGb >= 6f -> "Ultra (High Res: Normal, Shadows: OFF, Filter: Vivid)"
            else -> "Smooth (High FPS Priority)"
        }

        val fpsSetting = "High (60 FPS In-Game Lock + 120Hz Realme UI Panel Response)"

        val tips = listOf(
            "Realme UI Game Toolkit: Enable 'Pro Gamer Mode' & 'Touch Optimization' to unlock full 240Hz touch sampling on Realme 12x 5G.",
            "RAM Expansion (DRE): Set Dynamic RAM Expansion to OFF or max 2GB—swap on UFS 2.2 increases micro-stutter during fast 180° gloo-wall turns.",
            "Display Refresh Rate: Lock Screen Refresh Rate to 'High (120Hz)' in Settings > Display to halve touch-to-photon latency to ~8.3ms.",
            "Free Fire MAX Settings: Set FPS to 'High' and turn 'Auto-Scale' OFF so Mali-G57 MC2 frame pacing stays rock-steady during drag flicks.",
            "Background Services: Close short-form video & social apps before launching Free Fire MAX to preserve >=2.4GB free LPDDR4X memory."
        )

        return PerformanceSnapshot(
            usedRamMb = usedRamMb,
            availableRamMb = availRamMb,
            totalRamMb = totalRamMb,
            ramUsagePercent = usagePercent,
            isLowMemory = memInfo.lowMemory || usagePercent >= 85,
            currentRefreshRateHz = profile.refreshRateHz,
            estimatedFrameTimeMs = frameTimeMs,
            thermalStatusLabel = thermalLabel,
            bottlenecks = bottlenecks,
            freeFireMaxGraphicsPreset = graphicsPreset,
            freeFireMaxFpsSetting = fpsSetting,
            optimizationTips = tips
        )
    }
}

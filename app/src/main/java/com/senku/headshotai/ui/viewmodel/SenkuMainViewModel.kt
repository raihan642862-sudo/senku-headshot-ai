package com.senku.headshotai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.senku.headshotai.data.local.SenkuDao
import com.senku.headshotai.data.local.SensitivityProfileEntity
import com.senku.headshotai.data.local.TrainingSessionEntity
import com.senku.headshotai.data.preferences.UserPreferencesRepository
import com.senku.headshotai.domain.ai.SenkuAiEngine
import com.senku.headshotai.domain.hardware.DeviceHardwareMonitor
import com.senku.headshotai.domain.model.AiCoachingInsight
import com.senku.headshotai.domain.model.DeviceHardwareProfile
import com.senku.headshotai.domain.model.PerformanceSnapshot
import com.senku.headshotai.domain.model.Playstyle
import com.senku.headshotai.domain.model.SensitivityConfig
import com.senku.headshotai.domain.model.TrainingSessionSummary
import com.senku.headshotai.domain.model.WeaponClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SenkuUiState(
    val hardwareProfile: DeviceHardwareProfile,
    val performanceSnapshot: PerformanceSnapshot,
    val activeSensitivity: SensitivityConfig,
    val selectedPlaystyle: Playstyle = Playstyle.ONE_TAP_DRAG,
    val selectedWeaponClass: WeaponClass = WeaponClass.PISTOL_ONETAP,
    val savedProfiles: List<SensitivityConfig> = emptyList(),
    val trainingHistory: List<TrainingSessionSummary> = emptyList(),
    val aiCoachingInsight: AiCoachingInsight,
    val forceRealme12xOptimization: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val targetSpeedMultiplier: Float = 1.0f,
    val statusBannerMessage: String? = null
)

class SenkuMainViewModel(
    application: Application,
    private val dao: SenkuDao,
    private val prefsRepo: UserPreferencesRepository,
    private val hardwareMonitor: DeviceHardwareMonitor
) : AndroidViewModel(application) {

    private val _selectedPlaystyle = MutableStateFlow(Playstyle.ONE_TAP_DRAG)
    private val _selectedWeapon = MutableStateFlow(WeaponClass.PISTOL_ONETAP)
    private val _performanceSnapshot = MutableStateFlow(
        hardwareMonitor.capturePerformanceSnapshot(forceRealme12xProfile = true)
    )
    private val _statusBanner = MutableStateFlow<String?>(null)

    private val initialHardware = hardwareMonitor.inspectHardwareProfile(forceRealme12xProfile = true)
    private val initialConfig = SenkuAiEngine.calculateRecommendedSensitivity(
        hardware = initialHardware,
        playstyle = Playstyle.ONE_TAP_DRAG,
        weaponClass = WeaponClass.PISTOL_ONETAP
    )

    val uiState: StateFlow<SenkuUiState> = combine(
        prefsRepo.userPreferencesFlow,
        dao.observeAllProfiles(),
        dao.observeRecentTrainingSessions(),
        _selectedPlaystyle,
        _selectedWeapon,
        _performanceSnapshot,
        _statusBanner
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val prefs = values[0] as com.senku.headshotai.data.preferences.UserPreferencesState
        @Suppress("UNCHECKED_CAST")
        val profileEntities = values[1] as List<SensitivityProfileEntity>
        @Suppress("UNCHECKED_CAST")
        val sessionEntities = values[2] as List<TrainingSessionEntity>
        val playstyle = values[3] as Playstyle
        val weapon = values[4] as WeaponClass
        val perf = values[5] as PerformanceSnapshot
        val banner = values[6] as String?

        val hw = hardwareMonitor.inspectHardwareProfile(prefs.forceRealme12xOptimization)
        val sessions = sessionEntities.map { it.toDomain() }
        val saved = profileEntities.map { it.toDomain() }
        val insight = SenkuAiEngine.analyzeTrainingAndCoach(
            currentConfig = prefs.activeConfig,
            sessions = sessions,
            hardware = hw
        )

        SenkuUiState(
            hardwareProfile = hw,
            performanceSnapshot = perf,
            activeSensitivity = prefs.activeConfig,
            selectedPlaystyle = playstyle,
            selectedWeaponClass = weapon,
            savedProfiles = saved,
            trainingHistory = sessions,
            aiCoachingInsight = insight,
            forceRealme12xOptimization = prefs.forceRealme12xOptimization,
            hapticFeedbackEnabled = prefs.hapticFeedbackEnabled,
            targetSpeedMultiplier = prefs.targetSpeedMultiplier,
            statusBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SenkuUiState(
            hardwareProfile = initialHardware,
            performanceSnapshot = _performanceSnapshot.value,
            activeSensitivity = initialConfig,
            aiCoachingInsight = SenkuAiEngine.analyzeTrainingAndCoach(initialConfig, emptyList(), initialHardware)
        )
    )

    init {
        // Periodic lightweight telemetry refresh while UI is active (no background service overhead)
        viewModelScope.launch {
            while (isActive) {
                val force12x = uiState.value.forceRealme12xOptimization
                _performanceSnapshot.value = hardwareMonitor.capturePerformanceSnapshot(force12x)
                delay(4_000L)
            }
        }
    }

    fun selectPlaystyle(playstyle: Playstyle) {
        _selectedPlaystyle.value = playstyle
    }

    fun selectWeaponClass(weaponClass: WeaponClass) {
        _selectedWeapon.value = weaponClass
    }

    fun generateAiRecommendedSensitivity() {
        viewModelScope.launch {
            val state = uiState.value
            val generated = SenkuAiEngine.calculateRecommendedSensitivity(
                hardware = state.hardwareProfile,
                playstyle = state.selectedPlaystyle,
                weaponClass = state.selectedWeaponClass
            )
            prefsRepo.saveActiveSensitivity(generated)
            showBanner("AI Calibrated for ${state.hardwareProfile.model} (${state.selectedPlaystyle.title})")
        }
    }

    fun updateManualSensitivity(
        general: Int = uiState.value.activeSensitivity.general,
        redDot: Int = uiState.value.activeSensitivity.redDot,
        scope2x: Int = uiState.value.activeSensitivity.scope2x,
        scope4x: Int = uiState.value.activeSensitivity.scope4x,
        sniperScope: Int = uiState.value.activeSensitivity.sniperScope,
        freeLook: Int = uiState.value.activeSensitivity.freeLook,
        fireButtonSize: Int = uiState.value.activeSensitivity.recommendedFireButtonSizePercent
    ) {
        viewModelScope.launch {
            val updated = uiState.value.activeSensitivity.copy(
                general = general.coerceIn(1, 200),
                redDot = redDot.coerceIn(1, 200),
                scope2x = scope2x.coerceIn(1, 200),
                scope4x = scope4x.coerceIn(1, 200),
                sniperScope = sniperScope.coerceIn(1, 200),
                freeLook = freeLook.coerceIn(1, 200),
                recommendedFireButtonSizePercent = fireButtonSize.coerceIn(25, 80),
                timestamp = System.currentTimeMillis()
            )
            prefsRepo.saveActiveSensitivity(updated)
        }
    }

    fun applyAiCoachingAdjustments() {
        viewModelScope.launch {
            val cur = uiState.value.activeSensitivity
            val insight = uiState.value.aiCoachingInsight
            val adjusted = cur.copy(
                general = (cur.general + insight.generalDelta).coerceIn(1, 200),
                redDot = (cur.redDot + insight.redDotDelta).coerceIn(1, 200),
                scope2x = (cur.scope2x + insight.scope2xDelta).coerceIn(1, 200),
                scope4x = (cur.scope4x + insight.scope4xDelta).coerceIn(1, 200),
                timestamp = System.currentTimeMillis()
            )
            prefsRepo.saveActiveSensitivity(adjusted)
            showBanner("Applied SENKU AI Coaching Delta to Active Sensitivity")
        }
    }

    fun saveCurrentProfileToDatabase(customName: String) {
        viewModelScope.launch {
            val cleanName = customName.trim().ifBlank {
                "Realme 12x Preset #${uiState.value.savedProfiles.size + 1}"
            }
            val configToSave = uiState.value.activeSensitivity.copy(
                id = 0L,
                profileName = cleanName,
                timestamp = System.currentTimeMillis()
            )
            dao.insertProfile(SensitivityProfileEntity.fromDomain(configToSave))
            showBanner("Saved profile '$cleanName' to Room Database")
        }
    }

    fun loadProfile(profile: SensitivityConfig) {
        viewModelScope.launch {
            prefsRepo.saveActiveSensitivity(profile)
            showBanner("Loaded '${profile.profileName}'")
        }
    }

    fun deleteProfile(profile: SensitivityConfig) {
        viewModelScope.launch {
            dao.deleteProfile(SensitivityProfileEntity.fromDomain(profile))
            showBanner("Deleted '${profile.profileName}'")
        }
    }

    fun recordTrainingSession(summary: TrainingSessionSummary) {
        viewModelScope.launch {
            dao.insertTrainingSession(TrainingSessionEntity.fromDomain(summary))
            showBanner("Training saved: ${summary.headshotRatePercent.toInt()}% Headshot Rate!")
        }
    }

    fun clearTrainingHistory() {
        viewModelScope.launch {
            dao.clearTrainingHistory()
            showBanner("Training history cleared")
        }
    }

    fun refreshPerformanceNow() {
        val force12x = uiState.value.forceRealme12xOptimization
        System.gc()
        _performanceSnapshot.value = hardwareMonitor.capturePerformanceSnapshot(force12x)
        showBanner("Telemetry refreshed & app memory trimmed")
    }

    fun setForceRealme12xOptimization(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setForceRealme12xOptimization(enabled)
            _performanceSnapshot.value = hardwareMonitor.capturePerformanceSnapshot(enabled)
        }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setHapticFeedback(enabled)
        }
    }

    fun setTargetSpeedPercent(percent: Int) {
        viewModelScope.launch {
            prefsRepo.setTargetSpeedPercent(percent)
        }
    }

    private fun showBanner(message: String) {
        viewModelScope.launch {
            _statusBanner.value = message
            delay(2_800L)
            if (_statusBanner.value == message) {
                _statusBanner.value = null
            }
        }
    }

    class Factory(
        private val application: Application,
        private val dao: SenkuDao,
        private val prefsRepo: UserPreferencesRepository,
        private val hardwareMonitor: DeviceHardwareMonitor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SenkuMainViewModel(application, dao, prefsRepo, hardwareMonitor) as T
        }
    }
}

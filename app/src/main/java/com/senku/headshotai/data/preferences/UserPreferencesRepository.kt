package com.senku.headshotai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.senku.headshotai.domain.model.Playstyle
import com.senku.headshotai.domain.model.SensitivityConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "senku_user_prefs")

data class UserPreferencesState(
    val activeConfig: SensitivityConfig,
    val forceRealme12xOptimization: Boolean,
    val hapticFeedbackEnabled: Boolean,
    val targetSpeedMultiplier: Float
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val GENERAL = intPreferencesKey("sens_general")
        val RED_DOT = intPreferencesKey("sens_red_dot")
        val SCOPE_2X = intPreferencesKey("sens_scope_2x")
        val SCOPE_4X = intPreferencesKey("sens_scope_4x")
        val SNIPER = intPreferencesKey("sens_sniper")
        val FREE_LOOK = intPreferencesKey("sens_free_look")
        val FIRE_BTN_SIZE = intPreferencesKey("fire_btn_size")
        val FIRE_BTN_Y = intPreferencesKey("fire_btn_y")
        val DRAG_SPEED = intPreferencesKey("drag_speed")
        val DPI_REC = intPreferencesKey("dpi_rec")
        val PLAYSTYLE = stringPreferencesKey("playstyle")
        val FORCE_REALME_12X = booleanPreferencesKey("force_realme_12x")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val TARGET_SPEED_PCT = intPreferencesKey("target_speed_pct")
    }

    val userPreferencesFlow: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        val playstyleName = prefs[Keys.PLAYSTYLE] ?: Playstyle.ONE_TAP_DRAG.name
        val playstyle = runCatching { Playstyle.valueOf(playstyleName) }.getOrDefault(Playstyle.ONE_TAP_DRAG)

        val config = SensitivityConfig(
            profileName = prefs[Keys.PROFILE_NAME] ?: "Realme 12x 5G Pro One-Tap",
            general = prefs[Keys.GENERAL] ?: 186,
            redDot = prefs[Keys.RED_DOT] ?: 178,
            scope2x = prefs[Keys.SCOPE_2X] ?: 168,
            scope4x = prefs[Keys.SCOPE_4X] ?: 162,
            sniperScope = prefs[Keys.SNIPER] ?: 118,
            freeLook = prefs[Keys.FREE_LOOK] ?: 145,
            recommendedFireButtonSizePercent = prefs[Keys.FIRE_BTN_SIZE] ?: 45,
            recommendedFireButtonYPositionPercent = prefs[Keys.FIRE_BTN_Y] ?: 78,
            optimalDragSpeedDpPerSec = prefs[Keys.DRAG_SPEED] ?: 1450,
            recommendedDpiNoRoot = prefs[Keys.DPI_REC] ?: 411,
            playstyle = playstyle
        )
        UserPreferencesState(
            activeConfig = config,
            forceRealme12xOptimization = prefs[Keys.FORCE_REALME_12X] ?: true,
            hapticFeedbackEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            targetSpeedMultiplier = (prefs[Keys.TARGET_SPEED_PCT] ?: 100) / 100f
        )
    }

    suspend fun saveActiveSensitivity(config: SensitivityConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROFILE_NAME] = config.profileName
            prefs[Keys.GENERAL] = config.general.coerceIn(1, 200)
            prefs[Keys.RED_DOT] = config.redDot.coerceIn(1, 200)
            prefs[Keys.SCOPE_2X] = config.scope2x.coerceIn(1, 200)
            prefs[Keys.SCOPE_4X] = config.scope4x.coerceIn(1, 200)
            prefs[Keys.SNIPER] = config.sniperScope.coerceIn(1, 200)
            prefs[Keys.FREE_LOOK] = config.freeLook.coerceIn(1, 200)
            prefs[Keys.FIRE_BTN_SIZE] = config.recommendedFireButtonSizePercent.coerceIn(25, 80)
            prefs[Keys.FIRE_BTN_Y] = config.recommendedFireButtonYPositionPercent.coerceIn(55, 92)
            prefs[Keys.DRAG_SPEED] = config.optimalDragSpeedDpPerSec
            prefs[Keys.DPI_REC] = config.recommendedDpiNoRoot
            prefs[Keys.PLAYSTYLE] = config.playstyle.name
        }
    }

    suspend fun setForceRealme12xOptimization(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FORCE_REALME_12X] = enabled
        }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setTargetSpeedPercent(percent: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TARGET_SPEED_PCT] = percent.coerceIn(60, 160)
        }
    }
}

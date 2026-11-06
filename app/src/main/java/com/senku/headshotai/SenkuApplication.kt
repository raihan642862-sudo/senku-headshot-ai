package com.senku.headshotai

import android.app.Application
import com.senku.headshotai.data.local.SenkuDatabase
import com.senku.headshotai.data.preferences.UserPreferencesRepository
import com.senku.headshotai.domain.hardware.DeviceHardwareMonitor

class SenkuApplication : Application() {
    val database: SenkuDatabase by lazy { SenkuDatabase.getInstance(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val hardwareMonitor: DeviceHardwareMonitor by lazy { DeviceHardwareMonitor(this) }
}

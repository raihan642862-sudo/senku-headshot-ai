package com.senku.headshotai.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SenkuDao {
    @Query("SELECT * FROM sensitivity_profiles ORDER BY timestamp DESC")
    fun observeAllProfiles(): Flow<List<SensitivityProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SensitivityProfileEntity): Long

    @Delete
    suspend fun deleteProfile(profile: SensitivityProfileEntity)

    @Query("SELECT * FROM training_sessions ORDER BY timestamp DESC LIMIT 50")
    fun observeRecentTrainingSessions(): Flow<List<TrainingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainingSession(session: TrainingSessionEntity): Long

    @Query("DELETE FROM training_sessions")
    suspend fun clearTrainingHistory()
}

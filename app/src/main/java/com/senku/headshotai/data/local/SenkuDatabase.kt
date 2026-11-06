package com.senku.headshotai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SensitivityProfileEntity::class, TrainingSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SenkuDatabase : RoomDatabase() {
    abstract fun senkuDao(): SenkuDao

    companion object {
        @Volatile
        private var INSTANCE: SenkuDatabase? = null

        fun getInstance(context: Context): SenkuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SenkuDatabase::class.java,
                    "senku_headshot_ai.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.genesiscruz.adwarehuli.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RedirectEventEntity::class, AppRiskEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun redirectEventDao(): RedirectEventDao
    abstract fun appRiskDao(): AppRiskDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adwarehuli.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}

package com.genesiscruz.adwarehuli.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RedirectEventEntity::class, AppRiskEntity::class, DomainHitEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun redirectEventDao(): RedirectEventDao
    abstract fun appRiskDao(): AppRiskDao
    abstract fun domainHitDao(): DomainHitDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `domain_hits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `uid` INTEGER NOT NULL,
                        `domain` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isFlagged` INTEGER NOT NULL,
                        `protocol` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adwarehuli.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }
    }
}

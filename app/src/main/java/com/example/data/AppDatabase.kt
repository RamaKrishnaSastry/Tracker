package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [JapaEntry::class, CustomPractice::class, CustomPracticeEntry::class, JapaSession::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun japaDao(): JapaDao
    abstract fun customPracticeDao(): CustomPracticeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun safeAddColumn(db: SupportSQLiteDatabase, table: String, column: String, type: String) {
            try {
                db.execSQL("ALTER TABLE $table ADD COLUMN $column $type")
            } catch (e: Exception) {
                android.util.Log.w("AppDatabase", "Notice: Column $column in $table might already exist or migration skipped: ${e.message}")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeAddColumn(db, "japa_entries", "pratahPunascharanaCount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "japa_entries", "madhyahnikaPunascharanaCount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "japa_entries", "sayamPunascharanaCount", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `custom_practices` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `defaultTarget` INTEGER NOT NULL, `displayOrder` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `custom_practice_entries` (`practiceId` INTEGER NOT NULL, `date` TEXT NOT NULL, `count` INTEGER NOT NULL, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`practiceId`, `date`))")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeAddColumn(db, "custom_practices", "practiceType", "TEXT NOT NULL DEFAULT 'CONTINUOUS'")
                safeAddColumn(db, "custom_practices", "punasTarget", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "custom_practices", "quickAddValues", "TEXT NOT NULL DEFAULT '10,54,108'")
                safeAddColumn(db, "custom_practices", "initialLifetimeCount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "custom_practices", "reminderTime", "TEXT NOT NULL DEFAULT ''")
                safeAddColumn(db, "custom_practices", "isReminderEnabled", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "custom_practices", "themeColor", "TEXT NOT NULL DEFAULT 'Teal'")
                safeAddColumn(db, "custom_practices", "isWidgetPinned", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "custom_practices", "isArchived", "INTEGER NOT NULL DEFAULT 0")
                
                safeAddColumn(db, "custom_practice_entries", "morningCount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "custom_practice_entries", "afternoonCount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "custom_practice_entries", "eveningCount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "custom_practice_entries", "morningPunasCount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "custom_practice_entries", "afternoonPunasCount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "custom_practice_entries", "eveningPunasCount", "INTEGER NOT NULL DEFAULT 0")
                
                db.execSQL("CREATE TABLE IF NOT EXISTS `japa_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `practiceId` INTEGER NOT NULL, `date` TEXT NOT NULL, `time` TEXT NOT NULL, `count` INTEGER NOT NULL, `typeDetail` TEXT NOT NULL DEFAULT '')")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeAddColumn(db, "custom_practices", "isMorningEnabled", "INTEGER NOT NULL DEFAULT 1")
                safeAddColumn(db, "custom_practices", "isMiddayEnabled", "INTEGER NOT NULL DEFAULT 1")
                safeAddColumn(db, "custom_practices", "isEveningEnabled", "INTEGER NOT NULL DEFAULT 1")
                safeAddColumn(db, "custom_practices", "isPunascharanaEnabled", "INTEGER NOT NULL DEFAULT 1")
                safeAddColumn(db, "custom_practices", "incrementValue", "INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeAddColumn(db, "japa_entries", "placeName", "TEXT")
                safeAddColumn(db, "japa_entries", "placeCategory", "TEXT")
                safeAddColumn(db, "japa_entries", "latitude", "REAL")
                safeAddColumn(db, "japa_entries", "longitude", "REAL")

                safeAddColumn(db, "japa_sessions", "placeName", "TEXT")
                safeAddColumn(db, "japa_sessions", "placeCategory", "TEXT")
                safeAddColumn(db, "japa_sessions", "latitude", "REAL")
                safeAddColumn(db, "japa_sessions", "longitude", "REAL")

                safeAddColumn(db, "custom_practice_entries", "placeName", "TEXT")
                safeAddColumn(db, "custom_practice_entries", "placeCategory", "TEXT")
                safeAddColumn(db, "custom_practice_entries", "latitude", "REAL")
                safeAddColumn(db, "custom_practice_entries", "longitude", "REAL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gayatri_japa_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

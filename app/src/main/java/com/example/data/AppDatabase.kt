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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE japa_entries ADD COLUMN pratahPunascharanaCount INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE japa_entries ADD COLUMN madhyahnikaPunascharanaCount INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE japa_entries ADD COLUMN sayamPunascharanaCount INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
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
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN practiceType TEXT NOT NULL DEFAULT 'CONTINUOUS'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN punasTarget INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN quickAddValues TEXT NOT NULL DEFAULT '10,54,108'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN initialLifetimeCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN reminderTime TEXT NOT NULL DEFAULT ''") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isReminderEnabled INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN themeColor TEXT NOT NULL DEFAULT 'Teal'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isWidgetPinned INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN morningCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN afternoonCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN eveningCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN morningPunasCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN afternoonPunasCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN eveningPunasCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                
                db.execSQL("CREATE TABLE IF NOT EXISTS `japa_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `practiceId` INTEGER NOT NULL, `date` TEXT NOT NULL, `time` TEXT NOT NULL, `count` INTEGER NOT NULL, `typeDetail` TEXT NOT NULL DEFAULT '')")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isMorningEnabled INTEGER NOT NULL DEFAULT 1") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isMiddayEnabled INTEGER NOT NULL DEFAULT 1") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isEveningEnabled INTEGER NOT NULL DEFAULT 1") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isPunascharanaEnabled INTEGER NOT NULL DEFAULT 1") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN incrementValue INTEGER NOT NULL DEFAULT 1") } catch (e: Exception) {}
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE japa_entries ADD COLUMN placeName TEXT") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE japa_entries ADD COLUMN placeCategory TEXT") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE japa_entries ADD COLUMN latitude REAL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE japa_entries ADD COLUMN longitude REAL") } catch (e: Exception) {}

                try { db.execSQL("ALTER TABLE japa_sessions ADD COLUMN placeName TEXT") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE japa_sessions ADD COLUMN placeCategory TEXT") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE japa_sessions ADD COLUMN latitude REAL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE japa_sessions ADD COLUMN longitude REAL") } catch (e: Exception) {}

                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN placeName TEXT") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN placeCategory TEXT") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN latitude REAL") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN longitude REAL") } catch (e: Exception) {}
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

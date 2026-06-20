package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [JapaEntry::class, CustomPractice::class, CustomPracticeEntry::class, JapaSession::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun japaDao(): JapaDao
    abstract fun customPracticeDao(): CustomPracticeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Check if columns already exist before adding them (safe backup migration)
                try {
                    db.execSQL("ALTER TABLE japa_entries ADD COLUMN pratahPunascharanaCount INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist
                }
                try {
                    db.execSQL("ALTER TABLE japa_entries ADD COLUMN madhyahnikaPunascharanaCount INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist
                }
                try {
                    db.execSQL("ALTER TABLE japa_entries ADD COLUMN sayamPunascharanaCount INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist
                }
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
                // Add columns to custom_practices
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN practiceType TEXT NOT NULL DEFAULT 'CONTINUOUS'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN punasTarget INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN quickAddValues TEXT NOT NULL DEFAULT '10,54,108'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN initialLifetimeCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN reminderTime TEXT NOT NULL DEFAULT ''") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isReminderEnabled INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN themeColor TEXT NOT NULL DEFAULT 'Teal'") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isWidgetPinned INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                
                // Add columns to custom_practice_entries
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN morningCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN afternoonCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN eveningCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN morningPunasCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN afternoonPunasCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practice_entries ADD COLUMN eveningPunasCount INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
                
                // Create japa_sessions table
                db.execSQL("CREATE TABLE IF NOT EXISTS `japa_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `practiceId` INTEGER NOT NULL, `date` TEXT NOT NULL, `time` TEXT NOT NULL, `count` INTEGER NOT NULL, `typeDetail` TEXT NOT NULL DEFAULT '')")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the very latest fields added in last turn
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isMorningEnabled INTEGER NOT NULL DEFAULT 1") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isMiddayEnabled INTEGER NOT NULL DEFAULT 1") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isEveningEnabled INTEGER NOT NULL DEFAULT 1") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN isPunascharanaEnabled INTEGER NOT NULL DEFAULT 1") } catch (e: Exception) {}
                try { db.execSQL("ALTER TABLE custom_practices ADD COLUMN incrementValue INTEGER NOT NULL DEFAULT 1") } catch (e: Exception) {}
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gayatri_japa_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // We can insert defaults later, or here.
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

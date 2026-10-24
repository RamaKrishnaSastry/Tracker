package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JapaMitra", appName)
    }

    private fun createDatabaseAtVersion1(context: Context, dbName: String): SupportSQLiteDatabase {
        context.deleteDatabase(dbName)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `japa_entries` (`date` TEXT PRIMARY KEY NOT NULL, `pratahSandhyaCount` INTEGER NOT NULL DEFAULT 0, `madhyahnikaSandhyaCount` INTEGER NOT NULL DEFAULT 0, `sayamSandhyaCount` INTEGER NOT NULL DEFAULT 0, `updatedAt` TEXT NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        return helper.writableDatabase
    }

    @Test
    fun `test MIGRATION_1_2 adds punascharana columns to japa_entries`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = createDatabaseAtVersion1(context, "test_db_m12")

        db.execSQL("INSERT INTO japa_entries (date, pratahSandhyaCount, madhyahnikaSandhyaCount, sayamSandhyaCount, updatedAt) VALUES ('2026-01-01', 108, 108, 108, '2026-01-01T00:00:00Z')")

        AppDatabase.MIGRATION_1_2.migrate(db)

        val cursor = db.query("SELECT pratahPunascharanaCount, madhyahnikaPunascharanaCount, sayamPunascharanaCount FROM japa_entries WHERE date = '2026-01-01'")
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
        assertEquals(0, cursor.getInt(1))
        assertEquals(0, cursor.getInt(2))
        cursor.close()
        db.close()
    }

    @Test
    fun `test MIGRATION_2_3 creates custom practices tables`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = createDatabaseAtVersion1(context, "test_db_m23")

        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)

        db.execSQL("INSERT INTO custom_practices (name, defaultTarget, displayOrder) VALUES ('Om Namah Shivaya', 108, 1)")
        val cursor = db.query("SELECT id, name, defaultTarget FROM custom_practices")
        assertTrue(cursor.moveToFirst())
        assertEquals("Om Namah Shivaya", cursor.getString(1))
        cursor.close()
        db.close()
    }

    @Test
    fun `test MIGRATION_3_4 adds extended practice columns and japa_sessions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = createDatabaseAtVersion1(context, "test_db_m34")

        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)

        db.execSQL("INSERT INTO custom_practices (name, defaultTarget, displayOrder) VALUES ('Test Mantra', 108, 1)")
        val cursor = db.query("SELECT practiceType, themeColor FROM custom_practices WHERE name = 'Test Mantra'")
        assertTrue(cursor.moveToFirst())
        assertEquals("CONTINUOUS", cursor.getString(0))
        assertEquals("Teal", cursor.getString(1))
        cursor.close()

        db.execSQL("INSERT INTO japa_sessions (practiceId, date, time, count) VALUES (1, '2026-01-01', '08:00', 108)")
        val sessionCursor = db.query("SELECT count FROM japa_sessions WHERE practiceId = 1")
        assertTrue(sessionCursor.moveToFirst())
        assertEquals(108, sessionCursor.getInt(0))
        sessionCursor.close()
        db.close()
    }

    @Test
    fun `test MIGRATION_4_5 adds sandhya toggles and increment value`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = createDatabaseAtVersion1(context, "test_db_m45")

        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)

        db.execSQL("INSERT INTO custom_practices (name, defaultTarget, displayOrder) VALUES ('Mantra 45', 108, 1)")
        val cursor = db.query("SELECT isPunascharanaEnabled, incrementValue FROM custom_practices WHERE name = 'Mantra 45'")
        assertTrue(cursor.moveToFirst())
        assertEquals(1, cursor.getInt(0))
        assertEquals(1, cursor.getInt(1))
        cursor.close()
        db.close()
    }

    @Test
    fun `test MIGRATION_5_6 adds location columns across all entry tables`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = createDatabaseAtVersion1(context, "test_db_m56")

        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)

        db.execSQL("INSERT INTO japa_entries (date, pratahSandhyaCount, madhyahnikaSandhyaCount, sayamSandhyaCount, updatedAt, placeName, placeCategory, latitude, longitude) VALUES ('2026-01-01', 108, 0, 0, '2026-01-01T00:00:00Z', 'Local Temple', 'TEMPLE', 12.9716, 77.5946)")
        val cursor = db.query("SELECT placeName, placeCategory, latitude, longitude FROM japa_entries WHERE date = '2026-01-01'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Local Temple", cursor.getString(0))
        assertEquals("TEMPLE", cursor.getString(1))
        assertEquals(12.9716, cursor.getDouble(2), 0.0001)
        assertEquals(77.5946, cursor.getDouble(3), 0.0001)
        cursor.close()
        db.close()
    }

    @Test
    fun `test chained migration from version 1 to 6`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = createDatabaseAtVersion1(context, "test_db_chained")

        // Execute all migrations sequentially
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)

        // Verify that the final schema supports modern queries
        db.execSQL("INSERT INTO japa_entries (date, updatedAt, placeName) VALUES ('2026-02-01', '2026-02-01T00:00:00Z', 'Ashram')")
        val cursor = db.query("SELECT placeName FROM japa_entries WHERE date = '2026-02-01'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Ashram", cursor.getString(0))
        cursor.close()
        db.close()
    }
}

package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JapaDao {
    @Query("SELECT * FROM japa_entries ORDER BY date DESC")
    fun getAllEntriesFlow(): Flow<List<JapaEntry>>

    @Query("SELECT * FROM japa_entries ORDER BY date DESC")
    suspend fun getAllEntries(): List<JapaEntry>

    @Query("SELECT * FROM japa_entries WHERE date = :date LIMIT 1")
    suspend fun getEntryByDate(date: String): JapaEntry?

    @Query("SELECT * FROM japa_entries WHERE date = :date LIMIT 1")
    fun getEntryByDateFlow(date: String): Flow<JapaEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: JapaEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<JapaEntry>)

    @Query("SELECT SUM(CASE WHEN morningCount > 0 THEN morningCount ELSE 0 END + CASE WHEN afternoonCount > 0 THEN afternoonCount ELSE 0 END + CASE WHEN eveningCount > 0 THEN eveningCount ELSE 0 END + CASE WHEN pratahPunascharanaCount > 0 THEN pratahPunascharanaCount ELSE 0 END + CASE WHEN madhyahnikaPunascharanaCount > 0 THEN madhyahnikaPunascharanaCount ELSE 0 END + CASE WHEN sayamPunascharanaCount > 0 THEN sayamPunascharanaCount ELSE 0 END) FROM japa_entries")
    fun getTotalCountFlow(): Flow<Int?>

    @Query("SELECT SUM(CASE WHEN morningCount > 0 THEN morningCount ELSE 0 END + CASE WHEN afternoonCount > 0 THEN afternoonCount ELSE 0 END + CASE WHEN eveningCount > 0 THEN eveningCount ELSE 0 END) FROM japa_entries")
    fun getTotalSandhyaCountFlow(): Flow<Int?>

    @Query("SELECT SUM(CASE WHEN pratahPunascharanaCount > 0 THEN pratahPunascharanaCount ELSE 0 END + CASE WHEN madhyahnikaPunascharanaCount > 0 THEN madhyahnikaPunascharanaCount ELSE 0 END + CASE WHEN sayamPunascharanaCount > 0 THEN sayamPunascharanaCount ELSE 0 END) FROM japa_entries")
    fun getTotalPunascharanaCountFlow(): Flow<Int?>

    @Query("DELETE FROM japa_entries")
    suspend fun clearAll()
}

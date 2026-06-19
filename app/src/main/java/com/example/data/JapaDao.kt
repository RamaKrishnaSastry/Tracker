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

    @Query("SELECT SUM(morningCount + afternoonCount + eveningCount + pratahPunascharanaCount + madhyahnikaPunascharanaCount + sayamPunascharanaCount) FROM japa_entries")
    fun getTotalCountFlow(): Flow<Int?>

    @Query("SELECT SUM(morningCount + afternoonCount + eveningCount) FROM japa_entries")
    fun getTotalSandhyaCountFlow(): Flow<Int?>

    @Query("SELECT SUM(pratahPunascharanaCount + madhyahnikaPunascharanaCount + sayamPunascharanaCount) FROM japa_entries")
    fun getTotalPunascharanaCountFlow(): Flow<Int?>

    @Query("DELETE FROM japa_entries")
    suspend fun clearAll()
}

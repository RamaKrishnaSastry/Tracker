package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomPracticeDao {
    @Query("SELECT * FROM custom_practices ORDER BY displayOrder ASC, id ASC")
    fun getAllPracticesFlow(): Flow<List<CustomPractice>>

    @Query("SELECT * FROM custom_practices ORDER BY displayOrder ASC, id ASC")
    suspend fun getAllPractices(): List<CustomPractice>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPractice(practice: CustomPractice): Long

    @Query("DELETE FROM custom_practice_entries WHERE practiceId = :practiceId")
    suspend fun deleteEntriesForPractice(practiceId: Long)

    @Query("DELETE FROM japa_sessions WHERE practiceId = :practiceId")
    suspend fun deleteSessionsForPractice(practiceId: Long)

    @Delete
    suspend fun deletePractice(practice: CustomPractice)

    @Update
    suspend fun updatePractice(practice: CustomPractice)

    @Query("SELECT * FROM custom_practices WHERE isArchived = 0 ORDER BY displayOrder ASC")
    fun getActivePracticesFlow(): Flow<List<CustomPractice>>

    @Query("SELECT * FROM custom_practices WHERE isArchived = 1 ORDER BY displayOrder ASC")
    fun getArchivedPracticesFlow(): Flow<List<CustomPractice>>

    @Query("UPDATE custom_practices SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateArchivedStatus(id: Long, isArchived: Boolean)

    @Query("UPDATE custom_practices SET displayOrder = :order WHERE id = :id")
    suspend fun updatePracticeOrder(id: Long, order: Int)

    @Query("SELECT * FROM custom_practices WHERE id = :id LIMIT 1")
    suspend fun getPracticeById(id: Long): CustomPractice?

    @Query("SELECT * FROM custom_practice_entries WHERE practiceId = :practiceId ORDER BY date DESC")
    fun getEntriesForPracticeFlow(practiceId: Long): Flow<List<CustomPracticeEntry>>

    @Query("SELECT * FROM custom_practice_entries WHERE practiceId = :practiceId AND date = :date LIMIT 1")
    suspend fun getEntryByDate(practiceId: Long, date: String): CustomPracticeEntry?

    @Query("SELECT * FROM custom_practice_entries WHERE practiceId = :practiceId AND date = :date LIMIT 1")
    fun getEntryByDateFlow(practiceId: Long, date: String): Flow<CustomPracticeEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEntry(entry: CustomPracticeEntry)
    
    @Query("SELECT * FROM custom_practice_entries ORDER BY date DESC")
    fun getAllCustomPracticeEntriesFlow(): Flow<List<CustomPracticeEntry>>

    @Query("SELECT * FROM custom_practice_entries")
    suspend fun getAllCustomPracticeEntries(): List<CustomPracticeEntry>
    
    @Query("SELECT practiceId, SUM(CASE WHEN count>0 THEN count ELSE 0 END + CASE WHEN morningCount>0 THEN morningCount ELSE 0 END + CASE WHEN afternoonCount>0 THEN afternoonCount ELSE 0 END + CASE WHEN eveningCount>0 THEN eveningCount ELSE 0 END) as totalCount, SUM(CASE WHEN morningPunasCount>0 THEN morningPunasCount ELSE 0 END + CASE WHEN afternoonPunasCount>0 THEN afternoonPunasCount ELSE 0 END + CASE WHEN eveningPunasCount>0 THEN eveningPunasCount ELSE 0 END) as punasCount FROM custom_practice_entries GROUP BY practiceId")
    fun getAllPracticeTotalsFlow(): Flow<List<PracticeTotal>>

    // JapaSession CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: JapaSession): Long

    @Query("SELECT * FROM japa_sessions WHERE practiceId = :practiceId AND date = :date ORDER BY id DESC")
    fun getSessionsForPracticeAndDateFlow(practiceId: Long, date: String): Flow<List<JapaSession>>

    @Query("SELECT * FROM japa_sessions WHERE practiceId = :practiceId AND date = :date ORDER BY id DESC")
    suspend fun getSessionsForPracticeAndDate(practiceId: Long, date: String): List<JapaSession>

    @Query("DELETE FROM japa_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM japa_sessions WHERE practiceId = :practiceId AND date = :date")
    suspend fun clearSessionsForPracticeAndDate(practiceId: Long, date: String)
}

data class PracticeTotal(
    val practiceId: Long,
    val totalCount: Int,
    val punasCount: Int = 0
)

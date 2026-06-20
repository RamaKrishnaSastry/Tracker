package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "japa_sessions")
data class JapaSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val practiceId: Long, // GAYATRI_PRACTICE_ID or CustomPractice.id
    val date: String, // YYYY-MM-DD
    val time: String, // e.g. "06:30 AM" or "12:45 PM"
    val count: Int,
    val typeDetail: String = "" // e.g., "Morning", "Midday", "Evening", "Session"
)

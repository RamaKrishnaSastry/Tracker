package com.example.data

import androidx.room.Entity

@Entity(tableName = "custom_practice_entries", primaryKeys = ["practiceId", "date"])
data class CustomPracticeEntry(
    val practiceId: Long,
    val date: String, // YYYY-MM-DD
    val count: Int = 0, // Used for CONTINUOUS and RECITATION
    val morningCount: Int = 0,
    val afternoonCount: Int = 0,
    val eveningCount: Int = 0,
    val morningPunasCount: Int = 0,
    val afternoonPunasCount: Int = 0,
    val eveningPunasCount: Int = 0,
    val updatedAt: String
) {
    val dailyTotal: Int
        get() = count + morningCount + afternoonCount + eveningCount + morningPunasCount + afternoonPunasCount + eveningPunasCount
}

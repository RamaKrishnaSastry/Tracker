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
        get() = maxOf(0, count) + maxOf(0, morningCount) + maxOf(0, afternoonCount) + maxOf(0, eveningCount) + maxOf(0, morningPunasCount) + maxOf(0, afternoonPunasCount) + maxOf(0, eveningPunasCount)
}

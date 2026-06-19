package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "japa_entries")
data class JapaEntry(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    
    @ColumnInfo(name = "morningCount")
    val pratahSandhyaCount: Int = 0,
    
    @ColumnInfo(name = "afternoonCount")
    val madhyahnikaSandhyaCount: Int = 0,
    
    @ColumnInfo(name = "eveningCount")
    val sayamSandhyaCount: Int = 0,
    
    @ColumnInfo(name = "pratahPunascharanaCount", defaultValue = "0")
    val pratahPunascharanaCount: Int = 0,
    
    @ColumnInfo(name = "madhyahnikaPunascharanaCount", defaultValue = "0")
    val madhyahnikaPunascharanaCount: Int = 0,
    
    @ColumnInfo(name = "sayamPunascharanaCount", defaultValue = "0")
    val sayamPunascharanaCount: Int = 0,
    
    val updatedAt: String // ISO-8601 UTC timestamp
) {
    val dailyTotal: Int
        get() = pratahSandhyaCount + pratahPunascharanaCount +
                madhyahnikaSandhyaCount + madhyahnikaPunascharanaCount +
                sayamSandhyaCount + sayamPunascharanaCount
}

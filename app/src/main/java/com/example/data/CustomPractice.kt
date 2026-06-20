package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_practices")
data class CustomPractice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val practiceType: String = "CONTINUOUS", // SANDHYA, CONTINUOUS, RECITATION
    val defaultTarget: Int = 108,
    val punasTarget: Int = 0, // optional punascharana target
    val quickAddValues: String = "10,54,108", // comma-separated values
    val initialLifetimeCount: Long = 0L,
    val reminderTime: String = "", // e.g., "06:30"
    val isReminderEnabled: Boolean = false,
    val themeColor: String = "Teal", // Indigo, Teal, Saffron, Crimson, Emerald, Amber, Slate
    val isWidgetPinned: Boolean = false,
    val isArchived: Boolean = false,
    val displayOrder: Int = 0,
    
    // Type-specific field extensions
    val isMorningEnabled: Boolean = true,
    val isMiddayEnabled: Boolean = true,
    val isEveningEnabled: Boolean = true,
    val isPunascharanaEnabled: Boolean = true,
    val incrementValue: Int = 1
)

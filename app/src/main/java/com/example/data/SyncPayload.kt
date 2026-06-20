package com.example.data

data class SyncPayload(
    val version: Int = 3,
    val lastSynced: String,
    val initialLifetimeCount: Long,
    val settings: SyncSettings = SyncSettings(),
    val entries: Map<String, SyncEntry> = emptyMap(),
    // Version 3 addition:
    val customPractices: List<SyncCustomPractice> = emptyList(),
    val customEntries: List<SyncCustomPracticeEntry> = emptyList()
)

data class SyncSettings(
    val themeMode: String = "dark",
    val isPunascharanaEnabled: Boolean? = true,
    val gayatriColor: String? = "violet",
    val universalColor: String? = "royal",
    val updatedAt: String? = "",
    val initialLifetimeCountUpdatedAt: String? = ""
)

data class SyncEntry(
    val morning: Int,
    val afternoon: Int,
    val evening: Int,
    val pratahPunascharana: Int? = 0,
    val madhyahnikaPunascharana: Int? = 0,
    val sayamPunascharana: Int? = 0,
    val updatedAt: String
)

data class SyncCustomPractice(
    val id: Long,
    val name: String,
    val practiceType: String = "CONTINUOUS",
    val defaultTarget: Int = 108,
    val punasTarget: Int = 0,
    val quickAddValues: String = "10,54,108",
    val initialLifetimeCount: Long = 0L,
    val reminderTime: String = "",
    val isReminderEnabled: Boolean = false,
    val themeColor: String = "Teal",
    val isWidgetPinned: Boolean = false,
    val isArchived: Boolean = false,
    val displayOrder: Int = 0,
    val isMorningEnabled: Boolean = true,
    val isMiddayEnabled: Boolean = true,
    val isEveningEnabled: Boolean = true,
    val isPunascharanaEnabled: Boolean = true,
    val incrementValue: Int = 1
)

data class SyncCustomPracticeEntry(
    val practiceId: Long,
    val date: String,
    val count: Int = 0,
    val morningCount: Int = 0,
    val afternoonCount: Int = 0,
    val eveningCount: Int = 0,
    val morningPunasCount: Int = 0,
    val afternoonPunasCount: Int = 0,
    val eveningPunasCount: Int = 0,
    val updatedAt: String
)

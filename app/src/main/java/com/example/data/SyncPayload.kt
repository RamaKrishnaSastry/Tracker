package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncPayload(
    val version: Int = 2, // Upgraded version for new fields
    val lastSynced: String,
    val initialLifetimeCount: Long,
    val settings: SyncSettings,
    val entries: Map<String, SyncEntry>
)

@JsonClass(generateAdapter = true)
data class SyncSettings(
    val themeMode: String
)

@JsonClass(generateAdapter = true)
data class SyncEntry(
    val morning: Int,
    val afternoon: Int,
    val evening: Int,
    val pratahPunascharana: Int? = 0,
    val madhyahnikaPunascharana: Int? = 0,
    val sayamPunascharana: Int? = 0,
    val updatedAt: String
)

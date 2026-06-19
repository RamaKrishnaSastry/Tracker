package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("gayatri_japa_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _initialLifetimeCount = MutableStateFlow(getInitialLifetimeCount())
    val initialLifetimeCount: StateFlow<Long> = _initialLifetimeCount.asStateFlow()

    fun getThemeMode(): String = prefs.getString("theme_mode", "system") ?: "system"
    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun getMorningReminderEnabled(): Boolean = prefs.getBoolean("morning_reminder_enabled", false)
    fun setMorningReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("morning_reminder_enabled", enabled).apply()
    }

    fun getAfternoonReminderEnabled(): Boolean = prefs.getBoolean("afternoon_reminder_enabled", false)
    fun setAfternoonReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("afternoon_reminder_enabled", enabled).apply()
    }

    fun getEveningReminderEnabled(): Boolean = prefs.getBoolean("evening_reminder_enabled", false)
    fun setEveningReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("evening_reminder_enabled", enabled).apply()
    }

    fun getMorningReminderTime(): String = prefs.getString("morning_reminder_time", "06:00") ?: "06:00"
    fun setMorningReminderTime(time: String) {
        prefs.edit().putString("morning_reminder_time", time).apply()
    }

    fun getAfternoonReminderTime(): String = prefs.getString("afternoon_reminder_time", "12:00") ?: "12:00"
    fun setAfternoonReminderTime(time: String) {
        prefs.edit().putString("afternoon_reminder_time", time).apply()
    }

    fun getEveningReminderTime(): String = prefs.getString("evening_reminder_time", "18:00") ?: "18:00"
    fun setEveningReminderTime(time: String) {
        prefs.edit().putString("evening_reminder_time", time).apply()
    }

    // Reminder intensity settings: "disabled", "minimal", "standard" (Default: "minimal")
    fun getReminderIntensity(): String = prefs.getString("reminder_intensity", "minimal") ?: "minimal"
    fun setReminderIntensity(intensity: String) {
        prefs.edit().putString("reminder_intensity", intensity).apply()
    }

    // Keep track of last notified date to enforce max 1 reminder per day per Sandhya
    fun getLastRemindedDate(type: String): String = prefs.getString("last_reminded_date_${type}", "") ?: ""
    fun setLastRemindedDate(type: String, dateStr: String) {
        prefs.edit().putString("last_reminded_date_${type}", dateStr).apply()
    }

    fun getInitialLifetimeCount(): Long = prefs.getLong("initial_lifetime_count", 0L)
    fun setInitialLifetimeCount(count: Long) {
        prefs.edit().putLong("initial_lifetime_count", count).apply()
        _initialLifetimeCount.value = count
    }

    fun getLastSync(): String = prefs.getString("last_sync", "Never") ?: "Never"
    fun setLastSync(status: String) {
        prefs.edit().putString("last_sync", status).apply()
    }

    fun getLastSyncTimeMs(): Long = prefs.getLong("last_sync_time_ms", 0L)
    fun setLastSyncTimeMs(timeMs: Long) {
        prefs.edit().putLong("last_sync_time_ms", timeMs).apply()
    }

    private val _isPunascharanaEnabled = MutableStateFlow(isPunascharanaEnabled())
    val isPunascharanaEnabled: StateFlow<Boolean> = _isPunascharanaEnabled.asStateFlow()

    fun isPunascharanaEnabled(): Boolean = prefs.getBoolean("is_punascharana_enabled", true)
    fun setPunascharanaEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_punascharana_enabled", enabled).apply()
        _isPunascharanaEnabled.value = enabled
    }

    fun isOnboarded(): Boolean = prefs.getBoolean("is_onboarded", false)
    fun setOnboarded(onboarded: Boolean) {
        prefs.edit().putBoolean("is_onboarded", onboarded).apply()
    }
}

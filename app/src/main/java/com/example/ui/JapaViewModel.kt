package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.*
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class JapaViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "JapaViewModel"

    val database = AppDatabase.getDatabase(application)
    val dao = database.japaDao()
    val prefs = PreferencesManager(application)
    val authManager = AuthManager(application)
    val syncService = GoogleDriveSyncService()
    val repository = JapaRepository(application, dao, prefs, authManager, syncService)
    val reminderManager = ReminderManager(application)

    // Reactive states
    val themeMode: StateFlow<String> = prefs.themeMode
    val initialLifetimeCount: StateFlow<Long> = prefs.initialLifetimeCount
    val isPunascharanaEnabled: StateFlow<Boolean> = prefs.isPunascharanaEnabled
    val userProfile = authManager.userProfile

    val allEntries: StateFlow<List<JapaEntry>> = repository.allEntriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _todayEntry = MutableStateFlow<JapaEntry?>(null)
    val todayEntry: StateFlow<JapaEntry?> = _todayEntry.asStateFlow()

    private val _onboardingStep = MutableStateFlow(if (prefs.isOnboarded()) 4 else 1)
    val onboardingStep: StateFlow<Int> = _onboardingStep.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Status messages for sync progress
    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun setAuthError(error: String?) {
        _authError.value = error
    }

    fun clearAuthError() {
        _authError.value = null
    }

    init {
        ensureTodayEntryExists()
        viewModelScope.launch {
            if (prefs.isOnboarded()) {
                authManager.checkCurrentSession {
                    viewModelScope.launch {
                        repository.syncWithCloud()
                    }
                }
            }
        }
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun ensureTodayEntryExists() {
        viewModelScope.launch {
            val todayStr = getTodayDateString()
            val existing = dao.getEntryByDate(todayStr)
            if (existing == null) {
                val newEntry = JapaEntry(
                    date = todayStr,
                    pratahSandhyaCount = 0,
                    madhyahnikaSandhyaCount = 0,
                    sayamSandhyaCount = 0,
                    pratahPunascharanaCount = 0,
                    madhyahnikaPunascharanaCount = 0,
                    sayamPunascharanaCount = 0,
                    updatedAt = repository.getCurrentUTCTimestamp()
                )
                dao.insertOrUpdate(newEntry)
                Log.d(TAG, "Auto-created fresh JapaEntry for today: $todayStr")
            }
            // Listen continuously to today's entry for instant reactive updates in the main dashboard view
            dao.getEntryByDateFlow(todayStr).collect {
                _todayEntry.value = it
            }
        }
    }

    /**
     * Set explicit counts or increment counts for a date.
     */
    fun updateCounts(
        date: String,
        morning: Int,
        afternoon: Int,
        evening: Int,
        pratahPunas: Int = _todayEntry.value?.pratahPunascharanaCount ?: 0,
        madhyahnikaPunas: Int = _todayEntry.value?.madhyahnikaPunascharanaCount ?: 0,
        sayamPunas: Int = _todayEntry.value?.sayamPunascharanaCount ?: 0
    ) {
        repository.saveEntry(date, morning, afternoon, evening, pratahPunas, madhyahnikaPunas, sayamPunas) {
            if (date == getTodayDateString()) {
                // Instantly update local today state
                _todayEntry.value = _todayEntry.value?.copy(
                    pratahSandhyaCount = morning,
                    madhyahnikaSandhyaCount = afternoon,
                    sayamSandhyaCount = evening,
                    pratahPunascharanaCount = pratahPunas,
                    madhyahnikaPunascharanaCount = madhyahnikaPunas,
                    sayamPunascharanaCount = sayamPunas
                )
            }
        }
    }

    /**
     * Compute summary statistics on historical items
     */
    fun getStatisticsFlow(): Flow<Stats> {
        return combine(allEntries, initialLifetimeCount) { entries, initial ->
            val todayStr = getTodayDateString()
            val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

            var todayTotal = 0
            var currentMonthTotal = 0
            var lifetimeSandhyaTotal = 0L
            var lifetimePunascharanaTotal = 0L

            entries.forEach { entry ->
                lifetimeSandhyaTotal += entry.pratahSandhyaCount + entry.madhyahnikaSandhyaCount + entry.sayamSandhyaCount
                lifetimePunascharanaTotal += entry.pratahPunascharanaCount + entry.madhyahnikaPunascharanaCount + entry.sayamPunascharanaCount

                val total = entry.dailyTotal
                if (entry.date == todayStr) {
                    todayTotal = total
                }
                if (entry.date.startsWith(currentMonthStr)) {
                    currentMonthTotal += total
                }
            }

            Stats(
                todayTotal = todayTotal,
                currentMonthTotal = currentMonthTotal,
                lifetimeTotal = initial + lifetimeSandhyaTotal + lifetimePunascharanaTotal,
                lifetimeSandhyaTotal = lifetimeSandhyaTotal,
                lifetimePunascharanaTotal = lifetimePunascharanaTotal
            )
        }
    }

    /**
     * Set onboarding variables
     */
    fun setCompletedOnboarding(initialCount: Long) {
        prefs.setInitialLifetimeCount(initialCount)
        prefs.setOnboarded(true)
        _onboardingStep.value = 4
        // Silently sync settings to cloud if auth exists
        viewModelScope.launch {
            repository.syncWithCloud()
        }
    }

    fun setOnboardingStep(step: Int) {
        _onboardingStep.value = step
    }

    fun updateThemeMode(mode: String) {
        prefs.setThemeMode(mode)
        viewModelScope.launch {
            repository.syncWithCloud()
        }
    }

    fun updateInitialLifetimeCount(count: Long) {
        prefs.setInitialLifetimeCount(count)
        viewModelScope.launch {
            repository.syncWithCloud()
        }
    }

    fun setPunascharanaEnabled(enabled: Boolean) {
        prefs.setPunascharanaEnabled(enabled)
        viewModelScope.launch {
            repository.syncWithCloud()
        }
    }

    // Reminder toggles
    fun setMorningReminder(enabled: Boolean, time: String) {
        prefs.setMorningReminderEnabled(enabled)
        prefs.setMorningReminderTime(time)
        reminderManager.scheduleAlarms()
    }

    fun setAfternoonReminder(enabled: Boolean, time: String) {
        prefs.setAfternoonReminderEnabled(enabled)
        prefs.setAfternoonReminderTime(time)
        reminderManager.scheduleAlarms()
    }

    fun setEveningReminder(enabled: Boolean, time: String) {
        prefs.setEveningReminderEnabled(enabled)
        prefs.setEveningReminderTime(time)
        reminderManager.scheduleAlarms()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            _syncMessage.value = "Syncing with Google Drive..."
            repository.syncWithCloud()
            _syncMessage.value = "Synced: ${prefs.getLastSync()}"
        }
    }

    fun signOutUser() {
        authManager.signOut {
            viewModelScope.launch {
                prefs.setLastSync("Never")
                prefs.setLastSyncTimeMs(0L)
            }
        }
    }

    data class Stats(
        val todayTotal: Int,
        val currentMonthTotal: Int,
        val lifetimeTotal: Long,
        val lifetimeSandhyaTotal: Long,
        val lifetimePunascharanaTotal: Long
    )
}

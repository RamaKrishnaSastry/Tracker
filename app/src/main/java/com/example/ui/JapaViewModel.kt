package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.*
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

const val GAYATRI_PRACTICE_ID = -1L

class JapaViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "JapaViewModel"

    val database = AppDatabase.getDatabase(application)
    val dao = database.japaDao()
    val customPracticeDao = database.customPracticeDao()
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
    val gayatriColor: StateFlow<String> = prefs.gayatriColor
    val gayatriName: StateFlow<String> = prefs.gayatriName
    val universalColor: StateFlow<String> = prefs.universalColor

    // Practice selection state
    private val _activePracticeId = MutableStateFlow(prefs.getActivePracticeId())
    val activePracticeId: StateFlow<Long> = _activePracticeId.asStateFlow()
    val defaultPracticeId: StateFlow<Long> = prefs.defaultPracticeId

    private val gayatriMetaFlow = combine(gayatriName, gayatriColor, defaultPracticeId) { name, color, defaultId ->
        Triple(name, color, defaultId)
    }

    val allCustomPractices: StateFlow<List<CustomPractice>> = combine(
        customPracticeDao.getAllPracticesFlow(),
        repository.allEntriesFlow,
        activePracticeId,
        gayatriMetaFlow
    ) { customPractices, entries, activeId, meta ->
        val (name, color, defaultId) = meta
        val hasGayatriData = activeId == GAYATRI_PRACTICE_ID || 
                             defaultId == GAYATRI_PRACTICE_ID || 
                             entries.any { it.pratahSandhyaCount > 0 || it.madhyahnikaSandhyaCount > 0 || it.sayamSandhyaCount > 0 || it.pratahPunascharanaCount > 0 || it.madhyahnikaPunascharanaCount > 0 || it.sayamPunascharanaCount > 0 }
        
        if (hasGayatriData) {
            val gayatriPractice = CustomPractice(
                id = GAYATRI_PRACTICE_ID,
                name = name,
                practiceType = "SANDHYA",
                themeColor = color,
                isPunascharanaEnabled = true,
                isMorningEnabled = true,
                isMiddayEnabled = true,
                isEveningEnabled = true,
                quickAddValues = "10,24,28,54,108"
            )
            listOf(gayatriPractice) + customPractices
        } else {
            customPractices
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCustomPractices: StateFlow<List<CustomPractice>> = combine(
        customPracticeDao.getActivePracticesFlow(),
        repository.allEntriesFlow,
        activePracticeId,
        gayatriMetaFlow
    ) { activeCustom, entries, activeId, meta ->
        val (name, color, defaultId) = meta
        val hasGayatriData = activeId == GAYATRI_PRACTICE_ID || 
                             defaultId == GAYATRI_PRACTICE_ID || 
                             entries.any { it.pratahSandhyaCount > 0 || it.madhyahnikaSandhyaCount > 0 || it.sayamSandhyaCount > 0 || it.pratahPunascharanaCount > 0 || it.madhyahnikaPunascharanaCount > 0 || it.sayamPunascharanaCount > 0 }
        
        if (hasGayatriData) {
            val gayatriPractice = CustomPractice(
                id = GAYATRI_PRACTICE_ID,
                name = name,
                practiceType = "SANDHYA",
                themeColor = color,
                isPunascharanaEnabled = true,
                isMorningEnabled = true,
                isMiddayEnabled = true,
                isEveningEnabled = true,
                quickAddValues = "10,24,28,54,108"
            )
            listOf(gayatriPractice) + activeCustom
        } else {
            activeCustom
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedCustomPractices: StateFlow<List<CustomPractice>> = customPracticeDao.getArchivedPracticesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPracticeTotals: StateFlow<Map<Long, Int>> = customPracticeDao.getAllPracticeTotalsFlow()
        .map { list -> list.associate { it.practiceId to it.totalCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allPracticePunasTotals: StateFlow<Map<Long, Int>> = customPracticeDao.getAllPracticeTotalsFlow()
        .map { list -> list.associate { it.practiceId to it.punasCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allEntries: StateFlow<List<JapaEntry>> = repository.allEntriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomPracticeEntries: StateFlow<List<CustomPracticeEntry>> = customPracticeDao.getAllCustomPracticeEntriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeCustomEntry = MutableStateFlow<CustomPracticeEntry?>(null)
    val activeCustomEntry: StateFlow<CustomPracticeEntry?> = _activeCustomEntry.asStateFlow()

    private val _todayEntry = MutableStateFlow<JapaEntry?>(null)
    val todayEntry: StateFlow<JapaEntry?> = _todayEntry.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentPracticeHistory: StateFlow<List<CustomPracticeEntry>> = activePracticeId.flatMapLatest { pid ->
        if (pid == GAYATRI_PRACTICE_ID) {
            flowOf(emptyList())
        } else {
            customPracticeDao.getEntriesForPracticeFlow(pid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _onboardingStep = MutableStateFlow(if (prefs.isOnboarded()) 4 else 1)
    val onboardingStep: StateFlow<Int> = _onboardingStep.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Status messages for sync progress
    val syncMessage: StateFlow<String> = repository.syncState.map { state ->
        when (state) {
            SyncState.DISCONNECTED -> "Not connected to Google Drive."
            SyncState.OFFLINE -> "Offline. Sync paused."
            SyncState.SYNC_PENDING -> "Sync pending (offline)."
            SyncState.SYNCING -> "Syncing with Google Drive..."
            SyncState.SYNCED -> "Synced: ${prefs.getLastSync()}"
            SyncState.ERROR -> repository.lastSyncError.value ?: "Sync failed. Check connection."
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Not connected to Google Drive.")

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun setAuthError(error: String?) {
        _authError.value = error
    }

    fun clearAuthError() {
        _authError.value = null
    }

    private val _todayDate = MutableStateFlow(getTodayDateString())
    val todayDate: StateFlow<String> = _todayDate.asStateFlow()

    init {
        ensureTodayEntryExists()
        
        // Use default practice ID if set and app just opened (active is -1)
        val currentPid = prefs.getActivePracticeId()
        val defaultPid = prefs.getDefaultPracticeId()

        // Only set active practice if it was already set, don't default to Gayatri Japa if not onboarded
        if (currentPid != -1L) {
             _activePracticeId.value = currentPid
        } else if (defaultPid != -1L) {
            _activePracticeId.value = defaultPid
            prefs.setActivePracticeId(defaultPid)
        }



        viewModelScope.launch {
            if (prefs.isOnboarded()) {
                authManager.checkCurrentSession {
                    viewModelScope.launch {
                        repository.syncWithCloud()
                    }
                }
            }
        }
        
        // Update todayDate
        viewModelScope.launch {
            while (true) {
                delay(60000) // Check every minute
                val newDate = getTodayDateString()
                if (newDate != _todayDate.value) {
                    _todayDate.value = newDate
                    ensureTodayEntryExists()
                }
            }
        }
        
        // Setup todayEntry continuous listener that scales reactively with todayDate
        var todayEntryJob: kotlinx.coroutines.Job? = null
        viewModelScope.launch {
            _todayDate.collect { todayStr ->
                todayEntryJob?.cancel()
                todayEntryJob = launch {
                    dao.getEntryByDateFlow(todayStr).collect {
                        _todayEntry.value = it
                    }
                }
            }
        }
        
        // Setup active custom entry continuous listener, eliminating blocking nested collections
        var customTrackingJob: kotlinx.coroutines.Job? = null
        viewModelScope.launch {
            combine(activePracticeId, todayDate) { pid, date -> pid to date }.collect { (pid, date) ->
                customTrackingJob?.cancel()
                if (pid != GAYATRI_PRACTICE_ID) {
                    customTrackingJob = launch {
                        customPracticeDao.getEntryByDateFlow(pid, date).collect {
                            _activeCustomEntry.value = it
                        }
                    }
                } else {
                    _activeCustomEntry.value = null
                }
            }
        }

        viewModelScope.launch {
            userProfile.collect { profile ->
                if (profile != null) {
                    val currentName = prefs.getUserName()
                    if (currentName.isBlank() || currentName == "Gayatri Practitioner" || currentName == "Japa Mitra" || currentName == "User") {
                        setUserName(profile.displayName)
                    }
                }
            }
        }
    }

    fun setActivePractice(practiceId: Long) {
        _activePracticeId.value = practiceId
        prefs.setActivePracticeId(practiceId)
    }

    fun setDefaultPractice(practiceId: Long) {
        prefs.setDefaultPracticeId(practiceId)
    }

    private val _userName = MutableStateFlow(prefs.getUserName())
    val userName: StateFlow<String> = _userName.asStateFlow()
    fun setUserName(name: String) {
        prefs.setUserName(name)
        _userName.value = name
    }

    fun setGayatriColor(color: String) {
        prefs.setGayatriColor(color)
        GayatriWidgetHelper.triggerAllWidgetsUpdate(getApplication())
    }

    fun setGayatriName(name: String) {
        prefs.setGayatriName(name)
        viewModelScope.launch {
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun setUniversalColor(color: String) {
        prefs.setUniversalColor(color)
        GayatriWidgetHelper.triggerAllWidgetsUpdate(getApplication())
    }

    fun addNewPractice(name: String, defaultTarget: Int = 108) {
        viewModelScope.launch {
            customPracticeDao.insertPractice(CustomPractice(name = name, defaultTarget = defaultTarget))
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun addNewPractice(practice: CustomPractice, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val newId = customPracticeDao.insertPractice(practice)
            prefs.updateSettingsTimestamp()
            repository.syncWithCloud(isLocalUpdate = true)
            onCreated(newId)
        }
    }

    fun updatePractice(practice: CustomPractice) {
        viewModelScope.launch {
            customPracticeDao.updatePractice(practice)
            prefs.updateSettingsTimestamp()
            GayatriWidgetHelper.triggerAllWidgetsUpdate(getApplication())
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun addPractice(practice: CustomPractice) {
        viewModelScope.launch {
            val newId = customPracticeDao.insertPractice(practice)
            prefs.updateSettingsTimestamp()
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun deletePractice(practice: CustomPractice) {
        viewModelScope.launch {
            customPracticeDao.deleteEntriesForPractice(practice.id)
            customPracticeDao.deleteSessionsForPractice(practice.id)
            customPracticeDao.deletePractice(practice)
            prefs.updateSettingsTimestamp()
            if (_activePracticeId.value == practice.id) {
                _activePracticeId.value = GAYATRI_PRACTICE_ID
                prefs.setActivePracticeId(GAYATRI_PRACTICE_ID)
            }
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun updatePracticeOrder(practices: List<CustomPractice>) {
        viewModelScope.launch {
            practices.forEachIndexed { index, practice ->
                customPracticeDao.updatePracticeOrder(practice.id, index)
            }
            prefs.updateSettingsTimestamp()
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun togglePracticeArchived(id: Long, isArchived: Boolean) {
        viewModelScope.launch {
            customPracticeDao.updateArchivedStatus(id, isArchived)
            prefs.updateSettingsTimestamp()
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todaySessions: StateFlow<List<JapaSession>> = activePracticeId.flatMapLatest { pid ->
        customPracticeDao.getSessionsForPracticeAndDateFlow(pid, getTodayDateString())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logJapaSession(practiceId: Long, count: Int, typeDetail: String = "Session") {
        viewModelScope.launch {
            val dateStr = getTodayDateString()
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            val session = JapaSession(
                practiceId = practiceId,
                date = dateStr,
                time = timeStr,
                count = count,
                typeDetail = typeDetail
            )
            customPracticeDao.insertSession(session)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            customPracticeDao.deleteSessionById(sessionId)
        }
    }

    fun updateCustomPracticeCount(practiceId: Long, date: String, count: Int) {
        viewModelScope.launch {
            var existing = customPracticeDao.getEntryByDate(practiceId, date)
            if (existing == null) {
                existing = CustomPracticeEntry(
                    practiceId = practiceId,
                    date = date,
                    count = count,
                    updatedAt = repository.getCurrentUTCTimestamp()
                )
            } else {
                existing = existing.copy(count = count, updatedAt = repository.getCurrentUTCTimestamp())
            }
            customPracticeDao.insertOrUpdateEntry(existing)
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun updateCustomPracticeSandhyaCounts(
        practiceId: Long,
        date: String,
        morning: Int,
        afternoon: Int,
        evening: Int,
        morningPunas: Int = 0,
        afternoonPunas: Int = 0,
        eveningPunas: Int = 0
    ) {
        viewModelScope.launch {
            var existing = customPracticeDao.getEntryByDate(practiceId, date)
            if (existing == null) {
                existing = CustomPracticeEntry(
                    practiceId = practiceId,
                    date = date,
                    count = 0,
                    morningCount = morning,
                    afternoonCount = afternoon,
                    eveningCount = evening,
                    morningPunasCount = morningPunas,
                    afternoonPunasCount = afternoonPunas,
                    eveningPunasCount = eveningPunas,
                    updatedAt = repository.getCurrentUTCTimestamp()
                )
            } else {
                existing = existing.copy(
                    morningCount = morning,
                    afternoonCount = afternoon,
                    eveningCount = evening,
                    morningPunasCount = morningPunas,
                    afternoonPunasCount = afternoonPunas,
                    eveningPunasCount = eveningPunas,
                    updatedAt = repository.getCurrentUTCTimestamp()
                )
            }
            customPracticeDao.insertOrUpdateEntry(existing)
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun updateGayatriSandhyaCounts(
        date: String,
        morning: Int,
        afternoon: Int,
        evening: Int,
        morningPunas: Int = 0,
        afternoonPunas: Int = 0,
        eveningPunas: Int = 0
    ) {
        viewModelScope.launch {
            var existing = database.japaDao().getEntryByDate(date)
            if (existing == null) {
                existing = JapaEntry(
                    date = date,
                    pratahSandhyaCount = morning,
                    madhyahnikaSandhyaCount = afternoon,
                    sayamSandhyaCount = evening,
                    pratahPunascharanaCount = morningPunas,
                    madhyahnikaPunascharanaCount = afternoonPunas,
                    sayamPunascharanaCount = eveningPunas,
                    updatedAt = repository.getCurrentUTCTimestamp()
                )
            } else {
                existing = existing.copy(
                    pratahSandhyaCount = morning,
                    madhyahnikaSandhyaCount = afternoon,
                    sayamSandhyaCount = evening,
                    pratahPunascharanaCount = morningPunas,
                    madhyahnikaPunascharanaCount = afternoonPunas,
                    sayamPunascharanaCount = eveningPunas,
                    updatedAt = repository.getCurrentUTCTimestamp()
                )
            }
            database.japaDao().insertOrUpdate(existing)
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun handleGoogleSignInSuccess(onOnboarded: () -> Unit) {
        _onboardingStep.value = 4 // Loading step indicating sync
        viewModelScope.launch {
            val hasCloudData = repository.performInitialSetupSync()
            if (hasCloudData) {
                prefs.setOnboarded(true)
                onOnboarded()
            } else {
                _onboardingStep.value = 3 // Go to count input
            }
        }
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
            // Trigger widget updates so widgets reflect the updated/cleared counts instantly
            GayatriWidgetHelper.triggerAllWidgetsUpdate(getApplication())
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
                val p = if (entry.pratahSandhyaCount > 0) entry.pratahSandhyaCount else 0
                val m = if (entry.madhyahnikaSandhyaCount > 0) entry.madhyahnikaSandhyaCount else 0
                val s = if (entry.sayamSandhyaCount > 0) entry.sayamSandhyaCount else 0
                lifetimeSandhyaTotal += p + m + s
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

    fun setCompletedOnboardingWithGayatri(initialCount: Long) {
        prefs.setInitialLifetimeCount(initialCount)
        setActivePractice(GAYATRI_PRACTICE_ID)
        prefs.setOnboarded(true)
        _onboardingStep.value = 4
        viewModelScope.launch {
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun setCompletedOnboardingWithCustom(
        name: String,
        themeColor: String,
        type: String,
        defaultTarget: Int,
        quickAddValues: String,
        initialCount: Long
    ) {
        viewModelScope.launch {
            val newId = customPracticeDao.insertPractice(
                com.example.data.CustomPractice(
                    name = name,
                    practiceType = type,
                    defaultTarget = defaultTarget,
                    quickAddValues = quickAddValues,
                    initialLifetimeCount = initialCount,
                    themeColor = themeColor
                )
            )
            setActivePractice(newId)
            prefs.setOnboarded(true)
            _onboardingStep.value = 4
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun setOnboardingStep(step: Int) {
        _onboardingStep.value = step
    }

    fun updateThemeMode(mode: String) {
        prefs.setThemeMode(mode)
        viewModelScope.launch {
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun updateInitialLifetimeCount(count: Long) {
        prefs.setInitialLifetimeCount(count)
        viewModelScope.launch {
            repository.syncWithCloud(isLocalUpdate = true)
        }
    }

    fun setPunascharanaEnabled(enabled: Boolean) {
        prefs.setPunascharanaEnabled(enabled)
        viewModelScope.launch {
            repository.syncWithCloud(isLocalUpdate = true)
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
            repository.syncWithCloud()
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

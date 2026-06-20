package com.example.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class SyncState {
    DISCONNECTED,
    OFFLINE,
    SYNC_PENDING,
    SYNCING,
    SYNCED,
    ERROR
}

class JapaRepository(
    private val context: Context,
    private val dao: JapaDao,
    private val prefs: PreferencesManager,
    private val authManager: AuthManager,
    private val syncService: GoogleDriveSyncService
) {
    private val TAG = "JapaRepository"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val customPracticeDao = AppDatabase.getDatabase(context).customPracticeDao()

    val allEntriesFlow: Flow<List<JapaEntry>> = dao.getAllEntriesFlow()
    val totalCountFlow: Flow<Int?> = dao.getTotalCountFlow()
    val totalSandhyaCountFlow: Flow<Int?> = dao.getTotalSandhyaCountFlow()
    val totalPunascharanaCountFlow: Flow<Int?> = dao.getTotalPunascharanaCountFlow()

    private val _syncState = MutableStateFlow(SyncState.DISCONNECTED)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    init {
        scope.launch {
            authManager.userProfile.collect { profile ->
                if (profile == null) {
                    _syncState.value = SyncState.DISCONNECTED
                } else if (!isNetworkAvailable()) {
                    _syncState.value = SyncState.OFFLINE
                } else {
                    _syncState.value = SyncState.SYNCED
                }
            }
        }
    }

    private val dfUTC = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun getCurrentUTCTimestamp(): String {
        return dfUTC.format(Date())
    }

    /**
     * Increment or set counters for a specific date, immediately saving locally
     * and scheduling background synchronization.
     */
    fun saveEntry(
        date: String,
        morning: Int,
        afternoon: Int,
        evening: Int,
        pratahPunascharana: Int = 0,
        madhyahnikaPunascharana: Int = 0,
        sayamPunascharana: Int = 0,
        onComplete: (() -> Unit)? = null
    ) {
        scope.launch {
            val nowStr = getCurrentUTCTimestamp()
            val entry = JapaEntry(
                date = date,
                pratahSandhyaCount = morning,
                madhyahnikaSandhyaCount = afternoon,
                sayamSandhyaCount = evening,
                pratahPunascharanaCount = pratahPunascharana,
                madhyahnikaPunascharanaCount = madhyahnikaPunascharana,
                sayamPunascharanaCount = sayamPunascharana,
                updatedAt = nowStr
            )
            dao.insertOrUpdate(entry)
            Log.d(TAG, "Saved JapaEntry locally for $date: Sandhya($morning, $afternoon, $evening) Punas($pratahPunascharana, $madhyahnikaPunascharana, $sayamPunascharana)")
            syncWithCloud(isLocalUpdate = true)
            onComplete?.let {
                CoroutineScope(Dispatchers.Main).launch { it() }
            }
        }
    }

    /**
     * Checks if internet is available.
     */
    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun performInitialSetupSync(): Boolean = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext false

        val profile = authManager.userProfile.value ?: return@withContext false
        val token = authManager.getAccessToken() ?: return@withContext false

        _syncState.value = SyncState.SYNCING
        try {
            val cloudPayload = syncService.fetchCloudData(token)
            if (cloudPayload != null) {
                // If the file exists, we consider this user "already saved previously".
                prefs.setInitialLifetimeCount(cloudPayload.initialLifetimeCount)
                prefs.setThemeMode(cloudPayload.settings.themeMode)
                cloudPayload.settings.isPunascharanaEnabled?.let { prefs.setPunascharanaEnabled(it) }
                cloudPayload.settings.universalColor?.let { prefs.setUniversalColor(it) }

                if (cloudPayload.entries.isNotEmpty()) {
                    val cloudEntries = cloudPayload.entries.map { (date, cloudEntry) ->
                        JapaEntry(
                            date = date,
                            pratahSandhyaCount = cloudEntry.morning,
                            madhyahnikaSandhyaCount = cloudEntry.afternoon,
                            sayamSandhyaCount = cloudEntry.evening,
                            pratahPunascharanaCount = cloudEntry.pratahPunascharana ?: 0,
                            madhyahnikaPunascharanaCount = cloudEntry.madhyahnikaPunascharana ?: 0,
                            sayamPunascharanaCount = cloudEntry.sayamPunascharana ?: 0,
                            updatedAt = cloudEntry.updatedAt
                        )
                    }
                    dao.insertEntries(cloudEntries)
                }

                val localTimeDisp = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                prefs.setLastSync(localTimeDisp)
                prefs.setLastSyncTimeMs(System.currentTimeMillis())
                _lastSyncError.value = null
                _syncState.value = SyncState.SYNCED
                return@withContext true
            }
        } catch (e: Exception) {
            _lastSyncError.value = "Initial sync exception: ${e.message}"
            _syncState.value = SyncState.ERROR
        }
        
        // If not successful or empty payload, we continue to manual input
        _syncState.value = SyncState.DISCONNECTED
        return@withContext false
    }

    private var syncJob: kotlinx.coroutines.Job? = null

    /**
     * Silently coordinates background synchronization between local SQLite database and Google Drive.
     * Respects last-write-wins by comparing `updatedAt` timestamps.
     */
    fun syncWithCloud(isLocalUpdate: Boolean = false) {
        if (authManager.userProfile.value == null) {
            _syncState.value = SyncState.DISCONNECTED
            Log.d(TAG, "Sync aborted: No active authenticated Google session found.")
            return
        }

        if (!isNetworkAvailable()) {
            _syncState.value = if (isLocalUpdate) SyncState.SYNC_PENDING else SyncState.OFFLINE
            Log.d(TAG, "Sync aborted: Network unavailable.")
            return
        }

        // Cancel any ongoing sync job to debounce rapid taps.
        syncJob?.cancel()

        if (isLocalUpdate) {
            _syncState.value = SyncState.SYNC_PENDING
        } else {
            _syncState.value = SyncState.SYNCING
        }

        syncJob = scope.launch {
            // Debounce delay to prevent rapid API requests
            if (isLocalUpdate) {
                kotlinx.coroutines.delay(4000) // Give 4 seconds of inactivity before starting auto-sync!
                _syncState.value = SyncState.SYNCING
            }
            try {
                val profile = authManager.userProfile.value
                if (profile == null) {
                    _syncState.value = SyncState.DISCONNECTED
                    Log.d(TAG, "Sync aborted: No active authenticated Google session found.")
                    return@launch
                }

                val token = authManager.getAccessToken()
                if (token == null) {
                    _lastSyncError.value = "Failed to fetch Drive access token. Ensure Drive API is enabled."
                    _syncState.value = SyncState.ERROR
                    Log.d(TAG, "Sync aborted: Unable to fetch active access token.")
                    return@launch
                }

                Log.d(TAG, "Initiating Silent Sync with Google Drive...")
                val localEntries = dao.getAllEntries()
                val localPractices = customPracticeDao.getAllPractices()
                val localCustomEntries = customPracticeDao.getAllCustomPracticeEntries()

                val cloudPayload = syncService.fetchCloudData(token)

                val mergedEntries = mutableMapOf<String, JapaEntry>()
                localEntries.forEach { mergedEntries[it.date] = it }

                val mergedCustomPractices = mutableMapOf<Long, CustomPractice>()
                localPractices.forEach { mergedCustomPractices[it.id] = it }

                val mergedCustomEntries = mutableMapOf<String, CustomPracticeEntry>() // "practiceId_date"
                localCustomEntries.forEach { mergedCustomEntries["${it.practiceId}_${it.date}"] = it }

                var hasLocalNewerChanges = false
                var hasCloudNewerChanges = false

                // Synchronize global settings and initial count base using timestamps.
                var syncedInitialLifetimeCount = prefs.getInitialLifetimeCount()
                var syncedThemeMode = prefs.getThemeMode()
                var syncedPunasEnabled = prefs.isPunascharanaEnabled()
                var syncedGayatriColor = prefs.getGayatriColor()
                var syncedUniversalColor = prefs.getUniversalColor()

                if (cloudPayload != null) {
                    val localSettingsDate = parseTimestamp(prefs.getSettingsUpdatedAt())
                    val cloudSettingsDate = parseTimestamp(cloudPayload.settings.updatedAt ?: "")
                    val isCloudSettingsNewer = cloudSettingsDate.after(localSettingsDate)

                    // Granular check for initial lifetime count (the accumulated base)
                    val localCountDate = parseTimestamp(prefs.getInitialLifetimeCountUpdatedAt())
                    val cloudCountDate = parseTimestamp(cloudPayload.settings.initialLifetimeCountUpdatedAt ?: "")
                    
                    if (cloudCountDate.after(localCountDate)) {
                        // Cloud has a definitely newer accumulated count
                        if (cloudPayload.initialLifetimeCount != prefs.getInitialLifetimeCount()) {
                            syncedInitialLifetimeCount = cloudPayload.initialLifetimeCount
                            prefs.setInitialLifetimeCount(syncedInitialLifetimeCount)
                            hasCloudNewerChanges = true
                        }
                    } else if (localCountDate.after(cloudCountDate)) {
                        // Local has a newer accumulated count
                        syncedInitialLifetimeCount = prefs.getInitialLifetimeCount()
                        hasLocalNewerChanges = true
                    } else {
                        // Timestamps same - the values should ideally match. 
                        // If they don't, we'll keep local as the authoritative anchor for this device.
                        syncedInitialLifetimeCount = prefs.getInitialLifetimeCount()
                    }

                    if (isCloudSettingsNewer) {
                        // Cloud settings are newer, adopt cloud settings
                        if (cloudPayload.settings.themeMode != prefs.getThemeMode()) {
                            syncedThemeMode = cloudPayload.settings.themeMode
                            prefs.setThemeMode(syncedThemeMode)
                            hasCloudNewerChanges = true
                        }
                        val cloudPunas = cloudPayload.settings.isPunascharanaEnabled ?: true
                        if (cloudPunas != prefs.isPunascharanaEnabled()) {
                            syncedPunasEnabled = cloudPunas
                            prefs.setPunascharanaEnabled(syncedPunasEnabled)
                            hasCloudNewerChanges = true
                        }
                        val cloudGayatriColor = cloudPayload.settings.gayatriColor ?: "saffron"
                        if (cloudGayatriColor != prefs.getGayatriColor()) {
                            syncedGayatriColor = cloudGayatriColor
                            prefs.setGayatriColor(syncedGayatriColor)
                            hasCloudNewerChanges = true
                        }
                        val cloudUniversalColor = cloudPayload.settings.universalColor ?: "royal"
                        if (cloudUniversalColor != prefs.getUniversalColor()) {
                            syncedUniversalColor = cloudUniversalColor
                            prefs.setUniversalColor(syncedUniversalColor)
                            hasCloudNewerChanges = true
                        }
                    } else if (localSettingsDate.after(cloudSettingsDate)) {
                        // Local settings are newer
                        hasLocalNewerChanges = true
                        syncedThemeMode = prefs.getThemeMode()
                        syncedPunasEnabled = prefs.isPunascharanaEnabled()
                        syncedGayatriColor = prefs.getGayatriColor()
                        syncedUniversalColor = prefs.getUniversalColor()
                    }

                    // Compare timestamps per entry for Gayatri Japa
                    cloudPayload.entries.forEach { (date, cloudEntry) ->
                        val localEntry = mergedEntries[date]
                        if (localEntry == null) {
                            val newLocal = JapaEntry(
                                date = date,
                                pratahSandhyaCount = cloudEntry.morning,
                                madhyahnikaSandhyaCount = cloudEntry.afternoon,
                                sayamSandhyaCount = cloudEntry.evening,
                                pratahPunascharanaCount = cloudEntry.pratahPunascharana ?: 0,
                                madhyahnikaPunascharanaCount = cloudEntry.madhyahnikaPunascharana ?: 0,
                                sayamPunascharanaCount = cloudEntry.sayamPunascharana ?: 0,
                                updatedAt = cloudEntry.updatedAt
                            )
                            mergedEntries[date] = newLocal
                            hasCloudNewerChanges = true
                        } else {
                            val localDate = parseTimestamp(localEntry.updatedAt)
                            val cloudDate = parseTimestamp(cloudEntry.updatedAt)
                            if (cloudDate.after(localDate)) {
                                val updatedLocal = JapaEntry(
                                    date = date,
                                    pratahSandhyaCount = cloudEntry.morning,
                                    madhyahnikaSandhyaCount = cloudEntry.afternoon,
                                    sayamSandhyaCount = cloudEntry.evening,
                                    pratahPunascharanaCount = cloudEntry.pratahPunascharana ?: 0,
                                    madhyahnikaPunascharanaCount = cloudEntry.madhyahnikaPunascharana ?: 0,
                                    sayamPunascharanaCount = cloudEntry.sayamPunascharana ?: 0,
                                    updatedAt = cloudEntry.updatedAt
                                )
                                mergedEntries[date] = updatedLocal
                                hasCloudNewerChanges = true
                            } else if (localDate.after(cloudDate)) {
                                hasLocalNewerChanges = true
                            }
                        }
                    }

                    // Compare custom practices from cloud
                    // Merge custom practices. 
                    // If cloud settings are newer, we should respect deletions from cloud.
                    // If local settings are newer, we should respect local deletions.
                    
                    if (isCloudSettingsNewer) {
                        // Adopt cloud's custom practice list exactly (handles deletions)
                        val cloudIds = cloudPayload.customPractices.map { it.id }.toSet()
                        val localPractices = customPracticeDao.getAllPractices()
                        localPractices.forEach { lp ->
                            if (!cloudIds.contains(lp.id)) {
                                customPracticeDao.deletePractice(lp)
                                hasCloudNewerChanges = true
                            }
                        }
                        
                        cloudPayload.customPractices.forEach { cp ->
                            val localCp = customPracticeDao.getPracticeById(cp.id)
                            val cloudCpModel = CustomPractice(
                                id = cp.id,
                                name = cp.name,
                                practiceType = cp.practiceType,
                                defaultTarget = cp.defaultTarget,
                                punasTarget = cp.punasTarget,
                                quickAddValues = cp.quickAddValues,
                                initialLifetimeCount = cp.initialLifetimeCount,
                                reminderTime = cp.reminderTime,
                                isReminderEnabled = cp.isReminderEnabled,
                                themeColor = cp.themeColor,
                                isWidgetPinned = cp.isWidgetPinned,
                                isArchived = cp.isArchived,
                                displayOrder = cp.displayOrder,
                                isMorningEnabled = cp.isMorningEnabled,
                                isMiddayEnabled = cp.isMiddayEnabled,
                                isEveningEnabled = cp.isEveningEnabled,
                                isPunascharanaEnabled = cp.isPunascharanaEnabled,
                                incrementValue = cp.incrementValue
                            )
                            if (localCp == null) {
                                mergedCustomPractices[cp.id] = cloudCpModel
                                hasCloudNewerChanges = true
                            } else if (cloudCpModel != localCp) {
                                mergedCustomPractices[cp.id] = cloudCpModel
                                hasCloudNewerChanges = true
                            } else {
                                mergedCustomPractices[cp.id] = localCp
                            }
                        }
                    } else if (localSettingsDate.after(cloudSettingsDate)) {
                        // Local is newer, cloud will adopt our list in the upload phase
                        val localPractices = customPracticeDao.getAllPractices()
                        mergedCustomPractices.putAll(localPractices.associateBy { it.id })
                        hasLocalNewerChanges = true
                    } else {
                        // Same timestamp, sync matching
                        val localPractices = customPracticeDao.getAllPractices()
                        mergedCustomPractices.putAll(localPractices.associateBy { it.id })
                        
                        // Check if cloud has something we don't (unlikely if timestamps match, but for safety)
                        cloudPayload.customPractices.forEach { cp ->
                            if (!mergedCustomPractices.containsKey(cp.id)) {
                                val newLocalCp = CustomPractice(
                                    id = cp.id,
                                    name = cp.name,
                                    practiceType = cp.practiceType,
                                    defaultTarget = cp.defaultTarget,
                                    punasTarget = cp.punasTarget,
                                    quickAddValues = cp.quickAddValues,
                                    initialLifetimeCount = cp.initialLifetimeCount,
                                    reminderTime = cp.reminderTime,
                                    isReminderEnabled = cp.isReminderEnabled,
                                    themeColor = cp.themeColor,
                                    isWidgetPinned = cp.isWidgetPinned,
                                    isArchived = cp.isArchived,
                                    displayOrder = cp.displayOrder,
                                    isMorningEnabled = cp.isMorningEnabled,
                                    isMiddayEnabled = cp.isMiddayEnabled,
                                    isEveningEnabled = cp.isEveningEnabled,
                                    isPunascharanaEnabled = cp.isPunascharanaEnabled,
                                    incrementValue = cp.incrementValue
                                )
                                mergedCustomPractices[cp.id] = newLocalCp
                                hasCloudNewerChanges = true
                            }
                        }
                    }

                    // Compare custom entries from cloud
                    cloudPayload.customEntries.forEach { ce ->
                        val key = "${ce.practiceId}_${ce.date}"
                        val localCe = mergedCustomEntries[key]
                        if (localCe == null) {
                            val newLocalCe = CustomPracticeEntry(
                                practiceId = ce.practiceId,
                                date = ce.date,
                                count = ce.count,
                                morningCount = ce.morningCount,
                                afternoonCount = ce.afternoonCount,
                                eveningCount = ce.eveningCount,
                                morningPunasCount = ce.morningPunasCount,
                                afternoonPunasCount = ce.afternoonPunasCount,
                                eveningPunasCount = ce.eveningPunasCount,
                                updatedAt = ce.updatedAt
                            )
                            mergedCustomEntries[key] = newLocalCe
                            hasCloudNewerChanges = true
                        } else {
                            val localDate = parseTimestamp(localCe.updatedAt)
                            val cloudDate = parseTimestamp(ce.updatedAt)
                            if (cloudDate.after(localDate)) {
                                val updatedCe = CustomPracticeEntry(
                                    practiceId = ce.practiceId,
                                    date = ce.date,
                                    count = ce.count,
                                    morningCount = ce.morningCount,
                                    afternoonCount = ce.afternoonCount,
                                    eveningCount = ce.eveningCount,
                                    morningPunasCount = ce.morningPunasCount,
                                    afternoonPunasCount = ce.afternoonPunasCount,
                                    eveningPunasCount = ce.eveningPunasCount,
                                    updatedAt = ce.updatedAt
                                )
                                mergedCustomEntries[key] = updatedCe
                                hasCloudNewerChanges = true
                            } else if (localDate.after(cloudDate)) {
                                hasLocalNewerChanges = true
                            }
                        }
                    }

                    // Check if local has dates entirely missing from cloud
                    if (mergedEntries.keys.any { !cloudPayload.entries.containsKey(it) }) {
                        hasLocalNewerChanges = true
                    }
                    if (mergedCustomPractices.keys.any { id -> cloudPayload.customPractices.none { it.id == id } }) {
                        hasLocalNewerChanges = true
                    }
                    if (mergedCustomEntries.keys.any { key -> cloudPayload.customEntries.none { "${it.practiceId}_${it.date}" == key } }) {
                        hasLocalNewerChanges = true
                    }
                } else {
                    hasLocalNewerChanges = true
                }

                // If cloud has newer, insert into local database
                if (hasCloudNewerChanges) {
                    dao.insertEntries(mergedEntries.values.toList())
                    mergedCustomPractices.values.forEach {
                        customPracticeDao.insertPractice(it)
                    }
                    mergedCustomEntries.values.forEach {
                        customPracticeDao.insertOrUpdateEntry(it)
                    }
                    Log.d(TAG, "Local database updated with newer records from Cloud sync.")
                }

                // If local has newer, upload merged changes back up to the cloud
                if (hasLocalNewerChanges || cloudPayload == null) {
                    val entriesMap = mergedEntries.mapValues { (_, entry) ->
                        SyncEntry(
                            morning = entry.pratahSandhyaCount,
                            afternoon = entry.madhyahnikaSandhyaCount,
                            evening = entry.sayamSandhyaCount,
                            pratahPunascharana = entry.pratahPunascharanaCount,
                            madhyahnikaPunascharana = entry.madhyahnikaPunascharanaCount,
                            sayamPunascharana = entry.sayamPunascharanaCount,
                            updatedAt = entry.updatedAt
                        )
                    }
                    
                    val syncCustomPractices = mergedCustomPractices.values.map { cp ->
                        SyncCustomPractice(
                            id = cp.id,
                            name = cp.name,
                            practiceType = cp.practiceType,
                            defaultTarget = cp.defaultTarget,
                            punasTarget = cp.punasTarget,
                            quickAddValues = cp.quickAddValues,
                            initialLifetimeCount = cp.initialLifetimeCount,
                            reminderTime = cp.reminderTime,
                            isReminderEnabled = cp.isReminderEnabled,
                            themeColor = cp.themeColor,
                            isWidgetPinned = cp.isWidgetPinned,
                            isArchived = cp.isArchived,
                            displayOrder = cp.displayOrder,
                            isMorningEnabled = cp.isMorningEnabled,
                            isMiddayEnabled = cp.isMiddayEnabled,
                            isEveningEnabled = cp.isEveningEnabled,
                            isPunascharanaEnabled = cp.isPunascharanaEnabled,
                            incrementValue = cp.incrementValue
                        )
                    }

                    val syncCustomEntries = mergedCustomEntries.values.map { ce ->
                        SyncCustomPracticeEntry(
                            practiceId = ce.practiceId,
                            date = ce.date,
                            count = ce.count,
                            morningCount = ce.morningCount,
                            afternoonCount = ce.afternoonCount,
                            eveningCount = ce.eveningCount,
                            morningPunasCount = ce.morningPunasCount,
                            afternoonPunasCount = ce.afternoonPunasCount,
                            eveningPunasCount = ce.eveningPunasCount,
                            updatedAt = ce.updatedAt
                        )
                    }

                    val currentSyncTime = getCurrentUTCTimestamp()
                    val payload = SyncPayload(
                        version = 3,
                        lastSynced = currentSyncTime,
                        initialLifetimeCount = syncedInitialLifetimeCount,
                        settings = SyncSettings(
                            themeMode = syncedThemeMode,
                            isPunascharanaEnabled = syncedPunasEnabled,
                            gayatriColor = syncedGayatriColor,
                            universalColor = syncedUniversalColor,
                            updatedAt = prefs.getSettingsUpdatedAt(),
                            initialLifetimeCountUpdatedAt = prefs.getInitialLifetimeCountUpdatedAt()
                        ),
                        entries = entriesMap,
                        customPractices = syncCustomPractices,
                        customEntries = syncCustomEntries
                    )
                    val isSuccess = syncService.saveCloudData(token, payload)
                    if (isSuccess) {
                        val localTimeDisp = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                        prefs.setLastSync(localTimeDisp)
                        prefs.setLastSyncTimeMs(System.currentTimeMillis())
                        _lastSyncError.value = null
                        _syncState.value = SyncState.SYNCED
                        Log.d(TAG, "Backup synced successfully at $localTimeDisp")
                    } else {
                        _lastSyncError.value = "Failed to upload to Google Drive. Drive API might not be enabled."
                        _syncState.value = SyncState.ERROR
                        Log.e(TAG, "Cloud save task failed.")
                    }
                } else {
                    val localTimeDisp = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                    prefs.setLastSync(localTimeDisp)
                    prefs.setLastSyncTimeMs(System.currentTimeMillis())
                    _lastSyncError.value = null
                    _syncState.value = SyncState.SYNCED
                    Log.d(TAG, "Synced and fully matching cloud. Sync timestamp set.")
                }

            } catch (e: Exception) {
                _lastSyncError.value = "Exception: ${e.message}"
                _syncState.value = SyncState.ERROR
                Log.e(TAG, "Exception during background synchronization process: ${e.message}", e)
            }
        }
    }

    private fun parseTimestamp(timeStr: String): Date {
        return try {
            dfUTC.parse(timeStr) ?: Date(0)
        } catch (e: Exception) {
            Date(0)
        }
    }
}

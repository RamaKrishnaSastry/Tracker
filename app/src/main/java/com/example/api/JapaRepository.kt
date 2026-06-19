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

    val allEntriesFlow: Flow<List<JapaEntry>> = dao.getAllEntriesFlow()
    val totalCountFlow: Flow<Int?> = dao.getTotalCountFlow()
    val totalSandhyaCountFlow: Flow<Int?> = dao.getTotalSandhyaCountFlow()
    val totalPunascharanaCountFlow: Flow<Int?> = dao.getTotalPunascharanaCountFlow()

    private val _syncState = MutableStateFlow(SyncState.DISCONNECTED)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

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

        _syncState.value = SyncState.SYNCING


        scope.launch {
            try {
                val profile = authManager.userProfile.value
                if (profile == null) {
                    Log.d(TAG, "Sync aborted: No active authenticated Google session found.")
                    return@launch
                }

                val token = authManager.getAccessToken()
                if (token == null) {
                    Log.d(TAG, "Sync aborted: Unable to fetch active access token.")
                    return@launch
                }

                Log.d(TAG, "Initiating Silent Sync with Google Drive...")
                val localEntries = dao.getAllEntries()
                val cloudPayload = syncService.fetchCloudData(token)

                val mergedEntries = mutableMapOf<String, JapaEntry>()
                localEntries.forEach { mergedEntries[it.date] = it }

                var hasLocalNewerChanges = false
                var hasCloudNewerChanges = false

                var syncedInitialLifetimeCount = prefs.getInitialLifetimeCount()
                var syncedThemeMode = prefs.getThemeMode()

                if (cloudPayload != null) {
                    // Update settings if newer
                    // Compare cloud vs local initial values and theme.
                    if (cloudPayload.initialLifetimeCount > prefs.getInitialLifetimeCount()) {
                        syncedInitialLifetimeCount = cloudPayload.initialLifetimeCount
                        prefs.setInitialLifetimeCount(cloudPayload.initialLifetimeCount)
                    } else if (prefs.getInitialLifetimeCount() > cloudPayload.initialLifetimeCount) {
                        hasLocalNewerChanges = true
                    }

                    if (cloudPayload.settings.themeMode != prefs.getThemeMode()) {
                        syncedThemeMode = cloudPayload.settings.themeMode
                        prefs.setThemeMode(cloudPayload.settings.themeMode)
                    }

                    // Compare timestamps per entry
                    cloudPayload.entries.forEach { (date, cloudEntry) ->
                        val localEntry = mergedEntries[date]
                        if (localEntry == null) {
                            // Only exists in cloud
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
                            // Exists in both, compare timestamps
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
                } else {
                    hasLocalNewerChanges = true
                }

                // If cloud has newer, insert into local database
                if (hasCloudNewerChanges) {
                    dao.insertEntries(mergedEntries.values.toList())
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
                    val currentSyncTime = getCurrentUTCTimestamp()
                    val payload = SyncPayload(
                        version = 2,
                        lastSynced = currentSyncTime,
                        initialLifetimeCount = syncedInitialLifetimeCount,
                        settings = SyncSettings(themeMode = syncedThemeMode),
                        entries = entriesMap
                    )
                    val isSuccess = syncService.saveCloudData(token, payload)
                    if (isSuccess) {
                        val localTimeDisp = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                        prefs.setLastSync(localTimeDisp)
                        prefs.setLastSyncTimeMs(System.currentTimeMillis())
                        _syncState.value = SyncState.SYNCED
                        Log.d(TAG, "Backup synced successfully at $localTimeDisp")
                    } else {
                        _syncState.value = SyncState.ERROR
                        Log.e(TAG, "Cloud save task failed.")
                    }
                } else {
                    val localTimeDisp = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                    prefs.setLastSync(localTimeDisp)
                    prefs.setLastSyncTimeMs(System.currentTimeMillis())
                    _syncState.value = SyncState.SYNCED
                    Log.d(TAG, "Synced and fully matching cloud. Sync timestamp set.")
                }

            } catch (e: Exception) {
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

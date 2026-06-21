package com.example.ui.screens

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.JapaEntry
import com.example.ui.JapaViewModel
import com.example.api.SyncState
import com.example.ui.theme.getMantraColor
import com.example.ui.theme.toHexString
import com.example.ui.components.ColorPickerDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: JapaViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val activePracticeId by viewModel.activePracticeId.collectAsState()
    val allPractices by viewModel.allCustomPractices.collectAsState()
    val activeCustomEntry by viewModel.activeCustomEntry.collectAsState()
    val todayDate by viewModel.todayDate.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val todayEntry by viewModel.todayEntry.collectAsState()
    val isPunascharanaEnabled by viewModel.isPunascharanaEnabled.collectAsState()
    val gayatriName by viewModel.gayatriName.collectAsState()
    val defaultPracticeId by viewModel.defaultPracticeId.collectAsState()
    val stats by viewModel.getStatisticsFlow().collectAsState(
        initial = JapaViewModel.Stats(0, 0, viewModel.prefs.getInitialLifetimeCount(), 0, 0)
    )

    var showPunasModal by remember { mutableStateOf(false) }

    // Current Date Formatting (e.g. Friday, Jun 19, 2026)
    val todayLongStr = remember {
        SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date())
    }

    // Dynamic greeting based on local time
    val timeGreeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Peaceful Night"
        }
    }

    // Progress determination
    val completedSandhyas = remember(todayEntry) {
        var count = 0
        todayEntry?.let { entry ->
            if (entry.pratahSandhyaCount > 0 || entry.pratahSandhyaCount == -1) count++
            if (entry.madhyahnikaSandhyaCount > 0 || entry.madhyahnikaSandhyaCount == -1) count++
            if (entry.sayamSandhyaCount > 0 || entry.sayamSandhyaCount == -1) count++
        }
        count
    }

    val currentName = remember(activePracticeId, allPractices) {
        allPractices.find { it.id == activePracticeId }?.name ?: "No Practice Selected"
    }

    val syncState by viewModel.repository.syncState.collectAsState()
    val lastSyncTimeMs = viewModel.prefs.getLastSyncTimeMs()
    val coroutineScope = rememberCoroutineScope()
    var isLocalRefreshing by remember { mutableStateOf(false) }
    val isRefreshing = isLocalRefreshing || (syncState == SyncState.SYNCING)

    // Periodically update the "X minutes ago" text
    var lastSyncText by remember { mutableStateOf("Never synced") }
    LaunchedEffect(lastSyncTimeMs, syncState) {
        if (lastSyncTimeMs == 0L) {
            lastSyncText = "Never synced"
        } else {
            while (true) {
                val diffMs = System.currentTimeMillis() - lastSyncTimeMs
                val diffMin = diffMs / (1000 * 60)
                lastSyncText = when {
                    diffMin < 1 -> "Last synced: Just now"
                    diffMin == 1L -> "Last synced: 1 minute ago"
                    diffMin < 60 -> "Last synced: $diffMin minutes ago"
                    else -> {
                        val diffHrs = diffMin / 60
                        if (diffHrs == 1L) "Last synced: 1 hour ago"
                        else "Last synced: $diffHrs hours ago"
                    }
                }
                kotlinx.coroutines.delay(15000) // Update every 15 seconds
            }
        }
    }

    val gayatriColorName by viewModel.gayatriColor.collectAsState()
    val gayatriColor = getMantraColor(gayatriColorName)

    val activeMantraColor = allPractices.find { it.id == activePracticeId }?.themeColor.let { themeStr ->
        if (themeStr != null) getMantraColor(themeStr) else MaterialTheme.colorScheme.primary
    }

    val activeMantraContainerColor = activeMantraColor.copy(alpha = 0.12f)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isLocalRefreshing = true
                    viewModel.triggerManualSync()
                    kotlinx.coroutines.delay(1000)
                    isLocalRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(16.dp))
            // Practice Selector Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                var expanded by remember { mutableStateOf(false) }
                var showEditDialog by remember { mutableStateOf(false) }
                var showGayatriEditDialog by remember { mutableStateOf(false) }

                // Centered Title Area
                Box(
                    modifier = Modifier
                        .padding(horizontal = 48.dp) // Leave space for icons on sides
                ) {
                    TextButton(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = activeMantraColor)
                    ) {
                        Text(
                            text = currentName,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                letterSpacing = 1.sp
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select Practice")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allPractices.forEach { practice ->
                            DropdownMenuItem(
                                text = { Text(practice.name, fontWeight = if (activePracticeId == practice.id) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    viewModel.setActivePractice(practice.id)
                                    expanded = false
                                },
                                trailingIcon = if (activePracticeId == practice.id) {
                                    { Icon(Icons.Default.Check, "Selected") }
                                } else null
                            )
                        }

                        Divider()
                        var showAddDialog by remember { mutableStateOf(false) }
                        DropdownMenuItem(
                            text = { Text("Add Custom Practice...", color = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showAddDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }
                        )

                        if (showAddDialog) {
                            AddMantraTemplateDialog(
                                viewModel = viewModel,
                                onDismiss = {
                                    showAddDialog = false
                                    expanded = false
                                },
                                onCreated = { newId ->
                                    viewModel.setActivePractice(newId)
                                    showAddDialog = false
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Settings icon on the right
                if (allPractices.any { it.id == activePracticeId }) {
                    IconButton(
                        onClick = {
                            showEditDialog = true
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure Current Mantra",
                            tint = activeMantraColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (showEditDialog) {
                    val activePractice = allPractices.find { it.id == activePracticeId }
                    if (activePractice != null) {
                        EditMantraDialog(
                            practice = activePractice,
                            isDefaultMantra = defaultPracticeId == activePractice.id,
                            onDismiss = { showEditDialog = false },
                            onSave = { updated, isDef ->
                                viewModel.updatePractice(updated)
                                if (isDef) viewModel.setDefaultPractice(updated.id)
                                else if (defaultPracticeId == updated.id) viewModel.setDefaultPractice(-1L)
                                showEditDialog = false
                            },
                            onDelete = {
                                viewModel.deletePractice(activePractice)
                                showEditDialog = false
                            }
                        )
                    }
                }
            }

            // Title, Greeting and Date
            Text(
                text = "Namaskaram, $userName",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.testTag("today_screen_greeting")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = todayLongStr,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.testTag("today_screen_date")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtle progress indicator: "X/3 Sandhyas completed"
            val currentPracticeInfo = allPractices.find { it.id == activePracticeId }
            if (currentPracticeInfo?.practiceType == "SANDHYA") {
                val todayCustomEntry = viewModel.activeCustomEntry.collectAsState().value
                var customCompletedSandhyas = 0
                if (todayCustomEntry != null) {
                    if (todayCustomEntry.morningCount > 0 || todayCustomEntry.morningCount == -1) customCompletedSandhyas++
                    if (todayCustomEntry.afternoonCount > 0 || todayCustomEntry.afternoonCount == -1) customCompletedSandhyas++
                    if (todayCustomEntry.eveningCount > 0 || todayCustomEntry.eveningCount == -1) customCompletedSandhyas++
                }
                val totalSandhyasRequired = listOf(currentPracticeInfo.isMorningEnabled, currentPracticeInfo.isMiddayEnabled, currentPracticeInfo.isEveningEnabled).count { it }

                Text(
                    text = "$customCompletedSandhyas/$totalSandhyasRequired Sandhyas completed",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("today_sandhyas_progress")
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            color = when (syncState) {
                                SyncState.SYNCED -> activeMantraContainerColor.copy(alpha = 0.3f)
                                SyncState.SYNCING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                SyncState.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val (icon, color, label) = when (syncState) {
                        SyncState.DISCONNECTED -> Triple(Icons.Default.CloudOff, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), "Cloud Backup: Off")
                        SyncState.OFFLINE -> Triple(Icons.Default.CloudOff, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), "Offline")
                        SyncState.SYNC_PENDING -> Triple(Icons.Default.Cloud, MaterialTheme.colorScheme.secondary, "Sync pending")
                        SyncState.SYNCING -> Triple(Icons.Default.Sync, activeMantraColor, "Syncing...")
                        SyncState.SYNCED -> Triple(Icons.Default.CloudDone, activeMantraColor, lastSyncText)
                        SyncState.ERROR -> Triple(Icons.Default.Error, MaterialTheme.colorScheme.error, "Sync failed.")
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = color
                    )
                }

                if (syncState != SyncState.DISCONNECTED) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.triggerManualSync() },
                        modifier = Modifier
                            .size(28.dp)
                            .background(activeMantraColor.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = "Save Manually to Cloud",
                            tint = activeMantraColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentPractice = allPractices.find { it.id == activePracticeId }
            val resolvedEntry = if (activePracticeId == com.example.ui.GAYATRI_PRACTICE_ID) {
                todayEntry?.let { gEntry ->
                    com.example.data.CustomPracticeEntry(
                        practiceId = com.example.ui.GAYATRI_PRACTICE_ID,
                        date = todayDate,
                        morningCount = gEntry.pratahSandhyaCount,
                        afternoonCount = gEntry.madhyahnikaSandhyaCount,
                        eveningCount = gEntry.sayamSandhyaCount,
                        morningPunasCount = gEntry.pratahPunascharanaCount,
                        afternoonPunasCount = gEntry.madhyahnikaPunascharanaCount,
                        eveningPunasCount = gEntry.sayamPunascharanaCount,
                        updatedAt = gEntry.updatedAt
                    )
                }
            } else {
                activeCustomEntry
            }

            SimplePracticeContent(
                viewModel = viewModel,
                practice = currentPractice,
                entry = resolvedEntry,
                date = todayDate
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        }
    }

    // Modal Punascharana support popup
    if (showPunasModal) {
        todayEntry?.let { entry ->
            PunascharanaDialog(
                entry = entry,
                onDismiss = { showPunasModal = false },
                onSave = { pratahPunas, madhyahnikaPunas, sayamPunas ->
                    viewModel.updateCounts(
                        date = entry.date,
                        morning = entry.pratahSandhyaCount,
                        afternoon = entry.madhyahnikaSandhyaCount,
                        evening = entry.sayamSandhyaCount,
                        pratahPunas = pratahPunas,
                        madhyahnikaPunas = madhyahnikaPunas,
                        sayamPunas = sayamPunas
                    )
                    showPunasModal = false
                }
            )
        }
    }
}

@Composable
fun SimplePracticeContent(
    viewModel: com.example.ui.JapaViewModel,
    practice: com.example.data.CustomPractice?,
    entry: com.example.data.CustomPracticeEntry?,
    date: String
) {
    if (practice == null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Practice Selected", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select a practice from the dropdown above or add a new custom practice.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        }
        return
    }
    val count = entry?.count ?: 0
    val target = practice.defaultTarget
    val themeColor = practice.themeColor
    val mantraColor = getMantraColor(themeColor)

    val isGlobalPunasEnabled by viewModel.isPunascharanaEnabled.collectAsState()
    val isPunascharanaEnabledForPractice = practice.isPunascharanaEnabled && isGlobalPunasEnabled

    val isGayatri = practice.id == com.example.ui.GAYATRI_PRACTICE_ID

    // Calculate lifetime count
    val practiceTotals by viewModel.allPracticeTotals.collectAsState(initial = emptyMap())
    val gayatriStats by viewModel.getStatisticsFlow().collectAsState(
        initial = com.example.ui.JapaViewModel.Stats(0, 0, viewModel.prefs.getInitialLifetimeCount(), 0, 0)
    )

    val lifetimeTotal = remember(practice.id, practiceTotals, gayatriStats) {
        if (isGayatri) {
            gayatriStats.lifetimeTotal
        } else {
            val totalFromDatabase = practiceTotals[practice.id] ?: 0
            practice.initialLifetimeCount + totalFromDatabase
        }
    }

    val practicePunasTotals by viewModel.allPracticePunasTotals.collectAsState(initial = emptyMap())
    val lifetimePunasTotal = remember(practice.id, practicePunasTotals, gayatriStats) {
        if (isGayatri) {
            gayatriStats.lifetimePunascharanaTotal
        } else {
            (practicePunasTotals[practice.id] ?: 0).toLong()
        }
    }

    if (practice.practiceType == "SANDHYA") {
        // Sandhya specific slots (cloning the Gayatri style content)
        val morningCount = entry?.morningCount ?: 0
        val afternoonCount = entry?.afternoonCount ?: 0
        val eveningCount = entry?.eveningCount ?: 0

        val morningPunas = entry?.morningPunasCount ?: 0
        val afternoonPunas = entry?.afternoonPunasCount ?: 0
        val eveningPunas = entry?.eveningPunasCount ?: 0

        val totalToday = morningCount + afternoonCount + eveningCount + morningPunas + afternoonPunas + eveningPunas

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = mantraColor.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 0.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPunascharanaEnabledForPractice) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("TODAY'S JAPA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            AnimatedCounterText(value = totalToday, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = mantraColor)
                        }
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("LIFETIME TOTAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            AnimatedCounterLongText(value = lifetimeTotal, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = mantraColor)
                        }
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("PUNASCHARANA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            AnimatedCounterLongText(value = lifetimePunasTotal.toLong(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = mantraColor)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("TODAY'S JAPA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            AnimatedCounterText(value = totalToday, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = mantraColor)
                        }
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("LIFETIME TOTAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            AnimatedCounterLongText(value = lifetimeTotal, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = mantraColor)
                        }
                    }
                }
            }

            // Morning (Pratah)
            if (practice.isMorningEnabled) {
                SandhyaIntervalCard(
                    title = "Morning (Pratah)",
                    count = morningCount,
                    punasCount = morningPunas,
                    icon = { Icon(Icons.Outlined.WbTwilight, "Morning Icon", tint = Color(0xFFFF9800), modifier = Modifier.size(28.dp)) },
                    onSave = { newCount, newPunas ->
                        if (practice.id == -1L) {
                            viewModel.updateGayatriSandhyaCounts(date, newCount, afternoonCount, eveningCount, newPunas, afternoonPunas, eveningPunas)
                        } else {
                            viewModel.updateCustomPracticeSandhyaCounts(practice.id, date, newCount, afternoonCount, eveningCount, newPunas, afternoonPunas, eveningPunas)
                        }
                    },
                    onClearAll = {
                        if (practice.id == -1L) {
                            viewModel.updateGayatriSandhyaCounts(date, 0, afternoonCount, eveningCount, 0, afternoonPunas, eveningPunas)
                        } else {
                            viewModel.updateCustomPracticeSandhyaCounts(practice.id, date, 0, afternoonCount, eveningCount, 0, afternoonPunas, eveningPunas)
                        }
                    },
                    tagPrefix = "custom_morning",
                    isPunascharanaEnabled = isPunascharanaEnabledForPractice,
                    accentColor = mantraColor
                )
            }

            // Afternoon (Madhyahnika)
            if (practice.isMiddayEnabled) {
                SandhyaIntervalCard(
                    title = "Midday (Madhyahnika)",
                    count = afternoonCount,
                    punasCount = afternoonPunas,
                    icon = { Icon(Icons.Outlined.WbSunny, "Midday Icon", tint = Color(0xFFF1C40F), modifier = Modifier.size(28.dp)) },
                    onSave = { newCount, newPunas ->
                        if (practice.id == -1L) {
                            viewModel.updateGayatriSandhyaCounts(date, morningCount, newCount, eveningCount, morningPunas, newPunas, eveningPunas)
                        } else {
                            viewModel.updateCustomPracticeSandhyaCounts(practice.id, date, morningCount, newCount, eveningCount, morningPunas, newPunas, eveningPunas)
                        }
                    },
                    onClearAll = {
                        if (practice.id == -1L) {
                            viewModel.updateGayatriSandhyaCounts(date, morningCount, 0, eveningCount, morningPunas, 0, eveningPunas)
                        } else {
                            viewModel.updateCustomPracticeSandhyaCounts(practice.id, date, morningCount, 0, eveningCount, morningPunas, 0, eveningPunas)
                        }
                    },
                    tagPrefix = "custom_afternoon",
                    isPunascharanaEnabled = isPunascharanaEnabledForPractice,
                    accentColor = mantraColor
                )
            }

            // Evening (Sayam)
            if (practice.isEveningEnabled) {
                SandhyaIntervalCard(
                    title = "Evening (Sayam)",
                    count = eveningCount,
                    punasCount = eveningPunas,
                    icon = { Icon(Icons.Outlined.Brightness4, "Evening Icon", tint = Color(0xFF9B59B6), modifier = Modifier.size(28.dp)) },
                    onSave = { newCount, newPunas ->
                        if (practice.id == -1L) {
                            viewModel.updateGayatriSandhyaCounts(date, morningCount, afternoonCount, newCount, morningPunas, afternoonPunas, newPunas)
                        } else {
                            viewModel.updateCustomPracticeSandhyaCounts(practice.id, date, morningCount, afternoonCount, newCount, morningPunas, afternoonPunas, newPunas)
                        }
                    },
                    onClearAll = {
                        if (practice.id == -1L) {
                            viewModel.updateGayatriSandhyaCounts(date, morningCount, afternoonCount, 0, morningPunas, afternoonPunas, 0)
                        } else {
                            viewModel.updateCustomPracticeSandhyaCounts(practice.id, date, morningCount, afternoonCount, 0, morningPunas, afternoonPunas, 0)
                        }
                    },
                    tagPrefix = "custom_evening",
                    isPunascharanaEnabled = isPunascharanaEnabledForPractice,
                    accentColor = mantraColor
                )
            }
        }
    } else {
        // Both CONTINUOUS and RECITATION types have very similar high-fidelity UI but optimized features
        val isContinuous = practice.practiceType == "CONTINUOUS"
        val label = if (isContinuous) "Today's Japa" else "Today's Recitations"

        var showCustomAddDialog by remember { mutableStateOf(false) }
        var localCount by remember(practice.id) { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Summary Dashboard layout
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = mantraColor.copy(alpha = 0.10f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, mantraColor.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Lifetime subtle total
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "LIFETIME TOTAL",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%,d", lifetimeTotal + localCount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = mantraColor
                            )
                        }

                        // Right: Circular progress indicator if target configured
                        if (target > 0) {
                            val targetProgress = ((count + localCount).toFloat() / target.toFloat()).coerceIn(0f, 1f)
                            val progress by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = targetProgress,
                                animationSpec = androidx.compose.animation.core.tween(durationMillis = 500),
                                label = "ProgressAnimation"
                            )
                            JapaProgressCircle(
                                progress = progress,
                                current = count + localCount,
                                target = target,
                                color = mantraColor
                            )
                        } else {
                            // Simple text if no target
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "CURRENT COUNT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${count + localCount}",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = mantraColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Count adjusters card: [ - ]  count  [ + ]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    var textInput by remember(localCount) { mutableStateOf(if (localCount <= 0) "" else localCount.toString()) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Minus Button
                        IconButton(
                            onClick = {
                                if (localCount > 0) {
                                    localCount -= 1
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            enabled = localCount > 0
                        ) {
                            Icon(Icons.Default.Remove, "Decrement", tint = if (localCount > 0) MaterialTheme.colorScheme.onSurface else Color.Gray)
                        }

                        // TextField direct counter input
                        TextField(
                            value = textInput,
                            onValueChange = { input ->
                                if (input.isEmpty()) {
                                    textInput = ""
                                    localCount = 0
                                } else if (input.all { it.isDigit() }) {
                                    textInput = input
                                    localCount = input.toIntOrNull() ?: 0
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            textStyle = MaterialTheme.typography.displaySmall.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = mantraColor,
                                unfocusedIndicatorColor = mantraColor.copy(alpha = 0.4f)
                            )
                        )

                        // Plus Button
                        IconButton(
                            onClick = {
                                val increment = if (practice.practiceType == "RECITATION") practice.incrementValue else 1
                                localCount += increment
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(mantraColor.copy(alpha = 0.2f), shape = CircleShape)
                                .border(1.dp, mantraColor, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, "Increment", tint = mantraColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    var isSaving by remember { mutableStateOf(false) }
                    val buttonScope = rememberCoroutineScope()

                    AnimatedVisibility(visible = localCount > 0) {
                        Button(
                            onClick = { 
                                if (isSaving) return@Button
                                isSaving = true
                                buttonScope.launch {
                                    viewModel.updateCustomPracticeCount(practice.id, date, count + localCount)
                                    viewModel.logJapaSession(practice.id, localCount, "Saved count")
                                    localCount = 0
                                    isSaving = false
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(bottom = 12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.padding(end = 8.dp))
                            Text(if (isSaving) "Saving..." else "Save Session", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    // Buttons/Chips panel
                    if (practice.practiceType == "CONTINUOUS") {
                        // Quick Add Chips Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val rawIncrements = practice.quickAddValues
                            val incrementsList = remember(rawIncrements) {
                                rawIncrements.split(",").mapNotNull { it.trim().toIntOrNull() }
                            }
                            incrementsList.forEach { inc ->
                                InputChip(
                                    selected = false,
                                    onClick = {
                                        localCount += inc
                                    },
                                    label = { Text("+$inc", fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.padding(horizontal = 4.dp).testTag("quick_add_chip_$inc")
                                )
                            }

                            // Custom dialog opening chip
                            InputChip(
                                selected = false,
                                onClick = { showCustomAddDialog = true },
                                label = { Text("Custom", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.padding(horizontal = 4.dp).testTag("custom_entry_chip"),
                                leadingIcon = { Icon(Icons.Default.Schedule, "Custom Add", modifier = Modifier.size(16.dp)) }
                            )
                        }
                    } else if (practice.practiceType == "RECITATION") {
                        // RECITATION major complete button!
                        Button(
                            onClick = {
                                val inc = practice.incrementValue
                                localCount += inc
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("add_recitation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = mantraColor),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.padding(end = 8.dp))
                            Text("+${practice.incrementValue} Recitation", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    
                    if (localCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { 
                                localCount = 0
                            }
                        ) {
                            Text("Reset Active Sitting", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Session Logging section (Collapsed by default)
            SessionLoggingBlock(
                practiceId = practice.id,
                viewModel = viewModel,
                mantraColor = mantraColor,
                todayCount = count,
                date = date
            )
        }

        // Dialog for Custom manual entry
        if (showCustomAddDialog) {
            var customAmtString by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCustomAddDialog = false },
                title = { Text(if (isContinuous) "Add Custom Count" else "Add Custom Recitations") },
                text = {
                    Column {
                        Text("Enter count sitting amount to record for this session:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customAmtString,
                            onValueChange = { if (it.all { char -> char.isDigit() }) customAmtString = it },
                            label = { Text("Count") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("custom_amt_text_field")
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val amt = customAmtString.toIntOrNull() ?: 0
                            if (amt > 0) {
                                localCount += amt
                            }
                            showCustomAddDialog = false
                        },
                        modifier = Modifier.testTag("custom_amt_confirm_btn")
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun GayatriSpecificContent(
    viewModel: com.example.ui.JapaViewModel,
    stats: com.example.ui.JapaViewModel.Stats,
    todayEntry: com.example.data.JapaEntry?,
    isPunascharanaEnabled: Boolean,
    completedSandhyas: Int,
    onShowPunasModal: () -> Unit,
    accentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val gayatriColor = accentColor
    Column {
        // Subtle progress indicator: "X/3 Sandhyas completed"
        Text(
            text = "$completedSandhyas/3 Sandhyas completed",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            ),
            color = gayatriColor,
            modifier = Modifier.testTag("today_sandhyas_progress").align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
             Card(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(bottom = 16.dp),
                 colors = CardDefaults.cardColors(
                     containerColor = gayatriColor.copy(alpha = 0.12f)
                 ),
                 border = androidx.compose.foundation.BorderStroke(1.dp, gayatriColor.copy(alpha = 0.25f)),
                 shape = RoundedCornerShape(24.dp)
             ) {
                 Row(
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(vertical = 24.dp, horizontal = 8.dp),
                     horizontalArrangement = Arrangement.SpaceEvenly,
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     if (isPunascharanaEnabled) {
                         // Column 1: Today's Total
                         Column(
                             horizontalAlignment = Alignment.CenterHorizontally,
                             modifier = Modifier.weight(1f)
                         ) {
                             Text(
                                 text = "TODAY'S JAPA",
                                 style = MaterialTheme.typography.labelSmall.copy(
                                     fontWeight = FontWeight.Bold,
                                     letterSpacing = 0.5.sp
                                 ),
                                 color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                 textAlign = TextAlign.Center
                             )
                             Spacer(modifier = Modifier.height(8.dp))
                             AnimatedCounterText(
                                 value = stats.todayTotal,
                                 style = MaterialTheme.typography.titleLarge.copy(
                                     fontWeight = FontWeight.Bold,
                                     fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                 ),
                                 color = gayatriColor,
                                 modifier = Modifier.testTag("stats_today")
                             )
                         }

                         // Symmetrical divider 1
                         Box(
                             modifier = Modifier
                                 .width(1.dp)
                                 .height(40.dp)
                                 .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                         )

                         // Column 2: Lifetime Total
                         Column(
                             horizontalAlignment = Alignment.CenterHorizontally,
                             modifier = Modifier.weight(1f)
                         ) {
                             Text(
                                 text = "LIFETIME TOTAL",
                                 style = MaterialTheme.typography.labelSmall.copy(
                                     fontWeight = FontWeight.Bold,
                                     letterSpacing = 0.5.sp
                                 ),
                                 color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                 textAlign = TextAlign.Center
                             )
                             Spacer(modifier = Modifier.height(8.dp))
                             AnimatedCounterLongText(
                                 value = stats.lifetimeTotal,
                                 style = MaterialTheme.typography.titleLarge.copy(
                                     fontWeight = FontWeight.Bold,
                                     fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                 ),
                                 color = gayatriColor,
                                 modifier = Modifier.testTag("stats_lifetime_total")
                             )
                         }

                         // Symmetrical divider 2
                         Box(
                             modifier = Modifier
                                 .width(1.dp)
                                 .height(40.dp)
                                 .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                         )

                         // Column 3: Total Punascharana
                         Column(
                             horizontalAlignment = Alignment.CenterHorizontally,
                             modifier = Modifier.weight(1f)
                         ) {
                             Text(
                                 text = "PUNASCHARANA",
                                 style = MaterialTheme.typography.labelSmall.copy(
                                     fontWeight = FontWeight.Bold,
                                     letterSpacing = 0.5.sp
                                 ),
                                 color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                 textAlign = TextAlign.Center
                             )
                             Spacer(modifier = Modifier.height(8.dp))
                             AnimatedCounterLongText(
                                 value = stats.lifetimePunascharanaTotal,
                                 style = MaterialTheme.typography.titleLarge.copy(
                                     fontWeight = FontWeight.Bold,
                                     fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                 ),
                                 color = gayatriColor,
                                 modifier = Modifier.testTag("stats_lifetime_punascharana")
                             )
                         }
                     } else {
                         // Column 1: Today's Total
                         Column(
                             horizontalAlignment = Alignment.CenterHorizontally,
                             modifier = Modifier.weight(1f)
                         ) {
                             Text(
                                 text = "TODAY'S JAPA",
                                 style = MaterialTheme.typography.labelSmall.copy(
                                     fontWeight = FontWeight.Bold,
                                     letterSpacing = 1.sp
                                 ),
                                 color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                 textAlign = TextAlign.Center
                             )
                             Spacer(modifier = Modifier.height(8.dp))
                             AnimatedCounterText(
                                 value = stats.todayTotal,
                                 style = MaterialTheme.typography.headlineLarge.copy(
                                     fontWeight = FontWeight.Bold,
                                     fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                 ),
                                 color = MaterialTheme.colorScheme.onPrimaryContainer,
                                 modifier = Modifier.testTag("stats_today")
                             )
                         }

                         // Symmetrical divider
                         Box(
                             modifier = Modifier
                                 .width(1.dp)
                                 .height(48.dp)
                                 .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f))
                         )

                         // Column 2: Lifetime Total
                         Column(
                             horizontalAlignment = Alignment.CenterHorizontally,
                             modifier = Modifier.weight(1f)
                         ) {
                             Text(
                                 text = "LIFETIME TOTAL",
                                 style = MaterialTheme.typography.labelSmall.copy(
                                     fontWeight = FontWeight.Bold,
                                     letterSpacing = 1.sp
                                 ),
                                 color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                 textAlign = TextAlign.Center
                             )
                             Spacer(modifier = Modifier.height(8.dp))
                             AnimatedCounterLongText(
                                 value = stats.lifetimeTotal,
                                 style = MaterialTheme.typography.headlineLarge.copy(
                                     fontWeight = FontWeight.Bold,
                                     fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                 ),
                                 color = MaterialTheme.colorScheme.onPrimaryContainer,
                                 modifier = Modifier.testTag("stats_lifetime_total")
                             )
                         }
                     }
                 }
             }

            // Section Cards
            todayEntry?.let { entry ->
                // Pratah Sandhya
                SandhyaIntervalCard(
                    title = "Morning (Pratah Sandhya)",
                    count = entry.pratahSandhyaCount,
                    punasCount = entry.pratahPunascharanaCount,
                    icon = {
                        Icon(
                            Icons.Outlined.WbTwilight,
                            contentDescription = "Sunrise icon for Pratah",
                            tint = gayatriColor,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    onSave = { newCount, newPunas ->
                        viewModel.updateCounts(
                            date = entry.date,
                            morning = newCount,
                            afternoon = entry.madhyahnikaSandhyaCount,
                            evening = entry.sayamSandhyaCount,
                            pratahPunas = newPunas
                        )
                    },
                    onClearAll = {
                        viewModel.updateCounts(
                            date = entry.date,
                            morning = 0,
                            afternoon = entry.madhyahnikaSandhyaCount,
                            evening = entry.sayamSandhyaCount,
                            pratahPunas = 0
                        )
                    },
                    tagPrefix = "morning",
                    isPunascharanaEnabled = isPunascharanaEnabled,
                    accentColor = gayatriColor
                )

                // Midday (Madhyahnika Sandhya)
                SandhyaIntervalCard(
                    title = "Midday (Madhyahnika)",
                    count = entry.madhyahnikaSandhyaCount,
                    punasCount = entry.madhyahnikaPunascharanaCount,
                    icon = {
                        Icon(
                            Icons.Outlined.WbSunny,
                            contentDescription = "Sun icon for Midday",
                            tint = Color(0xFFF1C40F),
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    onSave = { newCount, newPunas ->
                        viewModel.updateCounts(
                            date = entry.date,
                            morning = entry.pratahSandhyaCount,
                            afternoon = newCount,
                            evening = entry.sayamSandhyaCount,
                            madhyahnikaPunas = newPunas
                        )
                    },
                    onClearAll = {
                        viewModel.updateCounts(
                            date = entry.date,
                            morning = entry.pratahSandhyaCount,
                            afternoon = 0,
                            evening = entry.sayamSandhyaCount,
                            madhyahnikaPunas = 0
                        )
                    },
                    tagPrefix = "afternoon",
                    isPunascharanaEnabled = isPunascharanaEnabled,
                    accentColor = gayatriColor
                )

                // Evening (Sayam Sandhya)
                SandhyaIntervalCard(
                    title = "Evening (Sayam Sandhya)",
                    count = entry.sayamSandhyaCount,
                    punasCount = entry.sayamPunascharanaCount,
                    icon = {
                        Icon(
                            Icons.Outlined.Brightness4,
                            contentDescription = "Sunset icon for Sayam",
                            tint = Color(0xFF9B59B6),
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    onSave = { newCount, newPunas ->
                        viewModel.updateCounts(
                            date = entry.date,
                            morning = entry.pratahSandhyaCount,
                            afternoon = entry.madhyahnikaSandhyaCount,
                            evening = newCount,
                            sayamPunas = newPunas
                        )
                    },
                    onClearAll = {
                        viewModel.updateCounts(
                            date = entry.date,
                            morning = entry.pratahSandhyaCount,
                            afternoon = entry.madhyahnikaSandhyaCount,
                            evening = 0,
                            sayamPunas = 0
                        )
                    },
                    tagPrefix = "evening",
                    isPunascharanaEnabled = isPunascharanaEnabled,
                    accentColor = gayatriColor
                )
            }
        }
}

@Composable
fun AnimatedCounterText(
    value: Int,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith fadeOut(animationSpec = tween(durationMillis = 200))
        },
        label = "CounterAnimation",
        modifier = modifier
    ) { targetValue ->
        Text(
            text = targetValue.toString(),
            style = style,
            color = color
        )
    }
}

@Composable
fun AnimatedCounterLongText(
    value: Long,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith fadeOut(animationSpec = tween(durationMillis = 200))
        },
        label = "CounterLongAnimation",
        modifier = modifier
    ) { targetValue ->
        Text(
            text = targetValue.toString(),
            style = style,
            color = color
        )
    }
}

@Composable
fun SandhyaIntervalCard(
    title: String,
    count: Int,
    punasCount: Int,
    icon: @Composable () -> Unit,
    onSave: (Int, Int) -> Unit,
    onClearAll: () -> Unit,
    tagPrefix: String,
    isPunascharanaEnabled: Boolean = true,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    var isPunasExpanded by remember { mutableStateOf(punasCount > 0) }
    var showCustomAddDialog by remember { mutableStateOf(false) }

    // Local accumulators
    var localCount by remember(count) { mutableStateOf(count) }
    var localPunasCount by remember(punasCount) { mutableStateOf(punasCount) }
    var textInput by remember(localCount) { mutableStateOf(if (localCount <= 0) "" else localCount.toString()) }

    // Synchronize expansion with changes to punasCount
    LaunchedEffect(punasCount) {
        if (punasCount > 0) {
            isPunasExpanded = true
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon()
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Total display
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (localCount == -1) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("PASSED", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    } else {
                        AnimatedCounterText(
                            value = localCount,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            ),
                            color = accentColor
                        )
                    }
                    if (isPunascharanaEnabled && localPunasCount > 0) {
                        Text(
                            text = " (+$localPunasCount)",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = accentColor.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Adjustor Panel Row (Circular buttons with thin border, underlined input)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Minus Target
                IconButton(
                    onClick = {
                        if (localCount > 0) {
                            localCount = (localCount - 1).coerceAtLeast(0)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .testTag("${tagPrefix}_minus"),
                    enabled = localCount > 0
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrement",
                        tint = if (localCount > 0) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                }

                // Direct Numeric Manual Underlined Entry Text Field
                TextField(
                    value = textInput,
                    onValueChange = { input ->
                        if (input.isEmpty()) {
                            textInput = ""
                            localCount = 0
                        } else if (input.all { it.isDigit() }) {
                            textInput = input
                            val parsed = input.toIntOrNull() ?: 0
                            localCount = parsed
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .testTag("${tagPrefix}_input"),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = accentColor,
                        unfocusedIndicatorColor = accentColor.copy(alpha = 0.4f)
                    )
                )

                // Plus Target
                IconButton(
                    onClick = {
                        val base = if (localCount == -1) 0 else localCount
                        localCount = base + 1
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .testTag("${tagPrefix}_plus")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increment",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = localCount != count || localPunasCount != punasCount) {
                var isSaving by remember { mutableStateOf(false) }
                val saveScope = rememberCoroutineScope()
                Button(
                    onClick = { 
                        if (isSaving) return@Button
                        isSaving = true
                        saveScope.launch {
                            var finalCount = localCount
                            if (localCount != count && localCount == -1) {
                                finalCount = -1
                            }
                            onSave(finalCount, localPunasCount)
                            isSaving = false
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.padding(end = 8.dp))
                    Text(if (isSaving) "Saving..." else "Save", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

             // Quick-add Increment Chips
             androidx.compose.foundation.lazy.LazyRow(
                 modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_increments_row"),
                 horizontalArrangement = Arrangement.spacedBy(8.dp),
                 contentPadding = PaddingValues(horizontal = 4.dp)
             ) {
                 val increments = listOf(10, 24, 28, 54, 108)
                 items(increments.size) { index ->
                     val inc = increments[index]
                     val isSpecial = inc == 108
                     val containerColor = MaterialTheme.colorScheme.secondaryContainer
                     val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                     val border = if (isSpecial) BorderStroke(1.dp, accentColor) else null

                     Button(
                         onClick = {
                             val base = if (localCount == -1) 0 else localCount
                             localCount = base + inc
                         },
                         modifier = Modifier
                             .height(40.dp)
                             .testTag("${tagPrefix}_add_$inc"),
                         shape = RoundedCornerShape(20.dp),
                         border = border,
                         colors = ButtonDefaults.buttonColors(
                             containerColor = containerColor,
                             contentColor = contentColor
                         ),
                         contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                     ) {
                         Text(
                             text = "+$inc",
                             style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                         )
                     }
                 }
                 
                 item {
                     Button(
                         onClick = { showCustomAddDialog = true },
                         modifier = Modifier
                             .height(40.dp)
                             .testTag("${tagPrefix}_custom_add"),
                         shape = RoundedCornerShape(20.dp),
                         colors = ButtonDefaults.buttonColors(
                             containerColor = MaterialTheme.colorScheme.secondaryContainer,
                             contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                         ),
                         contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                     ) {
                         Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                         Text("Custom", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                     }
                 }
             }

             if (showCustomAddDialog) {
                 var customAmtString by remember { mutableStateOf("") }
                 AlertDialog(
                     onDismissRequest = { showCustomAddDialog = false },
                     title = { Text("Add Custom Count") },
                     text = {
                         Column {
                             Text("Enter amount to add to $title:", style = MaterialTheme.typography.bodyMedium)
                             Spacer(modifier = Modifier.height(8.dp))
                             OutlinedTextField(
                                 value = customAmtString,
                                 onValueChange = { if (it.all { char -> char.isDigit() }) customAmtString = it },
                                 label = { Text("Amount") },
                                 singleLine = true,
                                 keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                 modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_custom_amt_field")
                             )
                         }
                     },
                     confirmButton = {
                         Button(
                             onClick = {
                                 val amt = customAmtString.toIntOrNull() ?: 0
                                 if (amt > 0) {
                                     val base = if (localCount == -1) 0 else localCount
                                     localCount = base + amt
                                 }
                                 showCustomAddDialog = false
                             }
                         ) {
                             Text("Add")
                         }
                     },
                     dismissButton = {
                         TextButton(onClick = { showCustomAddDialog = false }) { Text("Cancel") }
                     }
                 )
             }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (localCount == 0 && localPunasCount == 0 && count == 0 && punasCount == 0) {
                     TextButton(
                         onClick = { onSave(-1, 0) },
                         modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_pass_button")
                     ) {
                         Text("Pass (Skip for today)")
                     }
                } else if (localCount > 0 || localCount == -1 || localPunasCount > 0) {
                     TextButton(
                         onClick = { 
                             localCount = 0 
                             localPunasCount = 0
                             onClearAll()
                         },
                         modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_clear_button")
                     ) {
                         Text("Clear Count", color = MaterialTheme.colorScheme.error)
                     }
                }
            }

            if (isPunascharanaEnabled) {
                // Punascharana Expanded Section
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Expand/Collapse header for Punascharana
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPunasExpanded = !isPunasExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Additional Japa (Punascharana)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isPunasExpanded) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Icon(
                        imageVector = if (isPunasExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Punascharana collapsible section",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isPunasExpanded,
                    enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                    exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(220))
                ) {
                    PunascharanaAdjusterSection(
                        punasCount = localPunasCount,
                        onPunasUpdated = { newPunas -> localPunasCount = newPunas },
                        tagPrefix = "${tagPrefix}_punas",
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun PunascharanaAdjusterSection(
    punasCount: Int,
    onPunasUpdated: (Int) -> Unit,
    tagPrefix: String,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    var punasInput by remember(punasCount) { mutableStateOf(punasCount.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        // Simple manual adjust buttons + manual inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Minus
            IconButton(
                onClick = {
                    if (punasCount > 0) {
                        onPunasUpdated((punasCount - 1).coerceAtLeast(0))
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .testTag("${tagPrefix}_minus"),
                enabled = punasCount > 0
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Decrement punascharana",
                    tint = if (punasCount > 0) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Direct Numeric Manual Input
            TextField(
                value = punasInput,
                onValueChange = { input ->
                    if (input.isEmpty()) {
                        punasInput = ""
                        onPunasUpdated(0)
                    } else if (input.all { it.isDigit() }) {
                        punasInput = input
                        val parsed = input.toIntOrNull() ?: 0
                        onPunasUpdated(parsed)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .testTag("${tagPrefix}_input"),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = accentColor,
                    unfocusedIndicatorColor = accentColor.copy(alpha = 0.4f)
                )
            )

            // Plus
            IconButton(
                onClick = {
                    onPunasUpdated(punasCount + 1)
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .testTag("${tagPrefix}_plus")
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Increment punascharana",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick-adds
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            val increments = listOf(10, 24, 28, 54, 108)
            items(increments.size) { index ->
                val inc = increments[index]
                val isSpecial = inc == 108
                val containerColor = MaterialTheme.colorScheme.secondaryContainer
                val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                val border = if (isSpecial) BorderStroke(1.dp, accentColor) else null

                Button(
                    onClick = {
                        onPunasUpdated(punasCount + inc)
                    },
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("${tagPrefix}_add_$inc"),
                    shape = RoundedCornerShape(16.dp),
                    border = border,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "+$inc",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun PunascharanaDialog(
    entry: JapaEntry,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var pratahPunas by remember { mutableStateOf(entry.pratahPunascharanaCount) }
    var madhyahnikaPunas by remember { mutableStateOf(entry.madhyahnikaPunascharanaCount) }
    var sayamPunas by remember { mutableStateOf(entry.sayamPunascharanaCount) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Additional Japa (Punascharana)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Record additional Gayatri Japa outside standard Sandhyavandanam.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Pratah
                PunascharanaInputSection(
                    title = "Pratah Punascharana",
                    value = pratahPunas,
                    onValueChange = { pratahPunas = it },
                    tagPrefix = "dialog_pratah_punas"
                )
                // Madhyahnika
                PunascharanaInputSection(
                    title = "Madhyahnika Punascharana",
                    value = madhyahnikaPunas,
                    onValueChange = { madhyahnikaPunas = it },
                    tagPrefix = "dialog_madhyahnika_punas"
                )
                // Sayam
                PunascharanaInputSection(
                    title = "Sayam Punascharana",
                    value = sayamPunas,
                    onValueChange = { sayamPunas = it },
                    tagPrefix = "dialog_sayam_punas"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(pratahPunas, madhyahnikaPunas, sayamPunas)
                },
                modifier = Modifier.testTag("punas_dialog_save")
            ) {
                Text("Update Counts")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("punas_dialog_cancel")) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PunascharanaInputSection(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    tagPrefix: String
) {
    var textInput by remember(value) { mutableStateOf(value.toString()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { input ->
                    if (input.isEmpty()) {
                        textInput = ""
                        onValueChange(0)
                    } else if (input.all { it.isDigit() }) {
                        textInput = input
                        onValueChange(input.toIntOrNull() ?: 0)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${tagPrefix}_input"),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            )

            // Quick adds for dialog
            val increments = listOf(10, 24, 28, 54, 108)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    increments.take(3).forEach { inc ->
                        Button(
                            onClick = { onValueChange(value + inc) },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Text("+$inc", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    increments.drop(3).forEach { inc ->
                        val isSpecial = inc == 108
                        Button(
                            onClick = { onValueChange(value + inc) },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            border = if (isSpecial) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = "+$inc",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = if (isSpecial) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// CUSTOM MULTI-WORKFLOW JAPA HELPERS & DIALOGS
// ==========================================

// Local getMantraColor removed to use global one from theme package


@Composable
fun JapaProgressCircle(
    progress: Float,
    current: Int,
    target: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw background track
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            // Draw active progress
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = "$current/$target",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SessionLoggingBlock(
    practiceId: Long,
    viewModel: com.example.ui.JapaViewModel,
    mantraColor: androidx.compose.ui.graphics.Color,
    todayCount: Int,
    date: String
) {
    var sessionsExpanded by remember { mutableStateOf(false) }
    val sessions by viewModel.todaySessions.collectAsState(initial = emptyList())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { sessionsExpanded = !sessionsExpanded }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (sessionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Sessions",
                        tint = mantraColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Sessions (${sessions.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!sessionsExpanded && sessions.isNotEmpty()) {
                    val quickSum = sessions.sumOf { it.count }
                    Text(
                        text = "$quickSum Japa",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = mantraColor
                    )
                }
            }

            if (sessionsExpanded) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No sessions recorded yet today.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Column(modifier = Modifier.padding(8.dp)) {
                        sessions.forEach { session ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = "Time Saved",
                                        tint = mantraColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = session.time,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "(${session.typeDetail})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${session.count}",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = mantraColor
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    IconButton(
                                        onClick = {
                                            val decremented = (todayCount - session.count).coerceAtLeast(0)
                                            viewModel.updateCustomPracticeCount(practiceId, date, decremented)
                                            viewModel.deleteSession(session.id)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Session",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMantraTemplateDialog(
    viewModel: com.example.ui.JapaViewModel,
    onDismiss: () -> Unit,
    onCreated: (Long) -> Unit
) {
    var stage by remember { mutableStateOf("SELECT_TEMPLATE") }
    
    var mantraName by remember { mutableStateOf("") }
    var practiceType by remember { mutableStateOf("CONTINUOUS") }
    var defaultTarget by remember { mutableStateOf("108") }
    var quickAddValues by remember { mutableStateOf("10,54,108") }
    var initialLifetimeCount by remember { mutableStateOf("0") }
    var selectedColor by remember { mutableStateOf("Royal") }
    
    var isReminderEnabled by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf("06:30 AM") }

    val colorsList = listOf("Indigo", "Royal", "Teal", "Saffron", "Crimson", "Emerald", "Amber", "Slate", "Violet")

    if (stage == "SELECT_TEMPLATE") {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { 
                Text(
                    "Select Mantra Template",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                ) 
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Choose a preset template or configure from scratch:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    val templates = listOf(
                        Triple("Gayatri Japa", "Saffron", "Sandhya-based workflow (Morning, Midday, Evening)"),
                        Triple("Om Namah Shivaya", "Indigo", "Continuous repetitive count with 108 increments"),
                        Triple("Mahamrityunjaya Mantra", "Teal", "Healing repetitiveness with 11,27,54,108 increments"),
                        Triple("Om Namo Narayanaya", "Emerald", "Continuous repetitive count with 10,54,108 increments"),
                        Triple("Lalitha Sahasranama", "Crimson", "Recitation count incrementer (complete recitation units)"),
                        Triple("Custom Custom Mantra...", "Slate", "Configure manually with customizable fields")
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        templates.forEach { item ->
                            val (name, color, desc) = item
                            val itemColor = getMantraColor(color)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        mantraName = if (name.startsWith("Custom")) "" else name
                                        practiceType = when (name) {
                                            "Gayatri Japa" -> "SANDHYA"
                                            "Lalitha Sahasranama" -> "RECITATION"
                                            else -> "CONTINUOUS"
                                        }
                                        defaultTarget = if (name == "Lalitha Sahasranama") "1" else "108"
                                        quickAddValues = when (name) {
                                            "Gayatri Japa" -> "24,54,108"
                                            "Om Namah Shivaya" -> "108,216,324"
                                            "Mahamrityunjaya Mantra" -> "11,27,54,108"
                                            "Om Namo Narayanaya" -> "10,54,108"
                                            else -> "10,54,108"
                                        }
                                        selectedColor = color
                                        stage = "CONFIGURE"
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, itemColor.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(itemColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = { stage = "SELECT_TEMPLATE" },
            title = { Text("Configure Mantra Settings") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = mantraName,
                        onValueChange = { mantraName = it },
                        label = { Text("Mantra Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_mantra_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Practice Mode", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("SANDHYA" to "Sandhyas", "CONTINUOUS" to "Repetitive", "RECITATION" to "Recitations").forEach { mode ->
                            val (key, label) = mode
                            val isSelected = practiceType == key
                            InputChip(
                                selected = isSelected,
                                onClick = { practiceType = key },
                                label = { Text(label) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = getMantraColor(selectedColor).copy(alpha = 0.15f),
                                    selectedLabelColor = getMantraColor(selectedColor)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = defaultTarget,
                        onValueChange = { if (it.all { c -> c.isDigit() }) defaultTarget = it },
                        label = { Text("Daily Target Japa (0 for none)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (practiceType == "CONTINUOUS") {
                        OutlinedTextField(
                            value = quickAddValues,
                            onValueChange = { quickAddValues = it },
                            label = { Text("Quick-Add Increments (comma separated)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = initialLifetimeCount,
                        onValueChange = { if (it.all { c -> c.isDigit() }) initialLifetimeCount = it },
                        label = { Text("Initial Lifetime Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Theme Color Accent", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.foundation.lazy.LazyRow(modifier = Modifier.fillMaxWidth()) {
                        items(colorsList.size) { index ->
                            val colorName = colorsList[index]
                            val isColorSelected = selectedColor == colorName
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(32.dp)
                                    .background(getMantraColor(colorName), CircleShape)
                                    .border(
                                        width = if (isColorSelected) 3.dp else 1.dp,
                                        color = if (isColorSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorName },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isColorSelected) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    var showVisualPicker by remember { mutableStateOf(false) }
                    var customHex by remember { mutableStateOf(if (selectedColor.startsWith("#")) selectedColor else "") }
                    
                    if (showVisualPicker) {
                        ColorPickerDialog(
                            initialColor = getMantraColor(selectedColor),
                            onColorSelected = { 
                                val hex = it.toHexString()
                                selectedColor = hex
                                customHex = hex
                            },
                            onDismiss = { showVisualPicker = false }
                        )
                    }

                    OutlinedTextField(
                        value = customHex,
                        onValueChange = { 
                            customHex = it
                            if (it.length >= 4 && (it.startsWith("#"))) {
                                selectedColor = it
                            }
                        },
                        label = { Text("Custom Hex Color (e.g. #6449A7)") },
                        placeholder = { Text("#RRGGBB") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_custom_hex_input"),
                        leadingIcon = { 
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(getMantraColor(selectedColor), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showVisualPicker = true }) {
                                Icon(Icons.Default.Palette, contentDescription = "Pick Color")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Setup Daily Reminder", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Receive regular notification reminder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        Switch(checked = isReminderEnabled, onCheckedChange = { isReminderEnabled = it })
                    }

                    if (isReminderEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reminderTime,
                            onValueChange = { reminderTime = it },
                            label = { Text("Reminder Time (e.g. 06:30 AM)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mantraName.isNotBlank()) {
                            val newMantra = com.example.data.CustomPractice(
                                name = mantraName,
                                practiceType = practiceType,
                                defaultTarget = defaultTarget.toIntOrNull() ?: 108,
                                quickAddValues = quickAddValues,
                                initialLifetimeCount = initialLifetimeCount.toLongOrNull() ?: 0L,
                                themeColor = selectedColor,
                                isReminderEnabled = isReminderEnabled,
                                reminderTime = if (isReminderEnabled) reminderTime else ""
                            )
                            viewModel.addNewPractice(newMantra) { newId ->
                                onCreated(newId)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = getMantraColor(selectedColor)),
                    enabled = mantraName.isNotBlank()
                ) {
                    Text("Create Mantra")
                }
            },
            dismissButton = {
                TextButton(onClick = { stage = "SELECT_TEMPLATE" }) { Text("Back") }
            }
        )
    }
}

@Composable
fun EditMantraDialog(
    practice: com.example.data.CustomPractice,
    isDefaultMantra: Boolean,
    onDismiss: () -> Unit,
    onSave: (com.example.data.CustomPractice, Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var mantraName by remember { mutableStateOf(practice.name) }
    var practiceType by remember { mutableStateOf(practice.practiceType) }
    var defaultTarget by remember { mutableStateOf(practice.defaultTarget.toString()) }
    var quickAddValues by remember { mutableStateOf(practice.quickAddValues) }
    var initialLifetimeCount by remember { mutableStateOf(practice.initialLifetimeCount.toString()) }
    var selectedColor by remember { mutableStateOf(practice.themeColor) }
    var isDefault by remember { mutableStateOf(isDefaultMantra) }
    
    var isReminderEnabled by remember { mutableStateOf(practice.isReminderEnabled) }
    var reminderTime by remember { mutableStateOf(practice.reminderTime.ifEmpty { "06:30 AM" }) }

    val colorsList = listOf("Indigo", "Royal", "Teal", "Saffron", "Crimson", "Emerald", "Amber", "Slate", "Violet")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Selected Mantra") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = mantraName,
                    onValueChange = { mantraName = it },
                    label = { Text("Mantra Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Practice Mode", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("SANDHYA" to "Sandhyas", "CONTINUOUS" to "Repetitive", "RECITATION" to "Recitations").forEach { mode ->
                        val (key, label) = mode
                        val isSelected = practiceType == key
                        InputChip(
                            selected = isSelected,
                            onClick = { practiceType = key },
                            label = { Text(label) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = getMantraColor(selectedColor).copy(alpha = 0.15f),
                                selectedLabelColor = getMantraColor(selectedColor)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = defaultTarget,
                    onValueChange = { if (it.all { c -> c.isDigit() }) defaultTarget = it },
                    label = { Text("Daily Target Japa (0 for none)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (practiceType == "CONTINUOUS") {
                    OutlinedTextField(
                        value = quickAddValues,
                        onValueChange = { quickAddValues = it },
                        label = { Text("Quick-Add Increment Values") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = initialLifetimeCount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) initialLifetimeCount = it },
                    label = { Text("Initial Lifetime Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Theme Color Accent", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.foundation.lazy.LazyRow(modifier = Modifier.fillMaxWidth()) {
                    items(colorsList.size) { index ->
                        val colorName = colorsList[index]
                        val isColorSelected = selectedColor == colorName
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(32.dp)
                                .background(getMantraColor(colorName), CircleShape)
                                .border(
                                    width = if (isColorSelected) 3.dp else 1.dp,
                                    color = if (isColorSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorName },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isColorSelected) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                var showVisualPicker by remember { mutableStateOf(false) }
                var customHex by remember { mutableStateOf(if (selectedColor.startsWith("#")) selectedColor else "") }

                if (showVisualPicker) {
                    ColorPickerDialog(
                        initialColor = getMantraColor(selectedColor),
                        onColorSelected = { 
                            val hex = it.toHexString()
                            selectedColor = hex
                            customHex = hex
                        },
                        onDismiss = { showVisualPicker = false }
                    )
                }

                OutlinedTextField(
                    value = customHex,
                    onValueChange = { 
                        customHex = it
                        if (it.length >= 4 && (it.startsWith("#"))) {
                            selectedColor = it
                        }
                    },
                    label = { Text("Custom Hex Color (e.g. #6449A7)") },
                    placeholder = { Text("#RRGGBB") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_custom_hex_input"),
                    leadingIcon = { 
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(getMantraColor(selectedColor), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showVisualPicker = true }) {
                            Icon(Icons.Default.Palette, contentDescription = "Pick Color")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Daily Reminder Alert", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Send regular notification reminder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    Switch(checked = isReminderEnabled, onCheckedChange = { isReminderEnabled = it })
                }

                if (isReminderEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        label = { Text("Reminder Time (e.g. 06:30 AM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isDefault = !isDefault },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Set as Default", style = MaterialTheme.typography.bodyLarge)
                        Text("Open this mantra automatically when the app starts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        modifier = Modifier.testTag("mantra_default_switch")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Mantra Entirely", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mantraName.isNotBlank()) {
                        val updated = practice.copy(
                            name = mantraName,
                            practiceType = practiceType,
                            defaultTarget = defaultTarget.toIntOrNull() ?: 108,
                            quickAddValues = quickAddValues,
                            initialLifetimeCount = initialLifetimeCount.toLongOrNull() ?: 0L,
                            themeColor = selectedColor,
                            isReminderEnabled = isReminderEnabled,
                            reminderTime = if (isReminderEnabled) reminderTime else ""
                        )
                        onSave(updated, isDefault)
                    }
                },
                modifier = Modifier.testTag("save_custom_practice_button"),
                colors = ButtonDefaults.buttonColors(containerColor = getMantraColor(selectedColor)),
                enabled = mantraName.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditGayatriDialog(
    currentName: String,
    currentColor: String,
    isDefaultMantra: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var mantraName by remember { mutableStateOf(currentName) }
    var selectedColor by remember { mutableStateOf(currentColor) }
    var isDefault by remember { mutableStateOf(isDefaultMantra) }
    val colorsList = listOf("Indigo", "Royal", "Teal", "Saffron", "Crimson", "Emerald", "Amber", "Slate", "Violet")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Mantra Theme") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = mantraName,
                    onValueChange = { mantraName = it },
                    label = { Text("Mantra Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_gayatri_name_input")
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isDefault = !isDefault },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Set as Default", style = MaterialTheme.typography.bodyLarge)
                        Text("Open this mantra automatically when the app starts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        modifier = Modifier.testTag("gayatri_default_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Select your preferred theme color accent. This will instantly personalize the tracking, circle progress indicators, and counters.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Theme Color Accent", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(modifier = Modifier.fillMaxWidth()) {
                    items(colorsList.size) { index ->
                        val colorName = colorsList[index]
                        val isColorSelected = selectedColor.lowercase() == colorName.lowercase()
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(36.dp)
                                .background(getMantraColor(colorName), CircleShape)
                                .border(
                                    width = if (isColorSelected) 3.dp else 1.dp,
                                    color = if (isColorSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorName },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isColorSelected) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                var showVisualPicker by remember { mutableStateOf(false) }
                var customHex by remember { mutableStateOf(if (selectedColor.startsWith("#")) selectedColor else "") }

                if (showVisualPicker) {
                    ColorPickerDialog(
                        initialColor = getMantraColor(selectedColor),
                        onColorSelected = { 
                            val hex = it.toHexString()
                            selectedColor = hex
                            customHex = hex
                        },
                        onDismiss = { showVisualPicker = false }
                    )
                }

                OutlinedTextField(
                    value = customHex,
                    onValueChange = { 
                        customHex = it
                        if (it.length >= 4 && (it.startsWith("#"))) {
                            selectedColor = it
                        }
                    },
                    label = { Text("Custom Hex Color (e.g. #6449A7)") },
                    placeholder = { Text("#RRGGBB") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("gayatri_custom_hex_input"),
                    leadingIcon = { 
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(getMantraColor(selectedColor), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showVisualPicker = true }) {
                            Icon(Icons.Default.Palette, contentDescription = "Pick Color")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(mantraName, selectedColor, isDefault) },
                colors = ButtonDefaults.buttonColors(containerColor = getMantraColor(selectedColor)),
                enabled = mantraName.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

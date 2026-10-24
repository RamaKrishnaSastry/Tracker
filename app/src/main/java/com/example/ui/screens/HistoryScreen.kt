// NOTE TO CONTRIBUTORS: Keep HistoryScreen thin. Place reusable UI components in com.example.ui.components (e.g. HistoryComponents.kt).
package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomPractice
import com.example.data.CustomPracticeEntry
import com.example.data.JapaEntry
import com.example.ui.GAYATRI_PRACTICE_ID
import com.example.ui.JapaViewModel
import com.example.ui.components.*
import com.example.ui.theme.getMantraColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: JapaViewModel,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.allEntries.collectAsState()
    val allCustomEntries by viewModel.allCustomPracticeEntries.collectAsState()
    val allPractices by viewModel.allCustomPractices.collectAsState()
    val stats by viewModel.statistics.collectAsState()

    val gayatriColorName by viewModel.gayatriColor.collectAsState()
    val gayatriColor = getMantraColor(gayatriColorName)
    val gayatriName by viewModel.gayatriName.collectAsState()

    // Selection filter: null means "Combined History"
    var selectedMantraId by rememberSaveable { mutableStateOf<Long?>(null) }
    
    // Calendar visible month state hoisted to align metrics row
    val calendarSaver = remember {
        Saver<MutableState<Calendar>, Long>(
            save = { it.value.timeInMillis },
            restore = { mutableStateOf(Calendar.getInstance().apply { timeInMillis = it }) }
        )
    }
    val currentCalendarState = rememberSaveable(saver = calendarSaver) { mutableStateOf(Calendar.getInstance()) }
    val currentCalendar = currentCalendarState.value
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH) + 1 // 1-indexed

    // Dialog state
    var selectedEntryToEdit by remember { mutableStateOf<JapaEntry?>(null) }
    var selectedCustomEntryToEdit by remember { mutableStateOf<Pair<CustomPracticeEntry, CustomPractice>?>(null) }
    var selectedSummaryDate by rememberSaveable { mutableStateOf<String?>(null) }
    var locationEditDate by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPlaceCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var isBulkEditMode by rememberSaveable { mutableStateOf(false) }

    val setSaver = Saver<MutableState<Set<String>>, ArrayList<String>>(
        save = { ArrayList(it.value) },
        restore = { mutableStateOf(it.toSet()) }
    )
    val selectedBulkDates = rememberSaveable(saver = setSaver) { mutableStateOf(setOf<String>()) }
    var showBulkLocationPicker by rememberSaveable { mutableStateOf(false) }

    val practiceTotals by viewModel.allPracticeTotals.collectAsState()

    // System Back Handler exits selection mode
    BackHandler(enabled = isBulkEditMode) {
        isBulkEditMode = false
        selectedBulkDates.value = emptySet()
    }

    val handleEnterSelectionMode: (String) -> Unit = { dateStr ->
        isBulkEditMode = true
        selectedBulkDates.value = selectedBulkDates.value + dateStr
    }

    val handleDatesAdded: (Set<String>) -> Unit = { dates ->
        isBulkEditMode = true
        selectedBulkDates.value = selectedBulkDates.value + dates
    }

    val handleToggleDateSelection: (String) -> Unit = { dateStr ->
        val current = selectedBulkDates.value
        val next = if (current.contains(dateStr)) current - dateStr else current + dateStr
        selectedBulkDates.value = next
        if (next.isEmpty() && isBulkEditMode) {
            isBulkEditMode = false
        }
    }

    // Combined/Dynamic lifetime grand sum
    val combinedLifetimeTotal = remember(stats, allPractices, practiceTotals) {
        val customTotal = allPractices.filter { it.id != GAYATRI_PRACTICE_ID }.sumOf { practice ->
            practice.initialLifetimeCount + (practiceTotals[practice.id] ?: 0)
        }
        stats.lifetimeTotal + customTotal
    }

    // 1. Eligible records filtered by location (First stage independent filter)
    val locationFilteredGayatriEntries = remember(entries, selectedPlaceCategoryFilter) {
        if (selectedPlaceCategoryFilter == null) {
            entries
        } else {
            entries.filter { (it.placeCategory ?: "Home").equals(selectedPlaceCategoryFilter, ignoreCase = true) }
        }
    }

    val locationFilteredCustomEntries = remember(allCustomEntries, selectedPlaceCategoryFilter) {
        if (selectedPlaceCategoryFilter == null) {
            allCustomEntries
        } else {
            allCustomEntries.filter { (it.placeCategory ?: "Home").equals(selectedPlaceCategoryFilter, ignoreCase = true) }
        }
    }

    // 2. Filtered totals for Mantra Selection Grid & Banner
    val filteredGayatriLifetimeTotal = remember(locationFilteredGayatriEntries, selectedPlaceCategoryFilter, stats.lifetimeTotal) {
        if (selectedPlaceCategoryFilter == null) {
            stats.lifetimeTotal
        } else {
            locationFilteredGayatriEntries.sumOf { it.dailyTotal.toLong() }
        }
    }

    val filteredPracticeTotals = remember(locationFilteredCustomEntries, selectedPlaceCategoryFilter, practiceTotals, allPractices) {
        if (selectedPlaceCategoryFilter == null) {
            practiceTotals
        } else {
            allPractices.associate { practice ->
                practice.id to locationFilteredCustomEntries.filter { it.practiceId == practice.id }.sumOf { it.dailyTotal }
            }
        }
    }

    val filteredCombinedLifetimeTotal = remember(selectedPlaceCategoryFilter, combinedLifetimeTotal, filteredGayatriLifetimeTotal, filteredPracticeTotals, allPractices) {
        if (selectedPlaceCategoryFilter == null) {
            combinedLifetimeTotal
        } else {
            filteredGayatriLifetimeTotal + allPractices.filter { it.id != GAYATRI_PRACTICE_ID }.sumOf { (filteredPracticeTotals[it.id] ?: 0).toLong() }
        }
    }

    // 3. Filtered Lookup Maps for Calendar (respects active location filter)
    val gayatriEntriesMap = remember(locationFilteredGayatriEntries) {
        locationFilteredGayatriEntries.associateBy { it.date }
    }
    
    val customEntriesMap = remember(locationFilteredCustomEntries) {
        locationFilteredCustomEntries.associateBy { it.practiceId to it.date }
    }
    
    val combinedDailyTotalsMap = remember(locationFilteredGayatriEntries, locationFilteredCustomEntries) {
        val map = mutableMapOf<String, Int>()
        locationFilteredGayatriEntries.forEach { entry ->
            map[entry.date] = (map[entry.date] ?: 0) + entry.dailyTotal
        }
        locationFilteredCustomEntries.forEach { entry ->
            map[entry.date] = (map[entry.date] ?: 0) + entry.dailyTotal
        }
        map
    }

    val monthAnalyticsList = remember(selectedMantraId, locationFilteredGayatriEntries, locationFilteredCustomEntries) {
        if (selectedMantraId == null) return@remember emptyList<MonthAnalytics>()
        val dailyTotals = mutableListOf<Pair<String, Int>>()
        if (selectedMantraId == GAYATRI_PRACTICE_ID) {
            locationFilteredGayatriEntries.forEach { entry ->
                if (entry.dailyTotal > 0) {
                    dailyTotals.add(entry.date to entry.dailyTotal)
                }
            }
        } else {
            locationFilteredCustomEntries.forEach { entry ->
                if (entry.practiceId == selectedMantraId && entry.dailyTotal > 0) {
                    dailyTotals.add(entry.date to entry.dailyTotal)
                }
            }
        }

        if (dailyTotals.isEmpty()) return@remember emptyList<MonthAnalytics>()

        val grouped = dailyTotals.groupBy { it.first.substring(0, 7) }
        val inputFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

        grouped.map { (monthStr, items) ->
            val monthDate = try { inputFormat.parse(monthStr) } catch (e: Exception) { null }
            val monthName = if (monthDate != null) outputFormat.format(monthDate) else monthStr

            val totals = items.map { it.second }
            val totalCount = totals.sum().toLong()
            val daysLogged = totals.size
            val averageCount = if (daysLogged > 0) totalCount.toDouble() / daysLogged else 0.0
            
            val daysInMonth = if (monthDate != null) {
                val cal = Calendar.getInstance()
                cal.time = monthDate
                cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            } else 30

            MonthAnalytics(
                monthStr = monthStr,
                monthName = monthName,
                totalCount = totalCount,
                daysLogged = daysLogged,
                averageCount = averageCount,
                maxCount = 0,
                minCount = 0,
                openValue = daysInMonth.toDouble(),
                closeValue = 0.0
            )
        }.sortedByDescending { it.monthStr }
    }

    val locationStats = remember(entries, allCustomEntries, selectedMantraId) {
        val catMap = mutableMapOf<String, Long>()
        var totalChants = 0L

        if (selectedMantraId == null || selectedMantraId == GAYATRI_PRACTICE_ID) {
            entries.forEach { e ->
                val cat = e.placeCategory ?: "Home"
                val count = e.dailyTotal.toLong()
                if (count > 0) {
                    catMap[cat] = (catMap[cat] ?: 0L) + count
                    totalChants += count
                }
            }
        }
        if (selectedMantraId == null || selectedMantraId != GAYATRI_PRACTICE_ID) {
            allCustomEntries.forEach { ce ->
                if (selectedMantraId == null || ce.practiceId == selectedMantraId) {
                    val cat = ce.placeCategory ?: "Home"
                    val count = ce.dailyTotal.toLong()
                    if (count > 0) {
                        catMap[cat] = (catMap[cat] ?: 0L) + count
                        totalChants += count
                    }
                }
            }
        }
        catMap to totalChants
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = isBulkEditMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isBulkEditMode = false
                                    selectedBulkDates.value = emptySet()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit Selection Mode",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = if (selectedBulkDates.value.isEmpty()) "Select dates" else "${selectedBulkDates.value.size} selected",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = {
                                    val calTemp = Calendar.getInstance().apply {
                                        time = currentCalendar.time
                                        set(Calendar.DAY_OF_MONTH, 1)
                                    }
                                    val maxDay = calTemp.getActualMaximum(Calendar.DAY_OF_MONTH)
                                    val cYear = currentCalendar.get(Calendar.YEAR)
                                    val cMonth = currentCalendar.get(Calendar.MONTH) + 1
                                    val monthDates = (1..maxDay).map { d ->
                                        String.format(Locale.US, "%04d-%02d-%02d", cYear, cMonth, d)
                                    }.toSet()
                                    selectedBulkDates.value = selectedBulkDates.value + monthDates
                                    isBulkEditMode = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Select All Month",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Button(
                            onClick = { showBulkLocationPicker = true },
                            enabled = selectedBulkDates.value.isNotEmpty(),
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit Location",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                val headerText = remember(selectedMantraId, allPractices, gayatriName) {
                    if (selectedMantraId == null) {
                        "Combined History"
                    } else if (selectedMantraId == GAYATRI_PRACTICE_ID) {
                        "$gayatriName History"
                    } else {
                        val matchingName = allPractices.find { it.id == selectedMantraId }?.name ?: "Mantra"
                        "$matchingName History"
                    }
                }

                Text(
                    text = headerText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (selectedMantraId == null) MaterialTheme.colorScheme.primary else if (selectedMantraId == GAYATRI_PRACTICE_ID) gayatriColor else getMantraColor(allPractices.find { it.id == selectedMantraId }?.themeColor ?: "teal")
                )
                Text(
                    text = "Select a mantra card to filter the calendar view, view statistics, or update logs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            // 1. ALL JAPAS TOGETHER — retain existing aggregate japa summary and its functionality
            item {
                CombinedHistoryBanner(
                    combinedLifetimeTotal = filteredCombinedLifetimeTotal,
                    isSelected = selectedMantraId == null,
                    onClick = { selectedMantraId = null }
                )
            }

            // PRACTICE LOCATIONS BREAKDOWN SECTION (just after combined grand total bar)
            if (locationStats.first.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    PracticeLocationsBreakdownSection(
                        locationStats = locationStats,
                        selectedPlaceCategoryFilter = selectedPlaceCategoryFilter,
                        onCategoryClick = { catId ->
                            selectedPlaceCategoryFilter = if (selectedPlaceCategoryFilter == catId) null else catId
                        }
                    )
                }
            }

            // 2. LOCATION-BASED FILTERS — provided immediately above mantra filters
            item {
                PlaceCategoryFilterRow(
                    selectedPlaceCategoryFilter = selectedPlaceCategoryFilter,
                    onSelectFilter = { selectedPlaceCategoryFilter = it }
                )
            }

            // 3. MANTRA-BASED FILTERS — provided immediately below location filters
            item {
                MantraSelectionGrid(
                    selectedMantraId = selectedMantraId,
                    gayatriName = gayatriName,
                    gayatriColor = gayatriColor,
                    gayatriLifetimeTotal = filteredGayatriLifetimeTotal,
                    allPractices = allPractices,
                    practiceTotals = filteredPracticeTotals,
                    onSelectMantra = { selectedMantraId = it }
                )
            }

            // STATS SECTION (when a mantra is selected) or Hint Card
            if (selectedMantraId != null) {
                val currentMonthStr = String.format(Locale.US, "%04d-%02d", currentYear, currentMonth)
                val activeMonthStats = monthAnalyticsList.find { it.monthStr == currentMonthStr }
                val accentColor = if (selectedMantraId == GAYATRI_PRACTICE_ID) gayatriColor else getMantraColor(allPractices.find { it.id == selectedMantraId }?.themeColor ?: "teal")
                
                if (activeMonthStats != null) {
                    val daysInMonth = activeMonthStats.openValue.toInt()
                    item {
                        MonthlyCompactStatsRow(
                            monthName = activeMonthStats.monthName,
                            averageCount = activeMonthStats.averageCount,
                            totalCount = activeMonthStats.totalCount,
                            daysLogged = activeMonthStats.daysLogged,
                            daysInMonth = daysInMonth,
                            accentColor = accentColor
                        )
                    }
                } else {
                    item {
                        val activeMonthName = remember(currentYear, currentMonth) {
                            val calTemp = Calendar.getInstance().apply {
                                set(Calendar.YEAR, currentYear)
                                set(Calendar.MONTH, currentMonth - 1)
                            }
                            SimpleDateFormat("MMMM yyyy", Locale.US).format(calTemp.time)
                        }
                        val daysInActiveMonth = remember(currentYear, currentMonth) {
                            val calTemp = Calendar.getInstance().apply {
                                set(Calendar.YEAR, currentYear)
                                set(Calendar.MONTH, currentMonth - 1)
                            }
                            calTemp.getActualMaximum(Calendar.DAY_OF_MONTH)
                        }
                        MonthlyCompactStatsRow(
                            monthName = activeMonthName,
                            averageCount = 0.0,
                            totalCount = 0L,
                            daysLogged = 0,
                            daysInMonth = daysInActiveMonth,
                            accentColor = accentColor
                        )
                    }
                }
            } else {
                // Hint card for combined view
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Analytics Info",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select a specific mantra card above to view dynamic monthly averages, insights, and performance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // 4. CALENDAR WITH FILTERED JAPA DATA (Filtered by location and/or mantra)
            item {
                Text(
                    text = if (selectedMantraId == null) "COMBINED PRACTICE CALENDAR" else "PRACTICE CALENDAR",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            item {
                CalendarSection(
                    selectedMantraId = selectedMantraId,
                    gayatriEntriesMap = gayatriEntriesMap,
                    customEntriesMap = customEntriesMap,
                    combinedDailyTotalsMap = combinedDailyTotalsMap,
                    activeColor = if (selectedMantraId == null) MaterialTheme.colorScheme.primary else if (selectedMantraId == GAYATRI_PRACTICE_ID) gayatriColor else getMantraColor(allPractices.find { it.id == selectedMantraId }?.themeColor ?: "teal"),
                    allPractices = allPractices,
                    currentCalendar = currentCalendar,
                    onCalendarChange = { currentCalendarState.value = it },
                    onDateClick = { clickedDate ->
                        selectedSummaryDate = clickedDate
                    },
                    isBulkEditMode = isBulkEditMode,
                    selectedBulkDates = selectedBulkDates.value,
                    onToggleDateSelection = handleToggleDateSelection,
                    onEnterSelectionMode = handleEnterSelectionMode,
                    onDatesAdded = handleDatesAdded,
                    onToggleBulkEditMode = {
                        isBulkEditMode = !isBulkEditMode
                        if (!isBulkEditMode) selectedBulkDates.value = emptySet()
                    },
                    onSelectAllMonth = {
                        val calTemp = Calendar.getInstance().apply {
                            time = currentCalendar.time
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        val maxDay = calTemp.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val allMonthDates = (1..maxDay).map { d ->
                            String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, d)
                        }.toSet()
                        selectedBulkDates.value = allMonthDates
                        isBulkEditMode = true
                    },
                    onClearBulkSelection = {
                        selectedBulkDates.value = emptySet()
                        isBulkEditMode = false
                    },
                    onOpenBulkEditDialog = {
                        if (selectedBulkDates.value.isNotEmpty()) {
                            showBulkLocationPicker = true
                        }
                    }
                )
            }

            // 5. TREND / BAR CHART SECTION (when mantra selected)
            if (selectedMantraId != null && monthAnalyticsList.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    MonthlyTrendSection(
                        monthAnalyticsList = monthAnalyticsList,
                        accentColor = if (selectedMantraId == GAYATRI_PRACTICE_ID) gayatriColor else getMantraColor(allPractices.find { it.id == selectedMantraId }?.themeColor ?: "teal")
                    )
                }
            }

            // 6. LOGGED ENTRIES LIST (when mantra selected)
            if (selectedMantraId != null) {
                item {
                    Text(
                        text = "LOGGED ENTRIES",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                if (selectedMantraId == GAYATRI_PRACTICE_ID) {
                    val filteredList = locationFilteredGayatriEntries.filter { it.dailyTotal > 0 }.sortedByDescending { it.date }
                    if (filteredList.isEmpty()) {
                        item { EmptyHistoryState() }
                    } else {
                        items(filteredList, key = { "gayatri_${it.date}" }) { entry ->
                            HistoryItemRow(
                                entry = entry,
                                gayatriName = gayatriName,
                                gayatriColor = gayatriColor,
                                onClick = { selectedSummaryDate = entry.date },
                                isBulkEditMode = isBulkEditMode,
                                isSelected = selectedBulkDates.value.contains(entry.date),
                                onEnterSelectionMode = handleEnterSelectionMode,
                                onToggleSelection = handleToggleDateSelection
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                } else {
                    val practice = allPractices.find { it.id == selectedMantraId }
                    val filteredList = locationFilteredCustomEntries.filter { it.practiceId == selectedMantraId && it.dailyTotal > 0 }.sortedByDescending { it.date }
                    if (practice != null && filteredList.isNotEmpty()) {
                        items(filteredList, key = { "custom_${it.practiceId}_${it.date}" }) { entry ->
                            CustomHistoryItemRow(
                                entry = entry,
                                practice = practice,
                                onClick = { selectedSummaryDate = entry.date },
                                isBulkEditMode = isBulkEditMode,
                                isSelected = selectedBulkDates.value.contains(entry.date),
                                onEnterSelectionMode = handleEnterSelectionMode,
                                onToggleSelection = handleToggleDateSelection
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        item { EmptyHistoryState() }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Gayatri Model Edit Dialog
    selectedEntryToEdit?.let { entry ->
        val isGayatriPunasEnabled by viewModel.isPunascharanaEnabled.collectAsState()
        HistoricalEditDialog(
            entry = entry,
            accentColor = gayatriColor,
            isPunascharanaEnabled = isGayatriPunasEnabled,
            onDismiss = { selectedEntryToEdit = null },
            onSave = { morning, afternoon, evening, mornPunas, noonPunas, evePunas ->
                viewModel.updateCounts(entry.date, morning, afternoon, evening, mornPunas, noonPunas, evePunas)
                selectedEntryToEdit = null
            }
        )
    }

    // Custom Entries Edit Dialogs
    selectedCustomEntryToEdit?.let { (entry, practice) ->
        if (practice.practiceType == "SANDHYA") {
            CustomSandhyaEditDialog(
                practice = practice,
                entry = entry,
                onDismiss = { selectedCustomEntryToEdit = null },
                onSave = { morning, afternoon, evening, morningPunas, afternoonPunas, eveningPunas ->
                    viewModel.updateCustomPracticeSandhyaCounts(
                        practiceId = practice.id,
                        date = entry.date,
                        morning = morning,
                        afternoon = afternoon,
                        evening = evening,
                        morningPunas = morningPunas,
                        afternoonPunas = afternoonPunas,
                        eveningPunas = eveningPunas
                    )
                    selectedCustomEntryToEdit = null
                }
            )
        } else {
            CustomCountEditDialog(
                practice = practice,
                entry = entry,
                onDismiss = { selectedCustomEntryToEdit = null },
                onSave = { count ->
                    viewModel.updateCustomPracticeCount(
                        practiceId = practice.id,
                        date = entry.date,
                        count = count
                    )
                    selectedCustomEntryToEdit = null
                }
            )
        }
    }

    selectedSummaryDate?.let { date ->
        val gayatriEntry = gayatriEntriesMap[date]
        val customEntriesForDate = remember(allCustomEntries, date) {
            allCustomEntries.filter { it.date == date }.associateBy { it.practiceId }
        }
        DailySummaryDialog(
            date = date,
            gayatriName = gayatriName,
            gayatriColor = gayatriColor,
            gayatriEntry = gayatriEntry,
            allPractices = allPractices,
            customEntries = customEntriesForDate,
            filterMantraId = selectedMantraId,
            filterPlaceCategory = selectedPlaceCategoryFilter,
            onEditLocation = { locationEditDate = date },
            onEditGayatri = { entry ->
                selectedEntryToEdit = entry
                selectedSummaryDate = null
            },
            onEditCustom = { entry, practice ->
                selectedCustomEntryToEdit = entry to practice
                selectedSummaryDate = null
            },
            onDismiss = { selectedSummaryDate = null }
        )
    }

    // Location Picker Dialog for editing a past date's place
    locationEditDate?.let { date ->
        val gEntry = gayatriEntriesMap[date]
        val cEntry = allCustomEntries.find { it.date == date }
        val curName = gEntry?.placeName ?: cEntry?.placeName ?: "Home"
        val curCat = gEntry?.placeCategory ?: cEntry?.placeCategory ?: "Home"
        val curLat = gEntry?.latitude ?: cEntry?.latitude
        val curLon = gEntry?.longitude ?: cEntry?.longitude

        LocationPickerDialog(
            initialPlaceName = curName,
            initialCategory = curCat,
            initialLatitude = curLat,
            initialLongitude = curLon,
            title = "Edit Location for $date",
            confirmButtonText = "Save Place",
            onDismiss = { locationEditDate = null },
            onLocationSelected = { name, cat, lat, lon ->
                viewModel.updateEntryLocation(date, name, cat, lat, lon)
                locationEditDate = null
            }
        )
    }

    if (showBulkLocationPicker) {
        LocationPickerDialog(
            initialPlaceName = "Home",
            initialCategory = "Home",
            title = "Edit Location for ${selectedBulkDates.value.size} Selected Dates",
            confirmButtonText = "Apply to Selected Dates",
            onDismiss = { showBulkLocationPicker = false },
            onLocationSelected = { name, cat, lat, lon ->
                viewModel.bulkUpdateEntriesLocation(selectedBulkDates.value, name, cat, lat, lon)
                selectedBulkDates.value = emptySet()
                isBulkEditMode = false
                showBulkLocationPicker = false
            }
        )
    }
}

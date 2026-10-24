// NOTE TO CONTRIBUTORS: Keep HistoryScreen thin. Place reusable UI components in com.example.ui.components (e.g. HistoryComponents.kt).
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.horizontalScroll
import com.example.data.PlaceCategory
import com.example.ui.components.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.example.data.JapaEntry
import com.example.data.CustomPractice
import com.example.data.CustomPracticeEntry
import com.example.ui.JapaViewModel
import com.example.ui.GAYATRI_PRACTICE_ID
import com.example.ui.theme.getMantraColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AutoResizedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    minFontSize: TextUnit = 8.sp,
    textAlign: TextAlign? = null
) {
    var resizedTextStyle by remember(text, style) { mutableStateOf(style) }
    var shouldDraw by remember(text, style) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        textAlign = textAlign,
        modifier = modifier.drawWithContent {
            if (shouldDraw) {
                drawContent()
            }
        },
        style = resizedTextStyle,
        maxLines = maxLines,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if ((result.didOverflowWidth || result.didOverflowHeight) && resizedTextStyle.fontSize > minFontSize) {
                val nextSize = (resizedTextStyle.fontSize.value * 0.88f).sp
                resizedTextStyle = resizedTextStyle.copy(
                    fontSize = if (nextSize < minFontSize) minFontSize else nextSize
                )
            } else {
                shouldDraw = true
            }
        }
    )
}

sealed class UnifiedHistoryItem(val dateString: String) {
    data class GayatriItem(val entry: JapaEntry) : UnifiedHistoryItem(entry.date)
    data class CustomItem(val entry: CustomPracticeEntry, val practice: CustomPractice) : UnifiedHistoryItem(entry.date)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: JapaViewModel,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.allEntries.collectAsState()
    val allCustomEntries by viewModel.allCustomPracticeEntries.collectAsState()
    val allPractices by viewModel.allCustomPractices.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
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

    val practiceTotals by viewModel.allPracticeTotals.collectAsState()

    // Combined/Dynamic lifetime grand sum
    val combinedLifetimeTotal = remember(stats, allPractices, practiceTotals) {
        val customTotal = allPractices.filter { it.id != GAYATRI_PRACTICE_ID }.sumOf { practice ->
            practice.initialLifetimeCount + (practiceTotals[practice.id] ?: 0)
        }
        stats.lifetimeTotal + customTotal
    }

    // Lookup Maps for Calendar
    val gayatriEntriesMap = remember(entries) {
        entries.associateBy { it.date }
    }
    
    val customEntriesMap = remember(allCustomEntries) {
        allCustomEntries.associateBy { it.practiceId to it.date }
    }
    
    val combinedDailyTotalsMap = remember(entries, allCustomEntries) {
        val map = mutableMapOf<String, Int>()
        entries.forEach { entry ->
            map[entry.date] = (map[entry.date] ?: 0) + entry.dailyTotal
        }
        allCustomEntries.forEach { entry ->
            map[entry.date] = (map[entry.date] ?: 0) + entry.dailyTotal
        }
        map
    }

    val monthAnalyticsList = remember(selectedMantraId, entries, allCustomEntries) {
        if (selectedMantraId == null) return@remember emptyList<MonthAnalytics>()
        val dailyTotals = mutableListOf<Pair<String, Int>>()
        if (selectedMantraId == GAYATRI_PRACTICE_ID) {
            entries.forEach { entry ->
                if (entry.dailyTotal > 0) {
                    dailyTotals.add(entry.date to entry.dailyTotal)
                }
            }
        } else {
            allCustomEntries.forEach { entry ->
                if (entry.practiceId == selectedMantraId && entry.dailyTotal > 0) {
                    dailyTotals.add(entry.date to entry.dailyTotal)
                }
            }
        }

        if (dailyTotals.isEmpty()) return@remember emptyList<MonthAnalytics>()

        val grouped = dailyTotals.groupBy { it.first.substring(0, 7) }
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
        val outputFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US)

        grouped.map { (monthStr, items) ->
            val monthDate = try { inputFormat.parse(monthStr) } catch (e: Exception) { null }
            val monthName = if (monthDate != null) outputFormat.format(monthDate) else monthStr

            val totals = items.map { it.second }
            val totalCount = totals.sum().toLong()
            val daysLogged = totals.size
            val averageCount = if (daysLogged > 0) totalCount.toDouble() / daysLogged else 0.0
            
            val daysInMonth = if (monthDate != null) {
                val cal = java.util.Calendar.getInstance()
                cal.time = monthDate
                cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
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

    // List of unified history items sorted descending
    val combinedList = remember(entries, allCustomEntries, allPractices) {
        val list = mutableListOf<UnifiedHistoryItem>()
        entries.forEach { list.add(UnifiedHistoryItem.GayatriItem(it)) }
        allCustomEntries.forEach { entry ->
            val p = allPractices.find { it.id == entry.practiceId }
            if (p != null) {
                list.add(UnifiedHistoryItem.CustomItem(entry, p))
            }
        }
        list.sortedByDescending { it.dateString }
    }

    // Filter items based on selected selection, category filter AND search query
    val filteredItems = remember(combinedList, selectedMantraId, searchQuery, selectedPlaceCategoryFilter) {
        combinedList.filter { item ->
            // Match selected mantra ID
            val matchesFilter = when {
                selectedMantraId == null -> true // Combined
                selectedMantraId == GAYATRI_PRACTICE_ID -> item is UnifiedHistoryItem.GayatriItem
                else -> item is UnifiedHistoryItem.CustomItem && item.entry.practiceId == selectedMantraId
            }

            // Match place category filter
            val matchesCategory = if (selectedPlaceCategoryFilter == null) {
                true
            } else {
                val cat = when (item) {
                    is UnifiedHistoryItem.GayatriItem -> item.entry.placeCategory ?: "Home"
                    is UnifiedHistoryItem.CustomItem -> item.entry.placeCategory ?: "Home"
                }
                cat.equals(selectedPlaceCategoryFilter, ignoreCase = true)
            }

            // Match search query (date, practice name, place name or category)
            val matchesQuery = if (searchQuery.trim().isEmpty()) {
                true
            } else {
                val query = searchQuery.trim()
                val placeName = when (item) {
                    is UnifiedHistoryItem.GayatriItem -> item.entry.placeName ?: ""
                    is UnifiedHistoryItem.CustomItem -> item.entry.placeName ?: ""
                }
                val placeCat = when (item) {
                    is UnifiedHistoryItem.GayatriItem -> item.entry.placeCategory ?: ""
                    is UnifiedHistoryItem.CustomItem -> item.entry.placeCategory ?: ""
                }
                item.dateString.contains(query, ignoreCase = true) ||
                placeName.contains(query, ignoreCase = true) ||
                placeCat.contains(query, ignoreCase = true) ||
                when (item) {
                    is UnifiedHistoryItem.GayatriItem -> gayatriName.contains(query, ignoreCase = true) || "Gayatri".contains(query, ignoreCase = true)
                    is UnifiedHistoryItem.CustomItem -> {
                        val practiceName = allPractices.find { it.id == item.entry.practiceId }?.name ?: ""
                        practiceName.contains(query, ignoreCase = true)
                    }
                }
            }

            matchesFilter && matchesCategory && matchesQuery
        }
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
        modifier = modifier.fillMaxSize()
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
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
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

            // TOP HORIZONTAL BANNER (Combined History)
            item {
                val isCombinedSelected = selectedMantraId == null
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMantraId = null }
                        .testTag("combined_history_banner_tile"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCombinedSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isCombinedSelected) 2.dp else 1.dp,
                        color = if (isCombinedSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isCombinedSelected) 3.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AllInclusive,
                                    contentDescription = "Combined Total Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "COMBINED GRAND TOTAL",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "Unified feed of all japas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                        AutoResizedText(
                            text = String.format("%,d", combinedLifetimeTotal),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            minFontSize = 11.sp
                        )
                    }
                }
            }

            // MANTRA BLOCKS (2 Columns Grid)
            item {
                Text(
                    text = "MANTRAS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(6.dp))

                val allBlocks = mutableListOf<@Composable () -> Unit>()
                
                // Gayatri block
                val isGayatriSelected = selectedMantraId == GAYATRI_PRACTICE_ID
                allBlocks.add {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 86.dp)
                            .clickable { selectedMantraId = GAYATRI_PRACTICE_ID }
                            .testTag("block_gayatri_japa"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isGayatriSelected) gayatriColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isGayatriSelected) 2.dp else 1.dp,
                            color = if (isGayatriSelected) gayatriColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isGayatriSelected) 2.dp else 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = gayatriName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isGayatriSelected) gayatriColor else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            AutoResizedText(
                                text = String.format("%,d", stats.lifetimeTotal),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = gayatriColor,
                                maxLines = 1,
                                minFontSize = 10.sp
                            )
                        }
                    }
                }

                // Custom blocks
                allPractices.filter { it.id != GAYATRI_PRACTICE_ID }.forEach { practice ->
                    val isSelected = selectedMantraId == practice.id
                    val color = getMantraColor(practice.themeColor)
                    val total = practice.initialLifetimeCount + (practiceTotals[practice.id] ?: 0)
                    allBlocks.add {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 86.dp)
                                .clickable { selectedMantraId = practice.id }
                                .testTag("block_mantra_${practice.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = practice.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                AutoResizedText(
                                    text = String.format("%,d", total),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = color,
                                    maxLines = 1,
                                    minFontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Render in pairs
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in allBlocks.indices step 2) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                allBlocks[i]()
                            }
                            if (i + 1 < allBlocks.size) {
                                Box(modifier = Modifier.weight(1f)) {
                                    allBlocks[i + 1]()
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f)) {} // Empty space for odd number
                            }
                        }
                    }
                }
            }

            // 1. STATS SECTION (AT THE TOP, ABOVE CALENDAR)
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

            // 2. CALENDAR SECTION (IN THE MIDDLE)
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
                        val mId = selectedMantraId
                        if (mId == null) {
                            // Read-only summary dialog for all mantras
                            selectedSummaryDate = clickedDate
                        } else if (mId == GAYATRI_PRACTICE_ID) {
                            // Edit Gayatri
                            val existing = gayatriEntriesMap[clickedDate]
                            selectedEntryToEdit = existing ?: JapaEntry(
                                date = clickedDate,
                                pratahSandhyaCount = 0,
                                madhyahnikaSandhyaCount = 0,
                                sayamSandhyaCount = 0,
                                pratahPunascharanaCount = 0,
                                madhyahnikaPunascharanaCount = 0,
                                sayamPunascharanaCount = 0,
                                updatedAt = ""
                            )
                        } else {
                            // Edit Custom
                            val practice = allPractices.find { it.id == mId }
                            if (practice != null) {
                                val existing = customEntriesMap[mId to clickedDate]
                                selectedCustomEntryToEdit = (existing ?: CustomPracticeEntry(
                                    practiceId = mId,
                                    date = clickedDate,
                                    count = 0,
                                    morningCount = 0,
                                    afternoonCount = 0,
                                    eveningCount = 0,
                                    morningPunasCount = 0,
                                    afternoonPunasCount = 0,
                                    eveningPunasCount = 0,
                                    updatedAt = ""
                                )) to practice
                            }
                        }
                    }
                )
            }

            // 3. TREND / BAR CHART SECTION (AT THE BOTTOM)
            if (selectedMantraId != null && monthAnalyticsList.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    MonthlyTrendSection(
                        monthAnalyticsList = monthAnalyticsList,
                        accentColor = if (selectedMantraId == GAYATRI_PRACTICE_ID) gayatriColor else getMantraColor(allPractices.find { it.id == selectedMantraId }?.themeColor ?: "teal")
                    )
                }
            }

            // 4. PRACTICE LOCATIONS BREAKDOWN SECTION
            if (locationStats.first.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Practice Locations Breakdown",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = "${locationStats.first.size} Categories",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            locationStats.first.entries.sortedByDescending { it.value }.forEach { (catId, count) ->
                                val category = PlaceCategory.fromId(catId)
                                val percentage = if (locationStats.second > 0) (count * 100f / locationStats.second) else 0f

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPlaceCategoryFilter = if (selectedPlaceCategoryFilter == category.id) null else category.id
                                        },
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(category.primaryColor, CircleShape)
                                            )
                                            Text(
                                                text = category.displayName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                        }
                                        Text(
                                            text = String.format(Locale.US, "%,d chants (%.1f%%)", count, percentage),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = category.primaryColor
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (percentage / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = category.primaryColor,
                                        trackColor = category.containerColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. FILTER BY CATEGORY CHIPS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FILTER BY PLACE CATEGORY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedPlaceCategoryFilter == null,
                            onClick = { selectedPlaceCategoryFilter = null },
                            label = {
                                Text(
                                    "All Places",
                                    fontWeight = if (selectedPlaceCategoryFilter == null) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )

                        PlaceCategory.entries.forEach { category ->
                            val isSelected = selectedPlaceCategoryFilter.equals(category.id, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPlaceCategoryFilter = if (isSelected) null else category.id
                                },
                                label = {
                                    Text(
                                        text = category.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) category.primaryColor else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(category.primaryColor, CircleShape)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = category.containerColor,
                                    selectedLabelColor = category.contentColor
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) category.primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    borderWidth = if (isSelected) 2.dp else 1.dp
                                )
                            )
                        }
                    }
                }
            }

            // 6. PRACTICE LOG ENTRIES SECTION
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRACTICE LOG ENTRIES (${filteredItems.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )

                    if (selectedPlaceCategoryFilter != null) {
                        TextButton(
                            onClick = { selectedPlaceCategoryFilter = null },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Clear Filter", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No practice entries match your current search or location filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredItems) { item ->
                    when (item) {
                        is UnifiedHistoryItem.GayatriItem -> {
                            HistoryItemRow(
                                entry = item.entry,
                                gayatriName = gayatriName,
                                gayatriColor = gayatriColor,
                                onClick = { selectedEntryToEdit = item.entry }
                            )
                        }
                        is UnifiedHistoryItem.CustomItem -> {
                            CustomHistoryItemRow(
                                entry = item.entry,
                                practice = item.practice,
                                onClick = { selectedCustomEntryToEdit = item.entry to item.practice }
                            )
                        }
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
            onEditLocation = { locationEditDate = date },
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
}

@Composable
fun CalendarSection(
    selectedMantraId: Long?,
    gayatriEntriesMap: Map<String, JapaEntry>,
    customEntriesMap: Map<Pair<Long, String>, CustomPracticeEntry>,
    combinedDailyTotalsMap: Map<String, Int>,
    activeColor: Color,
    allPractices: List<CustomPractice>,
    currentCalendar: Calendar,
    onCalendarChange: (Calendar) -> Unit,
    onDateClick: (String) -> Unit
) {
    val year = currentCalendar.get(Calendar.YEAR)
    val month = currentCalendar.get(Calendar.MONTH) + 1 // 1-indexed

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month switcher header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newCal = Calendar.getInstance().apply {
                            time = currentCalendar.time
                            add(Calendar.MONTH, -1)
                        }
                        onCalendarChange(newCal)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month"
                    )
                }

                val monthYearString = remember(year, month) {
                    val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    val calTemp = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                    }
                    sdf.format(calTemp.time)
                }

                Text(
                    text = monthYearString,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        val newCal = Calendar.getInstance().apply {
                            time = currentCalendar.time
                            add(Calendar.MONTH, 1)
                        }
                        onCalendarChange(newCal)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month"
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            InteractiveMonthlyCalendar(
                year = year,
                month = month,
                selectedMantraId = selectedMantraId,
                gayatriEntriesMap = gayatriEntriesMap,
                customEntriesMap = customEntriesMap,
                combinedDailyTotalsMap = combinedDailyTotalsMap,
                activeColor = activeColor,
                allPractices = allPractices,
                onDateClick = onDateClick
            )
        }
    }
}

@Composable
fun HistoryItemRow(
    entry: JapaEntry,
    gayatriName: String,
    gayatriColor: Color,
    onClick: () -> Unit
) {
    val displayDate = remember(entry.date) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(entry.date)
            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date ?: Date())
        } catch (e: Exception) {
            entry.date
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("history_item_${entry.date}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, gayatriColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(gayatriColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = gayatriName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = gayatriColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val mStr = if (entry.pratahSandhyaCount == -1) "Pass" else entry.pratahSandhyaCount.toString()
                    val aStr = if (entry.madhyahnikaSandhyaCount == -1) "Pass" else entry.madhyahnikaSandhyaCount.toString()
                    val sStr = if (entry.sayamSandhyaCount == -1) "Pass" else entry.sayamSandhyaCount.toString()
                    
                    Text(
                        text = "Morn: $mStr" + if (entry.pratahPunascharanaCount > 0) "+${entry.pratahPunascharanaCount}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Noon: $aStr" + if (entry.madhyahnikaPunascharanaCount > 0) "+${entry.madhyahnikaPunascharanaCount}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Eve: $sStr" + if (entry.sayamPunascharanaCount > 0) "+${entry.sayamPunascharanaCount}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                PlaceBadge(
                    placeName = entry.placeName,
                    placeCategory = entry.placeCategory,
                    latitude = entry.latitude,
                    longitude = entry.longitude,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.dailyTotal.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = gayatriColor
                )
                Text(
                    text = "TOTAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun CustomHistoryItemRow(
    entry: CustomPracticeEntry,
    practice: CustomPractice,
    onClick: () -> Unit
) {
    val color = getMantraColor(practice.themeColor)
    val displayDate = remember(entry.date) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(entry.date)
            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date ?: Date())
        } catch (e: Exception) {
            entry.date
        }
    }

    val hasSandhyaCounts = entry.morningCount > 0 || entry.afternoonCount > 0 || entry.eveningCount > 0
    val displaySum = if (hasSandhyaCounts) {
        entry.morningCount + entry.afternoonCount + entry.eveningCount + entry.morningPunasCount + entry.afternoonPunasCount + entry.eveningPunasCount
    } else {
        entry.count
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("custom_history_item_${practice.id}_${entry.date}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = practice.name,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasSandhyaCounts) {
                    Spacer(modifier = Modifier.height(6.dp))
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Morn: ${entry.morningCount}" + if (entry.morningPunasCount > 0) "+${entry.morningPunasCount}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Noon: ${entry.afternoonCount}" + if (entry.afternoonPunasCount > 0) "+${entry.afternoonPunasCount}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Eve: ${entry.eveningCount}" + if (entry.eveningPunasCount > 0) "+${entry.eveningPunasCount}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                PlaceBadge(
                    placeName = entry.placeName,
                    placeCategory = entry.placeCategory,
                    latitude = entry.latitude,
                    longitude = entry.longitude,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = displaySum.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = color
                )
                Text(
                    text = "TOTAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No recorded entries found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Text(
            text = "Begin chanting on the 'Today' tab to start logging.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun CurrentCountSummaryBox(
    morning: Int,
    afternoon: Int,
    evening: Int,
    morningPunas: Int,
    afternoonPunas: Int,
    eveningPunas: Int,
    isPunasEnabled: Boolean,
    accentColor: Color
) {
    val m = if (morning == -1) 0 else morning
    val a = if (afternoon == -1) 0 else afternoon
    val e = if (evening == -1) 0 else evening
    val mp = if (morningPunas == -1) 0 else morningPunas
    val ap = if (afternoonPunas == -1) 0 else afternoonPunas
    val ep = if (eveningPunas == -1) 0 else eveningPunas
    
    val totalCount = m + a + e + mp + ap + ep

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CURRENT DAY TOTAL",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp),
                color = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(java.util.Locale.US, "%,d", totalCount),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = accentColor.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Pratah
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Pratah",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (morning == -1) "Passed" else "${m + mp}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Madhyahnika
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Noon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (afternoon == -1) "Passed" else "${a + ap}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Sayam
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Sayam",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (evening == -1) "Passed" else "${e + ep}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun HistoricalEditDialog(
    entry: JapaEntry,
    accentColor: Color,
    isPunascharanaEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int, Int, Int, Int) -> Unit
) {
    var morningInput by remember { mutableStateOf(entry.pratahSandhyaCount.toString()) }
    var afternoonInput by remember { mutableStateOf(entry.madhyahnikaSandhyaCount.toString()) }
    var eveningInput by remember { mutableStateOf(entry.sayamSandhyaCount.toString()) }
    var morningPunasInput by remember { mutableStateOf(entry.pratahPunascharanaCount.toString()) }
    var afternoonPunasInput by remember { mutableStateOf(entry.madhyahnikaPunascharanaCount.toString()) }
    var eveningPunasInput by remember { mutableStateOf(entry.sayamPunascharanaCount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        title = {
            val formattedTitleDate = remember(entry.date) {
                try {
                    val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val outputSdf = SimpleDateFormat("EEEE, d MMM yyyy", Locale.US)
                    val d = inputSdf.parse(entry.date)
                    if (d != null) outputSdf.format(d) else entry.date
                } catch (e: Exception) {
                    entry.date
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Edit Japa Counts",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.25.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedTitleDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CurrentCountSummaryBox(
                    morning = entry.pratahSandhyaCount,
                    afternoon = entry.madhyahnikaSandhyaCount,
                    evening = entry.sayamSandhyaCount,
                    morningPunas = entry.pratahPunascharanaCount,
                    afternoonPunas = entry.madhyahnikaPunascharanaCount,
                    eveningPunas = entry.sayamPunascharanaCount,
                    isPunasEnabled = isPunascharanaEnabled,
                    accentColor = accentColor
                )

                Text(
                    text = "Configure Counts below. Enter whole numbers, or -1 if the session was passed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                if (isPunascharanaEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Sandhya Counts",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )

                            OutlinedTextField(
                                value = morningInput,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) morningInput = it },
                                label = { Text("Pratah") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    focusedLabelColor = accentColor,
                                    cursorColor = accentColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.WbSunny,
                                        contentDescription = null,
                                        tint = accentColor.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_edit_morning")
                            )

                            OutlinedTextField(
                                value = afternoonInput,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) afternoonInput = it },
                                label = { Text("Madhyahnika") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    focusedLabelColor = accentColor,
                                    cursorColor = accentColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = accentColor.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_edit_afternoon")
                            )

                            OutlinedTextField(
                                value = eveningInput,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) eveningInput = it },
                                label = { Text("Sayam") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    focusedLabelColor = accentColor,
                                    cursorColor = accentColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.NightsStay,
                                        contentDescription = null,
                                        tint = accentColor.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_edit_evening")
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Punascharana",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )

                            OutlinedTextField(
                                value = morningPunasInput,
                                onValueChange = { if (it.all { c -> c.isDigit() }) morningPunasInput = it },
                                label = { Text("Pratah") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    focusedLabelColor = accentColor,
                                    cursorColor = accentColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.WbSunny,
                                        contentDescription = null,
                                        tint = accentColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_edit_morning_punas")
                            )

                            OutlinedTextField(
                                value = afternoonPunasInput,
                                onValueChange = { if (it.all { c -> c.isDigit() }) afternoonPunasInput = it },
                                label = { Text("Madhyahnika") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    focusedLabelColor = accentColor,
                                    cursorColor = accentColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.LightMode,
                                        contentDescription = null,
                                        tint = accentColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_edit_afternoon_punas")
                            )

                            OutlinedTextField(
                                value = eveningPunasInput,
                                onValueChange = { if (it.all { c -> c.isDigit() }) eveningPunasInput = it },
                                label = { Text("Sayam") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    focusedLabelColor = accentColor,
                                    cursorColor = accentColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.NightsStay,
                                        contentDescription = null,
                                        tint = accentColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_edit_evening_punas")
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Sandhya Counts",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )

                        OutlinedTextField(
                            value = morningInput,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) morningInput = it },
                            label = { Text("Morning (Pratah) Sandhya") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                focusedLabelColor = accentColor,
                                cursorColor = accentColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = accentColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_edit_morning")
                        )

                        OutlinedTextField(
                            value = afternoonInput,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) afternoonInput = it },
                            label = { Text("Noon (Madhyahnika) Sandhya") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                focusedLabelColor = accentColor,
                                cursorColor = accentColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = accentColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_edit_afternoon")
                        )

                        OutlinedTextField(
                            value = eveningInput,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) eveningInput = it },
                            label = { Text("Evening (Sayam) Sandhya") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                focusedLabelColor = accentColor,
                                cursorColor = accentColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = accentColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_edit_evening")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val m = morningInput.toIntOrNull() ?: 0
                        val a = afternoonInput.toIntOrNull() ?: 0
                        val ev = eveningInput.toIntOrNull() ?: 0
                        val mp = morningPunasInput.toIntOrNull() ?: 0
                        val ap = afternoonPunasInput.toIntOrNull() ?: 0
                        val ep = eveningPunasInput.toIntOrNull() ?: 0
                        onSave(m, a, ev, mp, ap, ep)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_save_button")
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dialog_dismiss_button")
                ) {
                    Text("Cancel", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        dismissButton = null
    )
}

@Composable
fun CustomSandhyaEditDialog(
    practice: CustomPractice,
    entry: CustomPracticeEntry,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int, Int, Int, Int) -> Unit
) {
    var morningInput by remember { mutableStateOf(entry.morningCount.toString()) }
    var afternoonInput by remember { mutableStateOf(entry.afternoonCount.toString()) }
    var eveningInput by remember { mutableStateOf(entry.eveningCount.toString()) }
    var morningPunasInput by remember { mutableStateOf(entry.morningPunasCount.toString()) }
    var afternoonPunasInput by remember { mutableStateOf(entry.afternoonPunasCount.toString()) }
    var eveningPunasInput by remember { mutableStateOf(entry.eveningPunasCount.toString()) }

    val mantraColor = getMantraColor(practice.themeColor)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        title = {
            val formattedTitleDate = remember(entry.date) {
                try {
                    val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val outputSdf = SimpleDateFormat("EEEE, d MMM yyyy", Locale.US)
                    val d = inputSdf.parse(entry.date)
                    if (d != null) outputSdf.format(d) else entry.date
                } catch (e: Exception) {
                    entry.date
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(mantraColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = mantraColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Edit Japa Counts",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.25.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedTitleDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CurrentCountSummaryBox(
                    morning = entry.morningCount,
                    afternoon = entry.afternoonCount,
                    evening = entry.eveningCount,
                    morningPunas = entry.morningPunasCount,
                    afternoonPunas = entry.afternoonPunasCount,
                    eveningPunas = entry.eveningPunasCount,
                    isPunasEnabled = practice.isPunascharanaEnabled,
                    accentColor = mantraColor
                )

                Text(
                    text = "Configure Counts below. Enter whole numbers, or -1 if the session was passed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                if (practice.isPunascharanaEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Sandhya Counts",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = mantraColor
                            )

                            OutlinedTextField(
                                value = morningInput,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) morningInput = it },
                                label = { Text("Pratah") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = mantraColor,
                                    focusedLabelColor = mantraColor,
                                    cursorColor = mantraColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.WbSunny,
                                        contentDescription = null,
                                        tint = mantraColor.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_edit_morning")
                            )

                            OutlinedTextField(
                                value = afternoonInput,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) afternoonInput = it },
                                label = { Text("Madhyahnika") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = mantraColor,
                                    focusedLabelColor = mantraColor,
                                    cursorColor = mantraColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = mantraColor.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_edit_afternoon")
                            )

                            OutlinedTextField(
                                value = eveningInput,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) eveningInput = it },
                                label = { Text("Sayam") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = mantraColor,
                                    focusedLabelColor = mantraColor,
                                    cursorColor = mantraColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.NightsStay,
                                        contentDescription = null,
                                        tint = mantraColor.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_edit_evening")
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Punascharana",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = mantraColor
                            )

                            OutlinedTextField(
                                value = morningPunasInput,
                                onValueChange = { if (it.all { c -> c.isDigit() }) morningPunasInput = it },
                                label = { Text("Pratah") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = mantraColor,
                                    focusedLabelColor = mantraColor,
                                    cursorColor = mantraColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.WbSunny,
                                        contentDescription = null,
                                        tint = mantraColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_edit_morning_punas")
                            )

                            OutlinedTextField(
                                value = afternoonPunasInput,
                                onValueChange = { if (it.all { c -> c.isDigit() }) afternoonPunasInput = it },
                                label = { Text("Madhyahnika") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = mantraColor,
                                    focusedLabelColor = mantraColor,
                                    cursorColor = mantraColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.LightMode,
                                        contentDescription = null,
                                        tint = mantraColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_edit_afternoon_punas")
                            )

                            OutlinedTextField(
                                value = eveningPunasInput,
                                onValueChange = { if (it.all { c -> c.isDigit() }) eveningPunasInput = it },
                                label = { Text("Sayam") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = mantraColor,
                                    focusedLabelColor = mantraColor,
                                    cursorColor = mantraColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.NightsStay,
                                        contentDescription = null,
                                        tint = mantraColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_edit_evening_punas")
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Sandhya Counts",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = mantraColor
                        )

                        OutlinedTextField(
                            value = morningInput,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) morningInput = it },
                            label = { Text("Morning (Pratah) Sandhya") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = mantraColor,
                                    focusedLabelColor = mantraColor,
                                    cursorColor = mantraColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = mantraColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_edit_morning")
                        )

                        OutlinedTextField(
                            value = afternoonInput,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) afternoonInput = it },
                            label = { Text("Noon (Madhyahnika) Sandhya") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = mantraColor,
                                    focusedLabelColor = mantraColor,
                                    cursorColor = mantraColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = mantraColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_edit_afternoon")
                        )

                        OutlinedTextField(
                            value = eveningInput,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) eveningInput = it },
                            label = { Text("Evening (Sayam) Sandhya") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = mantraColor,
                                    focusedLabelColor = mantraColor,
                                    cursorColor = mantraColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = mantraColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_edit_evening")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val m = morningInput.toIntOrNull() ?: 0
                        val a = afternoonInput.toIntOrNull() ?: 0
                        val ev = eveningInput.toIntOrNull() ?: 0
                        val mp = morningPunasInput.toIntOrNull() ?: 0
                        val ap = afternoonPunasInput.toIntOrNull() ?: 0
                        val ep = eveningPunasInput.toIntOrNull() ?: 0
                        onSave(m, a, ev, mp, ap, ep)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = mantraColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_save_button")
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dialog_dismiss_button")
                ) {
                    Text("Cancel", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        dismissButton = null
    )
}

@Composable
fun CustomCountEditDialog(
    practice: CustomPractice,
    entry: CustomPracticeEntry,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var countInput by remember { mutableStateOf(entry.count.toString()) }
    val mantraColor = getMantraColor(practice.themeColor)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        title = {
            val formattedTitleDate = remember(entry.date) {
                try {
                    val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val outputSdf = SimpleDateFormat("EEEE, d MMM yyyy", Locale.US)
                    val d = inputSdf.parse(entry.date)
                    if (d != null) outputSdf.format(d) else entry.date
                } catch (e: Exception) {
                    entry.date
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(mantraColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = mantraColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Edit ${practice.name}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.25.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedTitleDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Update the cumulative chants count logged for this date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                
                OutlinedTextField(
                    value = countInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) countInput = it },
                    label = { Text("Total Cumulative Count") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = mantraColor,
                        focusedLabelColor = mantraColor,
                        cursorColor = mantraColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AllInclusive,
                            contentDescription = null,
                            tint = mantraColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        onSave(countInput.toIntOrNull() ?: 0)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = mantraColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Cancel", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        dismissButton = null
    )
}

data class MonthAnalytics(
    val monthStr: String,
    val monthName: String,
    val totalCount: Long,
    val daysLogged: Int,
    val averageCount: Double,
    val maxCount: Int,
    val minCount: Int,
    val openValue: Double,
    val closeValue: Double
)

@Composable
fun MonthlyOverviewCard(
    selectedMantraId: Long?,
    allPractices: List<CustomPractice>,
    entries: List<JapaEntry>,
    allCustomEntries: List<CustomPracticeEntry>,
    gayatriName: String,
    gayatriColor: Color,
    modifier: Modifier = Modifier
) {
    if (selectedMantraId == null) {
        // Combined state: show an elegant, simple hint card
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
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
        return
    }

    // A specific mantra is selected, compute monthly analytics!
    val activeMantraName = remember(selectedMantraId, allPractices, gayatriName) {
        if (selectedMantraId == GAYATRI_PRACTICE_ID) gayatriName else allPractices.find { it.id == selectedMantraId }?.name ?: "Mantra"
    }

    val activeColor = remember(selectedMantraId, allPractices, gayatriColor) {
        if (selectedMantraId == GAYATRI_PRACTICE_ID) gayatriColor else getMantraColor(allPractices.find { it.id == selectedMantraId }?.themeColor ?: "teal")
    }

    val monthAnalyticsList = remember(selectedMantraId, entries, allCustomEntries) {
        val dailyTotals = mutableListOf<Pair<String, Int>>()
        if (selectedMantraId == GAYATRI_PRACTICE_ID) {
            entries.forEach { entry ->
                if (entry.dailyTotal > 0) {
                    dailyTotals.add(entry.date to entry.dailyTotal)
                }
            }
        } else {
            allCustomEntries.forEach { entry ->
                if (entry.practiceId == selectedMantraId && entry.dailyTotal > 0) {
                    dailyTotals.add(entry.date to entry.dailyTotal)
                }
            }
        }

        if (dailyTotals.isEmpty()) return@remember emptyList<MonthAnalytics>()

        val grouped = dailyTotals.groupBy { it.first.substring(0, 7) }
        val inputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

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
                openValue = daysInMonth.toDouble(), // Reuse openValue to hold daysInMonth for simple overview
                closeValue = 0.0
            )
        }.sortedByDescending { it.monthStr }
    }

    if (monthAnalyticsList.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = activeColor.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "No Data",
                    tint = activeColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No log data available for $activeMantraName yet. Start logging to view monthly analytics!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    val latestMonth = monthAnalyticsList.first()
    val daysInMonth = latestMonth.openValue.toInt()
    val percentActive = if (daysInMonth > 0) (latestMonth.daysLogged.toFloat() / daysInMonth) else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Monthly Overview",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = latestMonth.monthName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = activeColor)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            text = "Average Daily Japa",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = String.format("%,d", latestMonth.averageCount.toInt()),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total This Month",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = String.format("%,d", latestMonth.totalCount),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Practiced Days",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${latestMonth.daysLogged} / $daysInMonth",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    LinearProgressIndicator(
                        progress = { percentActive },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = activeColor,
                        trackColor = activeColor.copy(alpha = 0.15f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${(percentActive * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // 12-Month Trend bar chart displayed directly on this card
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "12-Month Trend",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                val last12 = monthAnalyticsList.take(12).reversed()
                HistoryBarTrendChart(last12, activeColor)
            }
        }
    }
}

@Composable
fun HistoryBarTrendChart(last12Months: List<MonthAnalytics>, activeColor: Color) {
    if (last12Months.isEmpty()) return
    val maxTotal = last12Months.maxOfOrNull { it.totalCount }?.toFloat() ?: 1f
    val safeMax = if (maxTotal > 0f) maxTotal else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        last12Months.forEach { month ->
            val heightFraction = (month.totalCount.toFloat() / safeMax).coerceIn(0.05f, 1f)
            
            val displayMonth = remember(month.monthName) {
                if (month.monthName.contains(" ")) {
                    month.monthName.substringBefore(" ").take(3)
                } else if (month.monthName.contains("-")) {
                    val parts = month.monthName.split("-")
                    if (parts.size == 2) {
                        val m = parts[1].toIntOrNull() ?: 1
                        val dfs = java.text.DateFormatSymbols.getInstance(Locale.US)
                        dfs.shortMonths.getOrNull(m - 1) ?: month.monthName.take(3)
                    } else {
                        month.monthName.take(3)
                    }
                } else {
                    month.monthName.take(3)
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Large/Clear compact value on top of bar
                Text(
                    text = formatCountCompact(month.totalCount.toInt()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = activeColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                // Rounded Sleek Bar with Back Track (Thicker design)
                Box(
                    modifier = Modifier
                        .height(90.dp)
                        .width(20.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(activeColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(heightFraction)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(activeColor)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Month Abbreviation
                Text(
                    text = displayMonth,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatCountCompact(count: Int): String {
    return when {
        count >= 1_000_000 -> {
            val millions = count / 1_000_000f
            if (millions % 1 == 0f) "${millions.toInt()}M" else String.format(Locale.US, "%.1fM", millions)
        }
        count >= 1000 -> {
            val thousands = count / 1000f
            if (thousands % 1 == 0f) "${thousands.toInt()}k" else String.format(Locale.US, "%.1fk", thousands)
        }
        else -> count.toString()
    }
}

@Composable
fun MonthlyCompactStatsRow(
    monthName: String,
    averageCount: Double,
    totalCount: Long,
    daysLogged: Int,
    daysInMonth: Int,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Month & Days
            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = monthName.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = accentColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$daysLogged / $daysInMonth Days",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Safe robust custom vertical divider
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            // Total Count
            Column(
                modifier = Modifier.weight(1.4f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL COUNT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                AutoResizedText(
                    text = String.format(Locale.US, "%,d", totalCount),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    minFontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Safe robust custom vertical divider
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            // Average Count
            Column(
                modifier = Modifier.weight(1.4f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DAILY AVERAGE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                AutoResizedText(
                    text = String.format(Locale.US, "%,d", averageCount.toInt()),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    minFontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MonthlyTrendSection(
    monthAnalyticsList: List<MonthAnalytics>,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "12-MONTH TREND",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = accentColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            val last12 = monthAnalyticsList.take(12).reversed()
            HistoryBarTrendChart(last12, accentColor)
        }
    }
}

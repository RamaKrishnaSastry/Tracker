package com.example.ui.components

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
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomPractice
import com.example.data.CustomPracticeEntry
import com.example.data.JapaEntry
import com.example.data.PlaceCategory
import com.example.ui.GAYATRI_PRACTICE_ID
import com.example.ui.theme.getMantraColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DailySummaryDialog(
    date: String,
    gayatriName: String = "Gayatri Mantra",
    gayatriColor: Color = MaterialTheme.colorScheme.primary,
    gayatriEntry: JapaEntry? = null,
    gEntry: JapaEntry? = gayatriEntry,
    allPractices: List<CustomPractice> = emptyList(),
    customEntries: Map<Long, com.example.data.CustomPracticeEntry> = emptyMap(),
    customEntriesMap: Map<Long, com.example.data.CustomPracticeEntry> = customEntries,
    filterMantraId: Long? = null,
    filterPlaceCategory: String? = null,
    onEditLocation: (() -> Unit)? = null,
    onEditGayatri: ((JapaEntry) -> Unit)? = null,
    onEditCustom: ((CustomPracticeEntry, CustomPractice) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val actualGEntry = gayatriEntry ?: gEntry
    val actualCustomMap = if (customEntries.isNotEmpty()) customEntries else customEntriesMap

    var formattedDate = date
    try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
        if (parsed != null) {
            formattedDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(parsed)
        }
    } catch (e: Exception) {
        // Fallback
    }

    // Determine eligible records satisfying BOTH filters
    val isGayatriLocMatch = actualGEntry != null && (filterPlaceCategory == null || (actualGEntry.placeCategory ?: "Home").equals(filterPlaceCategory, ignoreCase = true))
    val showGayatri = (filterMantraId == null || filterMantraId == com.example.ui.GAYATRI_PRACTICE_ID) && isGayatriLocMatch && (actualGEntry?.dailyTotal ?: 0) > 0

    val eligibleCustoms = allPractices.filter { practice ->
        if (practice.id == com.example.ui.GAYATRI_PRACTICE_ID) return@filter false
        if (filterMantraId != null && filterMantraId != practice.id) return@filter false
        val entry = actualCustomMap[practice.id] ?: return@filter false
        val matchesLoc = filterPlaceCategory == null || (entry.placeCategory ?: "Home").equals(filterPlaceCategory, ignoreCase = true)
        matchesLoc && entry.dailyTotal > 0
    }

    val hasAnyMatchingRecords = showGayatri || eligibleCustoms.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Place Badge
                val dayPlaceName = actualGEntry?.placeName ?: actualCustomMap.values.firstOrNull { !it.placeName.isNullOrBlank() }?.placeName ?: "Home"
                val dayPlaceCat = actualGEntry?.placeCategory ?: actualCustomMap.values.firstOrNull { !it.placeCategory.isNullOrBlank() }?.placeCategory ?: "Home"
                val dayLat = actualGEntry?.latitude ?: actualCustomMap.values.firstOrNull()?.latitude
                val dayLon = actualGEntry?.longitude ?: actualCustomMap.values.firstOrNull()?.longitude

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlaceBadge(
                        placeName = dayPlaceName,
                        placeCategory = dayPlaceCat,
                        latitude = dayLat,
                        longitude = dayLon
                    )

                    if (onEditLocation != null) {
                        TextButton(
                            onClick = onEditLocation,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Edit Place", fontSize = 11.sp)
                        }
                    }
                }

                if (!hasAnyMatchingRecords) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No practice recorded on this date matching the active filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Gayatri Entry if matching
                    if (showGayatri && actualGEntry != null) {
                        val gCount = actualGEntry.dailyTotal
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (onEditGayatri != null) Modifier.clickable { onEditGayatri(actualGEntry) } else Modifier),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = gayatriColor.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, gayatriColor.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = gayatriName,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = gayatriColor
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = String.format(Locale.US, "%,d", gCount),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = gayatriColor
                                        )
                                        if (onEditGayatri != null) {
                                            Text("(Edit)", fontSize = 10.sp, color = gayatriColor.copy(alpha = 0.7f))
                                        }
                                    }
                                }

                                if (actualGEntry.pratahSandhyaCount > 0 || actualGEntry.madhyahnikaSandhyaCount > 0 || actualGEntry.sayamSandhyaCount > 0) {
                                    Text(
                                        text = "Pratah: ${actualGEntry.pratahSandhyaCount}  •  Madhyahnika: ${actualGEntry.madhyahnikaSandhyaCount}  •  Sayam: ${actualGEntry.sayamSandhyaCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // Custom Practices matching filters
                    if (eligibleCustoms.isNotEmpty()) {
                        Text(
                            text = "PRACTICES",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        eligibleCustoms.forEach { practice ->
                            val entry = actualCustomMap[practice.id]
                            val cCount = entry?.dailyTotal ?: 0
                            val pColor = getMantraColor(practice.themeColor)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (onEditCustom != null && entry != null) Modifier.clickable { onEditCustom(entry, practice) } else Modifier),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(pColor)
                                        )
                                        Text(
                                            text = practice.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = String.format(Locale.US, "%,d", cCount),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = if (cCount > 0) pColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                        if (onEditCustom != null && entry != null) {
                                            Text("(Edit)", fontSize = 10.sp, color = pColor.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun InteractiveMonthlyCalendar(
    year: Int,
    month: Int,
    selectedMantraId: Long? = null,
    gayatriEntriesMap: Map<String, JapaEntry> = emptyMap(),
    customEntriesMap: Map<Pair<Long, String>, com.example.data.CustomPracticeEntry> = emptyMap(),
    combinedDailyTotalsMap: Map<String, Int> = emptyMap(),
    dailyTotalsMap: Map<String, Int> = combinedDailyTotalsMap,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = activeColor,
    allPractices: List<CustomPractice> = emptyList(),
    onDateClick: (String) -> Unit = {},
    onDayClick: (String) -> Unit = onDateClick,
    isBulkEditMode: Boolean = false,
    selectedBulkDates: Set<String> = emptySet(),
    onToggleDateSelection: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon ...

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val actualColor = if (activeColor != MaterialTheme.colorScheme.primary) activeColor else accentColor

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        var dayCounter = 1
        val totalSlotCount = (firstDayOfWeek - 1) + daysInMonth
        val rows = (totalSlotCount + 6) / 7

        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (c in 1..7) {
                    val slotIndex = r * 7 + c
                    if (slotIndex < firstDayOfWeek || dayCounter > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val currentDayNum = dayCounter
                        val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month, currentDayNum)
                        val total = if (selectedMantraId == null) {
                            dailyTotalsMap[dateStr] ?: combinedDailyTotalsMap[dateStr] ?: 0
                        } else if (selectedMantraId == com.example.ui.GAYATRI_PRACTICE_ID) {
                            gayatriEntriesMap[dateStr]?.dailyTotal ?: 0
                        } else {
                            customEntriesMap[selectedMantraId to dateStr]?.dailyTotal ?: 0
                        }
                        val isToday = dateStr == todayStr
                        val isSelectedForBulk = selectedBulkDates.contains(dateStr)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelectedForBulk -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                        total > 0 -> actualColor.copy(alpha = (0.2f + (total.toFloat() / 500f).coerceAtMost(0.8f)))
                                        isToday -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isSelectedForBulk) 2.dp else if (isToday) 1.5.dp else 0.dp,
                                    color = if (isSelectedForBulk) MaterialTheme.colorScheme.primary else if (isToday) actualColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (isBulkEditMode) {
                                        onToggleDateSelection(dateStr)
                                    } else {
                                        onDayClick(dateStr)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentDayNum.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (total > 0 || isToday || isSelectedForBulk) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelectedForBulk) MaterialTheme.colorScheme.primary else if (total > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                if (total > 0) {
                                    Text(
                                        text = if (total >= 1000) "${total / 1000}k" else "$total",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = actualColor
                                    )
                                }
                            }
                        }
                        dayCounter++
                    }
                }
            }
        }
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
    onDateClick: (String) -> Unit,
    isBulkEditMode: Boolean = false,
    selectedBulkDates: Set<String> = emptySet(),
    onToggleDateSelection: (String) -> Unit = {},
    onToggleBulkEditMode: () -> Unit = {},
    onSelectAllMonth: () -> Unit = {},
    onClearBulkSelection: () -> Unit = {},
    onOpenBulkEditDialog: () -> Unit = {}
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
            // Month switcher header & Bulk Edit button
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

            Spacer(modifier = Modifier.height(8.dp))

            // Bulk Edit Mode Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onToggleBulkEditMode,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isBulkEditMode) Icons.Default.Close else Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBulkEditMode) "Exit Bulk Edit" else "Bulk Edit Locations", fontSize = 12.sp)
                }

                if (isBulkEditMode) {
                    Text(
                        text = "${selectedBulkDates.size} selected",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isBulkEditMode) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSelectAllMonth,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Select Month", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onClearBulkSelection,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Clear", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onOpenBulkEditDialog,
                        enabled = selectedBulkDates.isNotEmpty(),
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Edit Location", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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
                onDateClick = onDateClick,
                isBulkEditMode = isBulkEditMode,
                selectedBulkDates = selectedBulkDates,
                onToggleDateSelection = onToggleDateSelection
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
fun CombinedHistoryBanner(
    combinedLifetimeTotal: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("combined_history_banner_tile"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
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
                text = String.format(Locale.US, "%,d", combinedLifetimeTotal),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                minFontSize = 11.sp
            )
        }
    }
}

@Composable
fun MantraSelectionGrid(
    selectedMantraId: Long?,
    gayatriName: String,
    gayatriColor: Color,
    gayatriLifetimeTotal: Long,
    allPractices: List<CustomPractice>,
    practiceTotals: Map<Long, Int>,
    onSelectMantra: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
                    .clickable { onSelectMantra(GAYATRI_PRACTICE_ID) }
                    .testTag("block_gayatri_japa"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGayatriSelected) gayatriColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
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
                        text = String.format(Locale.US, "%,d", gayatriLifetimeTotal),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
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
                    .clickable { onSelectMantra(practice.id) }
                        .testTag("block_mantra_${practice.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
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
                            text = String.format(Locale.US, "%,d", total),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
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
                        Box(modifier = Modifier.weight(1f)) {}
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeLocationsBreakdownSection(
    locationStats: Pair<Map<String, Long>, Long>,
    selectedPlaceCategoryFilter: String?,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (locationStats.first.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
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
                        text = "Location Analysis",
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
                        .clickable { onCategoryClick(category.id) },
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
                                fontFamily = FontFamily.Monospace,
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

@Composable
fun PlaceCategoryFilterRow(
    selectedPlaceCategoryFilter: String?,
    onSelectFilter: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                onClick = { onSelectFilter(null) },
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
                        onSelectFilter(if (isSelected) null else category.id)
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



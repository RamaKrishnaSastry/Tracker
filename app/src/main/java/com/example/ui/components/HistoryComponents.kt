package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomPractice
import com.example.data.JapaEntry
import com.example.data.PlaceCategory
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
    onEditLocation: (() -> Unit)? = null,
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
                    .heightIn(max = 360.dp)
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

                // Gayatri Entry
                val gCount = actualGEntry?.dailyTotal ?: 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            Text(
                                text = String.format(Locale.US, "%,d", gCount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = gayatriColor
                            )
                        }

                        if (actualGEntry != null && (actualGEntry.pratahSandhyaCount > 0 || actualGEntry.madhyahnikaSandhyaCount > 0 || actualGEntry.sayamSandhyaCount > 0)) {
                            Text(
                                text = "Pratah: ${actualGEntry.pratahSandhyaCount}  •  Madhyahnika: ${actualGEntry.madhyahnikaSandhyaCount}  •  Sayam: ${actualGEntry.sayamSandhyaCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Custom Practices
                val activeCustoms = allPractices.filter { it.id != com.example.ui.GAYATRI_PRACTICE_ID }
                if (activeCustoms.isNotEmpty()) {
                    Text(
                        text = "CUSTOM PRACTICES",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    activeCustoms.forEach { practice ->
                        val entry = actualCustomMap[practice.id]
                        val cCount = entry?.dailyTotal ?: 0
                        val pColor = getMantraColor(practice.themeColor)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
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

                                Text(
                                    text = String.format(Locale.US, "%,d", cCount),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (cCount > 0) pColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
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

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        total > 0 -> actualColor.copy(alpha = (0.2f + (total.toFloat() / 500f).coerceAtMost(0.8f)))
                                        isToday -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday) 1.5.dp else 0.dp,
                                    color = if (isToday) actualColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onDayClick(dateStr) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentDayNum.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (total > 0 || isToday) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (total > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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

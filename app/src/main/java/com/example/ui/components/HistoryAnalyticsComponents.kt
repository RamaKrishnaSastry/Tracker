package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.getMantraColor
import java.text.SimpleDateFormat
import java.util.*

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

fun formatCountCompact(count: Int): String {
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
                openValue = daysInMonth.toDouble(),
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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                                fontFamily = FontFamily.Monospace
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
                                fontFamily = FontFamily.Monospace
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

            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

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
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    minFontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

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
                        fontFamily = FontFamily.Monospace
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

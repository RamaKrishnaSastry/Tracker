package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: JapaViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val todayEntry by viewModel.todayEntry.collectAsState()
    val isPunascharanaEnabled by viewModel.isPunascharanaEnabled.collectAsState()
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
            if (entry.pratahSandhyaCount > 0) count++
            if (entry.madhyahnikaSandhyaCount > 0) count++
            if (entry.sayamSandhyaCount > 0) count++
        }
        count
    }

    val syncState by viewModel.repository.syncState.collectAsState()
    val lastSyncTimeMs = viewModel.prefs.getLastSyncTimeMs()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gayatri Japa",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Title, Greeting and Date
            Text(
                text = "$timeGreeting, Praying Soul",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("today_screen_greeting")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = todayLongStr,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("today_screen_date")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtle progress indicator: "X/3 Sandhyas completed"
            Text(
                text = "$completedSandhyas/3 Sandhyas completed",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("today_sandhyas_progress")
            )

            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .background(
                        color = when (syncState) {
                            SyncState.SYNCED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                    SyncState.SYNCING -> Triple(Icons.Default.Sync, MaterialTheme.colorScheme.primary, "Syncing...")
                    SyncState.SYNCED -> Triple(Icons.Default.CloudDone, MaterialTheme.colorScheme.primary, lastSyncText)
                    SyncState.ERROR -> Triple(Icons.Default.Error, MaterialTheme.colorScheme.error, "Sync failed. Changes saved locally.")
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

            Spacer(modifier = Modifier.height(16.dp))

             // Hero Statistics Panel - Styled cleanly based on Geometric Balance
             Card(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(bottom = 16.dp),
                 colors = CardDefaults.cardColors(
                     containerColor = MaterialTheme.colorScheme.primaryContainer
                 ),
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
                                 color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                 textAlign = TextAlign.Center
                             )
                             Spacer(modifier = Modifier.height(8.dp))
                             AnimatedCounterText(
                                 value = stats.todayTotal,
                                 style = MaterialTheme.typography.titleLarge.copy(
                                     fontWeight = FontWeight.Bold,
                                     fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                 ),
                                 color = MaterialTheme.colorScheme.onPrimaryContainer,
                                 modifier = Modifier.testTag("stats_today")
                             )
                         }

                         // Symmetrical divider 1
                         Box(
                             modifier = Modifier
                                 .width(1.dp)
                                 .height(40.dp)
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
                                     letterSpacing = 0.5.sp
                                 ),
                                 color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                 textAlign = TextAlign.Center
                             )
                             Spacer(modifier = Modifier.height(8.dp))
                             AnimatedCounterLongText(
                                 value = stats.lifetimeTotal,
                                 style = MaterialTheme.typography.titleLarge.copy(
                                     fontWeight = FontWeight.Bold,
                                     fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                 ),
                                 color = MaterialTheme.colorScheme.onPrimaryContainer,
                                 modifier = Modifier.testTag("stats_lifetime_total")
                             )
                         }

                         // Symmetrical divider 2
                         Box(
                             modifier = Modifier
                                 .width(1.dp)
                                 .height(40.dp)
                                 .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f))
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
                                 color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                 textAlign = TextAlign.Center
                             )
                             Spacer(modifier = Modifier.height(8.dp))
                             AnimatedCounterLongText(
                                 value = stats.lifetimePunascharanaTotal,
                                 style = MaterialTheme.typography.titleLarge.copy(
                                     fontWeight = FontWeight.Bold,
                                     fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                 ),
                                 color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    onCountUpdated = { newCount ->
                        viewModel.updateCounts(
                            date = entry.date,
                            morning = newCount,
                            afternoon = entry.madhyahnikaSandhyaCount,
                            evening = entry.sayamSandhyaCount
                        )
                    },
                    onPunasUpdated = { newPunas ->
                        viewModel.updateCounts(
                            date = entry.date,
                            morning = entry.pratahSandhyaCount,
                            afternoon = entry.madhyahnikaSandhyaCount,
                            evening = entry.sayamSandhyaCount,
                            pratahPunas = newPunas
                        )
                    },
                    tagPrefix = "morning",
                    isPunascharanaEnabled = isPunascharanaEnabled
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
                    onCountUpdated = { newCount ->
                        viewModel.updateCounts(
                            date = entry.date,
                            morning = entry.pratahSandhyaCount,
                            afternoon = newCount,
                            evening = entry.sayamSandhyaCount
                        )
                    },
                    onPunasUpdated = { newPunas ->
                        viewModel.updateCounts(
                            date = entry.date,
                            morning = entry.pratahSandhyaCount,
                            afternoon = entry.madhyahnikaSandhyaCount,
                            evening = entry.sayamSandhyaCount,
                            madhyahnikaPunas = newPunas
                        )
                    },
                    tagPrefix = "afternoon",
                    isPunascharanaEnabled = isPunascharanaEnabled
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
                    onCountUpdated = { newCount ->
                        viewModel.updateCounts(
                            date = entry.date,
                            morning = entry.pratahSandhyaCount,
                            afternoon = entry.madhyahnikaSandhyaCount,
                            evening = newCount
                        )
                    },
                    onPunasUpdated = { newPunas ->
                        viewModel.updateCounts(
                            date = entry.date,
                            morning = entry.pratahSandhyaCount,
                            afternoon = entry.madhyahnikaSandhyaCount,
                            evening = entry.sayamSandhyaCount,
                            sayamPunas = newPunas
                        )
                    },
                    tagPrefix = "evening",
                    isPunascharanaEnabled = isPunascharanaEnabled
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
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
    onCountUpdated: (Int) -> Unit,
    onPunasUpdated: (Int) -> Unit,
    tagPrefix: String,
    isPunascharanaEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Keep local buffer of raw text inputs for precise fluid editing
    var textInput by remember(count) { mutableStateOf(count.toString()) }
    var isPunasExpanded by remember { mutableStateOf(punasCount > 0) }

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
                    AnimatedCounterText(
                        value = count,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isPunascharanaEnabled && punasCount > 0) {
                        Text(
                            text = " (+$punasCount)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
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
                        if (count > 0) {
                            onCountUpdated((count - 1).coerceAtLeast(0))
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
                    enabled = count > 0
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrement",
                        tint = if (count > 0) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                }

                // Direct Numeric Manual Underlined Entry Text Field
                TextField(
                    value = textInput,
                    onValueChange = { input ->
                        if (input.isEmpty()) {
                            textInput = ""
                            onCountUpdated(0)
                        } else if (input.all { it.isDigit() }) {
                            textInput = input
                            val parsed = input.toIntOrNull() ?: 0
                            onCountUpdated(parsed)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .testTag("${tagPrefix}_input"),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
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
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                )

                // Plus Target
                IconButton(
                    onClick = {
                        onCountUpdated(count + 1)
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

             // Quick-add Increment Chips
             Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.spacedBy(8.dp)
             ) {
                 val increments = listOf(10, 24, 28, 54, 108)
                 increments.forEach { inc ->
                     val isSpecial = inc == 108
                     val containerColor = MaterialTheme.colorScheme.secondaryContainer
                     val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                     val border = if (isSpecial) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
 
                     Button(
                         onClick = {
                             onCountUpdated(count + inc)
                         },
                         modifier = Modifier
                             .weight(1f)
                             .height(40.dp)
                             .testTag("${tagPrefix}_add_$inc"),
                         shape = RoundedCornerShape(20.dp),
                         border = border,
                         colors = ButtonDefaults.buttonColors(
                             containerColor = containerColor,
                             contentColor = contentColor
                         ),
                         contentPadding = PaddingValues(0.dp)
                     ) {
                         Text(
                             text = "+$inc",
                             style = MaterialTheme.typography.bodySmall.copy(
                                 fontWeight = if (isSpecial) FontWeight.Bold else FontWeight.Medium,
                                 fontSize = 11.sp
                             )
                         )
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
                            color = if (isPunasExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                        punasCount = punasCount,
                        onPunasUpdated = onPunasUpdated,
                        tagPrefix = "${tagPrefix}_punas"
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
    tagPrefix: String
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
                    focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val increments = listOf(10, 24, 28, 54, 108)
            increments.forEach { inc ->
                val isSpecial = inc == 108
                val containerColor = MaterialTheme.colorScheme.secondaryContainer
                val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                val border = if (isSpecial) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null

                Button(
                    onClick = {
                        onPunasUpdated(punasCount + inc)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("${tagPrefix}_add_$inc"),
                    shape = RoundedCornerShape(16.dp),
                    border = border,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "+$inc",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSpecial) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp
                        )
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

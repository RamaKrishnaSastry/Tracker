package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.JapaEntry
import com.example.data.CustomPractice
import com.example.data.CustomPracticeEntry
import com.example.ui.JapaViewModel
import com.example.ui.GAYATRI_PRACTICE_ID
import com.example.ui.theme.getMantraColor
import java.text.SimpleDateFormat
import java.util.*

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
    val stats by viewModel.getStatisticsFlow().collectAsState(
        initial = JapaViewModel.Stats(0, 0, viewModel.prefs.getInitialLifetimeCount(), 0, 0)
    )

    val gayatriColorName by viewModel.gayatriColor.collectAsState()
    val gayatriColor = getMantraColor(gayatriColorName)
    val gayatriName by viewModel.gayatriName.collectAsState()

    // Selection filter: null means "Combined History"
    var selectedMantraId by remember { mutableStateOf<Long?>(null) }

    // Dialog state
    var selectedEntryToEdit by remember { mutableStateOf<JapaEntry?>(null) }
    var selectedCustomEntryToEdit by remember { mutableStateOf<Pair<CustomPracticeEntry, CustomPractice>?>(null) }

    val practiceTotals by viewModel.allPracticeTotals.collectAsState(initial = emptyMap())

    // Combined/Dynamic lifetime grand sum
    val combinedLifetimeTotal = remember(stats, allPractices, practiceTotals) {
        val customTotal = allPractices.sumOf { practice ->
            practice.initialLifetimeCount + (practiceTotals[practice.id] ?: 0)
        }
        stats.lifetimeTotal + customTotal
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

    // Filter items based on selected selection AND search query
    val filteredItems = remember(combinedList, selectedMantraId, searchQuery) {
        combinedList.filter { item ->
            // Match selected mantra ID
            val matchesFilter = when {
                selectedMantraId == null -> true // Combined
                selectedMantraId == GAYATRI_PRACTICE_ID -> item is UnifiedHistoryItem.GayatriItem
                else -> item is UnifiedHistoryItem.CustomItem && item.entry.practiceId == selectedMantraId
            }

            // Match search query (date)
            val matchesQuery = if (searchQuery.trim().isEmpty()) {
                true
            } else {
                item.dateString.contains(searchQuery, ignoreCase = true)
            }

            matchesFilter && matchesQuery
        }
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
                    text = "Select a mantra card to filter, view statistics, or update logs.",
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
                        Text(
                            text = String.format("%,d", combinedLifetimeTotal),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.primary
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
                            .height(86.dp)
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
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = gayatriName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isGayatriSelected) gayatriColor else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = String.format("%,d", stats.lifetimeTotal),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = gayatriColor
                            )
                        }
                    }
                }

                // Custom blocks
                allPractices.forEach { practice ->
                    val isSelected = selectedMantraId == practice.id
                    val color = getMantraColor(practice.themeColor)
                    val total = practice.initialLifetimeCount + (practiceTotals[practice.id] ?: 0)
                    allBlocks.add {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(86.dp)
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
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = practice.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = String.format("%,d", total),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = color
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

            // Search Bar Field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search dates (e.g. 2026-06)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input"),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search dates")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                )
            }

            // HISTORY ENTRIES LIST
            item {
                Text(
                    text = "LOGS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            if (filteredItems.isEmpty()) {
                item {
                    EmptyHistoryState()
                }
            } else {
                items(filteredItems, key = {
                    when (it) {
                        is UnifiedHistoryItem.GayatriItem -> "G_${it.entry.date}"
                        is UnifiedHistoryItem.CustomItem -> "C_${it.entry.practiceId}_${it.entry.date}"
                    }
                }) { unifiedItem ->
                    when (unifiedItem) {
                        is UnifiedHistoryItem.GayatriItem -> {
                            HistoryItemRow(
                                entry = unifiedItem.entry,
                                gayatriName = gayatriName,
                                gayatriColor = gayatriColor,
                                onClick = { selectedEntryToEdit = unifiedItem.entry }
                            )
                        }
                        is UnifiedHistoryItem.CustomItem -> {
                            CustomHistoryItemRow(
                                entry = unifiedItem.entry,
                                practice = unifiedItem.practice,
                                onClick = { selectedCustomEntryToEdit = unifiedItem.entry to unifiedItem.practice }
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
        HistoricalEditDialog(
            entry = entry,
            accentColor = gayatriColor,
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
fun HistoricalEditDialog(
    entry: JapaEntry,
    accentColor: Color,
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
        title = {
            Text(
                "Edit Japa & Punascharana: ${entry.date}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Editing counts for a historical date. Enter whole numbers for Sandhya and Punascharana.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Sandhyavandanam Counts (-1 for Passed)", style = MaterialTheme.typography.titleSmall, color = accentColor)
                OutlinedTextField(
                    value = morningInput,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) morningInput = it },
                    label = { Text("Morning (Pratah) Sandhya") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_edit_morning")
                )

                OutlinedTextField(
                    value = afternoonInput,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) afternoonInput = it },
                    label = { Text("Noon (Madhyahnika) Sandhya") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_edit_afternoon")
                )

                OutlinedTextField(
                    value = eveningInput,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) eveningInput = it },
                    label = { Text("Evening (Sayam) Sandhya") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_edit_evening")
                )

                Text("Additional Japa (Punascharana)", style = MaterialTheme.typography.titleSmall, color = accentColor)
                OutlinedTextField(
                    value = morningPunasInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) morningPunasInput = it },
                    label = { Text("Pratah Punascharana") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_edit_morning_punas")
                )

                OutlinedTextField(
                    value = afternoonPunasInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) afternoonPunasInput = it },
                    label = { Text("Madhyahnika Punascharana") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_edit_afternoon_punas")
                )

                OutlinedTextField(
                    value = eveningPunasInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) eveningPunasInput = it },
                    label = { Text("Sayam Punascharana") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_edit_evening_punas")
                )
            }
        },
        confirmButton = {
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
                modifier = Modifier.testTag("dialog_save_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_dismiss_button")
            ) {
                Text("Cancel")
            }
        }
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
        title = {
            Text(
                "Edit ${practice.name}: ${entry.date}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Configure individual Sandhya and additional japa counts below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Sandhya Counts", style = MaterialTheme.typography.titleSmall, color = mantraColor)
                OutlinedTextField(
                    value = morningInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) morningInput = it },
                    label = { Text("Morning (Pratah) Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = afternoonInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) afternoonInput = it },
                    label = { Text("Noon (Madhyahnika) Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = eveningInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) eveningInput = it },
                    label = { Text("Evening (Sayam) Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Additional Japa (Punascharana)", style = MaterialTheme.typography.titleSmall, color = mantraColor)
                OutlinedTextField(
                    value = morningPunasInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) morningPunasInput = it },
                    label = { Text("Pratah Punascharana") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = afternoonPunasInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) afternoonPunasInput = it },
                    label = { Text("Madhyahnika Punascharana") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = eveningPunasInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) eveningPunasInput = it },
                    label = { Text("Sayam Punascharana") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
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
                colors = ButtonDefaults.buttonColors(containerColor = mantraColor)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
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
        title = {
            Text(
                "Edit ${practice.name}: ${entry.date}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Update total cumulative chants for this date.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = countInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) countInput = it },
                    label = { Text("Total Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(countInput.toIntOrNull() ?: 0)
                },
                colors = ButtonDefaults.buttonColors(containerColor = mantraColor)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

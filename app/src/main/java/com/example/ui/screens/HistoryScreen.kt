package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.ui.JapaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: JapaViewModel,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.allEntries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val stats by viewModel.getStatisticsFlow().collectAsState(
        initial = JapaViewModel.Stats(0, 0, viewModel.prefs.getInitialLifetimeCount(), 0, 0)
    )

    var selectedEntryToEdit by remember { mutableStateOf<JapaEntry?>(null) }

    // Filtered entries strictly checking match with search query
    val filteredEntries = remember(entries, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            entries
        } else {
            entries.filter { entry ->
                entry.date.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "History & Records",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "View and modify previous Gayatri sandhyavandanam entries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar Field
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
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LIFETIME GRAND TOTAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stats.lifetimeTotal.toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("history_lifetime_total")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "LIFETIME SANDHYA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = stats.lifetimeSandhyaTotal.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "LIFETIME PUNASCHARANA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = stats.lifetimePunascharanaTotal.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (filteredEntries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEntries, key = { it.date }) { entry ->
                        HistoryItemRow(
                            entry = entry,
                            onClick = { selectedEntryToEdit = entry }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Modal Edit Dialog popup
    selectedEntryToEdit?.let { entry ->
        HistoricalEditDialog(
            entry = entry,
            onDismiss = { selectedEntryToEdit = null },
            onSave = { morning, afternoon, evening, mornPunas, noonPunas, evePunas ->
                viewModel.updateCounts(entry.date, morning, afternoon, evening, mornPunas, noonPunas, evePunas)
                selectedEntryToEdit = null
            }
        )
    }
}

@Composable
fun HistoryItemRow(
    entry: JapaEntry,
    onClick: () -> Unit
) {
    // Elegant formatted date string (e.g. Jun 19, 2026)
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
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Morn: ${entry.pratahSandhyaCount}" + if (entry.pratahPunascharanaCount > 0) "+${entry.pratahPunascharanaCount}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Noon: ${entry.madhyahnikaSandhyaCount}" + if (entry.madhyahnikaPunascharanaCount > 0) "+${entry.madhyahnikaPunascharanaCount}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Eve: ${entry.sayamSandhyaCount}" + if (entry.sayamPunascharanaCount > 0) "+${entry.sayamPunascharanaCount}" else "",
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
                    color = MaterialTheme.colorScheme.primary
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
fun HistoricalEditDialog(
    entry: JapaEntry,
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

                Text("Sandhyavandanam Counts", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = morningInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) morningInput = it },
                    label = { Text("Morning (Pratah) Sandhya") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_edit_morning")
                )

                OutlinedTextField(
                    value = afternoonInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) afternoonInput = it },
                    label = { Text("Noon (Madhyahnika) Sandhya") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_edit_afternoon")
                )

                OutlinedTextField(
                    value = eveningInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) eveningInput = it },
                    label = { Text("Evening (Sayam) Sandhya") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_edit_evening")
                )

                Text("Additional Japa (Punascharana)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
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

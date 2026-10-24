package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomPractice
import com.example.data.CustomPracticeEntry
import com.example.data.JapaEntry
import com.example.ui.theme.getMantraColor
import java.text.SimpleDateFormat
import java.util.Locale

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

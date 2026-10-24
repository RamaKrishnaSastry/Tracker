package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.CustomPractice
import com.example.data.JapaEntry
import com.example.ui.JapaViewModel
import com.example.ui.theme.getMantraColor
import com.example.ui.theme.toHexString

@Composable
fun AddMantraTemplateDialog(
    viewModel: JapaViewModel,
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
    var isPunascharanaEnabledForMantra by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.widget.Toast.makeText(context, "Notification permission is needed for reminders.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

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
                                border = BorderStroke(1.dp, itemColor.copy(alpha = 0.4f)),
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
                        modifier = Modifier.fillMaxWidth().clickable { isPunascharanaEnabledForMantra = !isPunascharanaEnabledForMantra },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Punascharana", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Track secondary/auxiliary counts for this practice", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        Switch(checked = isPunascharanaEnabledForMantra, onCheckedChange = { isPunascharanaEnabledForMantra = it })
                    }

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
                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { enabled ->
                                isReminderEnabled = enabled
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            }
                        )
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
                            if (mantraName.contains("Gayatri", ignoreCase = true)) {
                                viewModel.setGayatriConfig(
                                    name = mantraName,
                                    color = selectedColor,
                                    isDefault = false,
                                    initialCount = initialLifetimeCount.toLongOrNull() ?: 0L
                                )
                                onCreated(com.example.ui.GAYATRI_PRACTICE_ID)
                            } else {
                                val newMantra = CustomPractice(
                                    name = mantraName,
                                    practiceType = practiceType,
                                    defaultTarget = defaultTarget.toIntOrNull() ?: 108,
                                    quickAddValues = quickAddValues,
                                    initialLifetimeCount = initialLifetimeCount.toLongOrNull() ?: 0L,
                                    themeColor = selectedColor,
                                    isReminderEnabled = isReminderEnabled,
                                    isPunascharanaEnabled = isPunascharanaEnabledForMantra,
                                    reminderTime = if (isReminderEnabled) reminderTime else ""
                                )
                                viewModel.addNewPractice(newMantra) { newId ->
                                    onCreated(newId)
                                }
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
    practice: CustomPractice,
    isDefaultMantra: Boolean,
    onDismiss: () -> Unit,
    onSave: (CustomPractice, Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var mantraName by remember { mutableStateOf(practice.name) }
    var practiceType by remember { mutableStateOf(practice.practiceType) }
    var defaultTarget by remember { mutableStateOf(practice.defaultTarget.toString()) }
    var quickAddValues by remember { mutableStateOf(practice.quickAddValues) }
    var initialLifetimeCount by remember { mutableStateOf(practice.initialLifetimeCount.toString()) }
    var showPriorCountWarning by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(practice.themeColor) }
    var isDefault by remember { mutableStateOf(isDefaultMantra) }
    
    var isReminderEnabled by remember { mutableStateOf(practice.isReminderEnabled) }
    var reminderTime by remember { mutableStateOf(practice.reminderTime.ifEmpty { "06:30 AM" }) }
    var isPunascharanaEnabledForMantra by remember { mutableStateOf(practice.isPunascharanaEnabled) }

    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.widget.Toast.makeText(context, "Notification permission is needed for reminders.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

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
                    modifier = Modifier.fillMaxWidth().clickable { isPunascharanaEnabledForMantra = !isPunascharanaEnabledForMantra },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Punascharana", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Track secondary/auxiliary counts for this practice", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    Switch(checked = isPunascharanaEnabledForMantra, onCheckedChange = { isPunascharanaEnabledForMantra = it })
                }

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
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { enabled ->
                            isReminderEnabled = enabled
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    )
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
                HorizontalDivider()
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
                        val parsedCount = initialLifetimeCount.toLongOrNull() ?: 0L
                        if (parsedCount != practice.initialLifetimeCount) {
                            showPriorCountWarning = true
                        } else {
                            val updated = practice.copy(
                                name = mantraName,
                                practiceType = practiceType,
                                defaultTarget = defaultTarget.toIntOrNull() ?: 108,
                                quickAddValues = quickAddValues,
                                initialLifetimeCount = parsedCount,
                                themeColor = selectedColor,
                                isReminderEnabled = isReminderEnabled,
                                isPunascharanaEnabled = isPunascharanaEnabledForMantra,
                                reminderTime = if (isReminderEnabled) reminderTime else ""
                            )
                            onSave(updated, isDefault)
                        }
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

    if (showPriorCountWarning) {
        AlertDialog(
            onDismissRequest = { showPriorCountWarning = false },
            title = { Text("Change Prior Count?") },
            text = { Text("You are about to explicitly change the prior accumulated count to ${initialLifetimeCount}. This will override the count across your synced devices to match this new value. Are you sure you want to proceed?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsedCount = initialLifetimeCount.toLongOrNull() ?: 0L
                        val updated = practice.copy(
                            name = mantraName,
                            practiceType = practiceType,
                            defaultTarget = defaultTarget.toIntOrNull() ?: 108,
                            quickAddValues = quickAddValues,
                            initialLifetimeCount = parsedCount,
                            themeColor = selectedColor,
                            isReminderEnabled = isReminderEnabled,
                            isPunascharanaEnabled = isPunascharanaEnabledForMantra,
                            reminderTime = if (isReminderEnabled) reminderTime else ""
                        )
                        onSave(updated, isDefault)
                        showPriorCountWarning = false
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriorCountWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
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

                PunascharanaInputSection(
                    title = "Pratah Punascharana",
                    value = pratahPunas,
                    onValueChange = { pratahPunas = it },
                    tagPrefix = "dialog_pratah_punas"
                )
                PunascharanaInputSection(
                    title = "Madhyahnika Punascharana",
                    value = madhyahnikaPunas,
                    onValueChange = { madhyahnikaPunas = it },
                    tagPrefix = "dialog_madhyahnika_punas"
                )
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
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)
            )

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

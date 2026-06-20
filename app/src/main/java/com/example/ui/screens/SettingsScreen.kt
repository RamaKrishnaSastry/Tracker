package com.example.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Space
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.example.ui.JapaViewModel
import com.example.ui.theme.getMantraColor
import com.example.ui.theme.toHexString
import com.example.ui.components.ColorPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: JapaViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val themeMode by viewModel.themeMode.collectAsState()
    val initialLifetimeCount by viewModel.initialLifetimeCount.collectAsState()
    val isPunascharanaEnabled by viewModel.isPunascharanaEnabled.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val universalColor by viewModel.universalColor.collectAsState()

    val context = LocalContext.current
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.authManager.updateProfile(account)
                viewModel.clearAuthError()
                viewModel.triggerManualSync()
                android.widget.Toast.makeText(context, "Google Sign-In Successful!", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            val message = when (e.statusCode) {
                12500 -> "Sign-In failed (12500). Verify OAuth credentials or Consent Screen."
                12501 -> "Google Sign-In was cancelled (12501)."
                10    -> "Developer Error (10): SHA-1 mismatch or Drive API not enabled. Check Cloud Console."
                7     -> "Network error (7). Please verify internet connection."
                else  -> "Google Sign-In failed (code ${e.statusCode})."
            }
            Log.e("SettingsScreen", "ApiException: code ${e.statusCode}, message: ${e.message}")
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            viewModel.setAuthError(message)
        }
    }

    var showTimePickerDialogFor by remember { mutableStateOf<String?>(null) } // "morning", "afternoon", "evening" or null

    var editingLifetimeCount by remember { mutableStateOf(false) }
    var tempLifetimeInput by remember(initialLifetimeCount) { mutableStateOf(initialLifetimeCount.toString()) }

    var morningEnabled by remember { mutableStateOf(viewModel.prefs.getMorningReminderEnabled()) }
    var morningTime by remember { mutableStateOf(viewModel.prefs.getMorningReminderTime()) }

    var afternoonEnabled by remember { mutableStateOf(viewModel.prefs.getAfternoonReminderEnabled()) }
    var afternoonTime by remember { mutableStateOf(viewModel.prefs.getAfternoonReminderTime()) }

    var eveningEnabled by remember { mutableStateOf(viewModel.prefs.getEveningReminderEnabled()) }
    var eveningTime by remember { mutableStateOf(viewModel.prefs.getEveningReminderTime()) }

    var reminderIntensity by remember { mutableStateOf(viewModel.prefs.getReminderIntensity()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Settings & Config",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Set reminders, adjust profiles, and manage silent backup files.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Google Account & Cloud Sync Section
        SettingsGroupHeader(title = "Google Drive Sync", icon = Icons.Outlined.CloudQueue)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (userProfile != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile?.displayName ?: "Google User",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = userProfile?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Last Sync Status",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = viewModel.prefs.getLastSync(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("sync_last_status")
                            )
                            if (syncMessage.isNotEmpty()) {
                                Text(
                                    text = syncMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.triggerManualSync() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("sync_now_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Manual sync", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Now", fontSize = 12.sp)
                        }
                    }
                } else {
                    Text(
                        text = "Cloud Sync is Disconnected",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Connecting a Google account enables automatic, background syncing of counts to private Google Drive App Storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    if (authError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = authError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("auth_error_display")
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val client = viewModel.authManager.getSignInClient()
                            googleSignInLauncher.launch(client.signInIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("connect_google_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Connect Google Drive Backup")
                    }
                }
            }
        }

        // 1b. Chant Preferences
        SettingsGroupHeader(title = "Chant Preferences", icon = Icons.Outlined.CheckCircle)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Punascharana Japa",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Include auxiliary Punascharana counts globally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isPunascharanaEnabled,
                        onCheckedChange = { viewModel.setPunascharanaEnabled(it) },
                        modifier = Modifier.testTag("punascharana_toggle_switch")
                    )
                }
                
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                
                var showManageDialog by remember { mutableStateOf(false) }
                
                ListItem(
                    headlineContent = { Text("Manage My Practices", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Re-order, archive, or set default practice.") },
                    leadingContent = { Icon(Icons.Default.SettingsSuggest, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier.clickable { showManageDialog = true }.testTag("manage_practices_item")
                )
                
                if (showManageDialog) {
                    ManagePracticesDialog(
                        viewModel = viewModel,
                        onDismiss = { showManageDialog = false }
                    )
                }
            }
        }

        // 2. Theme Selection
        SettingsGroupHeader(title = "App Theme", icon = Icons.Outlined.Palette)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val themes = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                    themes.forEach { (mode, name) ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { viewModel.updateThemeMode(mode) },
                            label = { Text(name) },
                            modifier = Modifier.testTag("theme_chip_$mode")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Universal App Color",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "This color reflects globally across the application shell and default components.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colorsList = listOf("Indigo", "Royal", "Teal", "Saffron", "Crimson", "Emerald", "Amber", "Slate", "Violet")
                    colorsList.forEach { colorName ->
                        val isSelected = universalColor.equals(colorName, ignoreCase = true)
                        val color = com.example.ui.theme.getMantraColor(colorName)
                        
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { viewModel.setUniversalColor(colorName) }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .testTag("universal_color_$colorName"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                var showVisualPicker by remember { mutableStateOf(false) }
                var customHex by remember { mutableStateOf(if (universalColor.startsWith("#")) universalColor else "") }

                if (showVisualPicker) {
                    ColorPickerDialog(
                        initialColor = getMantraColor(universalColor),
                        onColorSelected = { 
                            val hex = it.toHexString()
                            viewModel.setUniversalColor(hex)
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
                            viewModel.setUniversalColor(it)
                        }
                    },
                    label = { Text("Custom Hex Color (e.g. #6449A7)") },
                    placeholder = { Text("#RRGGBB") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("universal_custom_hex_input"),
                    leadingIcon = { 
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(getMantraColor(universalColor), CircleShape)
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
        }

        // 3. Edit Cumulative Counts
        SettingsGroupHeader(title = "Initial Count Configuration", icon = Icons.Outlined.Equalizer)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (editingLifetimeCount) {
                    OutlinedTextField(
                        value = tempLifetimeInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) tempLifetimeInput = input
                        },
                        label = { Text("Prior Count Accumulation") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_lifetime_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editingLifetimeCount = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val parsed = tempLifetimeInput.toLongOrNull() ?: 0L
                                viewModel.updateInitialLifetimeCount(parsed)
                                editingLifetimeCount = false
                            },
                            modifier = Modifier.testTag("settings_lifetime_save")
                        ) {
                            Text("Save")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Prior Accumulated Counts",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = initialLifetimeCount.toString(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("settings_lifetime_display")
                            )
                        }

                        IconButton(
                            onClick = { editingLifetimeCount = true },
                            modifier = Modifier.testTag("edit_lifetime_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit initial count")
                        }
                    }
                }
            }
        }

        // 4. Sandhya REMINDERS configuration
        SettingsGroupHeader(title = "Reminders (Offline-First)", icon = Icons.Outlined.Alarm)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Morning Sandhya Alarm
                ReminderAlarmRow(
                    label = "Morning Pratah Alarm",
                    enabled = morningEnabled,
                    time = morningTime,
                    onEnabledChange = {
                        morningEnabled = it
                        viewModel.setMorningReminder(it, morningTime)
                    },
                    onTimeClick = { showTimePickerDialogFor = "morning" },
                    tagPrefix = "morning"
                )

                Divider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Midday Sandhya Alarm
                ReminderAlarmRow(
                    label = "Midday Madhyahnika Alarm",
                    enabled = afternoonEnabled,
                    time = afternoonTime,
                    onEnabledChange = {
                        afternoonEnabled = it
                        viewModel.setAfternoonReminder(it, afternoonTime)
                    },
                    onTimeClick = { showTimePickerDialogFor = "afternoon" },
                    tagPrefix = "afternoon"
                )

                Divider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Evening Sandhya Alarm
                ReminderAlarmRow(
                    label = "Evening Sayam Alarm",
                    enabled = eveningEnabled,
                    time = eveningTime,
                    onEnabledChange = {
                        eveningEnabled = it
                        viewModel.setEveningReminder(it, eveningTime)
                    },
                    onTimeClick = { showTimePickerDialogFor = "evening" },
                    tagPrefix = "evening"
                )
            }
        }

        // 5. Intelligent Reminder Intensity Configuration
        SettingsGroupHeader(title = "Reminder Intensity", icon = Icons.Outlined.NotificationsActive)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Configure how active warnings are displayed. All options check local database lists programmatically to ensure silence if Japa counts have been recorded for the Sandhya interval.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val intensities = listOf("disabled" to "Disabled", "minimal" to "Minimal", "standard" to "Standard")
                    intensities.forEach { (mode, name) ->
                        FilterChip(
                            selected = reminderIntensity == mode,
                            onClick = {
                                reminderIntensity = mode
                                viewModel.prefs.setReminderIntensity(mode)
                            },
                            label = { Text(name) },
                            modifier = Modifier.testTag("intensity_chip_$mode")
                        )
                    }
                }
            }
        }

        // Account management
        if (userProfile != null) {
            Button(
                onClick = { viewModel.signOutUser() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disconnect Google Account Backup")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Direct sleek Custom Time Picker selection dialog popup
    showTimePickerDialogFor?.let { period ->
        val currentTimeStr = when (period) {
            "morning" -> morningTime
            "afternoon" -> afternoonTime
            "evening" -> eveningTime
            else -> "06:00"
        }

        SleekTimeSelectionDialog(
            initialTime = currentTimeStr,
            onDismiss = { showTimePickerDialogFor = null },
            onConfirm = { selectedTime ->
                when (period) {
                    "morning" -> {
                        morningTime = selectedTime
                        viewModel.setMorningReminder(morningEnabled, selectedTime)
                    }
                    "afternoon" -> {
                        afternoonTime = selectedTime
                        viewModel.setAfternoonReminder(afternoonEnabled, selectedTime)
                    }
                    "evening" -> {
                        eveningTime = selectedTime
                        viewModel.setEveningReminder(eveningEnabled, selectedTime)
                    }
                }
                showTimePickerDialogFor = null
            }
        )
    }
}

@Composable
fun SettingsGroupHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ReminderAlarmRow(
    label: String,
    enabled: Boolean,
    time: String,
    onEnabledChange: (Boolean) -> Unit,
    onTimeClick: () -> Unit,
    tagPrefix: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier
                    .clickable(enabled = enabled, onClick = onTimeClick)
                    .testTag("${tagPrefix}_reminder_time_text")
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            modifier = Modifier.testTag("${tagPrefix}_reminder_switch")
        )
    }
}

@Composable
fun SleekTimeSelectionDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val explodedStr = initialTime.split(":")
    var hour by remember { mutableStateOf(explodedStr.getOrNull(0)?.toIntOrNull() ?: 6) }
    var minute by remember { mutableStateOf(explodedStr.getOrNull(1)?.toIntOrNull() ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Adjust Alert Time",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // HOUR Increment column
                DialColumn(
                    label = "Hour",
                    value = hour,
                    maxValue = 23,
                    onValueChange = { hour = it },
                    tag = "hour"
                )

                Text(
                    text = ":",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // MINUTE Increment Column
                DialColumn(
                    label = "Min",
                    value = minute,
                    maxValue = 59,
                    onValueChange = { minute = it },
                    tag = "minute"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hStr = hour.toString().padStart(2, '0')
                    val mStr = minute.toString().padStart(2, '0')
                    onConfirm("$hStr:$mStr")
                },
                modifier = Modifier.testTag("dial_confirm_button")
            ) {
                Text("Set Time")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dial_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DialColumn(
    label: String,
    value: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        IconButton(
            onClick = { onValueChange(if (value == maxValue) 0 else value + 1) },
            modifier = Modifier.testTag("btn_${tag}_up")
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
        }

        Box(
            modifier = Modifier
                .width(64.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("text_dial_$tag")
            )
        }

        IconButton(
            onClick = { onValueChange(if (value == 0) maxValue else value - 1) },
            modifier = Modifier.testTag("btn_${tag}_down")
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down")
        }
    }
}

@Composable
fun ManagePracticesDialog(
    viewModel: com.example.ui.JapaViewModel,
    onDismiss: () -> Unit
) {
    val activePractices by viewModel.activeCustomPractices.collectAsState()
    val archivedPractices by viewModel.archivedCustomPractices.collectAsState()
    val defaultPid by viewModel.defaultPracticeId.collectAsState()
    
    var showArchived by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Practices") },
        text = {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                TabRow(selectedTabIndex = if (showArchived) 1 else 0) {
                    Tab(selected = !showArchived, onClick = { showArchived = false }, text = { Text("Active") })
                    Tab(selected = showArchived, onClick = { showArchived = true }, text = { Text("Archived") })
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!showArchived) {
                    androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(activePractices.size) { index ->
                            val p = activePractices[index]
                            PracticeManagementRow(
                                practice = p,
                                isDefault = p.id == defaultPid,
                                onMoveUp = if (index > 0) { { 
                                    val newList = activePractices.toMutableList()
                                    // Room order is 0-indexed, update all or swap
                                    val swapWith = activePractices[index - 1]
                                    viewModel.updatePracticeOrder(listOf(p.copy(displayOrder = index - 1), swapWith.copy(displayOrder = index)))
                                } } else null,
                                onMoveDown = if (index < activePractices.size - 1) { { 
                                    val swapWith = activePractices[index + 1]
                                    viewModel.updatePracticeOrder(listOf(p.copy(displayOrder = index + 1), swapWith.copy(displayOrder = index)))
                                } } else null,
                                onArchive = { viewModel.togglePracticeArchived(p.id, true) },
                                onSetDefault = { viewModel.setDefaultPractice(p.id) }
                            )
                        }
                        
                        if (activePractices.isEmpty()) {
                            item { Text("No active practices found.", color = Color.Gray) }
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(archivedPractices.size) { index ->
                            val p = archivedPractices[index]
                            ListItem(
                                headlineContent = { Text(p.name) },
                                supportingContent = { Text(p.practiceType) },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.togglePracticeArchived(p.id, false) }) {
                                        Icon(Icons.Default.Unarchive, "Unarchive")
                                    }
                                }
                            )
                        }
                        if (archivedPractices.isEmpty()) {
                            item { Text("No archived practices.", color = Color.Gray) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun PracticeManagementRow(
    practice: com.example.data.CustomPractice,
    isDefault: Boolean,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onArchive: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(practice.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    if (isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("DEFAULT", fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        )
                    }
                }
                Text(practice.practiceType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Row {
                if (!isDefault) {
                    IconButton(onClick = onSetDefault) {
                        Icon(Icons.Default.PushPin, "Set Default", modifier = Modifier.size(20.dp))
                    }
                }
                
                IconButton(onClick = onArchive) {
                    Icon(Icons.Default.Archive, "Archive", modifier = Modifier.size(20.dp))
                }
                
                IconButton(onClick = onMoveUp ?: {}, enabled = onMoveUp != null) {
                    Icon(Icons.Default.ArrowUpward, "Move Up", modifier = Modifier.size(20.dp))
                }
                
                IconButton(onClick = onMoveDown ?: {}, enabled = onMoveDown != null) {
                    Icon(Icons.Default.ArrowDownward, "Move Down", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

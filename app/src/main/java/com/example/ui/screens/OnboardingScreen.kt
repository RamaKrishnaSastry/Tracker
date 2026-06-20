@file:OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
package com.example.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CustomPractice
import com.example.ui.JapaViewModel
import com.example.ui.theme.getMantraColor
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun OnboardingScreen(
    viewModel: JapaViewModel,
    onOnboarded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val step by viewModel.onboardingStep.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.authManager.updateProfile(account)
                viewModel.handleGoogleSignInSuccess(onOnboarded)
            }
        } catch (e: ApiException) {
            Log.e("OnboardingScreen", "Google Sign-In failed with code ${e.statusCode}: ${e.message}")
            val errorMessage = when (e.statusCode) {
                10 -> "Developer Error (10): SHA-1 mismatch or Drive API not enabled. Check Cloud Console."
                12501 -> "Sign-In cancelled. Proceeding offline."
                12500 -> "Sign-In failed (12500). Verify OAuth credentials or Consent Screen."
                else -> "Sign-In error (code ${e.statusCode}). Proceeding offline."
            }
            android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
            Log.e("OnboardingScreen", "Full error: $errorMessage")
            viewModel.setOnboardingStep(3)
        } catch (e: Exception) {
            Log.e("OnboardingScreen", "Unexpected error during sign-in: ${e.message}")
            android.widget.Toast.makeText(context, "Unexpected sign-in error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            viewModel.setOnboardingStep(3)
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() with
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "OnboardingStepTransition"
            ) { targetStep ->
                when (targetStep) {
                    1 -> {
                        // Welcome Screen
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "JapaMitra",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your Spiritual Companion",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        letterSpacing = 4.sp,
                                        fontWeight = FontWeight.Light
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(32.dp))

                                // Render our custom illustration
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color.White)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.gayatri_meditation),
                                        contentDescription = "Meditative Gayatri Rising Sun",
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "Track your daily mantra counts quietly, privately, and distraction-free. Organize all your spiritual practices in one place with seamless cloud backup.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { viewModel.setOnboardingStep(2) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("onboarding_continue_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        "Begin Setup",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    2 -> {
                        // Google Authenticate Step
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Secure login",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Secure Google Synch",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "To allow seamless backup and access across your devices, connect your Google Account.\n\nAll counts will be synced silently inside a private, hidden folder in your Google Drive (App Data folder) where nobody else can read or access it.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "No analytics, ads, or standard data collection. The app works 100% offline-first. Sharing settings is entirely private.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        val client = viewModel.authManager.getSignInClient()
                                        googleSignInLauncher.launch(client.signInIntent)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("google_login_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onBackground,
                                        contentColor = MaterialTheme.colorScheme.background
                                    )
                                ) {
                                    Text(
                                        "Connect Google Account",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = { viewModel.setOnboardingStep(3) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("skip_login_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    border = ButtonDefaults.outlinedButtonBorder
                                ) {
                                    Text(
                                        "Skip (Proceed Offline)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    3 -> {
                        // Choose Your Practice Screen
                        var selectedTab by remember { mutableStateOf(0) } // 0: Sandhyas, 1: Mantras, 2: Stotras
                        var showConfigDialog by remember { mutableStateOf<com.example.data.CustomPractice?>(null) }
                        val addedPracticeIds = remember { mutableStateListOf<Long>() }
                        val addedPracticeNames = remember { mutableStateListOf<String>() }

                        val tabs = listOf("Sandhyas", "Repetitive Mantras", "Stotras & Recitations")

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Choose Your Practice",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Browse and select one or more spiritual practices to add to your dashboard.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                divider = {}
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                when (selectedTab) {
                                    0 -> SandhyasTabContent(addedNames = addedPracticeNames, onSelect = { showConfigDialog = it })
                                    1 -> MantrasTabContent(addedNames = addedPracticeNames, onSelect = { showConfigDialog = it })
                                    2 -> StotrasTabContent(addedNames = addedPracticeNames, onSelect = { showConfigDialog = it })
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    onOnboarded()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("onboarding_finish_button"),
                                shape = RoundedCornerShape(16.dp),
                                enabled = addedPracticeIds.isNotEmpty()
                            ) {
                                Text("Finish & Begin Chanting", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        val configDialogPractice = showConfigDialog
                        if (configDialogPractice != null) {
                            ConfigurePracticeDialog(
                                practiceTemplate = configDialogPractice,
                                onDismiss = { showConfigDialog = null },
                                onSave = { practice, isDefault ->
                                    viewModel.addNewPractice(practice) { newId ->
                                        if (isDefault) {
                                            viewModel.setDefaultPractice(newId)
                                        }
                                        viewModel.setActivePractice(newId)
                                        addedPracticeIds.add(newId)
                                        addedPracticeNames.add(practice.name)
                                    }
                                    showConfigDialog = null
                                    android.widget.Toast.makeText(context, "${practice.name} added to dashboard", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    4 -> {
                        // Syncing Step
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Checking for existing backup...",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SandhyasTabContent(addedNames: List<String>, onSelect: (com.example.data.CustomPractice) -> Unit) {
    val items = listOf(
        com.example.data.CustomPractice(
            name = "Gayatri Japa",
            practiceType = "SANDHYA",
            themeColor = "Violet",
            quickAddValues = "10,24,28,54,108"
        ),
        com.example.data.CustomPractice(
            name = "Custom Sandhya",
            practiceType = "SANDHYA",
            themeColor = "Indigo",
            quickAddValues = "10,24,108"
        )
    )
    PracticeTemplateList(items, addedNames, onSelect)
}

@Composable
fun MantrasTabContent(addedNames: List<String>, onSelect: (com.example.data.CustomPractice) -> Unit) {
    val items = listOf(
        com.example.data.CustomPractice(
            name = "Om Namah Shivaya",
            practiceType = "CONTINUOUS",
            themeColor = "Indigo",
            defaultTarget = 108,
            quickAddValues = "11,27,54,108"
        ),
        com.example.data.CustomPractice(
            name = "Om Namo Narayanaya",
            practiceType = "CONTINUOUS",
            themeColor = "Royal",
            defaultTarget = 108,
            quickAddValues = "8,16,54,108"
        ),
        com.example.data.CustomPractice(
            name = "Mahamrityunjaya Mantra",
            practiceType = "CONTINUOUS",
            themeColor = "Teal",
            defaultTarget = 108,
            quickAddValues = "11,27,54,108"
        ),
        com.example.data.CustomPractice(
            name = "Bala Mantra",
            practiceType = "CONTINUOUS",
            themeColor = "Violet",
            quickAddValues = "10,24,54,108"
        ),
        com.example.data.CustomPractice(
            name = "Sri Vidya Mantra",
            practiceType = "CONTINUOUS",
            themeColor = "Crimson",
            quickAddValues = "10,24,54,108"
        ),
        com.example.data.CustomPractice(
            name = "Custom Mantra",
            practiceType = "CONTINUOUS",
            themeColor = "Saffron",
            quickAddValues = "10,54,108"
        )
    )
    PracticeTemplateList(items, addedNames, onSelect)
}

@Composable
fun StotrasTabContent(addedNames: List<String>, onSelect: (com.example.data.CustomPractice) -> Unit) {
    val items = listOf(
        com.example.data.CustomPractice(
            name = "Vishnu Sahasranama",
            practiceType = "RECITATION",
            themeColor = "Royal",
            defaultTarget = 1
        ),
        com.example.data.CustomPractice(
            name = "Lalitha Sahasranama",
            practiceType = "RECITATION",
            themeColor = "Crimson",
            defaultTarget = 1
        ),
        com.example.data.CustomPractice(
            name = "Hanuman Chalisa",
            practiceType = "RECITATION",
            themeColor = "Saffron",
            defaultTarget = 1
        ),
        com.example.data.CustomPractice(
            name = "Aditya Hridayam",
            practiceType = "RECITATION",
            themeColor = "Amber",
            defaultTarget = 1
        ),
        com.example.data.CustomPractice(
            name = "Custom Stotra",
            practiceType = "RECITATION",
            themeColor = "Emerald",
            defaultTarget = 1
        )
    )
    PracticeTemplateList(items, addedNames, onSelect)
}

@Composable
fun PracticeTemplateList(items: List<com.example.data.CustomPractice>, addedNames: List<String>, onSelect: (com.example.data.CustomPractice) -> Unit) {
    androidx.compose.foundation.lazy.LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items.size) { index ->
            val item = items[index]
            val isAdded = addedNames.contains(item.name)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!isAdded) onSelect(item) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(com.example.ui.theme.getMantraColor(item.themeColor))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(item.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            when(item.practiceType) {
                                "SANDHYA" -> "Sandhya Workflow"
                                "RECITATION" -> "Recitation Count"
                                else -> "Repetitive Mantra"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(if (isAdded) Icons.Default.Check else Icons.Default.Add, contentDescription = if (isAdded) "Selected" else "Add")
                }
            }
        }
    }
}

@Composable
fun ConfigurePracticeDialog(
    practiceTemplate: com.example.data.CustomPractice,
    onDismiss: () -> Unit,
    onSave: (com.example.data.CustomPractice, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(practiceTemplate.name) }
    var target by remember { mutableStateOf(practiceTemplate.defaultTarget.toString()) }
    var initialCount by remember { mutableStateOf(practiceTemplate.initialLifetimeCount.toString()) }
    var selectedColor by remember { mutableStateOf(practiceTemplate.themeColor) }
    var morningEnabled by remember { mutableStateOf(practiceTemplate.isMorningEnabled) }
    var middayEnabled by remember { mutableStateOf(practiceTemplate.isMiddayEnabled) }
    var eveningEnabled by remember { mutableStateOf(practiceTemplate.isEveningEnabled) }
    var punasEnabled by remember { mutableStateOf(practiceTemplate.isPunascharanaEnabled) }
    var increment by remember { mutableStateOf(practiceTemplate.incrementValue.toString()) }
    var isSetAsDefault by remember { mutableStateOf(false) }

    val colorsList = listOf("Indigo", "Royal", "Teal", "Saffron", "Crimson", "Emerald", "Amber", "Slate", "Violet")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure ${practiceTemplate.name}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Practice Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (practiceTemplate.practiceType != "SANDHYA") {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { if (it.all { c -> c.isDigit() }) target = it },
                        label = { Text("Daily Target") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (practiceTemplate.practiceType == "RECITATION") {
                    OutlinedTextField(
                        value = increment,
                        onValueChange = { if (it.all { c -> c.isDigit() }) increment = it },
                        label = { Text("Increment Step") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = initialCount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) initialCount = it },
                    label = { Text("Initial Lifetime Count") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                if (practiceTemplate.practiceType == "SANDHYA") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Session Sections", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = morningEnabled, onCheckedChange = { morningEnabled = it })
                        Text("Morning")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = middayEnabled, onCheckedChange = { middayEnabled = it })
                        Text("Midday")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = eveningEnabled, onCheckedChange = { eveningEnabled = it })
                        Text("Evening")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = punasEnabled, onCheckedChange = { punasEnabled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Punascharana (Additional)")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isSetAsDefault, onCheckedChange = { isSetAsDefault = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set as Default Practice", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Theme Color", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorsList.forEach { colorName ->
                        val color = getMantraColor(colorName)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorName }
                                .border(
                                    width = if (selectedColor == colorName) 3.dp else 0.dp,
                                    color = if (selectedColor == colorName) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(practiceTemplate.copy(
                        name = name,
                        defaultTarget = target.toIntOrNull() ?: 108,
                        initialLifetimeCount = initialCount.toLongOrNull() ?: 0L,
                        themeColor = selectedColor,
                        isMorningEnabled = morningEnabled,
                        isMiddayEnabled = middayEnabled,
                        isEveningEnabled = eveningEnabled,
                        isPunascharanaEnabled = punasEnabled,
                        incrementValue = increment.toIntOrNull() ?: 1
                    ), isSetAsDefault)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add to Dashboard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

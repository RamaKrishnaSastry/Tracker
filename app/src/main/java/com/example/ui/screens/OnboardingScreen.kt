package com.example.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.ui.JapaViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalAnimationApi::class)
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
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    viewModel.authManager.updateProfile(account)
                    // Move to second part: get initial lifetime counts
                    viewModel.setOnboardingStep(3)
                }
            } catch (e: ApiException) {
                Log.e("OnboardingScreen", "Google Sign-In failed with code ${e.statusCode}: ${e.message}")
                val errorMessage = when (e.statusCode) {
                    10 -> "SHA-1 certificate mismatch or Google API not configured. Please check cloud console."
                    12501 -> "Sign-In cancelled. You can proceed offline for now."
                    12500 -> "Sign-In failed. Verify OAuth credentials or Google Play Services."
                    else -> "Sign-In error (code ${e.statusCode}). You can proceed offline."
                }
                Log.e("OnboardingScreen", "Full error: $errorMessage")
                // Still allow proceeding offline, but log the diagnostic
                viewModel.setOnboardingStep(3)
            } catch (e: Exception) {
                Log.e("OnboardingScreen", "Unexpected error during sign-in: ${e.message}")
                viewModel.setOnboardingStep(3)
            }
        } else {
            // Cancel or failure, we gracefully bypass to entering counts so they are never stuck
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
                                    text = "GAYATRI JAPA",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 4.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "TRACKER",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        letterSpacing = 6.sp,
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
                                    text = "Track your daily Gayatri counts quietly, privately, and distraction-free during morning, noon, and evening Sandhya.",
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
                        // Life Count Input Step
                        var countInput by remember { mutableStateOf("") }
                        val suggestions = listOf(0L, 10000L, 50000L, 108000L, 1000000L)

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
                                Text(
                                    text = "Prior Counts Accumulation",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Approximately how many Gayatri Japa repetitions have you completed before using this app?",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(32.dp))

                                OutlinedTextField(
                                    value = countInput,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() }) countInput = input
                                    },
                                    label = { Text("Prior Accomplished Count") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("initial_count_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Or choose a starting default:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    suggestions.forEach { count ->
                                        AssistChip(
                                            onClick = { countInput = count.toString() },
                                            label = {
                                                Text(
                                                    if (count >= 1000000L) "${count / 1000000L}M"
                                                    else if (count >= 1000L) "${count / 1000L}k"
                                                    else count.toString()
                                                )
                                            },
                                            modifier = Modifier.testTag("chip_$count")
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        val actualCount = countInput.toLongOrNull() ?: 0L
                                        viewModel.setCompletedOnboarding(actualCount)
                                        onOnboarded()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("onboarding_finish_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        "Finish & Begin Chanting",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

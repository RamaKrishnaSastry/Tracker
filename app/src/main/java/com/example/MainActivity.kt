package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.JapaViewModel
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: JapaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val onboardingStep by viewModel.onboardingStep.collectAsState()
            val universalColorName by viewModel.universalColor.collectAsState()

            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(
                darkTheme = isDark,
                baseColor = com.example.ui.theme.getMantraColor(universalColorName)
            ) {
                if (onboardingStep < 4) {
                    // Show onboarding flows
                    OnboardingScreen(
                        viewModel = viewModel,
                        onOnboarded = {
                            viewModel.setOnboardingStep(4)
                        }
                    )
                } else {
                    // Show main dashboard shell
                    MainDashboardShell(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainDashboardShell(viewModel: JapaViewModel) {
    var activeTab by remember { mutableStateOf(0) } // 0: Today, 1: History, 2: Settings

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Shorter, custom-styled bottom navigation for a more immersive feel
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().testTag("app_navigation_bar")
            ) {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .fillMaxWidth()
                        .height(64.dp), // Reduced height from typical 80dp
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        Triple(0, Icons.Default.CalendarToday, "Today"),
                        Triple(1, Icons.Default.History, "History"),
                        Triple(2, Icons.Default.Settings, "Settings")
                    )

                    val testTags = listOf("nav_tab_today", "nav_tab_history", "nav_tab_settings")

                    for (i in tabs.indices) {
                        val (index, icon, label) = tabs[i]
                        val selected = activeTab == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeTab = index }
                                .testTag(testTags[i])
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            color = MaterialTheme.colorScheme.background
        ) {
            when (activeTab) {
                0 -> TodayScreen(viewModel = viewModel)
                1 -> HistoryScreen(viewModel = viewModel)
                2 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

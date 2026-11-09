package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JapaViewModel
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.Dimens
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: JapaViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        viewModel.checkForRemoteChanges()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

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
                    OnboardingScreen(
                        viewModel = viewModel,
                        onOnboarded = {
                            viewModel.setOnboardingStep(4)
                        }
                    )
                } else {
                    MainDashboardShell(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainDashboardShell(viewModel: JapaViewModel) {
    var activeTab by rememberSaveable { mutableStateOf(0) } // 0: Today, 1: History, 2: Settings
    val haptic = LocalHapticFeedback.current
    val isImeVisible = WindowInsets.isImeVisible

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main Screen Content (flows edge-to-edge behind the floating glass bar)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width / 4 } + fadeIn(tween(250))).togetherWith(
                                    slideOutHorizontally { width -> -width / 4 } + fadeOut(tween(200))
                                )
                            } else {
                                (slideInHorizontally { width -> -width / 4 } + fadeIn(tween(250))).togetherWith(
                                    slideOutHorizontally { width -> width / 4 } + fadeOut(tween(200))
                                )
                            }
                        },
                        label = "MainScreenTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            0 -> TodayScreen(viewModel = viewModel)
                            1 -> HistoryScreen(viewModel = viewModel)
                            2 -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }

        // Floating Glass Bottom Navigation Bar (Hides when IME/keyboard is open)
        val glassSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        val glassBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        val activeColor = MaterialTheme.colorScheme.primary

        AnimatedVisibility(
            visible = !isImeVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .wrapContentHeight()
        ) {
            Surface(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = Dimens.navBarMargin)
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .height(Dimens.navBarHeight)
                    .testTag("app_navigation_bar"),
                shape = CircleShape,
                color = glassSurfaceColor,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = glassBorderColor
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
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
                        val isSelected = activeTab == index

                        val tabShape = when (index) {
                            0 -> RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp, topEnd = 18.dp, bottomEnd = 18.dp)
                            2 -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 26.dp, bottomEnd = 26.dp)
                            else -> RoundedCornerShape(18.dp)
                        }

                        val animatedWeight by animateFloatAsState(
                            targetValue = if (isSelected) 1.6f else 1.0f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioNoBouncy
                            ),
                            label = "tabWeight"
                        )

                        val tabBgColor by animateColorAsState(
                            targetValue = if (isSelected) activeColor.copy(alpha = 0.18f) else Color.Transparent,
                            animationSpec = tween(durationMillis = 200),
                            label = "tabBgColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(animatedWeight)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                                .clip(tabShape)
                                .background(tabBgColor)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) activeColor.copy(alpha = 0.4f) else Color.Transparent,
                                    shape = tabShape
                                )
                                .semantics {
                                    role = Role.Tab
                                    selected = isSelected
                                    contentDescription = label
                                }
                                .clickable {
                                    if (activeTab != index) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        activeTab = index
                                    }
                                }
                                .testTag(testTags[i]),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    modifier = Modifier.size(20.dp)
                                )
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = expandHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(150)),
                                    exit = shrinkHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(100))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            color = activeColor,
                                            softWrap = false,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

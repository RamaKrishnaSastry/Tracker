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
import androidx.compose.runtime.saveable.rememberSaveable
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

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset

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
    var activeTab by rememberSaveable { mutableStateOf(0) } // 0: Today, 1: History, 2: Settings
    var isNavBarVisible by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10f) {
                    // Scrolling down: hide floating bar for full focus
                    if (isNavBarVisible) isNavBarVisible = false
                } else if (available.y > 10f) {
                    // Scrolling up: show floating bar
                    if (!isNavBarVisible) isNavBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Main Screen Content (flows edge-to-edge behind the floating liquid glass bar)
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

        // Floating Translucent Frosted Liquid Glass Bottom Bar with Scroll Hide/Show
        val glassSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        val glassBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
        val glassHighlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

        AnimatedVisibility(
            visible = isNavBarVisible,
            enter = slideInVertically(
                initialOffsetY = { it * 2 },
                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
            ) + fadeIn(tween(250)),
            exit = slideOutVertically(
                targetOffsetY = { it * 2 },
                animationSpec = tween(durationMillis = 200)
            ) + fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .testTag("app_navigation_bar"),
                shape = CircleShape,
                color = glassSurfaceColor,
                tonalElevation = 10.dp,
                shadowElevation = 18.dp,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            glassBorderColor,
                            glassHighlightColor
                        )
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
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
                        val selected = activeTab == index
                        val activeColor = MaterialTheme.colorScheme.primary

                        // Edge-flush pill shape according to item index
                        val tabShape = when (index) {
                            0 -> RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                            2 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 28.dp, bottomEnd = 28.dp)
                            else -> RoundedCornerShape(20.dp)
                        }

                        // Subtle spring zoom scale animation when selected (1.08f zoom element)
                        val tabScale by animateFloatAsState(
                            targetValue = if (selected) 1.08f else 1.0f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            ),
                            label = "tabScale"
                        )

                        // Smooth color transition for background pill highlight
                        val tabBgColor by animateColorAsState(
                            targetValue = if (selected) activeColor.copy(alpha = 0.22f) else Color.Transparent,
                            animationSpec = tween(durationMillis = 250),
                            label = "tabBgColor"
                        )

                        // Glow border alpha for active pill highlight
                        val borderAlpha by animateFloatAsState(
                            targetValue = if (selected) 0.35f else 0.0f,
                            animationSpec = tween(durationMillis = 300),
                            label = "borderAlpha"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(
                                    start = if (index == 0) 2.dp else 2.dp,
                                    end = if (index == 2) 2.dp else 2.dp,
                                    top = 2.dp,
                                    bottom = 2.dp
                                )
                                .graphicsLayer {
                                    scaleX = tabScale
                                    scaleY = tabScale
                                }
                                .clip(tabShape)
                                .background(tabBgColor)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) activeColor.copy(alpha = borderAlpha) else Color.Transparent,
                                    shape = tabShape
                                )
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                                AnimatedVisibility(
                                    visible = selected,
                                    enter = expandHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(200)),
                                    exit = shrinkHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(150))
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
                                            maxLines = 1
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

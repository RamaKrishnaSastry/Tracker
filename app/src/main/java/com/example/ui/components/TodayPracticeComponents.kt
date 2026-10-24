package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.SyncState
import com.example.data.PlaceCategory
import java.util.Locale

@Composable
fun PracticeMetaRow(
    activePlaceName: String,
    activePlaceCategory: String,
    syncState: SyncState,
    activeMantraContainerColor: Color,
    onLocationClick: () -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Location Chip
        Surface(
            onClick = onLocationClick,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier
                .weight(1f, fill = false)
                .testTag("today_location_chip")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val activeCategory = PlaceCategory.fromId(activePlaceCategory)
                val primaryColor = activeCategory.primaryColor
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Practice Location",
                    tint = primaryColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = activePlaceName.ifBlank { activeCategory.displayName },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = activeCategory.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = primaryColor,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.EditLocation,
                    contentDescription = "Change Location",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Sync Pill
        Surface(
            onClick = { if (syncState != SyncState.DISCONNECTED) onSyncClick() },
            shape = RoundedCornerShape(20.dp),
            color = when (syncState) {
                SyncState.SYNCED -> activeMantraContainerColor.copy(alpha = 0.35f)
                SyncState.SYNCING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                SyncState.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.testTag("today_sync_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val (icon, color, label) = when (syncState) {
                    SyncState.DISCONNECTED -> Triple(Icons.Default.CloudOff, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), "Offline")
                    SyncState.OFFLINE -> Triple(Icons.Default.CloudOff, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), "Offline")
                    SyncState.SYNC_PENDING -> Triple(Icons.Default.Cloud, MaterialTheme.colorScheme.secondary, "Syncing")
                    SyncState.SYNCING -> Triple(Icons.Default.Sync, MaterialTheme.colorScheme.primary, "Syncing...")
                    SyncState.SYNCED -> Triple(Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary, "Synced")
                    SyncState.ERROR -> Triple(Icons.Default.ErrorOutline, MaterialTheme.colorScheme.error, "Sync Error")
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Sync Status",
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }
        }
    }
}

@Composable
fun WeeklyProgressCard(
    weeklyCount: Int,
    weeklySankalpaTarget: Int,
    activeMantraColor: Color,
    modifier: Modifier = Modifier
) {
    if (weeklySankalpaTarget <= 0) return
    val progress = (weeklyCount.toFloat() / weeklySankalpaTarget.toFloat()).coerceIn(0f, 1f)
    val isGoalReached = weeklyCount >= weeklySankalpaTarget

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGoalReached) Color(0xFF4CAF50).copy(alpha = 0.12f) else activeMantraColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            1.dp,
            if (isGoalReached) Color(0xFF4CAF50).copy(alpha = 0.4f) else activeMantraColor.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isGoalReached) Icons.Default.EmojiEvents else Icons.Default.DateRange,
                        contentDescription = null,
                        tint = if (isGoalReached) Color(0xFF4CAF50) else activeMantraColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isGoalReached) "Weekly Goal Accomplished!" else "Weekly Sankalpa Target",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isGoalReached) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = String.format(Locale.US, "%,d / %,d", weeklyCount, weeklySankalpaTarget),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (isGoalReached) Color(0xFF2E7D32) else activeMantraColor
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isGoalReached) Color(0xFF4CAF50) else activeMantraColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun JapaProgressCircle(
    progress: Float,
    current: Int,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = "$current/$target",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun AutoResizedTextToday(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    targetFontSize: TextUnit = 28.sp
) {
    var resizedFontSize by remember(text) { mutableStateOf(targetFontSize) }
    var shouldDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        modifier = modifier.drawWithContent {
            if (shouldDraw) drawContent()
        },
        maxLines = maxLines,
        style = style.copy(fontSize = resizedFontSize),
        onTextLayout = { result ->
            if (result.didOverflowWidth) {
                if (resizedFontSize.value > 12f) {
                    resizedFontSize = (resizedFontSize.value - 1.5f).sp
                } else {
                    shouldDraw = true
                }
            } else {
                shouldDraw = true
            }
        }
    )
}

@Composable
fun AnimatedCounterText(
    value: Int,
    style: androidx.compose.ui.text.TextStyle,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "CounterAnimation"
    ) { targetValue ->
        Text(
            text = String.format(Locale.US, "%,d", targetValue),
            style = style,
            color = color,
            modifier = modifier
        )
    }
}

@Composable
fun AnimatedCounterLongText(
    value: Long,
    style: androidx.compose.ui.text.TextStyle,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "CounterLongAnimation"
    ) { targetValue ->
        Text(
            text = String.format(Locale.US, "%,d", targetValue),
            style = style,
            color = color,
            modifier = modifier
        )
    }
}

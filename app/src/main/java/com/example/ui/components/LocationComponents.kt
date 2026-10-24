package com.example.ui.components

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlaceCategory
import com.example.util.LocationHelper
import kotlinx.coroutines.launch

/**
 * Color-coded badge displaying place name and category
 */
@Composable
fun PlaceBadge(
    placeName: String?,
    placeCategory: String?,
    modifier: Modifier = Modifier,
    latitude: Double? = null,
    longitude: Double? = null,
    onClick: (() -> Unit)? = null
) {
    val category = PlaceCategory.fromId(placeCategory)
    val displayName = if (!placeName.isNullOrBlank()) placeName else category.displayName

    Surface(
        color = category.containerColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, category.primaryColor.copy(alpha = 0.25f)),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("place_badge_${category.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(category.primaryColor)
            )

            Text(
                text = displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                ),
                color = category.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "• ${category.displayName}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                color = category.contentColor.copy(alpha = 0.7f),
                maxLines = 1
            )

            if (latitude != null && longitude != null) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "GPS Coordinates Attached",
                    tint = category.primaryColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Sub-composable: GPS Detection Action Card
 */
@Composable
fun GpsDetectionCard(
    isDetectingLocation: Boolean,
    detectionStatusMessage: String?,
    formattedCoordinates: String,
    onDetectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automatic GPS Detection",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Detects your current temple, ashram, or location name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                FilledTonalButton(
                    onClick = onDetectClick,
                    enabled = !isDetectingLocation,
                    modifier = Modifier.testTag("detect_gps_button")
                ) {
                    if (isDetectingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Detecting...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.MyLocation,
                            contentDescription = "Detect GPS",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Detect GPS", fontSize = 12.sp)
                    }
                }
            }

            if (!detectionStatusMessage.isNullOrBlank()) {
                Text(
                    text = detectionStatusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (formattedCoordinates.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formattedCoordinates,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

/**
 * Sub-composable: Quick Place Name Suggestions
 */
@Composable
fun QuickSuggestionsRow(
    onSelectSuggestion: (name: String, category: PlaceCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Quick Suggestions",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = listOf(
                "Home Shrine" to PlaceCategory.HOME,
                "Local Temple" to PlaceCategory.TEMPLE,
                "Ashram" to PlaceCategory.ASHRAM,
                "Ganga Ghat" to PlaceCategory.NATURE,
                "Nature / Park" to PlaceCategory.NATURE,
                "Travel / Commute" to PlaceCategory.TRAVEL,
                "Pilgrimage Site" to PlaceCategory.PILGRIMAGE
            )
            suggestions.forEach { (name, cat) ->
                SuggestionChip(
                    onClick = { onSelectSuggestion(name, cat) },
                    label = { Text(name, fontSize = 12.sp) }
                )
            }
        }
    }
}

/**
 * Sub-composable: Category Selector Row
 */
@Composable
fun CategorySelectorRow(
    selectedCategory: PlaceCategory,
    onCategorySelected: (PlaceCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Category (Color Code)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlaceCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    label = {
                        Text(
                            text = category.displayName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) category.primaryColor else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(category.primaryColor)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = category.containerColor,
                        selectedLabelColor = category.contentColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) category.primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        borderWidth = if (isSelected) 2.dp else 1.dp
                    )
                )
            }
        }
    }
}

/**
 * Location Selection Dialog with sub-component architecture
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerDialog(
    initialPlaceName: String = "Home",
    initialCategory: String = "Home",
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    title: String = "Select Practice Location",
    confirmButtonText: String = "Save Location",
    onDismiss: () -> Unit,
    onLocationSelected: (name: String, category: String, lat: Double?, lon: Double?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var placeName by remember { mutableStateOf(initialPlaceName) }
    var selectedCategory by remember { mutableStateOf(PlaceCategory.fromId(initialCategory)) }
    var currentLatitude by remember { mutableStateOf(initialLatitude) }
    var currentLongitude by remember { mutableStateOf(initialLongitude) }
    var formattedCoordinates by remember {
        mutableStateOf(
            if (initialLatitude != null && initialLongitude != null) {
                LocationHelper.formatCoordinates(initialLatitude, initialLongitude)
            } else ""
        )
    }

    var isDetectingLocation by remember { mutableStateOf(false) }
    var detectionStatusMessage by remember { mutableStateOf<String?>(null) }

    fun detectGpsLocation() {
        isDetectingLocation = true
        detectionStatusMessage = "Detecting GPS satellite coordinates..."

        coroutineScope.launch {
            try {
                val loc = LocationHelper.getLastKnownLocation(context)
                if (loc != null) {
                    currentLatitude = loc.latitude
                    currentLongitude = loc.longitude
                    formattedCoordinates = LocationHelper.formatCoordinates(loc.latitude, loc.longitude)

                    detectionStatusMessage = "Looking up place details..."
                    val result = LocationHelper.reverseGeocode(context, loc.latitude, loc.longitude)

                    placeName = result.placeName
                    selectedCategory = result.category
                    detectionStatusMessage = "Auto-detected: ${result.placeName} (${result.category.displayName})"
                } else {
                    detectionStatusMessage = "GPS active, but no fix yet. Please make sure Location is enabled on your device."
                    Toast.makeText(context, "Could not acquire GPS fix. Please ensure Location is enabled in Android Settings.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                detectionStatusMessage = "GPS error: ${e.message}"
            } finally {
                isDetectingLocation = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            detectGpsLocation()
        } else {
            detectionStatusMessage = "Location permission was denied. You can still type place name manually."
            Toast.makeText(context, "Location permission denied. Manual entry available.", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = selectedCategory.primaryColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GpsDetectionCard(
                    isDetectingLocation = isDetectingLocation,
                    detectionStatusMessage = detectionStatusMessage,
                    formattedCoordinates = formattedCoordinates,
                    onDetectClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            detectGpsLocation()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = placeName,
                    onValueChange = {
                        placeName = it
                        val detected = PlaceCategory.detectCategory(it)
                        if (it.isNotBlank() && detected != PlaceCategory.HOME) {
                            selectedCategory = detected
                        }
                    },
                    label = { Text("Place Name") },
                    placeholder = { Text("e.g. Home, Kashi Vishwanath, Rishikesh") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = selectedCategory.primaryColor
                        )
                    },
                    trailingIcon = {
                        if (placeName.isNotBlank()) {
                            IconButton(onClick = { placeName = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("place_name_input")
                )

                QuickSuggestionsRow(
                    onSelectSuggestion = { name, cat ->
                        placeName = name
                        selectedCategory = cat
                    }
                )

                CategorySelectorRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "History Badge Preview",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    PlaceBadge(
                        placeName = placeName.ifBlank { selectedCategory.displayName },
                        placeCategory = selectedCategory.id,
                        latitude = currentLatitude,
                        longitude = currentLongitude
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = placeName.trim().ifBlank { selectedCategory.displayName }
                    onLocationSelected(finalName, selectedCategory.id, currentLatitude, currentLongitude)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_location_button")
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

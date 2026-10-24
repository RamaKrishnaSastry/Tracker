package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.PlaceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class LocationResult(
    val placeName: String,
    val category: PlaceCategory,
    val latitude: Double,
    val longitude: Double,
    val formattedCoordinates: String,
    val fullAddress: String
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    /**
     * Get last known or available location from LocationManager without requiring Play Services
     */
    fun getLastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || loc.accuracy < bestLocation.accuracy || loc.time > bestLocation.time) {
                            bestLocation = loc
                        }
                    }
                }
            } catch (e: SecurityException) {
                // Permission revoked or restricted
            } catch (e: Exception) {
                // Provider unavailable
            }
        }
        return bestLocation
    }

    /**
     * Format latitude/longitude into standard N/S/E/W degrees.
     * Note: Uses Locale.US explicitly to ensure '.' is consistently used as the decimal separator
     * for geographic GPS coordinates regardless of device system locale.
     */
    fun formatCoordinates(lat: Double, lon: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return String.format(Locale.US, "%.4f° %s, %.4f° %s", Math.abs(lat), latDir, Math.abs(lon), lonDir)
    }

    /**
     * Reverse geocodes coordinates to a human-readable place name and auto-detects its spiritual category.
     */
    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): LocationResult = withContext(Dispatchers.IO) {
        val formattedCoords = formatCoordinates(latitude, longitude)
        var placeName = formattedCoords
        var fullAddress = formattedCoords
        var detectedCategory = PlaceCategory.HOME

        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                } catch (e: Exception) {
                    null
                }

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val featureName = address.featureName
                    val subLocality = address.subLocality
                    val locality = address.locality
                    val thoroughfare = address.thoroughfare
                    val adminArea = address.adminArea

                    val addressLines = (0..address.maxAddressLineIndex).mapNotNull { address.getAddressLine(it) }
                    fullAddress = addressLines.joinToString(", ").ifBlank { formattedCoords }

                    // Construct a clean, meaningful place label
                    val nameParts = mutableListOf<String>()
                    if (!featureName.isNullOrBlank() && featureName != locality && featureName != subLocality && !featureName.matches(Regex("^[0-9\\-]+$"))) {
                        nameParts.add(featureName)
                    } else if (!thoroughfare.isNullOrBlank()) {
                        nameParts.add(thoroughfare)
                    }

                    if (!subLocality.isNullOrBlank() && !nameParts.contains(subLocality)) {
                        nameParts.add(subLocality)
                    }

                    if (!locality.isNullOrBlank() && !nameParts.contains(locality)) {
                        nameParts.add(locality)
                    } else if (nameParts.isEmpty() && !adminArea.isNullOrBlank()) {
                        nameParts.add(adminArea)
                    }

                    if (nameParts.isNotEmpty()) {
                        placeName = nameParts.take(2).joinToString(", ")
                    } else {
                        placeName = locality ?: adminArea ?: formattedCoords
                    }

                    // Smart Category Detection
                    val searchableText = "$placeName $fullAddress ${address.premises.orEmpty()} ${address.subAdminArea.orEmpty()}"
                    detectedCategory = PlaceCategory.detectCategory(searchableText)
                }
            }
        } catch (e: Exception) {
            // Geocoder service may be offline; fallback to formatted coordinates
        }

        LocationResult(
            placeName = placeName,
            category = detectedCategory,
            latitude = latitude,
            longitude = longitude,
            formattedCoordinates = formattedCoords,
            fullAddress = fullAddress
        )
    }
}

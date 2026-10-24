package com.example.data

import androidx.compose.ui.graphics.Color

enum class PlaceCategory(
    val id: String,
    val displayName: String,
    val defaultKeywords: List<String>
) {
    HOME(
        id = "Home",
        displayName = "Home",
        defaultKeywords = listOf("home", "house", "residence", "apartment", "flat", "room", "shrine", "puja room", "mandir at home", "living room")
    ),
    TEMPLE(
        id = "Temple",
        displayName = "Temple",
        defaultKeywords = listOf("temple", "mandir", "devalaya", "kovil", "shrine", "gopuram", "sanctum", "math", "tirtha", "amman", "shiva", "krishna", "vishnu", "rama", "hanuman", "ganesh", "devi", "durga")
    ),
    ASHRAM(
        id = "Ashram",
        displayName = "Ashram",
        defaultKeywords = listOf("ashram", "matha", "hermitage", "monastery", "sangha", "peeth", "peetham", "guru", "retreat", "ananda", "ramakrishna", "isha", "art of living", "chinmaya")
    ),
    NATURE(
        id = "Nature",
        displayName = "Nature / River",
        defaultKeywords = listOf("river", "ganga", "ghat", "lake", "forest", "tree", "mountain", "hill", "beach", "park", "garden", "nature", "waterfall", "sea", "ocean", "valley")
    ),
    TRAVEL(
        id = "Travel",
        displayName = "Travel / Transit",
        defaultKeywords = listOf("train", "station", "railway", "airport", "flight", "bus", "car", "travel", "commute", "highway", "transit", "metro", "road", "terminal")
    ),
    PILGRIMAGE(
        id = "Pilgrimage",
        displayName = "Pilgrimage",
        defaultKeywords = listOf("pilgrimage", "yatra", "char dham", "kashi", "haridwar", "rishikesh", "ayodhya", "varanasi", "tirupati", "puri", "kedarnath", "badrinath", "rameswaram", "dwarka", "ujjain")
    ),
    WORK(
        id = "Work",
        displayName = "Work / Study",
        defaultKeywords = listOf("work", "office", "desk", "workplace", "business", "campus", "college", "school", "library")
    ),
    OTHER(
        id = "Other",
        displayName = "Other",
        defaultKeywords = emptyList()
    );

    val primaryColor: Color
        @androidx.compose.runtime.Composable
        get() {
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            return when (this) {
                HOME, WORK -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                TEMPLE, PILGRIMAGE -> androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                ASHRAM -> androidx.compose.material3.MaterialTheme.colorScheme.secondary
                NATURE -> if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                TRAVEL -> if (isDark) Color(0xFF81D4FA) else Color(0xFF0277BD)
                OTHER -> androidx.compose.material3.MaterialTheme.colorScheme.outline
            }
        }

    val containerColor: Color
        @androidx.compose.runtime.Composable
        get() {
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            return when (this) {
                HOME, WORK -> androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                TEMPLE, PILGRIMAGE -> androidx.compose.material3.MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                ASHRAM -> androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                NATURE -> if (isDark) Color(0xFF1B382B) else Color(0xFFE8F5E9)
                TRAVEL -> if (isDark) Color(0xFF12344D) else Color(0xFFE1F5FE)
                OTHER -> androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        }

    val contentColor: Color
        @androidx.compose.runtime.Composable
        get() {
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            return when (this) {
                HOME, WORK -> androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                TEMPLE, PILGRIMAGE -> androidx.compose.material3.MaterialTheme.colorScheme.onTertiaryContainer
                ASHRAM -> androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                NATURE -> if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
                TRAVEL -> if (isDark) Color(0xFFB3E5FC) else Color(0xFF01579B)
                OTHER -> androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            }
        }

    companion object {
        fun fromId(id: String?): PlaceCategory {
            if (id.isNullOrBlank()) return HOME
            return entries.find { it.id.equals(id, ignoreCase = true) || it.displayName.equals(id, ignoreCase = true) } ?: OTHER
        }

        fun detectCategory(text: String): PlaceCategory {
            if (text.isBlank()) return HOME
            val lower = text.lowercase()

            // 1. Check for explicit primary category indicators (highest priority)
            val explicitTempleKeywords = listOf("temple", "mandir", "devalaya", "kovil", "gopuram", "sanctum", "tirtha")
            val explicitAshramKeywords = listOf("ashram", "matha", "hermitage", "monastery", "sangha", "peeth")
            val explicitNatureKeywords = listOf("river", "ganga", "ghat", "lake", "waterfall", "forest", "mountain")
            val explicitTravelKeywords = listOf("airport", "flight", "railway", "train station", "metro", "bus stand", "terminal")
            val explicitPilgrimageKeywords = listOf("pilgrimage", "yatra", "char dham", "kashi", "haridwar", "rishikesh", "ayodhya", "varanasi", "tirupati", "puri", "kedarnath", "badrinath")

            if (explicitTempleKeywords.any { lower.contains(it) }) return TEMPLE
            if (explicitAshramKeywords.any { lower.contains(it) }) return ASHRAM
            if (explicitNatureKeywords.any { lower.contains(it) }) return NATURE
            if (explicitTravelKeywords.any { lower.contains(it) }) return TRAVEL
            if (explicitPilgrimageKeywords.any { lower.contains(it) }) return PILGRIMAGE

            // 2. Check for locality/transit road suffixes (e.g., "Krishna Nagar Road", "Rama Layout")
            val roadLocalitySuffixes = listOf("road", "rd", "street", "st", "nagar", "colony", "layout", "pally", "marg", "chowk", "cross", "stage", "extension", "drive", "avenue", "lane", "path")
            val hasLocalitySuffix = roadLocalitySuffixes.any { suffix ->
                lower.split(" ", "-", ",").contains(suffix) || lower.endsWith(" $suffix")
            }

            if (hasLocalitySuffix) {
                // If it's a street or neighborhood address without explicit temple/ashram words, default to Travel/Commute or Home
                return if (lower.contains("station") || lower.contains("transit") || lower.contains("highway") || lower.contains("road") || lower.contains("expressway")) TRAVEL else HOME
            }

            // 3. Fallback matching across category default keywords
            for (category in entries) {
                if (category.defaultKeywords.any { keyword -> lower.contains(keyword) }) {
                    return category
                }
            }

            return HOME
        }
    }
}

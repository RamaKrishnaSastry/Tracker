package com.example.data

import androidx.compose.ui.graphics.Color

enum class PlaceCategory(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val containerColor: Color,
    val contentColor: Color,
    val defaultKeywords: List<String>
) {
    HOME(
        id = "Home",
        displayName = "Home",
        primaryColor = Color(0xFF5B73A8),       // Serene Slate Blue
        containerColor = Color(0xFFEEF2F9),
        contentColor = Color(0xFF263E70),
        defaultKeywords = listOf("home", "house", "residence", "apartment", "flat", "room", "shrine", "puja room", "mandir at home", "living room")
    ),
    TEMPLE(
        id = "Temple",
        displayName = "Temple",
        primaryColor = Color(0xFFC85A17),       // Sacred Deep Ochre / Terracotta
        containerColor = Color(0xFFFDF3EC),
        contentColor = Color(0xFF7E3206),
        defaultKeywords = listOf("temple", "mandir", "devalaya", "kovil", "shrine", "gopuram", "sanctum", "math", "tirtha", "amman", "shiva", "krishna", "vishnu", "rama", "hanuman", "ganesh", "devi", "durga")
    ),
    ASHRAM(
        id = "Ashram",
        displayName = "Ashram",
        primaryColor = Color(0xFF8D6E63),       // Warm Earthy Sandalwood
        containerColor = Color(0xFFF7F3F1),
        contentColor = Color(0xFF4E342E),
        defaultKeywords = listOf("ashram", "matha", "hermitage", "monastery", "sangha", "peeth", "peetham", "guru", "retreat", "ananda", "ramakrishna", "isha", "art of living", "chinmaya")
    ),
    NATURE(
        id = "Nature",
        displayName = "Nature / River",
        primaryColor = Color(0xFF388E67),       // Sage / Holy Basil Green
        containerColor = Color(0xFFEDF7F2),
        contentColor = Color(0xFF1B5338),
        defaultKeywords = listOf("river", "ganga", "ghat", "lake", "forest", "tree", "mountain", "hill", "beach", "park", "garden", "nature", "waterfall", "sea", "ocean", "valley")
    ),
    TRAVEL(
        id = "Travel",
        displayName = "Travel / Transit",
        primaryColor = Color(0xFF0288D1),       // Calm Ocean Blue
        containerColor = Color(0xFFE8F5FC),
        contentColor = Color(0xFF01579B),
        defaultKeywords = listOf("train", "station", "railway", "airport", "flight", "bus", "car", "travel", "commute", "highway", "transit", "metro", "road", "terminal")
    ),
    PILGRIMAGE(
        id = "Pilgrimage",
        displayName = "Pilgrimage",
        primaryColor = Color(0xFFB04A2F),       // Rudraksha / Kumkum
        containerColor = Color(0xFFFBF0EC),
        contentColor = Color(0xFF6E2310),
        defaultKeywords = listOf("pilgrimage", "yatra", "char dham", "kashi", "haridwar", "rishikesh", "ayodhya", "varanasi", "tirupati", "puri", "kedarnath", "badrinath", "rameswaram", "dwarka", "ujjain")
    ),
    WORK(
        id = "Work",
        displayName = "Work / Study",
        primaryColor = Color(0xFF6750A4),       // Geometric Primary Purple / Indigo
        containerColor = Color(0xFFF3EDF7),
        contentColor = Color(0xFF381E72),
        defaultKeywords = listOf("work", "office", "desk", "workplace", "business", "campus", "college", "school", "library")
    ),
    OTHER(
        id = "Other",
        displayName = "Other",
        primaryColor = Color(0xFF7D5260),       // Geometric Tertiary Mauve
        containerColor = Color(0xFFF8EFF1),
        contentColor = Color(0xFF492532),
        defaultKeywords = emptyList()
    );

    companion object {
        fun fromId(id: String?): PlaceCategory {
            if (id.isNullOrBlank()) return HOME
            return entries.find { it.id.equals(id, ignoreCase = true) || it.displayName.equals(id, ignoreCase = true) } ?: OTHER
        }

        fun detectCategory(text: String): PlaceCategory {
            val lower = text.lowercase()
            for (category in entries) {
                if (category.defaultKeywords.any { keyword -> lower.contains(keyword) }) {
                    return category
                }
            }
            return HOME
        }
    }
}

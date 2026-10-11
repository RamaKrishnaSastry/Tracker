# Location-Tagged Japa with GPS Detection & Color-Coded History

Track and visualize where spiritual practice takes place. Chanting sessions and daily japa records are tagged with both a **Place Name** and **Category** (e.g., Home, Temple, Ashram, Nature, Travel, Pilgrimage), complete with on-demand **GPS auto-detection**, reverse-geocoding, smart category classification, and rich **color-coded badges & filtering** in practice history.

---

## User Preferences & Clarifications Incorporated

1. **GPS Detection Timing**:
   - On-demand trigger: A dedicated "Detect GPS / Use Current Location" button in the location picker dialog and Today screen.
   - Graceful runtime permission workflow (`ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`) with rationale and fallback to manual text entry.

2. **Smart Category Classification**:
   - Automatic classification based on reverse-geocoded place names, feature names, and address components:
     - *Temple*: "temple", "mandir", "devalaya", "kovil", "shrine"
     - *Ashram*: "ashram", "matha", "hermitage", "monastery", "sangha"
     - *Nature*: "river", "ganga", "ghat", "lake", "forest", "garden", "mountain", "hill", "beach", "park"
     - *Travel*: "train", "station", "railway", "airport", "bus", "flight", "highway", "metro"
     - *Home*: default or matched home address
     - *Other / Sacred Site / Pilgrimage*: user selectable or customized.
   - The user can always override or fine-tune both the place name and category with one tap before saving.

3. **Data Stored Per Japa Entry**:
   - `placeName: String` (e.g., "Kashi Vishwanath Temple", "Home Shrine", "Ganga Ghat, Rishikesh")
   - `placeCategory: String` (e.g., "Temple", "Home", "Ashram", "Nature", "Travel", "Other")
   - `latitude: Double?` & `longitude: Double?` (exact or coarse GPS coordinates when detected)

---

## Architecture & Data Flow

```
[ GPS Hardware / LocationManager / FusedLocation ]
                     │
                     ▼ (on-demand button tap)
          [ Android Geocoder ]
                     │
                     ▼ (reverse geocode)
   [ Smart Category Classifier ] ──► (suggests "Temple", "Nature", etc.)
                     │
                     ▼
          [ Location Selection Dialog / Chip ]
          (Place Name, Category Chips, Coords)
                     │
                     ▼
     ┌───────────────────────────────┐
     │ Room Database (v6 Migration)  │
     ├───────────────────────────────┤
     │ JapaEntry                     │
     │  - placeName: String?         │
     │  - placeCategory: String?     │
     │  - latitude: Double?          │
     │  - longitude: Double?         │
     │ JapaSession                   │
     │  - placeName: String?         │
     │  - placeCategory: String?     │
     │  - latitude: Double?          │
     │  - longitude: Double?         │
     │ CustomPracticeEntry           │
     │  - placeName: String?         │
     │  - placeCategory: String?     │
     │  - latitude: Double?          │
     │  - longitude: Double?         │
     └───────────────┬───────────────┘
                     │
                     ▼
       ┌───────────────────────────┐
       │ History Screen UI         │
       ├───────────────────────────┤
       │ • Color-coded Place Badge │
       │ • Location Filter Chips   │
       │ • Location Breakdown Card │
       │ • Quick Edit Place Dialog │
       └───────────────────────────┘
```

---

## Detailed Implementation Steps

### 1. Android Manifest & Permissions
- Add `<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />` and `<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />` to `AndroidManifest.xml`.
- Ensure non-blocking runtime permission request launcher in Compose using `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())`.

### 2. Location Helper & Geocoding Service (`util/LocationHelper.kt`)
- `getCurrentCoordinates(context: Context, callback: (Location?) -> Unit)` using `LocationManager` or `FusedLocationProviderClient`.
- `reverseGeocode(context: Context, lat: Double, lon: Double): PlaceInfo` returning address name, locality, feature name, and coordinates.
- `classifyPlaceCategory(name: String, address: String): PlaceCategory`:
  - Keyword evaluation against Sanskrit/Indic & English spiritual and everyday location markers (Temple, Ashram, Nature/Ghat, Travel, Home, Work, Other).

### 3. Place Category Model & Visual Palette (`model/PlaceCategory.kt`)
- Defined enum / data object with distinctive color coding and icons:
  - **Temple** (`#E65100` / Sacred Saffron & Orange) • Icon: `TempleHindu` / `Place`
  - **Ashram** (`#8D6E63` / Terracotta & Earth) • Icon: `Spa` / `SelfImprovement`
  - **Nature / River** (`#2E7D32` / Forest Emerald & Teals) • Icon: `Park` / `WaterDrop`
  - **Home** (`#1565C0` / Serene Cobalt & Indigo) • Icon: `Home`
  - **Travel** (`#00838F` / Cyan & Sky) • Icon: `DirectionsSubway` / `Flight`
  - **Pilgrimage / Tirtha** (`#D84315` / Deep Ochre) • Icon: `Stars` / `LocationCity`
  - **Other** (`#6A1B9A` / Lavender & Purple) • Icon: `Bookmark`

### 4. Room Database Schema Update & Migration (`AppDatabase.kt`)
- Add schema migration (database version bump):
  - `ALTER TABLE japa_entries ADD COLUMN placeName TEXT`
  - `ALTER TABLE japa_entries ADD COLUMN placeCategory TEXT`
  - `ALTER TABLE japa_entries ADD COLUMN latitude REAL`
  - `ALTER TABLE japa_entries ADD COLUMN longitude REAL`
  - Corresponding columns in `japa_sessions` and `custom_practice_entries`.
- Default value fallback to `"Home"` and `"Home"` category for existing entries so past data displays seamlessly.

### 5. Today Screen Experience (`TodayScreen.kt`)
- **Active Location Indicator**:
  - Compact header chip next to date or under the practice selector displaying current place (e.g. `[📍 Home (Home)]` or `[📍 Kashi Vishwanath (Temple)]`).
  - Tapping opens the **Location Selector Sheet / Dialog**.
- **Location Selector Sheet / Dialog**:
  - "Detect GPS" button (triggers permission check and geocoding).
  - Quick Category filter chips (Home, Temple, Ashram, Nature, Travel, Other).
  - Place Name text field with recent places suggestion chips.
  - Coordinate badge when GPS was used (`30.0869° N, 78.2676° E`).
  - "Set as Active Location" for current session and subsequent logs.

### 6. History Screen Enhancements (`HistoryScreen.kt`)
- **Color-Coded Badges**:
  - Each day's unified card and individual session expandable rows display a rounded chip with the category background color, place icon, and place name.
- **Location Filter**:
  - Filter bar under search: filter by category ("All", "Temple", "Home", "Nature", "Ashram", "Travel") or search by place name.
- **Location Breakdown Summary**:
  - Expandable or summary card in History showing practice volume grouped by location:
    - e.g., "Temple: 2,160 chants (45%)", "Home: 1,800 chants (38%)", "Nature: 800 chants (17%)".
- **Edit Location for Past Dates**:
  - In the Day Detail dialog / options menu, allow updating or fixing the location tag of any past day's entry.

---

## Verification Plan

1. **Compilation**: Run `compile_applet` to ensure Room migrations, permission handlers, and Compose views build cleanly without missing symbols.
2. **Permission & GPS Flow**:
   - Test "Detect GPS" button behavior when permission is requested, granted, or denied.
   - Verify fallback to manual place name and category selection if GPS is offline or denied.
3. **Smart Category Classification**:
   - Verify that names like "Somnath Temple" auto-select *Temple*, "Rishikesh Ganga Ghat" auto-selects *Nature*, etc.
4. **History & Persistence**:
   - Log japa with different locations and categories.
   - Verify color-coded pills appear in History items with their distinct palette.
   - Verify location filtering and statistics breakdown work accurately.

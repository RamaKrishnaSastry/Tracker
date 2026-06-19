# Gayatri Japa Tracker (Android Native)

A simple, highly respectful, private, and distraction-free offline-first application for tracking daily Gayatri Japa counts performed during *Sandhyavandanam*. 

Designed strictly for long-term personal use, this application operates completely locally on your Android device and backs up your data silently to your private Google Drive App Data folder, requiring no external databases, server instances, or maintenance costs.

---

## 📖 Table of Contents
1. [Core Design Principles](#-core-design-principles)
2. [Technology Stack](#-technology-stack)
3. [Architecture & Sync Strategy](#-architecture---sync-strategy)
4. [Onboarding & Screen Flow](#-onboarding--screen-flow)
5. [Google Cloud Console Setup (MANDATORY for Online Backup)](#-google-cloud-console-setup)
6. [Local Testing & Fallbacks](#-local-testing--fallbacks)

---

## 🌅 Core Design Principles

- **Purity & Simplicity**: Zero notifications noise, zero distraction. It is designed to act as a peaceful digital companion.
- **Privacy & Safety**: No analytics, trackers, or commercial advertising SDKs are integrated.
- **Local Ownership**: You own 100% of your data. Synchronizations occur inside your hidden, secure Google Drive `appDataFolder` where backup files remain isolated from standard web files.

---

## 🛠️ Technology Stack

- **Framework**: Native Android (Kotlin & Jetpack Compose) running Material 3 components.
- **Theming**: Meditative *Saffron Sunrise* and *Midnight Warmth Sky* custom palettes.
- **Local Database**: Android Jetpack Room with SQLite under Flow bindings for instant reactive updates.
- **Synchronization**: Google Play Services Sign-In and silent, background Google Drive REST calls via lightweight OkHttp requests.
- **Alarm Engine**: Offline-safe Broadcast Alarms rescheduling with wake-locks for perfect calendar tracking during Sunset, Solar Noon, and Sunrise Sandhyas.

---

## 📂 Architecture & Sync Strategy

The app utilizes a single JSON file backed up in Google Drive: `/appDataFolder/gayatri_japa_data.json`

### Conflict Resolution (Last-Write-Wins)
When network connectivity is established, the application compares local records with cloud versions using detailed ISO 8601 UTC `updatedAt` timestamps down to the exact millisecond; the newest write is retained, preventing data duplication or overwrite losses during offline transitions.

### JSON Database Schema
```json
{
  "version": 1,
  "lastSynced": "2026-06-19T18:30:00Z",
  "initialLifetimeCount": 50000,
  "settings": {
    "themeMode": "system"
  },
  "entries": {
    "2026-06-19": {
      "morning": 108,
      "afternoon": 54,
      "evening": 108,
      "updatedAt": "2026-06-19T18:30:00Z"
    }
  }
}
```

---

## 📱 Onboarding & Screen Flow

1. **Onboarding Portal**: A golden meditative landscape welcomes you. Connect your Google account securely or select **Skip** to run entirely offline inside your secure local device.
2. **Prior Cumulative Count Setup**: Set any completed prior count (e.g., 10,000, 50,000, or 100,008) as `initialLifetimeCount`. This can be adjusted at any time inside Settings.
3. **Core Tabs**:
   - **Today**: View real-time totals and manage Morning, Noon, and Evening prayer slots. Supports single increments and rapid incremental suggestions (**+10**, **+24**, **+28**, **+54**, **+108**).
   - **History**: An elegant checklist detailing all records in reverse chronological order with date searching capability.
   - **Settings**: Turn on/off daily Sandhya alerts, set alert times with a custom scroll dial, manage accounts, and initiate manual synchronization backup runs.

---

## 🔑 Google Cloud Console Setup

To complete the end-to-end integration of Google Sign-In and Silent Drive Backups, **you must link your Android App to a Google Cloud Console Project**:

### 1. Register a Google Cloud Project
1. Open the [Google Cloud Console](https://console.cloud.google.com/).
2. Select or create a new project named **Gayatri Japa Tracker**.

### 2. Enable Google Drive APIs
1. Navigate to **APIs & Services** > **Library**.
2. Search for **Google Drive API** and click **Enable**.

### 3. Configure OAuth Consent Screen
1. Go to **APIs & Services** > **OAuth Consent Screen**:
   - Set User Type to **External** (or Internal if G-Suite).
   - Set App Name as `Gayatri Japa Tracker`.
   - Set support contact and developer contact emails.
2. Add the specific required OAuth Scope:
   - `https://www.googleapis.com/auth/drive.appdata` (Only view and manage its own private configuration data, **never** request full disk storage permissions!).

### 4. Setup Android OAuth Client ID
1. Navigate to **APIs & Services** > **Credentials**.
2. Click **+ Create Credentials** > **OAuth Client ID**.
3. Set the Application type to **Android**.
4. Configure package identification variables:
   - **Package name**: `com.aistudio.gayatrijapa.qywdzs`
   - **SHA-1 certificate fingerprint**: Enter the SHA-1 of your signing keystore.
     * To find your debug certificate SHA-1, execute the terminal command:
       `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android` or inspect Gradle build outputs.
5. Save the generated credentials. Android Play Services Auth handles token distribution automatically based on package matching.

---

## 🚫 Local Testing & Fallbacks

If Google Sign-In is skipped or if the Google Cloud SDK fails validation locally:
- **Full Offline Capabilities**: The app will operate perfectly using Room SQLite databases locally.
- **Local settings**: Prior counts and light/dark theme preferences are stored directly on-device in shared preferences.
- **Easy connection**: You can transition to online sync at any time inside the Settings tab by clicking **Connect Google Drive Backup**.

package com.example.api

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.JapaEntry
import com.example.data.PreferencesManager
import com.example.ui.theme.getMantraColor
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

object GayatriWidgetHelper {
    private const val TAG = "GayatriWidgetHelper"
    const val ACTION_QUICK_ADD = "com.example.GAYATRI_WIDGET_QUICK_ADD"
    const val ACTION_DIGIT_PRESS = "com.example.GAYATRI_WIDGET_DIGIT_PRESS"
    const val ACTION_DIGIT_CLEAR = "com.example.GAYATRI_WIDGET_DIGIT_CLEAR"
    const val ACTION_DIGIT_ADD = "com.example.GAYATRI_WIDGET_DIGIT_ADD"
    const val ACTION_SWITCH_MODE = "com.example.GAYATRI_WIDGET_SWITCH_MODE"
    const val ACTION_WIDGET_PASS = "com.example.GAYATRI_WIDGET_PASS"
    const val ACTION_WIDGET_RESET = "com.example.GAYATRI_WIDGET_RESET"

    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        layoutId: Int,
        providerClass: Class<*>
    ) {
        val views = RemoteViews(context.packageName, layoutId)

        // 1. Fetch current data
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val db = AppDatabase.getDatabase(context)
        val dao = db.japaDao()
        val prefs = PreferencesManager(context)

        val entry = runBlocking { dao.getEntryByDate(todayStr) }
        val allEntries = runBlocking { dao.getAllEntries() }
        val lifetimeSum = allEntries.sumOf { it.dailyTotal.toLong() }
        val finalLifetimeCount = prefs.getInitialLifetimeCount() + lifetimeSum
        
        // Universal color
        val universalColorName = prefs.getUniversalColor()
        val accentColor = getMantraColor(universalColorName).toArgb()

        // 2. Identify priority incomplete Sandhya based on TIME
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        var targetLabel = ""
        var targetType = ""
        var currentCount = 0
        var allComplete = false

        if (entry == null) {
             when {
                hour < 10 -> { targetLabel = "Morning"; targetType = "morning" }
                hour < 15 -> { targetLabel = "Noon"; targetType = "afternoon" }
                else -> { targetLabel = "Evening"; targetType = "evening" }
            }
        } else {
            if (hour < 10 && entry.pratahSandhyaCount == 0) {
                targetLabel = "Morning"; targetType = "morning"
            } else if (hour in 10..15 && entry.madhyahnikaSandhyaCount == 0) {
                targetLabel = "Noon"; targetType = "afternoon"
            } else if (hour > 15 && entry.sayamSandhyaCount == 0) {
                targetLabel = "Evening"; targetType = "evening"
            } 
            else if (entry.pratahSandhyaCount == 0) {
                 targetLabel = "Catch-up: Morning"; targetType = "morning"
            } else if (entry.madhyahnikaSandhyaCount == 0) {
                 targetLabel = "Catch-up: Noon"; targetType = "afternoon"
            } else if (entry.sayamSandhyaCount == 0) {
                 targetLabel = "Catch-up: Evening"; targetType = "evening"
            } else {
                allComplete = true
                targetLabel = "Done for Day!"
                targetType = "punas_evening"
                currentCount = entry.dailyTotal
            }
            
            if (!allComplete) {
                currentCount = when(targetType) {
                    "morning" -> entry.pratahSandhyaCount
                    "afternoon" -> entry.madhyahnikaSandhyaCount
                    "evening" -> entry.sayamSandhyaCount
                    else -> 0
                }
            }
        }

        // 3. Update Text and Progress
        views.setTextViewText(com.example.R.id.widget_label, targetLabel)
        views.setTextViewText(com.example.R.id.widget_count_value, currentCount.toString())
        views.setTextViewText(com.example.R.id.widget_lbl_lifetime, "LIFETIME: $finalLifetimeCount")
        
        // Colors
        views.setTextColor(com.example.R.id.widget_label, accentColor)
        
        // Progress bar
        val progress = ((currentCount.toFloat() / 108f) * 100).toInt().coerceIn(0, 100)
        views.setProgressBar(com.example.R.id.widget_progress, 100, progress, false)

        if (allComplete) {
            views.setTextViewText(com.example.R.id.widget_sub_label, "Goal Reached 🙏")
        } else {
            val remaining = if (currentCount < 108) 108 - currentCount else 0
            views.setTextViewText(com.example.R.id.widget_sub_label, "$remaining to 108")
        }

        // 4. Configure Action Buttons
        val sp = context.getSharedPreferences("gayatri_japa_prefs", Context.MODE_PRIVATE)
        val isManualMode = sp.getBoolean("widget_manual_mode_$widgetId", false)
        val currentDigits = sp.getString("widget_digits_$widgetId", "") ?: ""

        if (layoutId == com.example.R.layout.widget_layout_large || layoutId == com.example.R.layout.widget_layout_medium) {
            if (isManualMode) {
                views.setDisplayedChild(com.example.R.id.widget_flipper, 1) // Digit Pad
                val displayStr = if (layoutId == com.example.R.layout.widget_layout_large) {
                    "Enter: ${if (currentDigits.isEmpty()) "0" else currentDigits}"
                } else {
                    if (currentDigits.isEmpty()) "0" else currentDigits
                }
                views.setTextViewText(com.example.R.id.widget_input_display, displayStr)
            } else {
                views.setDisplayedChild(com.example.R.id.widget_flipper, 0) // Quick Actions
            }

            // Numpad setup
            (0..9).forEach { digit ->
                val resId = context.resources.getIdentifier("btn_digit_$digit", "id", context.packageName)
                if (resId != 0) {
                    val dIntent = Intent(context, providerClass).apply {
                        action = ACTION_DIGIT_PRESS
                        data = android.net.Uri.parse("gayatri://widget/$widgetId/digit/$digit")
                        putExtra("EXTRA_DIGIT", digit.toString())
                        putExtra("EXTRA_WIDGET_ID", widgetId)
                    }
                    val dp = PendingIntent.getBroadcast(context, 0, dIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(resId, dp)
                }
            }

            setupSimpleAction(views, context, com.example.R.id.btn_digit_clear, ACTION_DIGIT_CLEAR, widgetId, providerClass)
            
            // Back buttons
            setupSimpleAction(views, context, com.example.R.id.btn_input_back, ACTION_SWITCH_MODE, widgetId, providerClass, false)
            setupSimpleAction(views, context, com.example.R.id.btn_input_back_medium, ACTION_SWITCH_MODE, widgetId, providerClass, false)
            
            val addIntent = Intent(context, providerClass).apply {
                action = ACTION_DIGIT_ADD
                data = android.net.Uri.parse("gayatri://widget/$widgetId/action/ADD")
                putExtra("EXTRA_TYPE", targetType)
                putExtra("EXTRA_WIDGET_ID", widgetId)
            }
            val ap = PendingIntent.getBroadcast(context, 0, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(com.example.R.id.btn_digit_add, ap)

            // Pencil toggles manual mode
            setupSimpleAction(views, context, com.example.R.id.btn_open_app_large, ACTION_SWITCH_MODE, widgetId, providerClass, true)
            setupSimpleAction(views, context, com.example.R.id.btn_open_app_medium, ACTION_SWITCH_MODE, widgetId, providerClass, true)

            // Pass and Reset actions for current target
            val pIntent = Intent(context, providerClass).apply {
                action = ACTION_WIDGET_PASS
                data = android.net.Uri.parse("gayatri://widget/$widgetId/pass/$targetType")
                putExtra("EXTRA_TYPE", targetType)
                putExtra("EXTRA_WIDGET_ID", widgetId)
            }
            val pp = PendingIntent.getBroadcast(context, 0, pIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(com.example.R.id.btn_widget_pass_large, pp)
            views.setOnClickPendingIntent(com.example.R.id.btn_widget_pass_medium, pp)

            val rIntent = Intent(context, providerClass).apply {
                action = ACTION_WIDGET_RESET
                data = android.net.Uri.parse("gayatri://widget/$widgetId/reset/$targetType")
                putExtra("EXTRA_TYPE", targetType)
                putExtra("EXTRA_WIDGET_ID", widgetId)
            }
            val rp = PendingIntent.getBroadcast(context, 0, rIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(com.example.R.id.btn_widget_clear_large, rp)
            views.setOnClickPendingIntent(com.example.R.id.btn_widget_clear_medium, rp)
        }
        setupButton(views, context, com.example.R.id.btn_quick_10, 10, targetType, widgetId, providerClass)
        setupButton(views, context, com.example.R.id.btn_quick_24, 24, targetType, widgetId, providerClass)
        setupButton(views, context, com.example.R.id.btn_quick_28, 28, targetType, widgetId, providerClass)
        setupButton(views, context, com.example.R.id.btn_quick_54, 54, targetType, widgetId, providerClass)
        setupButton(views, context, com.example.R.id.btn_quick_108, 108, targetType, widgetId, providerClass)

        // Show/Hide Punascharana shortcut on large layout
        if (allComplete) {
            views.setViewVisibility(com.example.R.id.btn_punas_108, View.VISIBLE)
            setupButton(views, context, com.example.R.id.btn_punas_108, 108, "punas_evening", widgetId, providerClass)
        } else {
            views.setViewVisibility(com.example.R.id.btn_punas_108, View.GONE)
        }

        // Open app intent
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            data = android.net.Uri.parse("gayatri://widget/$widgetId/open")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPending = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Assigning to specific views ensuring no collisions with pencil icons
        views.setOnClickPendingIntent(com.example.R.id.widget_root, openPending)
        
        if (layoutId == com.example.R.layout.widget_layout_large) {
            // In Large, btn_open_app is the static "OPEN" button
            views.setOnClickPendingIntent(com.example.R.id.btn_open_app, openPending)
        } else if (layoutId == com.example.R.layout.widget_layout_small) {
             // In Small, we don't have ViewFlipper yet, so pencil icon opens app
            views.setOnClickPendingIntent(com.example.R.id.btn_open_app_small, openPending)
        }


        // Sync updates
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun setupButton(
        views: RemoteViews,
        context: Context,
        btnId: Int,
        amount: Int,
        type: String,
        widgetId: Int,
        providerClass: Class<*>
    ) {
        val intent = Intent(context, providerClass).apply {
            action = ACTION_QUICK_ADD
            data = android.net.Uri.parse("gayatri://widget/$widgetId/button/$btnId/amount/$amount")
            putExtra("EXTRA_AMOUNT", amount)
            putExtra("EXTRA_TYPE", type)
            putExtra("EXTRA_WIDGET_ID", widgetId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(btnId, pendingIntent)
    }

    private fun setupSimpleAction(views: RemoteViews, context: Context, resId: Int, actionStr: String, widgetId: Int, providerClass: Class<*>, value: Boolean? = null) {
        val intent = Intent(context, providerClass).apply {
            action = actionStr
            data = android.net.Uri.parse("gayatri://widget/$widgetId/action/$actionStr/res/$resId")
            putExtra("EXTRA_WIDGET_ID", widgetId)
            value?.let { putExtra("EXTRA_VALUE", it) }
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(resId, pendingIntent)
    }

    fun handleReceiverIntent(context: Context, intent: Intent) {
        val sp = context.getSharedPreferences("gayatri_japa_prefs", Context.MODE_PRIVATE)
        val widgetId = intent.getIntExtra("EXTRA_WIDGET_ID", AppWidgetManager.INVALID_APPWIDGET_ID)

        var dataChanged = false
        when (intent.action) {
            ACTION_QUICK_ADD -> {
                val amount = intent.getIntExtra("EXTRA_AMOUNT", 0)
                val type = intent.getStringExtra("EXTRA_TYPE") ?: return
                saveCount(context, amount, type)
                dataChanged = true
            }
            ACTION_DIGIT_PRESS -> {
                if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
                val digit = intent.getStringExtra("EXTRA_DIGIT") ?: ""
                val current = sp.getString("widget_digits_$widgetId", "") ?: ""
                if (current.length < 5) {
                    sp.edit().putString("widget_digits_$widgetId", current + digit).commit()
                }
            }
            ACTION_DIGIT_CLEAR -> {
                if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
                sp.edit().putString("widget_digits_$widgetId", "").commit()
            }
            ACTION_DIGIT_ADD -> {
                if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
                val type = intent.getStringExtra("EXTRA_TYPE") ?: return
                val current = sp.getString("widget_digits_$widgetId", "") ?: ""
                if (current.isNotEmpty()) {
                    val amount = current.toIntOrNull() ?: 0
                    if (amount > 0) {
                        saveCount(context, amount, type)
                        dataChanged = true
                    }
                }
                sp.edit().putString("widget_digits_$widgetId", "").commit()
                sp.edit().putBoolean("widget_manual_mode_$widgetId", false).commit()
            }
            ACTION_SWITCH_MODE -> {
                if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
                val mode = intent.getBooleanExtra("EXTRA_VALUE", false)
                sp.edit().putBoolean("widget_manual_mode_$widgetId", mode).commit()
                if (!mode) {
                    sp.edit().putString("widget_digits_$widgetId", "").commit()
                }
            }
            ACTION_WIDGET_PASS -> {
                val type = intent.getStringExtra("EXTRA_TYPE") ?: return
                setAbsoluteCount(context, -1, type)
                dataChanged = true
            }
            ACTION_WIDGET_RESET -> {
                val type = intent.getStringExtra("EXTRA_TYPE") ?: return
                setAbsoluteCount(context, 0, type)
                dataChanged = true
            }
        }
        
        if (dataChanged) {
            scheduleSync(context)
        }
        
        triggerAllWidgetsUpdate(context)
    }

    private fun scheduleSync(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.japaDao()
        val prefs = PreferencesManager(context)
        val authManager = AuthManager(context)
        val syncService = GoogleDriveSyncService()
        val repository = JapaRepository(context, dao, prefs, authManager, syncService)
        repository.syncWithCloud(isLocalUpdate = true)
    }

    private fun setAbsoluteCount(context: Context, value: Int, type: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val db = AppDatabase.getDatabase(context)
        val dao = db.japaDao()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val timestamp = sdf.format(Date())

        runBlocking {
            var entry = dao.getEntryByDate(todayStr)
            if (entry == null) {
                entry = JapaEntry(
                    date = todayStr,
                    pratahSandhyaCount = 0,
                    madhyahnikaSandhyaCount = 0,
                    sayamSandhyaCount = 0,
                    updatedAt = timestamp
                )
            }

            val updated = when (type) {
                "morning" -> entry.copy(pratahSandhyaCount = value, updatedAt = timestamp)
                "afternoon" -> entry.copy(madhyahnikaSandhyaCount = value, updatedAt = timestamp)
                "evening" -> entry.copy(sayamSandhyaCount = value, updatedAt = timestamp)
                "punas_morning" -> entry.copy(pratahPunascharanaCount = value, updatedAt = timestamp)
                "punas_afternoon" -> entry.copy(madhyahnikaPunascharanaCount = value, updatedAt = timestamp)
                "punas_evening" -> entry.copy(sayamPunascharanaCount = value, updatedAt = timestamp)
                else -> entry
            }
            dao.insertOrUpdate(updated)
        }
    }

    private fun saveCount(context: Context, amount: Int, type: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val db = AppDatabase.getDatabase(context)
        val dao = db.japaDao()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val timestamp = sdf.format(Date())

        runBlocking {
            var entry = dao.getEntryByDate(todayStr)
            if (entry == null) {
                entry = JapaEntry(
                    date = todayStr,
                    pratahSandhyaCount = 0,
                    madhyahnikaSandhyaCount = 0,
                    sayamSandhyaCount = 0,
                    updatedAt = timestamp
                )
            }

            val updated = when (type) {
                "morning" -> entry.copy(pratahSandhyaCount = (if (entry.pratahSandhyaCount == -1) 0 else entry.pratahSandhyaCount) + amount, updatedAt = timestamp)
                "afternoon" -> entry.copy(madhyahnikaSandhyaCount = (if (entry.madhyahnikaSandhyaCount == -1) 0 else entry.madhyahnikaSandhyaCount) + amount, updatedAt = timestamp)
                "evening" -> entry.copy(sayamSandhyaCount = (if (entry.sayamSandhyaCount == -1) 0 else entry.sayamSandhyaCount) + amount, updatedAt = timestamp)
                "punas_morning" -> entry.copy(pratahPunascharanaCount = entry.pratahPunascharanaCount + amount, updatedAt = timestamp)
                "punas_afternoon" -> entry.copy(madhyahnikaPunascharanaCount = entry.madhyahnikaPunascharanaCount + amount, updatedAt = timestamp)
                "punas_evening" -> entry.copy(sayamPunascharanaCount = entry.sayamPunascharanaCount + amount, updatedAt = timestamp)
                else -> entry
            }
            dao.insertOrUpdate(updated)
        }
    }

    fun triggerAllWidgetsUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val smallIds = appWidgetManager.getAppWidgetIds(ComponentName(context, GayatriWidgetProviderSmall::class.java))
        smallIds.forEach { updateWidget(context, appWidgetManager, it, com.example.R.layout.widget_layout_small, GayatriWidgetProviderSmall::class.java) }
        val mediumIds = appWidgetManager.getAppWidgetIds(ComponentName(context, GayatriWidgetProviderMedium::class.java))
        mediumIds.forEach { updateWidget(context, appWidgetManager, it, com.example.R.layout.widget_layout_medium, GayatriWidgetProviderMedium::class.java) }
        val largeIds = appWidgetManager.getAppWidgetIds(ComponentName(context, GayatriWidgetProviderLarge::class.java))
        largeIds.forEach { updateWidget(context, appWidgetManager, it, com.example.R.layout.widget_layout_large, GayatriWidgetProviderLarge::class.java) }
    }
}

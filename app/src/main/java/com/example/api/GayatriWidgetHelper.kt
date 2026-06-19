package com.example.api

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.JapaEntry
import com.example.data.PreferencesManager
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

object GayatriWidgetHelper {
    private const val TAG = "GayatriWidgetHelper"
    const val ACTION_QUICK_ADD = "com.example.GAYATRI_WIDGET_QUICK_ADD"

    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        layoutId: Int,
        providerClass: Class<*>
    ) {
        val views = RemoteViews(context.packageName, layoutId)

        // 1. Fetch current Japa quantities offline safely using local database
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val db = AppDatabase.getDatabase(context)
        val dao = db.japaDao()
        val prefs = PreferencesManager(context)

        val entry = runBlocking { dao.getEntryByDate(todayStr) }
        val allEntries = runBlocking { dao.getAllEntries() }
        val lifetimeSum = allEntries.sumOf { it.dailyTotal.toLong() }
        val finalLifetimeCount = prefs.getInitialLifetimeCount() + lifetimeSum

        // 2. Identify priority incomplete Sandhya
        var targetLabel = ""
        var targetType = ""
        var currentCount = 0
        var allComplete = false

        if (entry == null) {
            targetLabel = "Pratah Sandhya"
            targetType = "morning"
            currentCount = 0
        } else {
            if (entry.pratahSandhyaCount == 0) {
                targetLabel = "Pratah Sandhya"
                targetType = "morning"
                currentCount = 0
            } else if (entry.madhyahnikaSandhyaCount == 0) {
                targetLabel = "Madhyahnika"
                targetType = "afternoon"
                currentCount = entry.madhyahnikaSandhyaCount
            } else if (entry.sayamSandhyaCount == 0) {
                targetLabel = "Sayam Sandhya"
                targetType = "evening"
                currentCount = entry.sayamSandhyaCount
            } else {
                // All complete!
                allComplete = true
                targetLabel = "All Complete!"
                targetType = "punas_evening" // default
                currentCount = entry.dailyTotal
            }
        }

        // 3. Update Text representations
        views.setTextViewText(com.example.R.id.widget_label, targetLabel)
        
        if (allComplete) {
            views.setTextViewText(com.example.R.id.widget_count_value, currentCount.toString())
            // Under large layout, adjust labels
            views.setTextViewText(com.example.R.id.widget_sub_label, "All Sandhyas Completed!")
        } else {
            views.setTextViewText(com.example.R.id.widget_count_value, currentCount.toString())
            views.setTextViewText(com.example.R.id.widget_sub_label, "Sandhya is currently incomplete.")
        }

        // Lifetime total representation
        views.setTextViewText(com.example.R.id.widget_lbl_lifetime, "LIFETIME: $finalLifetimeCount")

        // 4. Configure Action Buttons Intents cleanly
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPending = PendingIntent.getActivity(
            context,
            widgetId * 10,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(com.example.R.id.btn_open_app, openPending)
        views.setOnClickPendingIntent(com.example.R.id.widget_root, openPending)

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
            putExtra("EXTRA_AMOUNT", amount)
            putExtra("EXTRA_TYPE", type)
            putExtra("EXTRA_WIDGET_ID", widgetId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId * 100 + btnId + amount,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(btnId, pendingIntent)
    }
}

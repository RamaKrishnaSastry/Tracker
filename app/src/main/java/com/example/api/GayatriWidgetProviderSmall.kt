package com.example.api

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.R
import com.example.data.AppDatabase
import com.example.data.JapaEntry
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class GayatriWidgetProviderSmall : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            GayatriWidgetHelper.updateWidget(
                context,
                appWidgetManager,
                appWidgetId,
                R.layout.widget_layout_small,
                GayatriWidgetProviderSmall::class.java
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        GayatriWidgetHelper.handleReceiverIntent(context, intent)
    }

    private fun triggerAllWidgetsUpdate(context: Context) {
        // Now handled inside GayatriWidgetHelper.handleReceiverIntent
    }
}

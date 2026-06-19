package com.example.api

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.R
import com.example.data.AppDatabase
import com.example.data.JapaEntry
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class GayatriWidgetProviderMedium : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            GayatriWidgetHelper.updateWidget(
                context,
                appWidgetManager,
                appWidgetId,
                R.layout.widget_layout_medium,
                GayatriWidgetProviderMedium::class.java
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == GayatriWidgetHelper.ACTION_QUICK_ADD) {
            val amount = intent.getIntExtra("EXTRA_AMOUNT", 0)
            val type = intent.getStringExtra("EXTRA_TYPE") ?: return

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
                        pratahPunascharanaCount = 0,
                        madhyahnikaPunascharanaCount = 0,
                        sayamPunascharanaCount = 0,
                        updatedAt = timestamp
                    )
                }

                val updated = when (type) {
                    "morning" -> entry.copy(pratahSandhyaCount = entry.pratahSandhyaCount + amount, updatedAt = timestamp)
                    "afternoon" -> entry.copy(madhyahnikaSandhyaCount = entry.madhyahnikaSandhyaCount + amount, updatedAt = timestamp)
                    "evening" -> entry.copy(sayamSandhyaCount = entry.sayamSandhyaCount + amount, updatedAt = timestamp)
                    "punas_morning" -> entry.copy(pratahPunascharanaCount = entry.pratahPunascharanaCount + amount, updatedAt = timestamp)
                    "punas_afternoon" -> entry.copy(madhyahnikaPunascharanaCount = entry.madhyahnikaPunascharanaCount + amount, updatedAt = timestamp)
                    "punas_evening" -> entry.copy(sayamPunascharanaCount = entry.sayamPunascharanaCount + amount, updatedAt = timestamp)
                    else -> entry
                }

                dao.insertOrUpdate(updated)
            }

            triggerAllWidgetsUpdate(context)
        }
    }

    private fun triggerAllWidgetsUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        
        val smallIds = appWidgetManager.getAppWidgetIds(ComponentName(context, GayatriWidgetProviderSmall::class.java))
        for (id in smallIds) {
            GayatriWidgetHelper.updateWidget(context, appWidgetManager, id, R.layout.widget_layout_small, GayatriWidgetProviderSmall::class.java)
        }

        val mediumIds = appWidgetManager.getAppWidgetIds(ComponentName(context, GayatriWidgetProviderMedium::class.java))
        for (id in mediumIds) {
            GayatriWidgetHelper.updateWidget(context, appWidgetManager, id, R.layout.widget_layout_medium, GayatriWidgetProviderMedium::class.java)
        }

        val largeIds = appWidgetManager.getAppWidgetIds(ComponentName(context, GayatriWidgetProviderLarge::class.java))
        for (id in largeIds) {
            GayatriWidgetHelper.updateWidget(context, appWidgetManager, id, R.layout.widget_layout_large, GayatriWidgetProviderLarge::class.java)
        }
    }
}

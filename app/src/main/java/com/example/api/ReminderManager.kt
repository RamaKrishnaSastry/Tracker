package com.example.api

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ReminderManager(private val context: Context) {
    private val TAG = "ReminderManager"
    private val prefs = PreferencesManager(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleAlarmsSuspend() {
        if (prefs.getMorningReminderEnabled()) {
            scheduleAlarm("morning", prefs.getMorningReminderTime(), 1001)
        } else {
            cancelAlarm(1001)
        }

        if (prefs.getAfternoonReminderEnabled()) {
            scheduleAlarm("afternoon", prefs.getAfternoonReminderTime(), 1002)
        } else {
            cancelAlarm(1002)
        }

        if (prefs.getEveningReminderEnabled()) {
            scheduleAlarm("evening", prefs.getEveningReminderTime(), 1003)
        } else {
            cancelAlarm(1003)
        }

        // Schedule Custom Practice Alarms
        try {
            val db = com.example.data.AppDatabase.getDatabase(context)
            val practices = db.customPracticeDao().getAllPractices()
            for (practice in practices) {
                val requestCode = (2000 + practice.id).toInt()
                if (practice.isReminderEnabled && practice.reminderTime.isNotEmpty() && !practice.isArchived) {
                    scheduleAlarm("custom_${practice.id}", practice.reminderTime, requestCode)
                } else {
                    cancelAlarm(requestCode)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling custom practice alarms: ${e.message}")
        }
    }

    fun scheduleAlarms() {
        CoroutineScope(Dispatchers.IO).launch {
            scheduleAlarmsSuspend()
        }
    }

    private fun scheduleAlarm(type: String, timeStr: String, requestCode: Int) {
        try {
            cancelAlarm(requestCode) // Clear any duplicate first

            var hour: Int? = null
            var minute: Int? = null

            val cleanStr = timeStr.trim().uppercase(Locale.US)
            val isPm = cleanStr.contains("PM")
            val isAm = cleanStr.contains("AM")
            val digitsStr = cleanStr.replace("AM", "").replace("PM", "").trim()
            val parts = digitsStr.split(":")
            if (parts.size >= 2) {
                var h = parts[0].trim().toIntOrNull()
                val m = parts[1].trim().toIntOrNull()
                if (h != null && m != null) {
                    if (isPm && h < 12) h += 12
                    if (isAm && h == 12) h = 0
                    hour = h
                    minute = m
                }
            }

            if (hour == null || minute == null) {
                Log.e(TAG, "Failed to parse time string: '$timeStr' for $type")
                return
            }

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("PRAYER_TYPE", type)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Efficiently schedules a real-time wake up alarm offline.
            // On modern Android we use setAndAllowWhileIdle for battery optimizations.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled $type Sandhya alarm at $timeStr")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        try {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Canceled alarm with requestCode $requestCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling alarm: ${e.message}")
        }
    }
}

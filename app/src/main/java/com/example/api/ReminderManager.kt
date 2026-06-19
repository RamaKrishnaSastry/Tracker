package com.example.api

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.PreferencesManager
import java.util.*

class ReminderManager(private val context: Context) {
    private val TAG = "ReminderManager"
    private val prefs = PreferencesManager(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarms() {
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
    }

    private fun scheduleAlarm(type: String, timeStr: String, requestCode: Int) {
        try {
            cancelAlarm(requestCode) // Clear any duplicate first

            val parts = timeStr.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: return

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

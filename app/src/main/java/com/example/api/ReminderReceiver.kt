package com.example.api

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {
    private val TAG = "ReminderReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        val type = intent?.getStringExtra("PRAYER_TYPE") ?: return
        val prefs = PreferencesManager(context)

        // Triple-check if the reminder is still enabled before notifying
        val isEnabled = when (type) {
            "morning" -> prefs.getMorningReminderEnabled()
            "afternoon" -> prefs.getAfternoonReminderEnabled()
            "evening" -> prefs.getEveningReminderEnabled()
            else -> false
        }

        if (!isEnabled) return

        // 1. Check reminder intensity (if disabled, do not notify)
        val intensity = prefs.getReminderIntensity()
        if (intensity == "disabled") {
            Log.d(TAG, "Reminders skipped: Intensity is disabled.")
            return
        }

        // 2. Enforce: Maximum one reminder per Sandhya period per day
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (prefs.getLastRemindedDate(type) == todayStr) {
            Log.d(TAG, "Reminders skipped: Already notified for $type sandhya today ($todayStr).")
            return
        }

        // 3. Enforce context-awareness: Notify only when a Sandhya count has not been recorded.
        // Stop reminders immediately after completion.
        val isCompleted = try {
            val dao = AppDatabase.getDatabase(context).japaDao()
            val entry = runBlocking { dao.getEntryByDate(todayStr) }
            when (type) {
                "morning" -> (entry?.pratahSandhyaCount ?: 0) > 0
                "afternoon" -> (entry?.madhyahnikaSandhyaCount ?: 0) > 0
                "evening" -> (entry?.sayamSandhyaCount ?: 0) > 0
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking database inside receiver: ${e.message}")
            false
        }

        if (isCompleted) {
            Log.d(TAG, "Reminders skipped: $type sandhya is already completed (count > 0).")
            return
        }

        // 4. Enforce: Respect Do Not Disturb (DND) settings programmatically
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val filter = manager.currentInterruptionFilter
            if (filter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                Log.d(TAG, "Reminders skipped: System is in Do Not Disturb (DND) mode.")
                return
            }
        }

        // 5. Structure messaging based on intensity settings (Minimal, Standard)
        val title = when (type) {
            "morning" -> "Pratah Sandhyavandanam"
            "afternoon" -> "Madhyahnika Sandhya"
            "evening" -> "Sayam Sandhyavandanam"
            else -> "Sandhyavandanam Time"
        }

        val message = if (intensity == "minimal") {
            // E.g. "Pratah Sandhya count has not been recorded today."
            val localizedName = when (type) {
                "morning" -> "Pratah Sandhya"
                "afternoon" -> "Madhyahnika Sandhya"
                else -> "Sayam Sandhya"
            }
            "$localizedName count has not been recorded today."
        } else {
            // Standard respectful reminder message
            val localizedName = when (type) {
                "morning" -> "Pratah Sandhya"
                "afternoon" -> "Madhyahnika Sandhya"
                else -> "Sayam Sandhya"
            }
            "$localizedName count has not been recorded today. Spend a few minutes for your sacred Gayatri Japa repetition."
        }

        showNotification(context, title, message, type.hashCode())

        // Save last notified date to prevent duplicates
        prefs.setLastRemindedDate(type, todayStr)

        // Reschedule the alarm for the next day to ensure it triggers daily reliably.
        val reminderManager = ReminderManager(context)
        reminderManager.scheduleAlarms()
    }

    private fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        val channelId = "gayatri_japa_reminders_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Gayatri Sandhya Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders for morning, noon, and evening Sandhyavandanam Gayatri Japa"
                }
                manager.createNotificationChannel(channel)
            }
        }

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Standard built-in App Icon representation
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Safe built-in clock icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }
}

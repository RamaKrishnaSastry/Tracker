package com.example.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted. Rescheduling alarms.")
            val reminderManager = ReminderManager(context)
            reminderManager.scheduleAlarms()
        }
    }
}

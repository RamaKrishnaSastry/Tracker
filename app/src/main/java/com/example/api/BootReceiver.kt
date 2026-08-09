package com.example.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Reboot/Update event received ($action). Rescheduling alarms.")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reminderManager = ReminderManager(context)
                    reminderManager.scheduleAlarmsSuspend()
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms on boot: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

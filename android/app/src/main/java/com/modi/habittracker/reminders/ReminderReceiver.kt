package com.modi.habittracker.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Fires at the reminder time, then re-arms tomorrow's alarm. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ReminderNotifications.checkAndNotify(appContext)
            } catch (_: Exception) {
                // A failed check must never leave the chain broken.
            } finally {
                ReminderScheduler(appContext).scheduleStored()
                pendingResult.finish()
            }
        }
    }
}

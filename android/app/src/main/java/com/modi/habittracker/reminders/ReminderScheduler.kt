package com.modi.habittracker.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Schedules the single daily reminder with AlarmManager. Exact alarms are used
 * when the system allows them, with a graceful inexact fallback otherwise.
 * The chosen time is cached locally so boot can restore the alarm without network.
 */
class ReminderScheduler(private val context: Context) {
    private val appContext = context.applicationContext
    private val preferences =
        appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun schedule(time: String?) {
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntent()

        if (time.isNullOrBlank()) {
            alarmManager.cancel(pendingIntent)
            preferences.edit().remove(KEY_REMINDER_TIME).apply()
            return
        }

        val localTime = runCatching { LocalTime.parse(time) }.getOrNull() ?: return
        preferences.edit().putString(KEY_REMINDER_TIME, time).apply()

        val now = ZonedDateTime.now()
        var next = now.toLocalDate().atTime(localTime).atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val triggerAtMillis = next.toInstant().toEpochMilli()

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
                return
            } catch (_: SecurityException) {
                // Fall through to the inexact alarm.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    /** Re-arms the alarm for tomorrow after a delivery, or restores it after boot. */
    fun scheduleStored() {
        schedule(preferences.getString(KEY_REMINDER_TIME, null))
    }

    fun storedTime(): String? = preferences.getString(KEY_REMINDER_TIME, null)

    fun cancel() {
        appContext.getSystemService(AlarmManager::class.java).cancel(pendingIntent())
        preferences.edit().remove(KEY_REMINDER_TIME).apply()
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val PREFERENCES_NAME = "habit_tracker_reminders"
        private const val KEY_REMINDER_TIME = "reminder_time"
        private const val REQUEST_CODE = 1001
    }
}

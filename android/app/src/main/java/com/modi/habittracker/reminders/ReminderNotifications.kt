package com.modi.habittracker.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.modi.habittracker.MainActivity
import com.modi.habittracker.R
import com.modi.habittracker.data.Habit
import com.modi.habittracker.data.HabitLog
import com.modi.habittracker.data.toSchedule
import com.modi.habittracker.logic.DateKeys
import com.modi.habittracker.logic.Recurrence
import com.modi.habittracker.logic.StreakCalculator
import java.time.LocalDate
import kotlinx.coroutines.tasks.await

object ReminderNotifications {
    private const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 2001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /**
     * Counts habits that are visible today and still need a check-in: weekday
     * habits unchecked today, and flexible habits whose period target is unmet.
     */
    suspend fun checkAndNotify(context: Context) {
        val appContext = context.applicationContext
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val firestore = FirebaseFirestore.getInstance()
        val userDocument = firestore.collection("users").document(user.uid)
        val today = LocalDate.now()

        val habits = userDocument.collection("habits").get().await().documents
            .mapNotNull { it.toObject(Habit::class.java) }
        val visible = habits
            .filter { !it.archived }
            .map { it to it.toSchedule() }
            .filter { (_, schedule) -> StreakCalculator.isVisibleToday(schedule, today) }
        if (visible.isEmpty()) return

        // Flexible habits may need check-ins from earlier in their period.
        val earliestStart = visible.minOf { (_, schedule) ->
            if (schedule.type.isFlexible) {
                Recurrence.periodStart(schedule.type, today)
            } else {
                today
            }
        }

        val logsByHabit = mutableMapOf<String, MutableSet<LocalDate>>()
        userDocument.collection("habitLogs")
            .whereGreaterThanOrEqualTo("date", DateKeys.format(earliestStart))
            .get().await().documents
            .mapNotNull { it.toObject(HabitLog::class.java) }
            .forEach { log ->
                val date = runCatching { DateKeys.parse(log.date) }.getOrNull()
                    ?: return@forEach
                logsByHabit.getOrPut(log.habitId) { mutableSetOf() }.add(date)
            }

        val remaining = visible.count { (habit, schedule) ->
            !StreakCalculator.isDoneForToday(
                schedule,
                logsByHabit[habit.id].orEmpty(),
                today,
            )
        }
        if (remaining > 0) postNotification(appContext, remaining)
    }

    private fun postNotification(context: Context, remaining: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = context.resources.getQuantityString(
            R.plurals.reminder_text,
            remaining,
            remaining,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

package com.modi.habittracker

import android.app.Application
import com.modi.habittracker.reminders.ReminderNotifications
import com.modi.habittracker.reminders.ReminderScheduler

class HabitTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.ensureChannel(this)
        // Restore the alarm if the process was killed or the device rebooted.
        ReminderScheduler(this).scheduleStored()
    }
}

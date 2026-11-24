package com.modi.habittracker.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.modi.habittracker.logic.HabitSchedule
import com.modi.habittracker.logic.ScheduleType
import java.time.ZoneId
import java.util.Date

/**
 * Firestore document at users/{uid}/habits/{habitId}.
 * Plain mutable properties with defaults so Firestore can map it reflectively.
 * Documents created before recurrence support simply keep the default `weekdays` type.
 */
data class Habit(
    @DocumentId var id: String = "",
    var name: String = "",
    var scheduleType: String = ScheduleType.WEEKDAYS.storedValue,
    var weekdays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    var targetCount: Int = 1,
    var archived: Boolean = false,
    @ServerTimestamp var createdAt: Date? = null,
    @ServerTimestamp var updatedAt: Date? = null,
)

/** What the editor sends when creating or updating a habit. */
data class HabitDraft(
    val name: String,
    val scheduleType: ScheduleType,
    val weekdays: List<Int>,
    val targetCount: Int,
)

/** Firestore document at users/{uid}/habitLogs/{habitId}_{yyyy-MM-dd}. */
data class HabitLog(
    @DocumentId var id: String = "",
    var habitId: String = "",
    var date: String = "",
    @ServerTimestamp var createdAt: Date? = null,
)

/** Firestore document at users/{uid}/settings/preferences. */
data class Preferences(
    var reminderTime: String? = null,
)

fun Habit.toSchedule(): HabitSchedule {
    val type = ScheduleType.fromStored(scheduleType)
    return HabitSchedule(
        weekdays = weekdays.toSet(),
        createdDate = createdAt
            ?.toInstant()
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalDate(),
        type = type,
        targetCount = targetCount.coerceIn(1, type.maxTarget()),
    )
}

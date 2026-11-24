package com.modi.habittracker.logic

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Local calendar dates are stored as yyyy-MM-dd strings so they never shift time zones. */
object DateKeys {
    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val FULL: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

    fun format(date: LocalDate): String = date.format(ISO)

    fun parse(key: String): LocalDate = LocalDate.parse(key, ISO)

    fun fullLabel(date: LocalDate): String = date.format(FULL)
}

object Weekdays {
    val names: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    fun label(day: Int): String = names.getOrElse(day - 1) { "?" }

    fun format(days: Collection<Int>): String {
        val sorted = days.filter { it in 1..7 }.sorted()
        return when {
            sorted.size == 7 -> "Every day"
            sorted == listOf(1, 2, 3, 4, 5) -> "Weekdays"
            sorted == listOf(6, 7) -> "Weekends"
            sorted.isEmpty() -> "Not scheduled"
            else -> sorted.joinToString(", ") { label(it) }
        }
    }

    /** "Every day", "Weekdays", "3 times per week", … */
    fun formatSchedule(schedule: HabitSchedule): String {
        if (!schedule.type.isFlexible) return format(schedule.weekdays)
        val count = schedule.targetCount.coerceAtLeast(1)
        return "$count ${if (count == 1) "time" else "times"} per ${schedule.type.periodName}"
    }
}

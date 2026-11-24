package com.modi.habittracker.logic

import java.time.LocalDate

/** Habit shape accepted by the streak logic. */
data class HabitSchedule(
    val weekdays: Set<Int>,
    val createdDate: LocalDate?,
    val type: ScheduleType = ScheduleType.WEEKDAYS,
    val targetCount: Int = 1,
)

enum class DayStatus { DONE, MISSED, EMPTY, OFF, FUTURE }

data class DayCell(
    val date: LocalDate,
    val status: DayStatus,
    val scheduled: Boolean,
    val done: Boolean,
)

data class Streak(val value: Int, val capped: Boolean)

data class Completion(val done: Int, val scheduled: Int, val rate: Double?)

/**
 * Pure streak and completion logic. This mirrors `web/src/lib/streaks.ts` exactly;
 * both clients must be tested against the same cases.
 */
object StreakCalculator {
    /** A day that can count toward the habit: on schedule and after creation. */
    fun isEligible(schedule: HabitSchedule, date: LocalDate): Boolean {
        val created = schedule.createdDate
        if (created != null && date.isBefore(created)) return false
        if (schedule.type == ScheduleType.WEEKDAYS) {
            return date.dayOfWeek.value in schedule.weekdays
        }
        return true
    }

    /** Fixed weekday schedules only; flexible habits have no specific scheduled days. */
    fun isScheduled(schedule: HabitSchedule, date: LocalDate): Boolean =
        schedule.type == ScheduleType.WEEKDAYS && isEligible(schedule, date)

    fun dayCell(
        schedule: HabitSchedule,
        logs: Set<LocalDate>,
        date: LocalDate,
        today: LocalDate,
    ): DayCell {
        val eligible = isEligible(schedule, date)
        val done = date in logs

        return when {
            date.isAfter(today) -> DayCell(date, DayStatus.FUTURE, eligible, done)
            // Done wins even on an off-schedule day (for example after the schedule changed).
            done -> DayCell(date, DayStatus.DONE, eligible, done)
            !eligible -> DayCell(date, DayStatus.OFF, false, done)
            schedule.type.isFlexible -> DayCell(date, DayStatus.EMPTY, true, done)
            else -> DayCell(date, DayStatus.MISSED, true, done)
        }
    }

    /** Oldest-first cells for the [count] days ending at [today]. */
    fun dayCells(
        schedule: HabitSchedule,
        logs: Set<LocalDate>,
        today: LocalDate,
        count: Int,
    ): List<DayCell> = (count - 1 downTo 0).map { offset ->
        dayCell(schedule, logs, today.minusDays(offset.toLong()), today)
    }

    /**
     * Consecutive scheduled days (weekday habits) or consecutive periods meeting the
     * target (flexible habits). An unmet current period does not break the streak;
     * periods before creation are ignored.
     */
    fun currentStreak(
        schedule: HabitSchedule,
        logs: Set<LocalDate>,
        today: LocalDate,
        windowDays: Int = 90,
    ): Streak {
        if (schedule.type.isFlexible) {
            val budget = Recurrence.budget(schedule.type)
            val currentStart = Recurrence.periodStart(schedule.type, today)
            var start = currentStart
            var value = 0
            var counted = 0

            while (counted < budget) {
                val isCurrent = start == currentStart
                val created = schedule.createdDate
                if (!isCurrent && created != null && start.isBefore(created)) break
                val met = Recurrence.countInPeriod(schedule, logs, start) >=
                    Recurrence.targetOf(schedule)
                if (met) value++ else if (!isCurrent) break
                start = Recurrence.shift(schedule.type, start, -1)
                counted++
            }

            return Streak(value, value >= budget && value > 0)
        }

        var value = 0
        for (offset in 0 until windowDays) {
            val date = today.minusDays(offset.toLong())
            val created = schedule.createdDate
            if (created != null && date.isBefore(created)) break
            if (!isScheduled(schedule, date)) continue
            if (date in logs) {
                value += 1
                continue
            }
            if (date == today) continue
            break
        }
        return Streak(value, value >= windowDays && value > 0)
    }

    /**
     * Weekday habits: scheduled days marked done divided by scheduled days in the
     * last 30 days. Flexible habits: periods whose target was met divided by elapsed
     * periods in their window (12 weeks, 6 months, or 3 years).
     */
    fun completionRate(
        schedule: HabitSchedule,
        logs: Set<LocalDate>,
        today: LocalDate,
        windowDays: Int = 30,
    ): Completion {
        if (schedule.type.isFlexible) {
            val budget = Recurrence.budget(schedule.type)
            var start = Recurrence.periodStart(schedule.type, today)
            var done = 0
            var elapsed = 0

            for (index in 0 until budget) {
                val created = schedule.createdDate
                // Only the current period may have begun before the habit was created.
                if (index > 0 && created != null && start.isBefore(created)) break
                elapsed += 1
                if (Recurrence.countInPeriod(schedule, logs, start) >= Recurrence.targetOf(schedule)) {
                    done += 1
                }
                start = Recurrence.shift(schedule.type, start, -1)
            }

            return Completion(
                done = done,
                scheduled = elapsed,
                rate = if (elapsed == 0) null else done.toDouble() / elapsed,
            )
        }

        var done = 0
        var scheduled = 0
        for (offset in 0 until windowDays) {
            val date = today.minusDays(offset.toLong())
            val created = schedule.createdDate
            if (created != null && date.isBefore(created)) break
            if (!isScheduled(schedule, date)) continue
            scheduled += 1
            if (date in logs) done += 1
        }
        return Completion(done, scheduled, if (scheduled == 0) null else done.toDouble() / scheduled)
    }

    /**
     * Detail grid: [days] days laid out in Monday-to-Sunday columns. Leading blanks
     * complete the first week; the current week is completed with future days.
     */
    fun habitGrid(
        schedule: HabitSchedule,
        logs: Set<LocalDate>,
        today: LocalDate,
        days: Int = 90,
    ): List<DayCell?> {
        val first = today.minusDays((days - 1).toLong())
        val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
        val last = today.plusDays((7 - today.dayOfWeek.value).toLong())
        val cells = mutableListOf<DayCell?>()
        var cursor = start
        while (!cursor.isAfter(last)) {
            cells += if (cursor.isBefore(first)) null else dayCell(schedule, logs, cursor, today)
            cursor = cursor.plusDays(1)
        }
        return cells
    }

    /** Weekday habits only appear on their weekdays; flexible habits appear every day. */
    fun isVisibleToday(schedule: HabitSchedule, today: LocalDate): Boolean =
        if (schedule.type.isFlexible) true else isScheduled(schedule, today)

    /** Weekday habits are done on the day; flexible habits once the period target is met. */
    fun isDoneForToday(
        schedule: HabitSchedule,
        logs: Set<LocalDate>,
        today: LocalDate,
    ): Boolean = if (schedule.type.isFlexible) {
        Recurrence.isMet(schedule, logs, today)
    } else {
        today in logs
    }

    fun needsAttention(
        schedule: HabitSchedule,
        logs: Set<LocalDate>,
        today: LocalDate,
    ): Boolean = isVisibleToday(schedule, today) && !isDoneForToday(schedule, logs, today)

    /** Number of visible habits that still need a check-in. */
    fun remainingToday(entries: List<Pair<HabitSchedule, Set<LocalDate>>>, today: LocalDate): Int =
        entries.count { (schedule, logs) -> needsAttention(schedule, logs, today) }
}

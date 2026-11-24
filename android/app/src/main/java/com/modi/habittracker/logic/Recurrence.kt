package com.modi.habittracker.logic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class PeriodProgress(
    val count: Int,
    val target: Int,
    val met: Boolean,
    val start: LocalDate,
    val end: LocalDate,
)

/** Calendar-period helpers mirrored by `web/src/lib/streaks.ts`. */
object Recurrence {
    fun periodStart(type: ScheduleType, date: LocalDate): LocalDate = when (type) {
        ScheduleType.TIMES_PER_WEEK ->
            date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        ScheduleType.TIMES_PER_MONTH -> date.withDayOfMonth(1)
        ScheduleType.TIMES_PER_YEAR -> date.withDayOfYear(1)
        ScheduleType.WEEKDAYS -> date
    }

    fun periodEnd(type: ScheduleType, date: LocalDate): LocalDate {
        val start = periodStart(type, date)
        return when (type) {
            ScheduleType.TIMES_PER_WEEK -> start.plusDays(6)
            ScheduleType.TIMES_PER_MONTH -> start.plusMonths(1).minusDays(1)
            ScheduleType.TIMES_PER_YEAR -> start.plusYears(1).minusDays(1)
            ScheduleType.WEEKDAYS -> date
        }
    }

    fun shift(type: ScheduleType, start: LocalDate, periods: Long): LocalDate = when (type) {
        ScheduleType.TIMES_PER_WEEK -> start.plusWeeks(periods)
        ScheduleType.TIMES_PER_MONTH -> start.plusMonths(periods)
        ScheduleType.TIMES_PER_YEAR -> start.plusYears(periods)
        ScheduleType.WEEKDAYS -> start
    }

    /** Periods loaded for flexible streak and rate windows: 12 weeks, 6 months, or 3 years. */
    fun budget(type: ScheduleType): Int = when (type) {
        ScheduleType.TIMES_PER_WEEK -> 12
        ScheduleType.TIMES_PER_MONTH -> 6
        ScheduleType.TIMES_PER_YEAR -> 3
        ScheduleType.WEEKDAYS -> 0
    }

    fun targetOf(schedule: HabitSchedule): Int = schedule.targetCount.coerceAtLeast(1)

    fun countInPeriod(schedule: HabitSchedule, logs: Set<LocalDate>, date: LocalDate): Int {
        val start = periodStart(schedule.type, date)
        val end = periodEnd(schedule.type, date)
        return logs.count { !it.isBefore(start) && !it.isAfter(end) }
    }

    fun progress(
        schedule: HabitSchedule,
        logs: Set<LocalDate>,
        date: LocalDate,
    ): PeriodProgress {
        val target = targetOf(schedule)
        val count = countInPeriod(schedule, logs, date)
        return PeriodProgress(
            count = count,
            target = target,
            met = count >= target,
            start = periodStart(schedule.type, date),
            end = periodEnd(schedule.type, date),
        )
    }

    fun isMet(schedule: HabitSchedule, logs: Set<LocalDate>, date: LocalDate): Boolean =
        countInPeriod(schedule, logs, date) >= targetOf(schedule)

    /** Oldest date that must be loaded for the current habits. 90 days by default. */
    fun historyStart(schedules: List<HabitSchedule>, today: LocalDate): LocalDate {
        var start = today.minusDays(89)
        schedules.forEach { schedule ->
            if (!schedule.type.isFlexible) return@forEach
            val candidate = shift(
                schedule.type,
                periodStart(schedule.type, today),
                -(budget(schedule.type) - 1).toLong(),
            )
            if (candidate.isBefore(start)) start = candidate
        }
        return start
    }

    fun unit(type: ScheduleType, value: Int): String {
        val name = type.periodName
        return if (value == 1) name else "${name}s"
    }

    fun thisPeriodLabel(type: ScheduleType): String = "this ${type.periodName}"
}

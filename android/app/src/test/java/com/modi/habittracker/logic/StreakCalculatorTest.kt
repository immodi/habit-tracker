package com.modi.habittracker.logic

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakCalculatorTest {
    private val daily = HabitSchedule((1..7).toSet(), null)
    private val weekdaysOnly = HabitSchedule(setOf(1, 2, 3, 4, 5), null)
    private val weeklyThree =
        HabitSchedule(emptySet(), null, ScheduleType.TIMES_PER_WEEK, 3)
    private val monthlyTen =
        HabitSchedule(emptySet(), null, ScheduleType.TIMES_PER_MONTH, 10)

    private fun logs(vararg dates: String): Set<LocalDate> = dates.map(DateKeys::parse).toSet()

    private fun today(key: String): LocalDate = DateKeys.parse(key)

    @Test
    fun `daily habit counts consecutive days`() {
        val result = StreakCalculator.currentStreak(
            daily,
            logs("2026-11-21", "2026-11-22", "2026-11-23"),
            today("2026-11-23"),
        )
        assertEquals(Streak(3, false), result)
    }

    @Test
    fun `weekday habit keeps its streak across the weekend`() {
        val result = StreakCalculator.currentStreak(
            weekdaysOnly,
            logs("2026-11-16", "2026-11-17", "2026-11-18", "2026-11-19", "2026-11-20"),
            today("2026-11-22"),
        )
        assertEquals(5, result.value)
    }

    @Test
    fun `today not yet done does not break the streak`() {
        val result = StreakCalculator.currentStreak(
            daily,
            logs("2026-11-21", "2026-11-22"),
            today("2026-11-23"),
        )
        assertEquals(Streak(2, false), result)
    }

    @Test
    fun `a gap in the middle stops the streak`() {
        val result = StreakCalculator.currentStreak(
            daily,
            logs("2026-11-23", "2026-11-22", "2026-11-20", "2026-11-19"),
            today("2026-11-23"),
        )
        assertEquals(2, result.value)
    }

    @Test
    fun `days before the habit was created are ignored`() {
        val created = daily.copy(createdDate = today("2026-11-21"))
        val result = StreakCalculator.currentStreak(
            created,
            logs("2026-11-20", "2026-11-21", "2026-11-22", "2026-11-23"),
            today("2026-11-23"),
        )
        assertEquals(Streak(3, false), result)
    }

    @Test
    fun `streak touching the window edge is capped`() {
        val today = today("2026-11-23")
        val all = (0 until 90).map { today.minusDays(it.toLong()) }.toSet()
        val result = StreakCalculator.currentStreak(daily, all, today)
        assertEquals(Streak(90, true), result)
    }

    @Test
    fun `off-schedule check-ins do not extend the streak`() {
        val result = StreakCalculator.currentStreak(
            weekdaysOnly,
            logs("2026-11-21", "2026-11-22"),
            today("2026-11-23"),
        )
        assertEquals(0, result.value)
    }

    @Test
    fun `completion rate divides done scheduled days`() {
        val created = daily.copy(createdDate = today("2026-11-21"))
        val result = StreakCalculator.completionRate(
            created,
            logs("2026-11-21", "2026-11-23"),
            today("2026-11-23"),
        )
        assertEquals(2, result.done)
        assertEquals(3, result.scheduled)
        assertEquals(2.0 / 3.0, result.rate ?: 0.0, 0.0001)
    }

    @Test
    fun `completion rate is null when nothing was scheduled`() {
        val created = weekdaysOnly.copy(createdDate = today("2026-11-22"))
        assertNull(StreakCalculator.completionRate(created, logs(), today("2026-11-22")).rate)
    }

    @Test
    fun `weekend days are skipped in the completion rate`() {
        val created = weekdaysOnly.copy(createdDate = today("2026-11-16"))
        val result = StreakCalculator.completionRate(
            created,
            logs("2026-11-16", "2026-11-17", "2026-11-18"),
            today("2026-11-23"),
        )
        assertEquals(3, result.done)
        assertEquals(6, result.scheduled)
        assertEquals(0.5, result.rate ?: 0.0, 0.0001)
    }

    @Test
    fun `off-schedule check-in shows as done but unscheduled`() {
        val cell = StreakCalculator.dayCell(
            weekdaysOnly,
            logs("2026-11-21"),
            today("2026-11-21"),
            today("2026-11-23"),
        )
        assertEquals(DayStatus.DONE, cell.status)
        assertEquals(false, cell.scheduled)
        assertTrue(cell.done)
    }

    @Test
    fun `future days are marked future`() {
        val cell = StreakCalculator.dayCell(daily, emptySet(), today("2026-11-24"), today("2026-11-23"))
        assertEquals(DayStatus.FUTURE, cell.status)
    }

    @Test
    fun `grid lays the window out in complete weeks`() {
        val cells = StreakCalculator.habitGrid(daily, emptySet(), today("2026-11-23"), 90)
        assertEquals(0, cells.size % 7)
        val real = cells.filterNotNull()
        assertEquals(96, real.size) // 90 days + 6 future days to finish the week.
        assertEquals(90, real.count { it.status != DayStatus.FUTURE })
        assertTrue(cells.first() == null)
    }

    @Test
    fun `grid starts on Monday and ends on today when today is Sunday`() {
        val cells = StreakCalculator.habitGrid(daily, emptySet(), today("2026-11-22"), 7)
        assertEquals(7, cells.size)
        assertTrue(cells.all { it != null })
        assertEquals(1, cells.first()!!.date.dayOfWeek.value)
        assertEquals(today("2026-11-22"), cells.last()!!.date)
    }

    @Test
    fun `remaining today counts unchecked scheduled habits`() {
        val entries = listOf(
            daily to logs("2026-11-23"),
            daily to logs(),
            weekdaysOnly to logs("2026-11-23"),
        )
        assertEquals(1, StreakCalculator.remainingToday(entries, today("2026-11-23")))
        // Sunday is not a weekday, so a weekday-only habit is not scheduled.
        assertEquals(
            0,
            StreakCalculator.remainingToday(
                listOf(weekdaysOnly to logs()),
                today("2026-11-22"),
            ),
        )
    }

    @Test
    fun `weekly habit counts consecutive met weeks`() {
        val done = logs(
            "2026-11-23", "2026-11-24", "2026-11-25",
            "2026-11-16", "2026-11-17", "2026-11-18",
        )
        assertEquals(
            Streak(2, false),
            StreakCalculator.currentStreak(weeklyThree, done, today("2026-11-25")),
        )
    }

    @Test
    fun `current week is skipped until its target is met`() {
        val done = logs(
            "2026-11-23", "2026-11-24",
            "2026-11-16", "2026-11-17", "2026-11-18",
            "2026-11-09", "2026-11-10", "2026-11-11",
        )
        assertEquals(2, StreakCalculator.currentStreak(weeklyThree, done, today("2026-11-25")).value)
    }

    @Test
    fun `weekly streak stops at an unfinished earlier week`() {
        val done = logs(
            "2026-11-23", "2026-11-24", "2026-11-25",
            "2026-11-16", "2026-11-17",
            "2026-11-09", "2026-11-10", "2026-11-11",
        )
        assertEquals(1, StreakCalculator.currentStreak(weeklyThree, done, today("2026-11-25")).value)
    }

    @Test
    fun `a met creation week counts`() {
        val created = weeklyThree.copy(createdDate = today("2026-11-23"))
        val done = logs("2026-11-23", "2026-11-24", "2026-11-25")
        assertEquals(
            Streak(1, false),
            StreakCalculator.currentStreak(created, done, today("2026-11-25")),
        )
    }

    @Test
    fun `monthly habit counts consecutive met months`() {
        val created = monthlyTen.copy(createdDate = today("2026-10-01"))
        val done = logs(
            "2026-11-01", "2026-11-02", "2026-11-03",
            "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04", "2026-10-05",
            "2026-10-06", "2026-10-07", "2026-10-08", "2026-10-09", "2026-10-10",
        )
        // November has only 3 of 10, so the streak comes from October.
        assertEquals(
            Streak(1, false),
            StreakCalculator.currentStreak(created, done, today("2026-11-15")),
        )
    }

    @Test
    fun `weekly completion rate counts met periods`() {
        val created = weeklyThree.copy(createdDate = today("2026-11-16"))
        val done = logs(
            "2026-11-16", "2026-11-17", "2026-11-18",
            "2026-11-23", "2026-11-24", "2026-11-25",
        )
        val result = StreakCalculator.completionRate(created, done, today("2026-11-25"))
        assertEquals(2, result.done)
        assertEquals(2, result.scheduled)
        assertEquals(1.0, result.rate ?: 0.0, 0.0001)
    }

    @Test
    fun `monthly completion rate includes the current unmet month`() {
        val created = monthlyTen.copy(createdDate = today("2026-10-01"))
        val done = logs(
            "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04", "2026-10-05",
            "2026-10-06", "2026-10-07", "2026-10-08", "2026-10-09", "2026-10-10",
        )
        val result = StreakCalculator.completionRate(created, done, today("2026-11-15"))
        assertEquals(1, result.done)
        assertEquals(2, result.scheduled)
        assertEquals(0.5, result.rate ?: 0.0, 0.0001)
    }

    @Test
    fun `flexible days are empty rather than missed`() {
        val cell = StreakCalculator.dayCell(weeklyThree, emptySet(), today("2026-11-24"), today("2026-11-25"))
        assertEquals(DayStatus.EMPTY, cell.status)
        assertEquals(true, cell.scheduled)
    }

    @Test
    fun `flexible habits are visible and done by period target`() {
        val schedule = weeklyThree
        assertEquals(true, StreakCalculator.isVisibleToday(schedule, today("2026-11-25")))
        val met = logs("2026-11-23", "2026-11-24", "2026-11-25")
        assertEquals(true, StreakCalculator.isDoneForToday(schedule, met, today("2026-11-25")))
        assertEquals(false, StreakCalculator.isDoneForToday(schedule, logs("2026-11-23"), today("2026-11-25")))
    }

    @Test
    fun `remaining today includes unmet flexible habits`() {
        val entries = listOf(
            daily to logs("2026-11-23"),
            daily to logs(),
            weekdaysOnly to logs("2026-11-23"),
            weeklyThree to logs("2026-11-23", "2026-11-24"),
        )
        assertEquals(2, StreakCalculator.remainingToday(entries, today("2026-11-23")))
    }

    @Test
    fun `period progress counts check-ins inside the week`() {
        val progress = Recurrence.progress(
            weeklyThree,
            logs("2026-11-23", "2026-11-24", "2026-11-29"),
            today("2026-11-25"),
        )
        assertEquals(3, progress.count)
        assertEquals(3, progress.target)
        assertEquals(true, progress.met)
        assertEquals(today("2026-11-23"), progress.start)
        assertEquals(today("2026-11-29"), progress.end)
    }

    @Test
    fun `history window widens for monthly and yearly habits`() {
        val now = today("2026-11-23")
        assertEquals(now.minusDays(89), Recurrence.historyStart(listOf(daily, weeklyThree), now))
        assertEquals(
            today("2026-06-01"),
            Recurrence.historyStart(listOf(monthlyTen), now),
        )
        val yearly = HabitSchedule(emptySet(), null, ScheduleType.TIMES_PER_YEAR, 12)
        assertEquals(
            today("2024-01-01"),
            Recurrence.historyStart(listOf(yearly), now),
        )
    }
}

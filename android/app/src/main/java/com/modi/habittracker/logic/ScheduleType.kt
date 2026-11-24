package com.modi.habittracker.logic

/**
 * Daily weekday schedules or "N times per week/month/year" targets.
 * Stored values are the shared contract with the web client.
 */
enum class ScheduleType(val storedValue: String) {
    WEEKDAYS("weekdays"),
    TIMES_PER_WEEK("timesPerWeek"),
    TIMES_PER_MONTH("timesPerMonth"),
    TIMES_PER_YEAR("timesPerYear");

    val isFlexible: Boolean get() = this != WEEKDAYS

    val periodName: String
        get() = when (this) {
            TIMES_PER_WEEK -> "week"
            TIMES_PER_MONTH -> "month"
            TIMES_PER_YEAR -> "year"
            WEEKDAYS -> "day"
        }

    /** Compact suffix for the streak pill: 12d, 4w, 2mo, 1y. */
    val streakSuffix: String
        get() = when (this) {
            TIMES_PER_WEEK -> "w"
            TIMES_PER_MONTH -> "mo"
            TIMES_PER_YEAR -> "y"
            WEEKDAYS -> "d"
        }

    val rateWindowLabel: String
        get() = when (this) {
            TIMES_PER_WEEK -> "Last 12 weeks"
            TIMES_PER_MONTH -> "Last 6 months"
            TIMES_PER_YEAR -> "Last 3 years"
            WEEKDAYS -> "Last 30 days"
        }

    fun maxTarget(): Int = when (this) {
        TIMES_PER_WEEK -> 7
        TIMES_PER_MONTH -> 31
        TIMES_PER_YEAR -> 366
        WEEKDAYS -> 7
    }

    companion object {
        val choices: List<ScheduleType> = listOf(
            WEEKDAYS,
            TIMES_PER_WEEK,
            TIMES_PER_MONTH,
            TIMES_PER_YEAR,
        )

        fun fromStored(value: String?): ScheduleType =
            entries.firstOrNull { it.storedValue == value } ?: WEEKDAYS

        fun optionLabel(type: ScheduleType): String = when (type) {
            WEEKDAYS -> "Specific days"
            TIMES_PER_WEEK -> "Times per week"
            TIMES_PER_MONTH -> "Times per month"
            TIMES_PER_YEAR -> "Times per year"
        }

        fun defaultTarget(type: ScheduleType): Int = when (type) {
            TIMES_PER_WEEK -> 3
            TIMES_PER_MONTH -> 10
            TIMES_PER_YEAR -> 12
            WEEKDAYS -> 1
        }
    }
}

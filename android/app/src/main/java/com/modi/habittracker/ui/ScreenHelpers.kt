package com.modi.habittracker.ui

import com.modi.habittracker.logic.DateKeys
import java.time.LocalDate

/** Parses the loaded yyyy-MM-dd check-in keys for one habit into LocalDates. */
fun Map<String, Set<String>>.datesFor(habitId: String): Set<LocalDate> =
    this[habitId].orEmpty().mapNotNull { key ->
        runCatching { DateKeys.parse(key) }.getOrNull()
    }.toSet()

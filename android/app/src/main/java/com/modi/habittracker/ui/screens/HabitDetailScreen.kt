package com.modi.habittracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.modi.habittracker.R
import com.modi.habittracker.data.toSchedule
import com.modi.habittracker.logic.DayCell
import com.modi.habittracker.logic.DayStatus
import com.modi.habittracker.logic.DateKeys
import com.modi.habittracker.logic.Recurrence
import com.modi.habittracker.logic.StreakCalculator
import com.modi.habittracker.logic.Weekdays
import com.modi.habittracker.ui.AppUiState
import com.modi.habittracker.ui.components.AppCard
import com.modi.habittracker.ui.components.EmptyState
import com.modi.habittracker.ui.components.LoadingRow
import com.modi.habittracker.ui.components.PageHeading
import com.modi.habittracker.ui.components.PeriodProgressCard
import com.modi.habittracker.ui.components.SecondaryButton
import com.modi.habittracker.ui.components.StatCard
import com.modi.habittracker.ui.datesFor
import com.modi.habittracker.ui.theme.AppColors
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun HabitDetailScreen(
    state: AppUiState,
    habitId: String?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggle: (habitId: String, dateKey: String, done: Boolean) -> Unit,
) {
    val habit = state.habits.firstOrNull { it.id == habitId }

    if (habit == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            if (state.dataLoading) {
                LoadingRow("Loading habit")
            } else {
                EmptyState(
                    title = "Habit not found",
                    body = "It may have been removed or belongs to another account.",
                    action = { SecondaryButton("Back to Today", onBack) },
                )
            }
        }
        return
    }

    val logs = state.logs.datesFor(habit.id)
    val schedule = habit.toSchedule()
    val flexible = schedule.type.isFlexible
    val streak = StreakCalculator.currentStreak(schedule, logs, state.today)
    val completion = StreakCalculator.completionRate(schedule, logs, state.today)
    val progress = if (flexible) Recurrence.progress(schedule, logs, state.today) else null
    val cells = StreakCalculator.habitGrid(schedule, logs, state.today, 90)
    val rateText = completion.rate?.let { "${(it * 100).roundToInt()}%" } ?: "—"
    val rateUnit = if (completion.scheduled > 0) {
        "${completion.done}/${completion.scheduled}"
    } else {
        "nothing yet"
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 28.dp),
    ) {
        Row(
            Modifier
                .clickable(onClick = onBack)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = AppColors.Muted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "Today",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Muted,
            )
        }

        Spacer(Modifier.height(10.dp))
        PageHeading(
            eyebrow = buildString {
                append(Weekdays.formatSchedule(schedule))
                if (habit.archived) append(" · archived")
            },
            title = habit.name,
            trailing = { SecondaryButton("Edit", onEdit) },
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Current streak",
                value = if (streak.capped) "${streak.value}+" else streak.value.toString(),
                unit = Recurrence.unit(schedule.type, streak.value),
                modifier = Modifier.weight(1f),
                icon = painterResource(R.drawable.ic_flame),
            )
            StatCard(
                label = schedule.type.rateWindowLabel,
                value = rateText,
                unit = rateUnit,
                modifier = Modifier.weight(1f),
            )
        }

        if (progress != null) {
            Spacer(Modifier.height(18.dp))
            PeriodProgressCard(progress, schedule.type)
        }

        Spacer(Modifier.height(18.dp))
        AppCard {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        "Last 90 days",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (flexible) {
                            "Tap any day to add or remove a check-in"
                        } else {
                            "Tap a day to fill in a missed check-in"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.MutedDim,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                HeatmapGrid(cells, state.today) { date, done ->
                    onToggle(habit.id, DateKeys.format(date), done)
                }
                Spacer(Modifier.height(14.dp))
                HeatmapLegend(flexible)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (flexible) {
                        "Every day can count toward this target. A check-in before the habit was created is ignored."
                    } else {
                        "Days outside the schedule are muted and can’t be toggled. A check-in that already exists on such a day still shows, but doesn’t count toward the streak or rate."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.MutedDim,
                )
            }
        }
    }
}

@Composable
private fun HeatmapGrid(
    cells: List<DayCell?>,
    today: LocalDate,
    onToggle: (LocalDate, Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        cells.chunked(7).forEach { week ->
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEach { cell ->
                    if (cell == null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                        )
                    } else {
                        HeatmapCell(
                            cell = cell,
                            today = today,
                            onToggle = onToggle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    cell: DayCell,
    today: LocalDate,
    onToggle: (LocalDate, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(5.dp)
    val enabled = cell.scheduled && cell.status != DayStatus.FUTURE
    val fill = when (cell.status) {
        DayStatus.DONE -> AppColors.Accent
        DayStatus.MISSED -> Color.White.copy(alpha = 0.055f)
        DayStatus.EMPTY -> Color.White.copy(alpha = 0.075f)
        DayStatus.OFF, DayStatus.FUTURE -> Color.Transparent
    }
    val borderColor = when (cell.status) {
        DayStatus.DONE -> AppColors.Accent
        DayStatus.OFF -> Color.White.copy(alpha = 0.09f)
        DayStatus.FUTURE -> Color.White.copy(alpha = 0.045f)
        DayStatus.MISSED, DayStatus.EMPTY -> Color.Transparent
    }

    var cellModifier = modifier
        .clip(shape)
        .background(fill)
        .border(1.dp, borderColor, shape)
    if (enabled) {
        cellModifier = cellModifier.clickable { onToggle(cell.date, !cell.done) }
    }
    if (cell.date == today) {
        cellModifier = cellModifier.border(1.5.dp, Color.White.copy(alpha = 0.45f), shape)
    }
    cellModifier = cellModifier.semantics {
        contentDescription = "${DateKeys.fullLabel(cell.date)}, ${statusLabel(cell.status)}"
        if (enabled) role = Role.Button
    }

    Box(cellModifier)
}

private fun statusLabel(status: DayStatus): String = when (status) {
    DayStatus.DONE -> "done"
    DayStatus.MISSED -> "missed"
    DayStatus.EMPTY -> "no check-in"
    DayStatus.OFF -> "not scheduled"
    DayStatus.FUTURE -> "upcoming"
}

@Composable
private fun HeatmapLegend(flexible: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem("Done", AppColors.Accent)
        if (flexible) {
            LegendItem("No check-in", Color.White.copy(alpha = 0.16f))
            LegendItem("Before start", Color.Transparent, outlined = true)
        } else {
            LegendItem("Missed", Color.White.copy(alpha = 0.12f))
            LegendItem("Not scheduled", Color.Transparent, outlined = true)
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, outlined: Boolean = false) {
    val shape = RoundedCornerShape(3.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(shape)
                .background(color)
                .then(
                    if (outlined) {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.18f), shape)
                    } else {
                        Modifier
                    },
                ),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.MutedDim,
        )
    }
}

package com.modi.habittracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.modi.habittracker.data.toSchedule
import com.modi.habittracker.logic.DateKeys
import com.modi.habittracker.logic.StreakCalculator
import com.modi.habittracker.ui.AppUiState
import com.modi.habittracker.ui.components.AppCard
import com.modi.habittracker.ui.components.EmptyState
import com.modi.habittracker.ui.components.HabitCard
import com.modi.habittracker.ui.components.LoadingRow
import com.modi.habittracker.ui.components.Notice
import com.modi.habittracker.ui.components.PageHeading
import com.modi.habittracker.ui.components.PrimaryButton
import com.modi.habittracker.ui.components.SummaryCard
import com.modi.habittracker.ui.datesFor
import com.modi.habittracker.ui.theme.AppColors

@Composable
fun TodayScreen(
    state: AppUiState,
    onToggle: (habitId: String, dateKey: String, done: Boolean) -> Unit,
    onOpenHabit: (String) -> Unit,
    onAddHabit: () -> Unit,
) {
    val active = state.habits.filter { !it.archived }
    val visible = active.filter { habit ->
        StreakCalculator.isVisibleToday(habit.toSchedule(), state.today)
    }
    val todayKey = DateKeys.format(state.today)
    val doneCount = visible.count { habit ->
        StreakCalculator.isDoneForToday(
            habit.toSchedule(),
            state.logs.datesFor(habit.id),
            state.today,
        )
    }
    val remaining = visible.size - doneCount
    val hasFlexible = visible.any { it.toSchedule().type.isFlexible }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 28.dp),
    ) {
        PageHeading(DateKeys.fullLabel(state.today), "Today")
        Spacer(Modifier.height(20.dp))

        if (state.error != null) {
            Notice(state.error)
            Spacer(Modifier.height(16.dp))
        }

        when {
            state.dataLoading -> LoadingRow("Loading habits")

            active.isEmpty() -> {
                EmptyState(
                    title = "Start with one habit",
                    body = "Pick something small you can do today. You can add more whenever you like.",
                    icon = {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(26.dp),
                        )
                    },
                    action = {
                        PrimaryButton(
                            text = "Add your first habit",
                            onClick = onAddHabit,
                            leadingIcon = {
                                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                            },
                        )
                    },
                )
            }

            else -> {
                SummaryCard(
                    done = doneCount,
                    total = visible.size,
                    remaining = remaining,
                    hasFlexible = hasFlexible,
                )
                Spacer(Modifier.height(18.dp))

                if (visible.isEmpty()) {
                    AppCard {
                        Text(
                            "Today is a rest day for all of your habits. Enjoy it.",
                            Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.Muted,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        visible.forEach { habit ->
                            HabitCard(
                                habit = habit,
                                logs = state.logs.datesFor(habit.id),
                                today = state.today,
                                onToggle = {
                                    onToggle(habit.id, todayKey, todayKey !in state.logs[habit.id].orEmpty())
                                },
                                onOpen = { onOpenHabit(habit.id) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                PrimaryButton(
                    text = "Add habit",
                    onClick = onAddHabit,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) },
                )
            }
        }
    }
}

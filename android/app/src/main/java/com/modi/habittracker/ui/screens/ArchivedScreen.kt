package com.modi.habittracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.modi.habittracker.data.toSchedule
import com.modi.habittracker.logic.Weekdays
import com.modi.habittracker.ui.AppUiState
import com.modi.habittracker.ui.components.AppCard
import com.modi.habittracker.ui.components.EmptyState
import com.modi.habittracker.ui.components.LoadingRow
import com.modi.habittracker.ui.components.Notice
import com.modi.habittracker.ui.components.PageHeading
import com.modi.habittracker.ui.components.SecondaryButton
import com.modi.habittracker.ui.theme.AppColors

@Composable
fun ArchivedScreen(
    state: AppUiState,
    onRestore: (String) -> Unit,
    onOpenHabit: (String) -> Unit,
) {
    val archived = state.habits.filter { it.archived }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 28.dp),
    ) {
        PageHeading("Kept, not gone", "Archived")
        Spacer(Modifier.height(20.dp))

        if (state.error != null) {
            Notice(state.error)
            Spacer(Modifier.height(16.dp))
        }

        when {
            state.dataLoading -> LoadingRow("Loading archived habits")

            archived.isEmpty() -> EmptyState(
                title = "Nothing archived",
                body = "Archiving a habit hides it from Today but keeps every check-in. You can restore it here.",
                icon = {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(26.dp),
                    )
                },
            )

            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                archived.forEach { habit ->
                    AppCard {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clickable { onOpenHabit(habit.id) },
                            ) {
                                Text(
                                    habit.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AppColors.Text,
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    Weekdays.formatSchedule(habit.toSchedule()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.Muted,
                                )
                            }
                            SecondaryButton(
                                text = "Restore",
                                onClick = { onRestore(habit.id) },
                                enabled = !state.actionBusy,
                            )
                        }
                    }
                }
            }
        }
    }
}

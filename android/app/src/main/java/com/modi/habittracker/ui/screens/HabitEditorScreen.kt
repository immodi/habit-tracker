package com.modi.habittracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modi.habittracker.data.Habit
import com.modi.habittracker.data.HabitDraft
import com.modi.habittracker.logic.ScheduleType
import com.modi.habittracker.logic.Weekdays
import com.modi.habittracker.ui.AppUiState
import com.modi.habittracker.ui.components.AppCard
import com.modi.habittracker.ui.components.EmptyState
import com.modi.habittracker.ui.components.LoadingRow
import com.modi.habittracker.ui.components.Notice
import com.modi.habittracker.ui.components.PageHeading
import com.modi.habittracker.ui.components.PrimaryButton
import com.modi.habittracker.ui.components.SecondaryButton
import com.modi.habittracker.ui.components.WeekdayChips
import com.modi.habittracker.ui.theme.AppColors

private const val ALL_DAYS_MASK = 0b11111110

@Composable
fun HabitEditorScreen(
    state: AppUiState,
    habitId: String?,
    onBack: () -> Unit,
    onCreate: (HabitDraft) -> Unit,
    onUpdate: (habitId: String, draft: HabitDraft) -> Unit,
    onArchive: (habitId: String) -> Unit,
) {
    val isEditing = habitId != null
    val habit: Habit? = habitId?.let { id -> state.habits.firstOrNull { it.id == id } }

    var name by rememberSaveable(habit?.id) { mutableStateOf(habit?.name ?: "") }
    var scheduleTypeValue by rememberSaveable(habit?.id) {
        mutableStateOf(habit?.scheduleType ?: ScheduleType.WEEKDAYS.storedValue)
    }
    var mask by rememberSaveable(habit?.id) {
        mutableIntStateOf(
            habit?.weekdays?.fold(0) { acc, day -> acc or (1 shl day) }
                ?.takeIf { it != 0 }
                ?: ALL_DAYS_MASK,
        )
    }
    var targetCount by rememberSaveable(habit?.id) {
        mutableIntStateOf(habit?.targetCount?.coerceAtLeast(1) ?: 3)
    }
    var localError by remember { mutableStateOf<String?>(null) }
    var confirmArchive by rememberSaveable { mutableStateOf(false) }

    val scheduleType = ScheduleType.fromStored(scheduleTypeValue)
    val selected = (1..7).filter { mask and (1 shl it) != 0 }
    val busy = state.actionBusy
    val error = localError ?: state.error

    fun chooseScheduleType(next: ScheduleType) {
        if (next == scheduleType) return
        val previous = scheduleType
        scheduleTypeValue = next.storedValue
        if (next.isFlexible) {
            targetCount = if (previous.isFlexible) {
                targetCount.coerceIn(1, next.maxTarget())
            } else {
                ScheduleType.defaultTarget(next)
            }
        }
    }

    fun submit() {
        val trimmed = name.trim()
        when {
            trimmed.isEmpty() -> localError = "Give the habit a name."
            scheduleType == ScheduleType.WEEKDAYS && selected.isEmpty() ->
                localError = "Pick at least one day."
            else -> {
                localError = null
                val draft = HabitDraft(
                    name = trimmed,
                    scheduleType = scheduleType,
                    weekdays = if (scheduleType == ScheduleType.WEEKDAYS) selected else emptyList(),
                    targetCount = if (scheduleType == ScheduleType.WEEKDAYS) {
                        1
                    } else {
                        targetCount.coerceIn(1, scheduleType.maxTarget())
                    },
                )
                if (habit == null) onCreate(draft) else onUpdate(habit.id, draft)
            }
        }
    }

    if (isEditing && habit == null) {
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
                habit?.name ?: "Today",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Muted,
            )
        }

        Spacer(Modifier.height(10.dp))
        PageHeading(
            eyebrow = if (isEditing) "Edit habit" else "New habit",
            title = if (isEditing) "Fine-tune it" else "What will you do?",
        )
        Spacer(Modifier.height(20.dp))

        AppCard {
            Column(Modifier.padding(20.dp)) {
                FieldLabel("Name")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Read 20 minutes", color = AppColors.MutedDim)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = editorFieldColors(),
                )

                Spacer(Modifier.height(20.dp))
                FieldLabel("How often")
                Spacer(Modifier.height(8.dp))
                RecurrenceChips(scheduleType, ::chooseScheduleType)

                Spacer(Modifier.height(20.dp))
                if (scheduleType == ScheduleType.WEEKDAYS) {
                    FieldLabel("Days")
                    Spacer(Modifier.height(8.dp))
                    WeekdayChips(
                        selected = selected.toSet(),
                        onToggle = { day -> mask = mask xor (1 shl day) },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (selected.isEmpty()) {
                            "Pick at least one day."
                        } else {
                            "Scheduled ${Weekdays.format(selected).lowercase()}."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.MutedDim,
                    )
                } else {
                    FieldLabel("Target")
                    Spacer(Modifier.height(8.dp))
                    TargetStepper(
                        target = targetCount,
                        type = scheduleType,
                        onChange = { targetCount = it },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "One check-in per day. Extra check-ins beyond the target are fine; they do not extend the streak further.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.MutedDim,
                    )
                }

                if (error != null) {
                    Spacer(Modifier.height(14.dp))
                    Notice(error)
                }

                Spacer(Modifier.height(22.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PrimaryButton(
                        text = if (isEditing) "Save changes" else "Create habit",
                        onClick = ::submit,
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                    )
                    SecondaryButton("Cancel", onBack, enabled = !busy)
                }
            }
        }

        if (habit != null) {
            Spacer(Modifier.height(18.dp))
            AppCard {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        if (confirmArchive) "Archive this habit?" else "Archive habit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "It disappears from Today, keeps its history, and can be restored later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Muted,
                    )
                    Spacer(Modifier.height(14.dp))
                    SecondaryButton(
                        text = if (confirmArchive) "Yes, archive" else "Archive",
                        onClick = {
                            if (confirmArchive) {
                                onArchive(habit.id)
                            } else {
                                confirmArchive = true
                            }
                        },
                        danger = confirmArchive,
                        enabled = !busy,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurrenceChips(
    selected: ScheduleType,
    onSelect: (ScheduleType) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScheduleType.choices.forEach { type ->
            val on = type == selected
            Surface(
                onClick = { onSelect(type) },
                shape = RoundedCornerShape(50),
                color = if (on) {
                    AppColors.Accent.copy(alpha = 0.14f)
                } else {
                    AppColors.BackgroundSoft
                },
                border = BorderStroke(
                    1.dp,
                    if (on) AppColors.Accent.copy(alpha = 0.45f) else AppColors.Line,
                ),
                modifier = Modifier.height(38.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        ScheduleType.optionLabel(type),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (on) AppColors.AccentStrong else AppColors.Muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetStepper(
    target: Int,
    type: ScheduleType,
    onChange: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StepperButton(
            label = "−",
            enabled = target > 1,
            onClick = { onChange((target - 1).coerceAtLeast(1)) },
        )
        OutlinedTextField(
            value = target.toString(),
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(3)
                onChange(digits.toIntOrNull()?.coerceIn(1, type.maxTarget()) ?: 1)
            },
            modifier = Modifier.width(86.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = editorFieldColors(),
        )
        StepperButton(
            label = "+",
            enabled = target < type.maxTarget(),
            onClick = { onChange((target + 1).coerceAtMost(type.maxTarget())) },
        )
        Column(Modifier.weight(1f)) {
            Text(
                "times per ${type.periodName}",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Muted,
            )
            Text(
                "Any day counts.",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.MutedDim,
            )
        }
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppColors.LineStrong),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(44.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = if (enabled) AppColors.Text else AppColors.MutedDim,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        color = AppColors.MutedDim,
    )
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.Accent.copy(alpha = 0.55f),
    unfocusedBorderColor = AppColors.LineStrong,
    focusedContainerColor = AppColors.BackgroundSoft,
    unfocusedContainerColor = AppColors.BackgroundSoft,
    cursorColor = AppColors.Accent,
    focusedTextColor = AppColors.Text,
    unfocusedTextColor = AppColors.Text,
)

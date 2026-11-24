package com.modi.habittracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.modi.habittracker.R
import com.modi.habittracker.data.Habit
import com.modi.habittracker.data.toSchedule
import com.modi.habittracker.logic.DayCell
import com.modi.habittracker.logic.DayStatus
import com.modi.habittracker.logic.PeriodProgress
import com.modi.habittracker.logic.Recurrence
import com.modi.habittracker.logic.ScheduleType
import com.modi.habittracker.logic.Streak
import com.modi.habittracker.logic.StreakCalculator
import com.modi.habittracker.logic.Weekdays
import com.modi.habittracker.ui.theme.AppColors
import java.time.LocalDate

enum class TopDestination(val route: String, val label: String) {
    TODAY("today", "Today"),
    ARCHIVED("archived", "Archived"),
    SETTINGS("settings", "Settings"),
}

@Composable
fun BrandMark(size: Dp = 28.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(Brush.linearGradient(listOf(Color(0xFF8FF0BE), Color(0xFF43C98A)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = AppColors.AccentInk,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

@Composable
fun AppTopBar(current: TopDestination, onNavigate: (TopDestination) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(AppColors.Background)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark()
                Spacer(Modifier.width(10.dp))
                Text(
                    "Habits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TopBarButton(
                    selected = current == TopDestination.TODAY,
                    label = TopDestination.TODAY.label,
                    onClick = { onNavigate(TopDestination.TODAY) },
                ) {
                    Icon(
                        painterResource(R.drawable.ic_habit_list),
                        contentDescription = TopDestination.TODAY.label,
                        tint = if (current == TopDestination.TODAY) {
                            AppColors.Accent
                        } else {
                            AppColors.Muted
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
                TopBarButton(
                    selected = current == TopDestination.ARCHIVED,
                    label = TopDestination.ARCHIVED.label,
                    onClick = { onNavigate(TopDestination.ARCHIVED) },
                ) {
                    Icon(
                        painterResource(R.drawable.ic_archive),
                        contentDescription = TopDestination.ARCHIVED.label,
                        tint = if (current == TopDestination.ARCHIVED) {
                            AppColors.Accent
                        } else {
                            AppColors.Muted
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
                TopBarButton(
                    selected = current == TopDestination.SETTINGS,
                    label = TopDestination.SETTINGS.label,
                    onClick = { onNavigate(TopDestination.SETTINGS) },
                ) {
                    Icon(
                        painterResource(R.drawable.ic_settings_gear),
                        contentDescription = TopDestination.SETTINGS.label,
                        tint = if (current == TopDestination.SETTINGS) {
                            AppColors.Accent
                        } else {
                            AppColors.Muted
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        HorizontalDivider(color = AppColors.Line)
    }
}

@Composable
private fun TopBarButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AppColors.SurfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
fun PageHeading(
    eyebrow: String?,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        if (eyebrow != null) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                color = AppColors.MutedDim,
            )
            Spacer(Modifier.height(6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
    }
}

@Composable
fun ProgressRing(percent: Int, modifier: Modifier = Modifier, size: Dp = 62.dp) {
    val accent = AppColors.Accent
    val track = AppColors.SurfaceVariant
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            if (percent > 0) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * percent / 100f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            "$percent%",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SummaryCard(
    done: Int,
    total: Int,
    remaining: Int,
    modifier: Modifier = Modifier,
    hasFlexible: Boolean = false,
) {
    val percent = if (total == 0) 0 else ((done.toFloat() / total) * 100).toInt()
    val animatedPercent by animateIntAsState(
        targetValue = percent,
        animationSpec = tween(450),
        label = "summaryPercent",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.Line),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(animatedPercent)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$done of $total",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (hasFlexible) " done" else " done today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.Muted,
                    )
                }
                Spacer(Modifier.height(10.dp))
                ProgressBar(percent / 100f)
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        total == 0 -> "Nothing is scheduled for today."
                        remaining == 0 -> "Everything is on track. Nicely done."
                        remaining == 1 -> "1 habit still needs attention."
                        else -> "$remaining habits still need attention."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.MutedDim,
                )
            }
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(450),
        label = "progressFraction",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(AppColors.SurfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animatedFraction)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF5FDD9C), Color(0xFF8FF0BE)),
                    ),
                ),
        )
    }
}

@Composable
fun WeekStrip(cells: List<DayCell>, today: LocalDate, modifier: Modifier = Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEach { cell ->
            val isToday = cell.date == today
            Box(
                Modifier
                    .size(12.dp)
                    .then(
                        if (isToday) {
                            Modifier.border(1.dp, AppColors.LineStrong, CircleShape)
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val fill = when (cell.status) {
                    DayStatus.DONE -> AppColors.Accent
                    DayStatus.MISSED -> Color.Transparent
                    DayStatus.EMPTY -> Color.White.copy(alpha = 0.075f)
                    DayStatus.OFF -> Color.White.copy(alpha = 0.06f)
                    DayStatus.FUTURE -> Color.Transparent
                }
                val border = when (cell.status) {
                    DayStatus.MISSED -> AppColors.LineStrong
                    DayStatus.FUTURE -> AppColors.LineStrong.copy(alpha = 0.4f)
                    else -> Color.Transparent
                }
                Box(
                    Modifier
                        .size(7.dp)
                        .background(fill, CircleShape)
                        .border(1.dp, border, CircleShape),
                )
            }
        }
    }
}

@Composable
fun StreakPill(streak: Streak, type: ScheduleType, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = AppColors.Streak.copy(alpha = 0.12f),
    ) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_flame),
                contentDescription = null,
                tint = AppColors.Streak,
                modifier = Modifier.size(12.dp),
            )
            Text(
                "${streak.value}${if (streak.capped) "+" else ""}${type.streakSuffix}",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Streak,
            )
        }
    }
}

@Composable
fun PeriodProgressCard(
    progress: PeriodProgress,
    type: ScheduleType,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.Line),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    "This ${type.periodName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Muted,
                )
                Text(
                    "${minOf(progress.count, progress.target)} of ${progress.target}",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(progress.count.toFloat() / progress.target)
            Spacer(Modifier.height(10.dp))
            Text(
                if (progress.met) {
                    "Target met. Extra check-ins are fine."
                } else {
                    "Any day can count; one check-in per day."
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.MutedDim,
            )
        }
    }
}

@Composable
fun HabitCard(
    habit: Habit,
    logs: Set<LocalDate>,
    today: LocalDate,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val schedule = habit.toSchedule()
    val done = today in logs
    val streak = StreakCalculator.currentStreak(schedule, logs, today)
    val week = StreakCalculator.dayCells(schedule, logs, today, 7)
    val progress = if (schedule.type.isFlexible) {
        Recurrence.progress(schedule, logs, today)
    } else {
        null
    }
    val statusText = when {
        progress != null ->
            "${minOf(progress.count, progress.target)} of ${progress.target} ${
                Recurrence.thisPeriodLabel(schedule.type)
            }"
        done -> "Done today"
        else -> "Not done yet"
    }

    val container by animateColorAsState(
        targetValue = if (done) {
            AppColors.Accent.copy(alpha = 0.06f).compositeOver(AppColors.Surface)
        } else {
            AppColors.Surface
        },
        animationSpec = tween(220),
        label = "habitContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (done) AppColors.Accent.copy(alpha = 0.26f) else AppColors.Line,
        animationSpec = tween(220),
        label = "habitBorder",
    )
    val toggleFill by animateColorAsState(
        targetValue = if (done) AppColors.Accent else Color.Transparent,
        animationSpec = tween(200),
        label = "toggleFill",
    )
    val toggleBorder by animateColorAsState(
        targetValue = if (done) AppColors.Accent else AppColors.LineStrong,
        animationSpec = tween(200),
        label = "toggleBorder",
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = tween(180),
        label = "toggleCheck",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (done) 1f else 0.7f,
        animationSpec = tween(200),
        label = "toggleCheckScale",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (done) Color(0xFFC9D4E2) else AppColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpen)
                        .semantics {
                            contentDescription = "Open ${habit.name}"
                            role = Role.Button
                        },
                )
                if (streak.value > 0) {
                    Spacer(Modifier.width(10.dp))
                    StreakPill(streak, schedule.type)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onToggle,
                    shape = CircleShape,
                    color = toggleFill,
                    border = BorderStroke(2.dp, toggleBorder),
                    modifier = Modifier
                        .size(46.dp)
                        .semantics {
                            contentDescription = if (done) {
                                "Mark ${habit.name} not done"
                            } else {
                                "Mark ${habit.name} done"
                            }
                            role = Role.Button
                        },
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = AppColors.AccentInk.copy(alpha = checkAlpha),
                            modifier = Modifier
                                .size(22.dp)
                                .scale(checkScale),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.MutedDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                WeekStrip(week, today)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekdayChips(
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..7).forEach { day ->
            val on = day in selected
            Surface(
                onClick = { onToggle(day) },
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
                modifier = Modifier
                    .height(38.dp)
                    .semantics {
                        contentDescription = Weekdays.label(day)
                        role = Role.Button
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        Weekdays.label(day),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (on) AppColors.AccentStrong else AppColors.Muted,
                    )
                }
            }
        }
    }
}

@Composable
fun Notice(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = true,
) {
    val color = if (isError) AppColors.Danger else AppColors.Accent
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) Color(0xFFFFB4B3) else Color(0xFFA9F2CC),
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = AppColors.LineStrong.copy(alpha = 0.5f),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                        ),
                    ),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                )
            }
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.Accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Spacer(Modifier.height(16.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.Muted,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = AppColors.AccentInk,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = AppColors.AccentInk,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
        modifier = modifier
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.55f)
            .background(
                Brush.linearGradient(listOf(Color(0xFF7CEBB4), Color(0xFF5FDD9C))),
                shape,
            ),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (danger) AppColors.Danger.copy(alpha = 0.32f) else AppColors.Line,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (danger) AppColors.Danger else AppColors.Text,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.height(44.dp),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.Line),
    ) {
        Column(content = content)
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.Line),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = AppColors.Streak,
                        modifier = Modifier.size(13.dp),
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.MutedDim,
                )
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.MutedDim,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}

@Composable
fun LoadingRow(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = AppColors.Accent,
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
    }
}

@Composable
fun BootScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = AppColors.Accent,
        )
    }
}

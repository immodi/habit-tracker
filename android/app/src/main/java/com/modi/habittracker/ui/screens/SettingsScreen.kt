package com.modi.habittracker.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.modi.habittracker.logic.DateKeys
import com.modi.habittracker.ui.AppUiState
import com.modi.habittracker.ui.components.AppCard
import com.modi.habittracker.ui.components.LoadingRow
import com.modi.habittracker.ui.components.Notice
import com.modi.habittracker.ui.components.PageHeading
import com.modi.habittracker.ui.theme.AppColors
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    state: AppUiState,
    onSignOut: () -> Unit,
    onSetReminder: (String?) -> Unit,
    onExport: suspend () -> String,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            localError = "Notifications are blocked. Allow them in system settings to receive reminders."
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            exporting = true
            localError = null
            message = null
            try {
                val json = onExport()
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri)
                        ?: error("Could not open the file for writing.")
                    stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
                message = "Exported every habit and check-in."
            } catch (error: Exception) {
                localError = error.message ?: "Export failed."
            } finally {
                exporting = false
            }
        }
    }

    fun openTimePicker() {
        val parts = state.reminderTime?.split(":")
        val hour = parts?.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts?.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, pickedHour, pickedMinute ->
                onSetReminder(String.format(Locale.US, "%02d:%02d", pickedHour, pickedMinute))
                message = "Reminder set for ${String.format(Locale.US, "%02d:%02d", pickedHour, pickedMinute)}."
            },
            hour,
            minute,
            DateFormat.is24HourFormat(context),
        ).show()
    }

    if (state.user == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) { LoadingRow("Loading account") }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 28.dp),
    ) {
        PageHeading("Your account", "Settings")
        Spacer(Modifier.height(20.dp))

        if (state.error != null) {
            Notice(state.error)
            Spacer(Modifier.height(16.dp))
        }

        AppCard {
            Column {
                SettingsRow(
                    title = "Signed in as",
                    subtitle = state.user.email ?: state.user.uid,
                )
                HorizontalDivider(color = AppColors.Line)
                SettingsRow(
                    title = "Reminder",
                    subtitle = if (state.reminderTime == null) {
                        "Off. Android posts one notification a day when habits are left unchecked."
                    } else {
                        "Every day at ${state.reminderTime}. Android only."
                    },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.reminderTime != null) {
                                TextButton(
                                    onClick = {
                                        onSetReminder(null)
                                        message = "Reminder turned off."
                                    },
                                    enabled = !state.actionBusy,
                                ) {
                                    Text(
                                        "Off",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppColors.Muted,
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    localError = null
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermission.launch(
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        )
                                    }
                                    openTimePicker()
                                },
                                enabled = !state.actionBusy,
                            ) {
                                Text(
                                    if (state.reminderTime == null) "Set time" else "Change",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppColors.Accent,
                                )
                            }
                        }
                    },
                )
                HorizontalDivider(color = AppColors.Line)
                SettingsRow(
                    title = "Export data",
                    subtitle = "Download every habit and check-in as JSON.",
                    trailing = {
                        TextButton(
                            onClick = {
                                localError = null
                                message = null
                                exportLauncher.launch(
                                    "habit-tracker-${DateKeys.format(LocalDate.now())}.json",
                                )
                            },
                            enabled = !exporting,
                        ) {
                            Text(
                                if (exporting) "Preparing…" else "Export",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.Accent,
                            )
                        }
                    },
                )
                HorizontalDivider(color = AppColors.Line)
                SettingsRow(
                    title = "Sign out",
                    subtitle = "Your data stays safe in your account.",
                    trailing = {
                        TextButton(onClick = onSignOut) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AppColors.Muted,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Sign out",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.Muted,
                            )
                        }
                    },
                )
            }
        }

        if (message != null) {
            Spacer(Modifier.height(16.dp))
            Notice(message!!, isError = false)
        }
        if (localError != null) {
            Spacer(Modifier.height(16.dp))
            Notice(localError!!)
        }

        Spacer(Modifier.height(26.dp))
        Text(
            "Habit Tracker · synced with Firebase. Private to your account.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.MutedDim,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Muted,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

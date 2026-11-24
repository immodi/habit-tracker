package com.modi.habittracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.modi.habittracker.data.AuthRepository
import com.modi.habittracker.data.Habit
import com.modi.habittracker.data.HabitDraft
import com.modi.habittracker.data.HabitRepository
import com.modi.habittracker.data.toSchedule
import com.modi.habittracker.logic.DateKeys
import com.modi.habittracker.logic.Recurrence
import com.modi.habittracker.reminders.ReminderScheduler
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AppUiState(
    val authLoading: Boolean = true,
    val authBusy: Boolean = false,
    val user: FirebaseUser? = null,
    val today: LocalDate = LocalDate.now(),
    val habits: List<Habit> = emptyList(),
    /** habitId -> set of yyyy-MM-dd check-in dates within the loaded window. */
    val logs: Map<String, Set<String>> = emptyMap(),
    val dataLoading: Boolean = true,
    val actionBusy: Boolean = false,
    val reminderTime: String? = null,
    val error: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()
    private val repository = HabitRepository()
    private val reminderScheduler = ReminderScheduler(application)

    private val todayFlow = MutableStateFlow(LocalDate.now())
    private val _state = MutableStateFlow(AppUiState(today = todayFlow.value))
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var dataJob: Job? = null

    init {
        watchAuth()
        watchMidnight()
    }

    private fun watchAuth() {
        viewModelScope.launch {
            authRepository.authState.collect { user ->
                val previousUid = _state.value.user?.uid
                _state.update { it.copy(user = user, authLoading = false) }

                dataJob?.cancel()
                if (user == null) {
                    _state.update {
                        it.copy(
                            habits = emptyList(),
                            logs = emptyMap(),
                            reminderTime = null,
                            dataLoading = false,
                        )
                    }
                    reminderScheduler.schedule(null)
                } else if (previousUid != user.uid) {
                    _state.update { it.copy(dataLoading = true, error = null) }
                    dataJob = viewModelScope.launch { observeData(user.uid) }
                }
            }
        }
    }

    private fun watchMidnight() {
        viewModelScope.launch {
            while (isActive) {
                val now = ZonedDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1)
                    .atStartOfDay(now.zone)
                    .plusSeconds(2)
                delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000))
                todayFlow.value = LocalDate.now()
                _state.update { it.copy(today = todayFlow.value) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeData(uid: String) {
        val habitsFlow = repository.observeHabits(uid)
        val reminderFlow = repository.observeReminderTime(uid)

        // Re-subscribe when the needed history window changes (daily or new habits).
        val base = combine(habitsFlow, todayFlow) { habits, today -> habits to today }
            .flatMapLatest { (habits, today) ->
                val start = DateKeys.format(
                    Recurrence.historyStart(habits.map { it.toSchedule() }, today),
                )
                repository.observeLogs(uid, start).map { logs ->
                    Snapshot(habits, logs, today)
                }
            }

        combine(base, reminderFlow) { snapshot, reminder ->
            snapshot.copy(reminder = reminder)
        }
            .catch { error ->
                _state.update {
                    it.copy(
                        error = error.message ?: "Could not sync data.",
                        dataLoading = false,
                    )
                }
            }
            .collect { snapshot ->
                _state.update {
                    it.copy(
                        habits = snapshot.habits,
                        logs = snapshot.logs,
                        today = snapshot.today,
                        reminderTime = snapshot.reminder,
                        dataLoading = false,
                    )
                }
                reminderScheduler.schedule(snapshot.reminder)
            }
    }

    private data class Snapshot(
        val habits: List<Habit>,
        val logs: Map<String, Set<String>>,
        val today: LocalDate,
        val reminder: String? = null,
    )

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun signIn(email: String, password: String) = runAuth {
        authRepository.signIn(email, password)
    }

    fun signUp(email: String, password: String) = runAuth {
        authRepository.signUp(email, password)
    }

    fun signInWithGoogle(idToken: String) = runAuth {
        authRepository.signInWithGoogle(idToken)
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun createHabit(draft: HabitDraft, onSuccess: () -> Unit) =
        runAction(onSuccess) { uid ->
            repository.addHabit(uid, draft)
        }

    fun updateHabit(habitId: String, draft: HabitDraft, onSuccess: () -> Unit) =
        runAction(onSuccess) { uid ->
            repository.updateHabit(uid, habitId, draft)
        }

    fun archiveHabit(habitId: String, onSuccess: () -> Unit) = runAction(onSuccess) { uid ->
        repository.setArchived(uid, habitId, archived = true)
    }

    fun restoreHabit(habitId: String) = runAction { uid ->
        repository.setArchived(uid, habitId, archived = false)
    }

    fun toggleCheckIn(habitId: String, dateKey: String, done: Boolean) {
        val uid = _state.value.user?.uid ?: return
        viewModelScope.launch {
            try {
                repository.setCheckIn(uid, habitId, dateKey, done)
            } catch (error: Exception) {
                _state.update { it.copy(error = error.message ?: "Could not save the check-in.") }
            }
        }
    }

    fun setReminderTime(time: String?) = runAction { uid ->
        repository.setReminderTime(uid, time)
        reminderScheduler.schedule(time)
    }

    suspend fun exportJson(): String {
        val uid = _state.value.user?.uid ?: error("Not signed in")
        return repository.exportJson(uid)
    }

    private fun runAuth(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(authBusy = true, error = null) }
            try {
                block()
            } catch (error: Exception) {
                _state.update { it.copy(error = authErrorMessage(error)) }
            } finally {
                _state.update { it.copy(authBusy = false) }
            }
        }
    }

    private fun runAction(onSuccess: (() -> Unit)? = null, block: suspend (String) -> Unit) {
        val uid = _state.value.user?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(actionBusy = true, error = null) }
            try {
                block(uid)
                onSuccess?.invoke()
            } catch (error: Exception) {
                _state.update {
                    it.copy(error = error.message ?: "Something went wrong. Please try again.")
                }
            } finally {
                _state.update { it.copy(actionBusy = false) }
            }
        }
    }

    private fun authErrorMessage(error: Exception): String {
        val code = (error as? FirebaseAuthException)?.errorCode
        return when (code) {
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_WRONG_PASSWORD",
            "ERROR_USER_NOT_FOUND",
            -> "That email and password combination does not match an account."

            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "An account with this email already exists. Try signing in instead."

            "ERROR_WEAK_PASSWORD" -> "Choose a password with at least 6 characters."
            "ERROR_INVALID_EMAIL" -> "That email address does not look right."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a moment and try again."
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "This sign-in method is not enabled for the project yet."

            else -> error.message ?: "Sign-in failed. Please try again."
        }
    }
}

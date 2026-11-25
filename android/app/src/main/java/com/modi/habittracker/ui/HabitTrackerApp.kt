package com.modi.habittracker.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.modi.habittracker.ui.components.AppTopBar
import com.modi.habittracker.ui.components.BootScreen
import com.modi.habittracker.ui.components.TopDestination
import com.modi.habittracker.ui.screens.ArchivedScreen
import com.modi.habittracker.ui.screens.AuthScreen
import com.modi.habittracker.ui.screens.HabitDetailScreen
import com.modi.habittracker.ui.screens.HabitEditorScreen
import com.modi.habittracker.ui.screens.SettingsScreen
import com.modi.habittracker.ui.screens.TodayScreen
import com.modi.habittracker.ui.theme.AppColors

@Composable
fun HabitTrackerApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize(), color = AppColors.Background) {
        when {
            state.authLoading -> BootScreen()
            state.user == null -> AuthScreen(
                state = state,
                onSignIn = viewModel::signIn,
                onSignUp = viewModel::signUp,
                onSignInWithGoogle = viewModel::signInWithGoogle,
                onClearError = viewModel::clearError,
            )
            else -> SignedInApp(state, viewModel)
        }
    }
}

@Composable
private fun SignedInApp(state: AppUiState, viewModel: MainViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val topDestination = when (route) {
        null, TopDestination.TODAY.route -> TopDestination.TODAY
        TopDestination.ARCHIVED.route -> TopDestination.ARCHIVED
        TopDestination.SETTINGS.route -> TopDestination.SETTINGS
        else -> null
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            val destination = topDestination
            if (destination != null) {
                AppTopBar(destination) { target ->
                    navigateTop(navController, target)
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.TODAY.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(tween(220)) +
                    slideInVertically(tween(220)) { height -> height / 24 }
            },
            exitTransition = { fadeOut(tween(140)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = {
                fadeOut(tween(140)) +
                    slideOutVertically(tween(140)) { height -> height / 24 }
            },
        ) {
            composable(TopDestination.TODAY.route) {
                TodayScreen(
                    state = state,
                    onToggle = viewModel::toggleCheckIn,
                    onOpenHabit = { habitId -> navController.navigate("habit/$habitId") },
                    onAddHabit = { navController.navigate("new-habit") },
                )
            }

            composable("new-habit") {
                HabitEditorScreen(
                    state = state,
                    habitId = null,
                    onBack = { navController.popBackStack() },
                    onCreate = { draft ->
                        viewModel.createHabit(draft) {
                            navController.popBackStack(TopDestination.TODAY.route, false)
                        }
                    },
                    onUpdate = { _, _ -> },
                    onArchive = {},
                    onRestore = {},
                )
            }

            composable("habit/{habitId}") { entry ->
                val habitId = entry.arguments?.getString("habitId")
                HabitDetailScreen(
                    state = state,
                    habitId = habitId,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        if (habitId != null) navController.navigate("edit/$habitId")
                    },
                    onToggle = viewModel::toggleCheckIn,
                )
            }

            composable("edit/{habitId}") { entry ->
                val habitId = entry.arguments?.getString("habitId")
                HabitEditorScreen(
                    state = state,
                    habitId = habitId,
                    onBack = { navController.popBackStack() },
                    onCreate = { _ -> },
                    onUpdate = { id, draft ->
                        viewModel.updateHabit(id, draft) {
                            navController.popBackStack()
                        }
                    },
                    onArchive = { id ->
                        viewModel.archiveHabit(id) {
                            navController.popBackStack(TopDestination.TODAY.route, false)
                            navController.navigate(TopDestination.ARCHIVED.route) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onRestore = { id ->
                        viewModel.restoreHabit(id) {
                            navController.popBackStack(TopDestination.TODAY.route, false)
                        }
                    },
                )
            }

            composable(TopDestination.ARCHIVED.route) {
                ArchivedScreen(
                    state = state,
                    onRestore = { id -> viewModel.restoreHabit(id) },
                    onOpenHabit = { habitId -> navController.navigate("habit/$habitId") },
                )
            }

            composable(TopDestination.SETTINGS.route) {
                SettingsScreen(
                    state = state,
                    onSignOut = viewModel::signOut,
                    onSetReminder = viewModel::setReminderTime,
                    onExport = viewModel::exportJson,
                )
            }
        }
    }
}

/**
 * Switches between the top-level destinations. If the target is already on the
 * back stack, everything above it is popped instead of pushing a duplicate.
 */
private fun navigateTop(navController: NavHostController, target: TopDestination) {
    if (navController.popBackStack(target.route, inclusive = false)) return
    navController.navigate(target.route) {
        popUpTo(TopDestination.TODAY.route) { inclusive = false }
        launchSingleTop = true
    }
}

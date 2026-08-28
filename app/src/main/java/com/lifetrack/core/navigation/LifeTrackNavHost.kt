package com.lifetrack.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lifetrack.R
import com.lifetrack.calorie.ui.CalorieScreen
import com.lifetrack.dashboard.ui.DashboardScreen
import com.lifetrack.diary.ui.DiaryScreen
import com.lifetrack.expense.ui.ExpenseScreen
import com.lifetrack.goal.ui.GoalScreen
import com.lifetrack.period.ui.PeriodScreen
import com.lifetrack.habit.ui.HabitScreen
import com.lifetrack.settings.ui.SettingsScreen
import com.lifetrack.water.ui.WaterScreen

@Composable
fun LifeTrackNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Dashboard.route,
        modifier = modifier,
    ) {
        composable(Destination.Dashboard.route) {
            DashboardScreen(
                contentPadding = contentPadding,
                onOpen = navController::navigateToTab,
            )
        }
        composable(Destination.Habits.route) {
            HabitScreen(contentPadding = contentPadding)
        }
        composable(Destination.Goals.route) {
            GoalScreen(contentPadding = contentPadding)
        }
        composable(Destination.Expenses.route) {
            ExpenseScreen(contentPadding = contentPadding)
        }
        composable(Destination.Calories.route) {
            CalorieScreen(contentPadding = contentPadding)
        }
        composable(Destination.Water.route) {
            WaterScreen(contentPadding = contentPadding)
        }
        composable(Destination.Period.route) {
            PeriodScreen(contentPadding = contentPadding)
        }
        composable(Destination.Diary.route) {
            DiaryScreen(contentPadding = contentPadding)
        }
        composable(Destination.Settings.route) {
            SettingsScreen(contentPadding = contentPadding)
        }
    }
}

/**
 * Single-instance tab navigation: pops back to the start destination so the back
 * stack cannot grow one entry per tab tap, and restores each tab's own state.
 */
fun NavHostController.navigateToTab(destination: Destination) {
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

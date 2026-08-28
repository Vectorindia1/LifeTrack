package com.lifetrack.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import com.lifetrack.R

/**
 * Every screen in the app. All nine are real destinations in the NavHost, but only
 * those with [inBottomBar] appear in the navigation bar — a Material 3 NavigationBar
 * holds 3–5 items, and CLAUDE.md's UX principles push against clutter. Goals,
 * Calories and Water are reached from dashboard cards instead. See MEMORY.md.
 */
enum class Destination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val inBottomBar: Boolean = false,
) {
    Dashboard("dashboard", R.string.dest_dashboard, Icons.Filled.Today, inBottomBar = true),
    Habits("habits", R.string.dest_habits, Icons.Filled.CheckCircle, inBottomBar = true),
    Expenses("expenses", R.string.dest_expenses, Icons.Filled.AccountBalanceWallet, inBottomBar = true),
    Diary("diary", R.string.dest_diary, Icons.Filled.Book, inBottomBar = true),
    Settings("settings", R.string.dest_settings, Icons.Filled.Settings, inBottomBar = true),

    Goals("goals", R.string.dest_goals, Icons.Filled.Flag),
    Calories("calories", R.string.dest_calories, Icons.Filled.Restaurant),
    Water("water", R.string.dest_water, Icons.Filled.LocalDrink),
    Period("period", R.string.dest_period, Icons.Filled.Bloodtype),
    ;

    companion object {
        val bottomBarDestinations: List<Destination> = entries.filter { it.inBottomBar }

        fun fromRoute(route: String?): Destination? = entries.find { it.route == route }
    }
}

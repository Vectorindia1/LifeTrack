package com.lifetrack.core.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Per-feature accent colors, used for icon badges, progress rings and chart series.
 *
 * MaterialTheme's own primary/secondary/tertiary roles stay in charge of the chrome
 * (buttons, selection, app bar) — these are a second, purely decorative layer that
 * lets each tracker read as its own color at a glance, the way the design reference
 * does. Every accent has a light and dark variant so it holds contrast in both themes.
 */
object Accents {
    val Habit = FeatureAccent(Color(0xFF2FB380), Color(0xFF6FE3B4))
    val Water = FeatureAccent(Color(0xFF2E8FE0), Color(0xFF7CC3FA))
    val Calorie = FeatureAccent(Color(0xFFE0952E), Color(0xFFF6C177))
    val Expense = FeatureAccent(Color(0xFFE0602E), Color(0xFFF6906E))
    val Goal = FeatureAccent(Color(0xFF8B5CF6), Color(0xFFC4A9FA))
    val Diary = FeatureAccent(Color(0xFF6C5CE7), Color(0xFFB5ABF5))
    val Period = FeatureAccent(Color(0xFFD6336C), Color(0xFFF783AC))

    /** A small rotation for goals, which don't otherwise have a natural per-item color. */
    val goalPalette = listOf(
        FeatureAccent(Color(0xFF8B5CF6), Color(0xFFC4A9FA)),
        FeatureAccent(Color(0xFF2FB380), Color(0xFF6FE3B4)),
        FeatureAccent(Color(0xFFE0952E), Color(0xFFF6C177)),
        FeatureAccent(Color(0xFF2E8FE0), Color(0xFF7CC3FA)),
        FeatureAccent(Color(0xFFE0602E), Color(0xFFF6906E)),
    )
}

data class FeatureAccent(val light: Color, val dark: Color)

/**
 * Resolves to the variant that reads well against the current theme's surfaces.
 * Reads [LocalIsDarkTheme] (the app's *effective* theme) rather than
 * `isSystemInDarkTheme()`, since a forced light/dark choice in Settings must win.
 */
val FeatureAccent.resolved: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) dark else light

/** Deterministic per-goal color, stable across recompositions and sessions alike. */
fun goalAccent(id: Long): FeatureAccent = Accents.goalPalette[(id % Accents.goalPalette.size).toInt()]

/** Icon paired with its feature's accent, for the small colored badges used throughout. */
data class FeatureGlyph(val icon: ImageVector, val accent: FeatureAccent)

object FeatureGlyphs {
    val Habit = FeatureGlyph(Icons.Filled.CheckCircle, Accents.Habit)
    val Water = FeatureGlyph(Icons.Filled.LocalDrink, Accents.Water)
    val Calorie = FeatureGlyph(Icons.Filled.Restaurant, Accents.Calorie)
    val Expense = FeatureGlyph(Icons.Filled.AccountBalanceWallet, Accents.Expense)
    val Diary = FeatureGlyph(Icons.Filled.Book, Accents.Diary)
    val Period = FeatureGlyph(Icons.Filled.Bloodtype, Accents.Period)
}

/** A small rotation of icons for goals, paired index-for-index with [Accents.goalPalette]. */
object GoalIcons {
    val icons = listOf(
        Icons.Filled.Savings,
        Icons.Filled.FitnessCenter,
        Icons.AutoMirrored.Filled.DirectionsRun,
        Icons.AutoMirrored.Filled.MenuBook,
        Icons.Filled.Flag,
    )
}

fun goalIcon(id: Long): androidx.compose.ui.graphics.vector.ImageVector =
    GoalIcons.icons[(id % GoalIcons.icons.size).toInt()]

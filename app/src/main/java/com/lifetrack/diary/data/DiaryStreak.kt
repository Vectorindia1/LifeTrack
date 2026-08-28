package com.lifetrack.diary.data

import java.time.LocalDate

/**
 * Diary streak, as a pure function so it is unit-testable.
 *
 * Same grace rule as habits (see `HabitSchedule`): today not being written yet does
 * not break the streak, because the day is not over. Missing yesterday does.
 */
object DiaryStreak {

    fun current(entryDates: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in entryDates) today else today.minusDays(1)
        var streak = 0
        while (cursor in entryDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}

/**
 * The numbers behind PRD 7.7's auto-prefilled summary line
 * ("3/4 habits done, ₹450 spent, 1.8L water").
 *
 * Kept as plain data so the ViewModel can assemble it and the UI can format it with
 * string resources — no Android types here.
 */
data class DaySummary(
    val habitsDone: Int = 0,
    val habitsDue: Int = 0,
    val spent: Double = 0.0,
    val waterMl: Int = 0,
    val calories: Int = 0,
) {
    /** Nothing tracked that day means there is nothing worth prefilling. */
    val isEmpty: Boolean
        get() = habitsDue == 0 && spent == 0.0 && waterMl == 0 && calories == 0
}

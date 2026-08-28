package com.lifetrack.habit.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * All habit scheduling, streak and completion-rate maths. Pure functions over
 * plain values — no Room, no Android — so this is unit-testable on the JVM, which
 * matters because the streak rules are the easiest thing in the app to get subtly
 * wrong. See `HabitScheduleTest`.
 */
object HabitSchedule {

    /** Bit 0 = Monday … bit 6 = Sunday, matching [Habit.daysOfWeekMask]. */
    fun dayBit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun maskOf(days: Set<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or dayBit(d) }

    fun daysOf(mask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filterTo(mutableSetOf()) { mask and dayBit(it) != 0 }

    /**
     * Whether the habit is expected on [date].
     *
     * [FrequencyType.WEEKLY] habits are a target of N completions per week rather
     * than specific days, so every day is a legitimate day to log one.
     */
    fun isScheduledOn(habit: Habit, date: LocalDate): Boolean = when (habit.frequencyType) {
        FrequencyType.DAILY -> true
        FrequencyType.WEEKLY -> true
        FrequencyType.CUSTOM_DAYS -> habit.daysOfWeekMask and dayBit(date.dayOfWeek) != 0
    }

    /** Monday-based week start, matching [Habit.daysOfWeekMask]'s bit order. */
    fun weekStart(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

    /** Weekly habits default to once a week if no explicit target was set. */
    fun weeklyTarget(habit: Habit): Int = (habit.timesPerWeek ?: 1).coerceAtLeast(1)

    /**
     * Current streak.
     *
     * Day-based habits ([FrequencyType.DAILY], [FrequencyType.CUSTOM_DAYS]) count
     * consecutive *scheduled* days completed — an unscheduled day never breaks a
     * streak. Weekly habits count consecutive weeks that met their target.
     *
     * Today (or the current week) is treated as a grace period: it is still in
     * progress, so failing to have logged it *yet* does not zero the streak, it
     * simply is not counted.
     */
    fun currentStreak(
        habit: Habit,
        completedDates: Set<LocalDate>,
        today: LocalDate,
    ): Int = when (habit.frequencyType) {
        FrequencyType.WEEKLY -> weeklyStreak(habit, completedDates, today)
        else -> dailyStreak(habit, completedDates, today)
    }

    private fun dailyStreak(habit: Habit, completed: Set<LocalDate>, today: LocalDate): Int {
        val createdOn = habit.createdAtDate()
        var cursor = today
        var streak = 0
        var isToday = true

        while (!cursor.isBefore(createdOn)) {
            if (isScheduledOn(habit, cursor)) {
                if (cursor in completed) {
                    streak++
                } else if (isToday) {
                    // Today is not over yet — skip it rather than break the streak.
                } else {
                    break
                }
            }
            cursor = cursor.minusDays(1)
            isToday = false
        }
        return streak
    }

    private fun weeklyStreak(habit: Habit, completed: Set<LocalDate>, today: LocalDate): Int {
        val target = weeklyTarget(habit)
        val createdWeek = weekStart(habit.createdAtDate())
        var cursor = weekStart(today)
        var streak = 0
        var isCurrentWeek = true

        while (!cursor.isBefore(createdWeek)) {
            val hits = completed.count { !it.isBefore(cursor) && it.isBefore(cursor.plusWeeks(1)) }
            if (hits >= target) {
                streak++
            } else if (isCurrentWeek) {
                // The week is still running — don't count it, don't break on it.
            } else {
                break
            }
            cursor = cursor.minusWeeks(1)
            isCurrentWeek = false
        }
        return streak
    }

    /**
     * Completion rate in [0f, 1f] over [from]..[to] inclusive.
     *
     * Days before the habit existed and days after [today] are excluded, so a habit
     * created on Thursday is not punished for Monday–Wednesday. Returns null when the
     * window contains nothing the habit was ever due for — the caller renders that as
     * an empty bar rather than a misleading zero.
     */
    fun completionRate(
        habit: Habit,
        completed: Set<LocalDate>,
        from: LocalDate,
        to: LocalDate,
        today: LocalDate,
    ): Float? {
        val start = maxOf(from, habit.createdAtDate())
        val end = minOf(to, today)
        if (end.isBefore(start)) return null

        if (habit.frequencyType == FrequencyType.WEEKLY) {
            // Pro-rate the weekly target across however much of the window is in range.
            val days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1
            val target = weeklyTarget(habit) * days / 7.0
            if (target <= 0.0) return null
            val hits = completed.count { !it.isBefore(start) && !it.isAfter(end) }
            return (hits / target).coerceIn(0.0, 1.0).toFloat()
        }

        var scheduled = 0
        var done = 0
        var cursor = start
        while (!cursor.isAfter(end)) {
            if (isScheduledOn(habit, cursor)) {
                scheduled++
                if (cursor in completed) done++
            }
            cursor = cursor.plusDays(1)
        }
        return if (scheduled == 0) null else done.toFloat() / scheduled
    }

    /** Aggregate completion rate across several habits, averaging the ones that apply. */
    fun aggregateRate(
        habits: List<Habit>,
        completedByHabit: Map<Long, Set<LocalDate>>,
        from: LocalDate,
        to: LocalDate,
        today: LocalDate,
    ): Float? {
        val rates = habits.mapNotNull { habit ->
            completionRate(habit, completedByHabit[habit.id].orEmpty(), from, to, today)
        }
        return if (rates.isEmpty()) null else rates.average().toFloat()
    }

    /** The last [count] Monday-started weeks, oldest first, ending with the current week. */
    fun recentWeeks(today: LocalDate, count: Int): List<ClosedRange<LocalDate>> {
        val thisWeek = weekStart(today)
        return (count - 1 downTo 0).map { back ->
            val start = thisWeek.minusWeeks(back.toLong())
            start..start.plusDays(6)
        }
    }

    /** The last [count] calendar months, oldest first, ending with the current month. */
    fun recentMonths(today: LocalDate, count: Int): List<ClosedRange<LocalDate>> {
        val thisMonth = YearMonth.from(today)
        return (count - 1 downTo 0).map { back ->
            val month = thisMonth.minusMonths(back.toLong())
            month.atDay(1)..month.atEndOfMonth()
        }
    }
}

/** The local date a habit was created, for "don't count days before I existed" logic. */
fun Habit.createdAtDate(): LocalDate =
    createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()

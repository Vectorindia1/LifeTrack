package com.lifetrack.goal.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Goal maths as pure functions, so the fiddly parts — deadline arithmetic and
 * "is this goal still active" — are unit-testable. See `GoalProgressTest`.
 */
object GoalProgress {

    /** Fraction complete, clamped so a progress bar can never overflow. */
    fun fraction(goal: Goal): Float {
        if (goal.targetValue <= 0.0) return 0f
        return (goal.currentValue / goal.targetValue).coerceIn(0.0, 1.0).toFloat()
    }

    fun isComplete(goal: Goal): Boolean =
        goal.targetValue > 0.0 && goal.currentValue >= goal.targetValue

    /**
     * Whole days from [today] until the deadline.
     *
     * 0 means the deadline is today, negative means overdue, null means the goal has
     * no deadline. Deliberately not clamped — an overdue goal should be able to say so.
     */
    fun daysRemaining(goal: Goal, today: LocalDate): Long? =
        goal.deadline?.let { ChronoUnit.DAYS.between(today, it) }

    /** Overdue means past the deadline *and* not finished. A finished goal is never overdue. */
    fun isOverdue(goal: Goal, today: LocalDate): Boolean {
        val days = daysRemaining(goal, today) ?: return false
        return days < 0 && !isComplete(goal)
    }

    /**
     * Deadline within [withinDays] and still unfinished.
     *
     * PRD 7.8 uses 3 days for the reminder digest; milestone 9 should reuse this
     * rather than reimplementing the window.
     */
    fun isDeadlineNear(goal: Goal, today: LocalDate, withinDays: Long = 3): Boolean {
        if (isComplete(goal)) return false
        val days = daysRemaining(goal, today) ?: return false
        return days in 0..withinDays
    }

    /** Unfinished goals — what the dashboard means by "active". */
    fun active(goals: List<Goal>): List<Goal> = goals.filterNot(::isComplete)

    /**
     * Ordering for the dashboard's "top 2–3": soonest deadline first, since those are
     * the ones needing attention, with undated goals last rather than interleaved.
     */
    fun byUrgency(goals: List<Goal>): List<Goal> =
        goals.sortedWith(
            compareBy(nullsLast()) { it.deadline },
        )
}

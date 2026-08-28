package com.lifetrack.goal

import com.lifetrack.goal.data.Goal
import com.lifetrack.goal.data.GoalProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private val TODAY: LocalDate = LocalDate.of(2026, 8, 28)

private fun goal(
    name: String = "g",
    target: Double = 100.0,
    current: Double = 0.0,
    deadline: LocalDate? = null,
) = Goal(name = name, targetValue = target, currentValue = current, unit = "km", deadline = deadline)

class GoalProgressTest {

    @Test
    fun `fraction is current over target`() {
        assertEquals(0.25f, GoalProgress.fraction(goal(current = 25.0)), 0.0001f)
    }

    @Test
    fun `fraction clamps so the bar cannot overflow`() {
        assertEquals(1f, GoalProgress.fraction(goal(current = 250.0)), 0.0001f)
    }

    @Test
    fun `a zero target cannot divide by zero`() {
        assertEquals(0f, GoalProgress.fraction(goal(target = 0.0, current = 5.0)), 0.0001f)
    }

    @Test
    fun `reaching the target exactly counts as complete`() {
        assertTrue(GoalProgress.isComplete(goal(current = 100.0)))
        assertFalse(GoalProgress.isComplete(goal(current = 99.9)))
    }

    @Test
    fun `days remaining is null without a deadline`() {
        assertNull(GoalProgress.daysRemaining(goal(), TODAY))
    }

    @Test
    fun `days remaining counts forward and goes negative when past`() {
        assertEquals(3L, GoalProgress.daysRemaining(goal(deadline = TODAY.plusDays(3)), TODAY))
        assertEquals(0L, GoalProgress.daysRemaining(goal(deadline = TODAY), TODAY))
        assertEquals(-2L, GoalProgress.daysRemaining(goal(deadline = TODAY.minusDays(2)), TODAY))
    }

    @Test
    fun `a finished goal is never overdue`() {
        val done = goal(current = 100.0, deadline = TODAY.minusDays(5))
        assertFalse("finishing late is still finishing", GoalProgress.isOverdue(done, TODAY))
        val unfinished = goal(current = 10.0, deadline = TODAY.minusDays(5))
        assertTrue(GoalProgress.isOverdue(unfinished, TODAY))
    }

    @Test
    fun `a goal with no deadline is never overdue`() {
        assertFalse(GoalProgress.isOverdue(goal(current = 0.0), TODAY))
    }

    @Test
    fun `deadline-near window matches PRD 7 dot 8's three days`() {
        assertTrue(GoalProgress.isDeadlineNear(goal(deadline = TODAY.plusDays(3)), TODAY))
        assertTrue(GoalProgress.isDeadlineNear(goal(deadline = TODAY), TODAY))
        assertFalse(GoalProgress.isDeadlineNear(goal(deadline = TODAY.plusDays(4)), TODAY))
        // Already overdue is not "approaching", and finished never nags.
        assertFalse(GoalProgress.isDeadlineNear(goal(deadline = TODAY.minusDays(1)), TODAY))
        assertFalse(
            GoalProgress.isDeadlineNear(goal(current = 100.0, deadline = TODAY.plusDays(1)), TODAY),
        )
    }

    @Test
    fun `active excludes completed goals`() {
        val goals = listOf(goal(name = "a", current = 100.0), goal(name = "b", current = 1.0))
        assertEquals(listOf("b"), GoalProgress.active(goals).map { it.name })
    }

    @Test
    fun `urgency puts soonest deadlines first and undated goals last`() {
        val goals = listOf(
            goal(name = "none"),
            goal(name = "far", deadline = TODAY.plusDays(30)),
            goal(name = "soon", deadline = TODAY.plusDays(1)),
        )
        assertEquals(listOf("soon", "far", "none"), GoalProgress.byUrgency(goals).map { it.name })
    }
}

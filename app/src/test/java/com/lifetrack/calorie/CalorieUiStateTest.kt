package com.lifetrack.calorie

import com.lifetrack.calorie.viewmodel.CalorieSpan
import com.lifetrack.calorie.viewmodel.CalorieUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieUiStateTest {

    @Test
    fun `progress is the eaten fraction of target`() {
        val state = CalorieUiState(eatenToday = 1000, target = 2000)
        assertEquals(0.5f, state.progress, 0.0001f)
        assertEquals(1000, state.remaining)
        assertFalse(state.isOverTarget)
    }

    @Test
    fun `progress clamps at full so the bar cannot overflow`() {
        val state = CalorieUiState(eatenToday = 3000, target = 2000)
        assertEquals(1f, state.progress, 0.0001f)
        assertTrue(state.isOverTarget)
        // Overshoot is carried by remaining going negative, not by progress > 1.
        assertEquals(-1000, state.remaining)
    }

    @Test
    fun `a zero or missing target cannot divide by zero`() {
        val state = CalorieUiState(eatenToday = 500, target = 0)
        assertEquals(0f, state.progress, 0.0001f)
    }

    @Test
    fun `exactly on target is not over`() {
        val state = CalorieUiState(eatenToday = 2000, target = 2000)
        assertEquals(1f, state.progress, 0.0001f)
        assertEquals(0, state.remaining)
        assertFalse("hitting the target exactly should not read as over", state.isOverTarget)
    }

    @Test
    fun `spans match PRD 7 dot 5`() {
        assertEquals(7, CalorieSpan.WEEK.days)
        assertEquals(30, CalorieSpan.MONTH.days)
    }
}

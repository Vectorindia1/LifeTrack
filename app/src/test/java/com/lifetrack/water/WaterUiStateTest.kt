package com.lifetrack.water

import com.lifetrack.water.viewmodel.WaterUiState
import com.lifetrack.water.viewmodel.WaterViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterUiStateTest {

    @Test
    fun `progress is the drunk fraction of target`() {
        val state = WaterUiState(drunkToday = 1250, targetMl = 2500)
        assertEquals(0.5f, state.progress, 0.0001f)
        assertEquals(1250, state.remainingMl)
        assertFalse(state.isGoalMet)
    }

    @Test
    fun `progress clamps so the ring cannot wrap past full`() {
        val state = WaterUiState(drunkToday = 4000, targetMl = 2500)
        assertEquals(1f, state.progress, 0.0001f)
        assertTrue(state.isGoalMet)
    }

    @Test
    fun `remaining never goes negative`() {
        val state = WaterUiState(drunkToday = 4000, targetMl = 2500)
        // Unlike calories, being over on water is not a problem worth reporting.
        assertEquals(0, state.remainingMl)
    }

    @Test
    fun `hitting the target exactly counts as met`() {
        val state = WaterUiState(drunkToday = 2500, targetMl = 2500)
        assertTrue(state.isGoalMet)
        assertEquals(0, state.remainingMl)
    }

    @Test
    fun `a zero target cannot divide by zero`() {
        assertEquals(0f, WaterUiState(drunkToday = 500, targetMl = 0).progress, 0.0001f)
    }

    @Test
    fun `quick add amounts match PRD 7 dot 6`() {
        assertEquals(250, WaterViewModel.QUICK_SMALL_ML)
        assertEquals(500, WaterViewModel.QUICK_LARGE_ML)
    }
}

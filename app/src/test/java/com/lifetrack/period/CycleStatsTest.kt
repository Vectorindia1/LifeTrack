package com.lifetrack.period

import com.lifetrack.period.data.CycleStats
import com.lifetrack.period.data.PeriodLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

private val TODAY: LocalDate = LocalDate.of(2026, 8, 29)

class CycleStatsTest {

    @Test
    fun `current cycle day is 1 on the start date itself`() {
        val logs = listOf(PeriodLog(startDate = TODAY))
        assertEquals(1, CycleStats.currentCycleDay(logs, TODAY))
    }

    @Test
    fun `current cycle day counts forward from the most recent start`() {
        val logs = listOf(
            PeriodLog(startDate = TODAY.minusDays(30)),
            PeriodLog(startDate = TODAY.minusDays(11)),
        )
        assertEquals(12, CycleStats.currentCycleDay(logs, TODAY))
    }

    @Test
    fun `current cycle day is null with no logs`() {
        assertNull(CycleStats.currentCycleDay(emptyList(), TODAY))
    }

    @Test
    fun `average cycle length needs at least two starts`() {
        assertNull(CycleStats.averageCycleLength(listOf(PeriodLog(startDate = TODAY))))
    }

    @Test
    fun `average cycle length is the mean gap between consecutive starts`() {
        val logs = listOf(
            PeriodLog(startDate = TODAY.minusDays(56)),
            PeriodLog(startDate = TODAY.minusDays(28)),
            PeriodLog(startDate = TODAY),
        )
        assertEquals(28.0, CycleStats.averageCycleLength(logs)!!, 0.001)
    }

    @Test
    fun `average cycle length is order independent`() {
        // Logs come back newest-first from the DAO; the calculation must not assume order.
        val logs = listOf(
            PeriodLog(startDate = TODAY),
            PeriodLog(startDate = TODAY.minusDays(30)),
            PeriodLog(startDate = TODAY.minusDays(60)),
        )
        assertEquals(30.0, CycleStats.averageCycleLength(logs)!!, 0.001)
    }

    @Test
    fun `duration counts inclusive of both the start and end day`() {
        val log = PeriodLog(startDate = TODAY, endDate = TODAY.plusDays(4))
        assertEquals(5, CycleStats.duration(log))
    }

    @Test
    fun `duration is null while a period has no end date yet`() {
        assertNull(CycleStats.duration(PeriodLog(startDate = TODAY)))
    }
}

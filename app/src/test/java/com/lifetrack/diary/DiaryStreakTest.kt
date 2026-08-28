package com.lifetrack.diary

import com.lifetrack.diary.data.DaySummary
import com.lifetrack.diary.data.DiaryStreak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private val TODAY: LocalDate = LocalDate.of(2026, 8, 28)

class DiaryStreakTest {

    @Test
    fun `consecutive days ending today count`() {
        val dates = setOf(TODAY, TODAY.minusDays(1), TODAY.minusDays(2))
        assertEquals(3, DiaryStreak.current(dates, TODAY))
    }

    @Test
    fun `not having written today yet does not break the streak`() {
        // Same grace rule as habits: the day is not over.
        val dates = setOf(TODAY.minusDays(1), TODAY.minusDays(2))
        assertEquals(2, DiaryStreak.current(dates, TODAY))
    }

    @Test
    fun `missing yesterday does break the streak`() {
        val dates = setOf(TODAY, TODAY.minusDays(2), TODAY.minusDays(3))
        assertEquals(1, DiaryStreak.current(dates, TODAY))
    }

    @Test
    fun `no entries is no streak`() {
        assertEquals(0, DiaryStreak.current(emptySet(), TODAY))
    }

    @Test
    fun `an old run that has since lapsed does not count`() {
        val dates = setOf(TODAY.minusDays(10), TODAY.minusDays(11))
        assertEquals(0, DiaryStreak.current(dates, TODAY))
    }

    @Test
    fun `future entries do not inflate the streak`() {
        val dates = setOf(TODAY.plusDays(1), TODAY, TODAY.minusDays(1))
        assertEquals(2, DiaryStreak.current(dates, TODAY))
    }

    @Test
    fun `a day with nothing tracked is empty so no summary is prefilled`() {
        assertTrue(DaySummary().isEmpty)
        assertTrue(DaySummary(habitsDone = 0, habitsDue = 0).isEmpty)
    }

    @Test
    fun `any tracked activity makes the summary non-empty`() {
        assertFalse(DaySummary(habitsDue = 2).isEmpty)
        assertFalse(DaySummary(spent = 10.0).isEmpty)
        assertFalse(DaySummary(waterMl = 250).isEmpty)
        assertFalse(DaySummary(calories = 100).isEmpty)
    }
}

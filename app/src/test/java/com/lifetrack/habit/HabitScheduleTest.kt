package com.lifetrack.habit

import com.lifetrack.habit.data.FrequencyType
import com.lifetrack.habit.data.Habit
import com.lifetrack.habit.data.HabitSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** 2026-08-24 is a Monday, which makes the week maths easy to read. */
private val MONDAY: LocalDate = LocalDate.of(2026, 8, 24)

private fun habit(
    frequencyType: FrequencyType = FrequencyType.DAILY,
    daysOfWeekMask: Int = Habit.ALL_DAYS_MASK,
    timesPerWeek: Int? = null,
    createdOn: LocalDate = LocalDate.of(2020, 1, 1),
) = Habit(
    id = 1L,
    name = "test",
    frequencyType = frequencyType,
    daysOfWeekMask = daysOfWeekMask,
    timesPerWeek = timesPerWeek,
    createdAt = createdOn.atStartOfDay(ZoneId.systemDefault()).toInstant(),
)

class HabitScheduleTest {

    @Test
    fun `day bits are monday indexed`() {
        assertEquals(0b000_0001, HabitSchedule.dayBit(DayOfWeek.MONDAY))
        assertEquals(0b100_0000, HabitSchedule.dayBit(DayOfWeek.SUNDAY))
        assertEquals(Habit.ALL_DAYS_MASK, HabitSchedule.maskOf(DayOfWeek.entries.toSet()))
    }

    @Test
    fun `mask round trips`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertEquals(days, HabitSchedule.daysOf(HabitSchedule.maskOf(days)))
    }

    @Test
    fun `custom day habit is only scheduled on its days`() {
        val h = habit(
            frequencyType = FrequencyType.CUSTOM_DAYS,
            daysOfWeekMask = HabitSchedule.maskOf(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
        )
        assertTrue(HabitSchedule.isScheduledOn(h, MONDAY))
        assertFalse(HabitSchedule.isScheduledOn(h, MONDAY.plusDays(1)))
        assertTrue(HabitSchedule.isScheduledOn(h, MONDAY.plusDays(4)))
    }

    @Test
    fun `daily streak counts consecutive completed days`() {
        val h = habit()
        val today = MONDAY.plusDays(4)
        val completed = setOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, HabitSchedule.currentStreak(h, completed, today))
    }

    @Test
    fun `missing today does not break the streak because the day is not over`() {
        val h = habit()
        val today = MONDAY.plusDays(4)
        val completed = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, HabitSchedule.currentStreak(h, completed, today))
    }

    @Test
    fun `missing yesterday does break the streak`() {
        val h = habit()
        val today = MONDAY.plusDays(4)
        val completed = setOf(today, today.minusDays(2), today.minusDays(3))
        assertEquals(1, HabitSchedule.currentStreak(h, completed, today))
    }

    @Test
    fun `unscheduled days never break a custom-day streak`() {
        // Mon/Wed/Fri habit. Completed all three this week; Tue and Thu are irrelevant.
        val h = habit(
            frequencyType = FrequencyType.CUSTOM_DAYS,
            daysOfWeekMask = HabitSchedule.maskOf(
                setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            ),
        )
        val friday = MONDAY.plusDays(4)
        val completed = setOf(MONDAY, MONDAY.plusDays(2), friday)
        assertEquals(3, HabitSchedule.currentStreak(h, completed, friday))
    }

    @Test
    fun `streak stops at habit creation date`() {
        val h = habit(createdOn = MONDAY)
        val today = MONDAY.plusDays(2)
        val completed = setOf(today, today.minusDays(1), MONDAY, MONDAY.minusDays(1))
        // The day before creation must not count even though a log exists for it.
        assertEquals(3, HabitSchedule.currentStreak(h, completed, today))
    }

    @Test
    fun `weekly streak counts weeks meeting the target`() {
        val h = habit(frequencyType = FrequencyType.WEEKLY, timesPerWeek = 3)
        val today = MONDAY.plusDays(6)
        val completed = buildSet {
            repeat(3) { add(MONDAY.plusDays(it.toLong())) }                 // this week: 3 ✓
            repeat(3) { add(MONDAY.minusWeeks(1).plusDays(it.toLong())) }   // last week: 3 ✓
            add(MONDAY.minusWeeks(2))                                       // 2 weeks ago: 1 ✗
        }
        assertEquals(2, HabitSchedule.currentStreak(h, completed, today))
    }

    @Test
    fun `incomplete current week does not break a weekly streak`() {
        val h = habit(frequencyType = FrequencyType.WEEKLY, timesPerWeek = 3)
        val today = MONDAY.plusDays(1)
        val completed = buildSet {
            add(MONDAY)                                                     // this week: 1, target not met yet
            repeat(3) { add(MONDAY.minusWeeks(1).plusDays(it.toLong())) }   // last week: 3 ✓
        }
        assertEquals(1, HabitSchedule.currentStreak(h, completed, today))
    }

    @Test
    fun `completion rate counts only scheduled days`() {
        val h = habit(
            frequencyType = FrequencyType.CUSTOM_DAYS,
            daysOfWeekMask = HabitSchedule.maskOf(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)),
        )
        val sunday = MONDAY.plusDays(6)
        // Scheduled twice this week; did one of them.
        val rate = HabitSchedule.completionRate(h, setOf(MONDAY), MONDAY, sunday, sunday)
        assertEquals(0.5f, rate!!, 0.0001f)
    }

    @Test
    fun `completion rate ignores days before the habit existed`() {
        val h = habit(createdOn = MONDAY.plusDays(5))
        val sunday = MONDAY.plusDays(6)
        // Only Sat and Sun are in range; both done.
        val completed = setOf(MONDAY.plusDays(5), sunday)
        assertEquals(1f, HabitSchedule.completionRate(h, completed, MONDAY, sunday, sunday)!!, 0.0001f)
    }

    @Test
    fun `completion rate ignores the future`() {
        val h = habit()
        val today = MONDAY.plusDays(2)
        val sunday = MONDAY.plusDays(6)
        val completed = setOf(MONDAY, MONDAY.plusDays(1), today)
        // Wed–Sun haven't happened yet, so this is 3/3, not 3/7.
        assertEquals(1f, HabitSchedule.completionRate(h, completed, MONDAY, sunday, today)!!, 0.0001f)
    }

    @Test
    fun `completion rate is null when nothing was ever due`() {
        val h = habit(createdOn = MONDAY.plusWeeks(4))
        assertNull(HabitSchedule.completionRate(h, emptySet(), MONDAY, MONDAY.plusDays(6), MONDAY.plusDays(6)))
    }

    @Test
    fun `recent weeks are monday started and end with the current week`() {
        val weeks = HabitSchedule.recentWeeks(MONDAY.plusDays(3), 4)
        assertEquals(4, weeks.size)
        assertEquals(MONDAY, weeks.last().start)
        assertEquals(MONDAY.plusDays(6), weeks.last().endInclusive)
        assertEquals(MONDAY.minusWeeks(3), weeks.first().start)
    }

    @Test
    fun `recent months end with the current month`() {
        val months = HabitSchedule.recentMonths(LocalDate.of(2026, 8, 15), 3)
        assertEquals(3, months.size)
        assertEquals(LocalDate.of(2026, 6, 1), months.first().start)
        assertEquals(LocalDate.of(2026, 8, 1), months.last().start)
        assertEquals(LocalDate.of(2026, 8, 31), months.last().endInclusive)
    }
}

package com.lifetrack.expense

import com.lifetrack.core.ui.Money
import com.lifetrack.expense.data.ExpenseCategories
import com.lifetrack.expense.data.endOfDay
import com.lifetrack.expense.data.startOfDay
import com.lifetrack.expense.viewmodel.SpendWindow
import com.lifetrack.expense.viewmodel.rangeEnding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

private val TODAY: LocalDate = LocalDate.of(2026, 8, 28)

class ExpenseLogicTest {

    @Test
    fun `day window is just today`() {
        val range = SpendWindow.DAY.rangeEnding(TODAY)
        assertEquals(TODAY, range.start)
        assertEquals(TODAY, range.endInclusive)
    }

    @Test
    fun `week window is seven days inclusive of today`() {
        val range = SpendWindow.WEEK.rangeEnding(TODAY)
        assertEquals(LocalDate.of(2026, 8, 22), range.start)
        assertEquals(TODAY, range.endInclusive)
    }

    @Test
    fun `month window starts at the first of the month, not 30 days back`() {
        val range = SpendWindow.MONTH.rangeEnding(TODAY)
        assertEquals(LocalDate.of(2026, 8, 1), range.start)
        assertEquals(TODAY, range.endInclusive)
    }

    @Test
    fun `day boundaries cover the whole local day without overlapping the next`() {
        val zone = ZoneId.of("Asia/Kolkata")
        val start = TODAY.startOfDay(zone)
        val end = TODAY.endOfDay(zone)
        val nextStart = TODAY.plusDays(1).startOfDay(zone)

        assertTrue(start.isBefore(end))
        assertTrue(end.isBefore(nextStart))
        // A midnight-local timestamp belongs to today, not yesterday.
        assertEquals(TODAY, start.atZone(zone).toLocalDate())
        assertEquals(TODAY, end.atZone(zone).toLocalDate())
    }

    @Test
    fun `day boundaries respect the zone, not UTC`() {
        val kolkata = ZoneId.of("Asia/Kolkata")
        val utc = ZoneId.of("UTC")
        // Same calendar date, different instants, because the zones differ.
        assertTrue(TODAY.startOfDay(kolkata).isBefore(TODAY.startOfDay(utc)))
    }

    @Test
    fun `custom categories appear after presets and are not duplicated`() {
        val all = ExpenseCategories.allKnown(listOf("Food", "Rent", "Rent", "Chai"))
        assertEquals(ExpenseCategories.PRESETS, all.take(ExpenseCategories.PRESETS.size))
        assertEquals(listOf("Chai", "Rent"), all.drop(ExpenseCategories.PRESETS.size))
    }

    @Test
    fun `whole amounts format without decimal noise`() {
        val formatted = Money.format(450.0, Locale.forLanguageTag("en-IN"))
        assertTrue(formatted, formatted.contains("450"))
        assertTrue("should not pad whole amounts: $formatted", !formatted.contains("450.00"))
    }

    @Test
    fun `fractional amounts keep two decimals`() {
        val formatted = Money.format(12.5, Locale.forLanguageTag("en-IN"))
        assertTrue(formatted, formatted.contains("12.5"))
    }

    @Test
    fun `compact formatting shortens large numbers for axes`() {
        assertEquals("450", Money.formatCompact(450.0))
        assertEquals("1.5k", Money.formatCompact(1500.0))
        assertEquals("2k", Money.formatCompact(2000.0))
        assertEquals("1M", Money.formatCompact(1_000_000.0))
    }
}

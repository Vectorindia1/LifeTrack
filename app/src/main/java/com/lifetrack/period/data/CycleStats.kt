package com.lifetrack.period.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Cycle maths as pure functions, so the "days since last start" / "average length"
 * arithmetic is unit-testable. No prediction of a future date here by design — the
 * user asked for a simple log with history and an average, not a forecast.
 */
object CycleStats {

    /** Days since the most recent logged start, 1-indexed (the start day itself is day 1). */
    fun currentCycleDay(logs: List<PeriodLog>, today: LocalDate): Int? {
        val lastStart = logs.maxOfOrNull { it.startDate } ?: return null
        return (ChronoUnit.DAYS.between(lastStart, today) + 1).toInt()
    }

    /**
     * Average gap between consecutive start dates, in days. Needs at least two logged
     * starts — one period has no cycle length to average. Null with fewer than two.
     */
    fun averageCycleLength(logs: List<PeriodLog>): Double? {
        val starts = logs.map { it.startDate }.sorted()
        if (starts.size < 2) return null
        val gaps = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b) }
        return gaps.average()
    }

    /** How many full days a period lasted, or null if it has no end date yet (ongoing). */
    fun duration(log: PeriodLog): Int? =
        log.endDate?.let { (ChronoUnit.DAYS.between(log.startDate, it) + 1).toInt() }
}

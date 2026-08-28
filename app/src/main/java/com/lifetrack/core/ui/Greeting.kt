package com.lifetrack.core.ui

/**
 * Time-of-day greeting bucket. Kept as a plain function returning an enum — not a
 * string — so it is unit-testable without Android; the caller resolves the actual
 * string resource and appends the name.
 */
enum class GreetingPeriod { MORNING, AFTERNOON, EVENING, NIGHT }

fun greetingFor(hourOfDay: Int): GreetingPeriod = when (hourOfDay) {
    in 5..11 -> GreetingPeriod.MORNING
    in 12..16 -> GreetingPeriod.AFTERNOON
    in 17..20 -> GreetingPeriod.EVENING
    else -> GreetingPeriod.NIGHT
}

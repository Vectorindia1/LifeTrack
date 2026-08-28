package com.lifetrack.core.ui

import java.text.NumberFormat
import java.util.Locale

/**
 * Currency formatting follows the device locale rather than being hardcoded.
 *
 * PRD 7.7's example shows "₹450", but hardcoding a symbol would be wrong on any
 * other device. `NumberFormat.getCurrencyInstance()` gives ₹ on an India-locale
 * phone and the right symbol everywhere else. Amounts are stored as plain numbers
 * with no currency code — this is a single-user, single-currency app.
 */
object Money {

    fun format(amount: Double, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getCurrencyInstance(locale).apply {
            // Whole numbers are the common case; don't force ".00" noise.
            maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            minimumFractionDigits = 0
        }.format(amount)

    /** Compact form for chart axes, where the symbol is repeated and adds no meaning. */
    fun formatCompact(amount: Double): String = when {
        amount >= 1_000_000 -> "${(amount / 1_000_000).trimmed()}M"
        amount >= 1_000 -> "${(amount / 1_000).trimmed()}k"
        else -> amount.trimmed()
    }

    private fun Double.trimmed(): String =
        if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.getDefault(), "%.1f", this)
}

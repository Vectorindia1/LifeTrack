package com.lifetrack.core

import com.lifetrack.core.data.AppPreferences
import com.lifetrack.core.data.effectiveCurrencyLocale
import com.lifetrack.core.ui.CurrencyOption
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Currency
import java.util.Locale

class CurrencyOptionTest {

    @Test
    fun `null tag resolves to the device locale, not a fixed one`() {
        val prefs = AppPreferences(currencyLocaleTag = null)
        assertEquals(Locale.getDefault(), prefs.effectiveCurrencyLocale())
    }

    @Test
    fun `a stored tag resolves to that exact locale`() {
        val prefs = AppPreferences(currencyLocaleTag = "en-IN")
        assertEquals(Locale.forLanguageTag("en-IN"), prefs.effectiveCurrencyLocale())
    }

    @Test
    fun `every curated option except System resolves to its intended currency`() {
        val expected = mapOf(
            CurrencyOption.INR to "INR",
            CurrencyOption.USD to "USD",
            CurrencyOption.EUR to "EUR",
            CurrencyOption.GBP to "GBP",
            CurrencyOption.JPY to "JPY",
            CurrencyOption.AUD to "AUD",
            CurrencyOption.CAD to "CAD",
        )
        expected.forEach { (option, code) ->
            val locale = Locale.forLanguageTag(requireNotNull(option.tag))
            assertEquals(code, Currency.getInstance(locale).currencyCode)
        }
    }

    @Test
    fun `fromTag round trips and falls back to System for an unknown tag`() {
        assertEquals(CurrencyOption.INR, CurrencyOption.fromTag("en-IN"))
        assertEquals(CurrencyOption.SYSTEM, CurrencyOption.fromTag(null))
        assertEquals(CurrencyOption.SYSTEM, CurrencyOption.fromTag("xx-YY"))
    }
}

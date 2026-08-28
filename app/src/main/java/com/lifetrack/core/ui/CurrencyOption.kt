package com.lifetrack.core.ui

/**
 * A curated currency picker, not a full ISO-4217 list — this is a personal app, not
 * an accounting tool, and a searchable 150-entry list would be overkill.
 *
 * Each option pairs a currency with an English-language locale in that currency's
 * country, so [com.lifetrack.core.ui.Money.format] gets both the right symbol *and*
 * sensible grouping (e.g. commas, not a different script) via one `Locale`. See
 * [AppPreferences.currencyLocaleTag] for how the choice is stored and
 * [AppPreferences.effectiveCurrencyLocale] for how it resolves.
 */
enum class CurrencyOption(val tag: String?, val displayName: String) {
    SYSTEM(null, "System default"),
    INR("en-IN", "₹ Indian Rupee"),
    USD("en-US", "$ US Dollar"),
    EUR("en-IE", "€ Euro"),
    GBP("en-GB", "£ British Pound"),
    JPY("en-JP", "¥ Japanese Yen"),
    AUD("en-AU", "$ Australian Dollar"),
    CAD("en-CA", "$ Canadian Dollar"),
    ;

    companion object {
        fun fromTag(tag: String?): CurrencyOption = entries.find { it.tag == tag } ?: SYSTEM
    }
}

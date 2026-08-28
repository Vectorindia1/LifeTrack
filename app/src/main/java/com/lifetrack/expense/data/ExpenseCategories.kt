package com.lifetrack.expense.data

/**
 * Preset categories, plus free-text custom ones.
 *
 * Resolves PRD section 11's open question: presets **and** user-defined, decided in
 * milestone 4. `Expense.category` stays a plain String, so a custom category needs no
 * schema change and no separate table — a category "exists" precisely because some
 * expense row uses it. See MEMORY.md.
 */
object ExpenseCategories {

    const val OTHER = "Other"

    /** Deliberately short. A long list is a scrolling decision, and PRD 8 wants speed. */
    val PRESETS: List<String> = listOf(
        "Food",
        "Transport",
        "Groceries",
        "Bills",
        "Shopping",
        "Health",
        "Fun",
        OTHER,
    )

    /**
     * Presets first, then any custom categories already used, so the chip row is
     * stable rather than reordering itself as spending habits change.
     */
    fun allKnown(used: Collection<String>): List<String> {
        val custom = used.filterNot { it in PRESETS }.distinct().sorted()
        return PRESETS + custom
    }
}

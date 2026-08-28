package com.lifetrack.expense.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ExpenseRepository(private val dao: ExpenseDao) {

    fun observeExpenses(): Flow<List<Expense>> = dao.observeExpenses()

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Expense>> =
        dao.observeExpensesBetween(from.startOfDay(), to.endOfDay())

    suspend fun add(amount: Double, category: String, note: String?) {
        dao.insert(
            Expense(
                category = category.trim().ifBlank { ExpenseCategories.OTHER },
                amount = amount,
                note = note?.trim()?.ifBlank { null },
            ),
        )
    }

    suspend fun delete(expense: Expense) = dao.delete(expense)
}

/**
 * Day boundaries in the device's own time zone.
 *
 * Expenses are stored as `Instant`, but "today's spend" is a local-calendar question,
 * so every range query has to convert rather than slicing on UTC.
 */
fun LocalDate.startOfDay(zone: ZoneId = ZoneId.systemDefault()): Instant =
    atStartOfDay(zone).toInstant()

fun LocalDate.endOfDay(zone: ZoneId = ZoneId.systemDefault()): Instant =
    plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1)

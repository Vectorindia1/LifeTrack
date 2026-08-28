package com.lifetrack.diary.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DiaryRepository(private val dao: DiaryDao) {

    fun observeEntryForDate(date: LocalDate): Flow<DiaryEntry?> = dao.observeEntryForDate(date)

    fun observeEntriesBetween(from: LocalDate, to: LocalDate): Flow<List<DiaryEntry>> =
        dao.observeEntriesBetween(from, to)

    /** Bounded history for streaks and the calendar's dots. */
    fun observeRecentEntries(today: LocalDate): Flow<List<DiaryEntry>> =
        dao.observeEntriesBetween(today.minusDays(HISTORY_DAYS), today)

    /**
     * One entry per day. The unique index on `date` makes this an update rather than
     * a duplicate, but the existing row's id has to be carried over or the upsert
     * would insert a second row and hit the constraint.
     */
    suspend fun save(existing: DiaryEntry?, date: LocalDate, text: String, mood: Mood?) {
        dao.upsert(
            DiaryEntry(
                id = existing?.id ?: 0L,
                date = date,
                text = text,
                mood = mood,
            ),
        )
    }

    suspend fun delete(entry: DiaryEntry) = dao.delete(entry)

    private companion object {
        const val HISTORY_DAYS = 400L
    }
}

package com.lifetrack.diary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DiaryDao {

    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun observeEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT COUNT(*) FROM diary_entries")
    fun observeEntryCount(): Flow<Int>

    @Query("SELECT * FROM diary_entries WHERE date = :date")
    fun observeEntryForDate(date: LocalDate): Flow<DiaryEntry?>

    @Query("SELECT * FROM diary_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeEntriesBetween(from: LocalDate, to: LocalDate): Flow<List<DiaryEntry>>

    /** One entry per day — the unique index on `date` makes this an update, not a duplicate. */
    @Upsert
    suspend fun upsert(entry: DiaryEntry)

    @Delete
    suspend fun delete(entry: DiaryEntry)
}

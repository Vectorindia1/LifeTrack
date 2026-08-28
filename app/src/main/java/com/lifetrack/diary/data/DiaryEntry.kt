package com.lifetrack.diary.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Five moods, per PRD 7.7. Stored by name, rendered as an emoji in the UI. */
enum class Mood(val emoji: String) {
    AWFUL("😞"),
    LOW("🙁"),
    OKAY("😐"),
    GOOD("🙂"),
    GREAT("😄"),
}

/** One entry per day — enforced by the unique index on [date]. */
@Entity(tableName = "diary_entries", indices = [Index(value = ["date"], unique = true)])
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: LocalDate,
    val text: String = "",
    val mood: Mood? = null,
)

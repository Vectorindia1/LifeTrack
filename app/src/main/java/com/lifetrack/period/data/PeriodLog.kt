package com.lifetrack.period.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * One logged period, by start date (and optionally end date once it's over).
 *
 * Deliberately minimal — start/end dates only, no flow intensity or symptoms. This
 * app is single-user and the log is neutral as to whose cycle it is: someone
 * tracking their own periods and someone tracking a partner's use the same log with
 * the same wording. See MEMORY.md.
 */
@Entity(tableName = "period_logs", indices = [Index(value = ["startDate"], unique = true)])
data class PeriodLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
)

package com.lifetrack.period.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {

    @Query("SELECT * FROM period_logs ORDER BY startDate DESC")
    fun observeAll(): Flow<List<PeriodLog>>

    @Upsert
    suspend fun upsert(log: PeriodLog)

    @Delete
    suspend fun delete(log: PeriodLog)
}

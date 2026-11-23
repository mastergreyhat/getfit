package com.getfit.data.dao

import androidx.room.*
import com.getfit.data.entity.StopwatchRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface StopwatchRecordDao {
    @Query("SELECT * FROM stopwatch_records ORDER BY createdAtMillis DESC")
    fun getAllRecords(): Flow<List<StopwatchRecord>>

    @Query("SELECT * FROM stopwatch_records WHERE title = :title ORDER BY elapsedMillis DESC")
    fun getRecordsByTitle(title: String): Flow<List<StopwatchRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: StopwatchRecord): Long

    @Delete
    suspend fun deleteRecord(record: StopwatchRecord)

    @Query("DELETE FROM stopwatch_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)
}

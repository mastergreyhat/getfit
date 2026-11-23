package com.getfit.data.dao

import androidx.room.*
import com.getfit.data.entity.WeightLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_logs ORDER BY date ASC, id ASC")
    fun getAllWeightLogs(): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getWeightLogsForDateRange(startDate: String, endDate: String): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs ORDER BY date DESC, id DESC LIMIT 1")
    fun getLatestWeightLog(): Flow<WeightLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: WeightLog): Long

    @Delete
    suspend fun deleteWeightLog(log: WeightLog)

    @Query("SELECT * FROM weight_logs ORDER BY date ASC, id ASC")
    suspend fun getAllWeightLogsSync(): List<WeightLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWeightLogs(logs: List<WeightLog>)

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteWeightLogById(id: Long)
}

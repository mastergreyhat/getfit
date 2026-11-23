package com.getfit.data.dao

import androidx.room.*
import com.getfit.data.entity.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs WHERE date = :date LIMIT 1")
    fun getWaterLogForDate(date: String): Flow<WaterLog?>

    @Query("SELECT amountMl FROM water_logs WHERE date = :date LIMIT 1")
    suspend fun getWaterAmountForDateSync(date: String): Int?

    @Query("SELECT DISTINCT date FROM water_logs WHERE amountMl > 0 ORDER BY date DESC")
    fun getWaterLogDates(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWaterLog(log: WaterLog): Long

    @Query("UPDATE water_logs SET amountMl = :amount WHERE date = :date")
    suspend fun updateWaterAmount(date: String, amount: Int)

    @Query("SELECT * FROM water_logs ORDER BY date ASC")
    suspend fun getAllWaterLogsSync(): List<WaterLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWaterLogs(logs: List<WaterLog>)

    @Query("DELETE FROM water_logs WHERE date = :date")
    suspend fun deleteWaterLogForDate(date: String)
}


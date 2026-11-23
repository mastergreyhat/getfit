package com.getfit.data.dao

import androidx.room.*
import com.getfit.data.entity.FoodLog
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY time ASC")
    fun getLogsForDate(date: String): Flow<List<FoodLog>>

    @Query("SELECT * FROM food_logs WHERE date = :date AND mealType = :mealType ORDER BY time ASC")
    fun getLogsForDateAndMeal(date: String, mealType: String): Flow<List<FoodLog>>

    @Query("SELECT SUM(totalCalories) FROM food_logs WHERE date = :date")
    fun getTotalCaloriesForDate(date: String): Flow<Double?>

    @Query("SELECT SUM(totalProtein) FROM food_logs WHERE date = :date")
    fun getTotalProteinForDate(date: String): Flow<Double?>

    @Query("SELECT SUM(totalCarbs) FROM food_logs WHERE date = :date")
    fun getTotalCarbsForDate(date: String): Flow<Double?>

    @Query("SELECT SUM(totalFat) FROM food_logs WHERE date = :date")
    fun getTotalFatForDate(date: String): Flow<Double?>

    @Query("SELECT SUM(totalFiber) FROM food_logs WHERE date = :date")
    fun getTotalFiberForDate(date: String): Flow<Double?>

    @Query("SELECT * FROM food_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, time DESC")
    fun getLogsForDateRange(startDate: String, endDate: String): Flow<List<FoodLog>>

    @Query("SELECT DISTINCT date FROM food_logs ORDER BY date DESC")
    fun getFoodLogDates(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FoodLog): Long

    @Update
    suspend fun updateLog(log: FoodLog)

    @Delete
    suspend fun deleteLog(log: FoodLog)

    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY time ASC")
    suspend fun getLogsForDateSync(date: String): List<FoodLog>

    @Query("SELECT * FROM food_logs ORDER BY date ASC, time ASC")
    suspend fun getAllFoodLogsSync(): List<FoodLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<FoodLog>)

    @Query("DELETE FROM food_logs WHERE date = :date")
    suspend fun deleteLogsForDate(date: String)
}

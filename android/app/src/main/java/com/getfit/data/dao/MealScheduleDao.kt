package com.getfit.data.dao

import androidx.room.*
import com.getfit.data.entity.MealSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface MealScheduleDao {
    @Query("SELECT * FROM meal_schedules ORDER BY scheduledTime ASC")
    fun getAllSchedules(): Flow<List<MealSchedule>>

    @Query("SELECT * FROM meal_schedules WHERE isEnabled = 1 ORDER BY scheduledTime ASC")
    fun getEnabledSchedules(): Flow<List<MealSchedule>>

    @Query("SELECT * FROM meal_schedules WHERE mealType = :mealType")
    fun getScheduleByMealType(mealType: String): Flow<MealSchedule?>

    @Query("SELECT * FROM meal_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): MealSchedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: MealSchedule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSchedules(schedules: List<MealSchedule>)

    @Update
    suspend fun updateSchedule(schedule: MealSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: MealSchedule)

    @Query("DELETE FROM meal_schedules")
    suspend fun deleteAllSchedules()
}

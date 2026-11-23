package com.getfit.data.dao

import androidx.room.*
import com.getfit.data.entity.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileSync(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET dailyCalorieTarget = :calories, dailyProteinTarget = :protein, dailyCarbsTarget = :carbs, dailyFatTarget = :fat WHERE id = 1")
    suspend fun updateNutritionTargets(calories: Int, protein: Int, carbs: Int, fat: Int)

    @Query("UPDATE user_profile SET dailyWaterTarget = :target WHERE id = 1")
    suspend fun updateWaterTarget(target: Int)

    @Query("UPDATE user_profile SET workoutPlan = :plan WHERE id = 1")
    suspend fun updateWorkoutPlan(plan: String)
}

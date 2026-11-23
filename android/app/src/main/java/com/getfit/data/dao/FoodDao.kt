package com.getfit.data.dao

import androidx.room.*
import com.getfit.data.entity.FoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun getAllFoods(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE category = :category ORDER BY name ASC")
    fun getFoodsByCategory(category: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY CASE WHEN LOWER(name) = LOWER(:query) THEN 0 WHEN LOWER(name) LIKE LOWER(:query) || '%' THEN 1 ELSE 2 END, name ASC")
    fun searchFoods(query: String): Flow<List<FoodItem>>

    @Query("SELECT name FROM food_items")
    suspend fun getAllFoodNames(): List<String>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFoodById(id: Long): FoodItem?

    @Query("SELECT DISTINCT category FROM food_items ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFoods(foods: List<FoodItem>)

    @Update
    suspend fun updateFood(food: FoodItem)

    @Delete
    suspend fun deleteFood(food: FoodItem)

    @Query("SELECT * FROM food_items WHERE isCustom = 1")
    suspend fun getAllCustomFoodsSync(): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE isCustom = 0")
    suspend fun getAllDefaultFoods(): List<FoodItem>

    @Query("SELECT * FROM food_items")
    suspend fun getAllFoodsSync(): List<FoodItem>

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun getFoodCount(): Int
}

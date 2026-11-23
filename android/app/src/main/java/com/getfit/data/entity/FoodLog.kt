package com.getfit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_logs")
data class FoodLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val foodItemId: Long,
    val foodName: String,
    val quantity: Double,
    val servingSize: Double,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val totalFiber: Double,
    val mealType: String,
    val date: String,
    val time: String
)

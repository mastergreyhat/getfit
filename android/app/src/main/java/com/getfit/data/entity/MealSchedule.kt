package com.getfit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_schedules")
data class MealSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealType: String,
    val scheduledTime: String,
    val suggestedFoods: String,
    val targetCalories: Int,
    val targetProtein: Int,
    val isEnabled: Boolean = true
)

package com.getfit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "",
    val age: Int = 25,
    val weight: Double = 70.0,
    val height: Double = 170.0,
    val gender: String = "Male",
    val activityLevel: String = "Moderate",
    val goal: String = "Muscle Building",
    val dailyCalorieTarget: Int = 2500,
    val dailyProteinTarget: Int = 150,
    val dailyCarbsTarget: Int = 300,
    val dailyFatTarget: Int = 70,
    val dailyWaterTarget: Int = 3000,
    val workoutPlan: String = "PPL"
)

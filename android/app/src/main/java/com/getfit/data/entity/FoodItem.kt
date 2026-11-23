package com.getfit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double = 0.0,
    val defaultServingSize: Double = 100.0,
    val servingUnit: String = "g",
    val isCustom: Boolean = false,
    val source: String = "ICMR-NIN Verified"
)

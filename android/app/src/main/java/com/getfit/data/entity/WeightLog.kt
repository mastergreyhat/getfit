package com.getfit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_logs")
data class WeightLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weightKg: Double,
    val date: String,
    val notes: String = ""
)

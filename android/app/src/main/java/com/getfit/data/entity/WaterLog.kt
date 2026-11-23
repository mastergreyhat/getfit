package com.getfit.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "water_logs",
    indices = [Index(value = ["date"], unique = true)]
)
data class WaterLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val amountMl: Int = 0
)


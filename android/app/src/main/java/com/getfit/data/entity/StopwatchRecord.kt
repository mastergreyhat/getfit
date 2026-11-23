package com.getfit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stopwatch_records")
data class StopwatchRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val elapsedMillis: Long,
    val formattedTime: String,
    val date: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

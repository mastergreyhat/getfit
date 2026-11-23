package com.getfit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    val exerciseName: String,
    val setNumber: Int,
    val reps: Int,
    val weight: Double,
    val date: String,
    val workoutPlanDay: String,
    val notes: String = "",
    val isPersonalBest: Boolean = false
)

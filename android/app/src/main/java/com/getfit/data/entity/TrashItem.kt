package com.getfit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_items")
data class TrashItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemType: String, // "Exercise", "Meal Plan", "Food Log"
    val title: String,
    val subtitle: String,
    val dataJson: String,
    val deletedAtMillis: Long = System.currentTimeMillis()
)


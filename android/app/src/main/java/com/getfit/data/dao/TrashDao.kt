package com.getfit.data.dao

import androidx.room.*
import com.getfit.data.entity.TrashItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_items WHERE deletedAtMillis >= :cutoffMillis ORDER BY deletedAtMillis DESC")
    fun getActiveTrashItems(cutoffMillis: Long): Flow<List<TrashItem>>

    @Query("DELETE FROM trash_items WHERE deletedAtMillis < :cutoffMillis")
    suspend fun purgeExpiredTrash(cutoffMillis: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(item: TrashItem): Long

    @Delete
    suspend fun deleteTrashItem(item: TrashItem)

    @Query("DELETE FROM trash_items")
    suspend fun emptyTrash()

    @Query("SELECT COUNT(*) FROM trash_items WHERE deletedAtMillis >= :cutoffMillis")
    fun getTrashCount(cutoffMillis: Long): Flow<Int>
}


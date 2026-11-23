package com.getfit.data.dao

import androidx.room.*
import com.getfit.data.entity.WorkoutLog
import kotlinx.coroutines.flow.Flow

data class MuscleVolumeSummary(
    val muscleGroup: String,
    val totalSets: Int,
    val totalVolumeKg: Double
)

@Dao
interface WorkoutLogDao {
    @Query("SELECT * FROM workout_logs WHERE date = :date ORDER BY workoutPlanDay ASC, exerciseName ASC, setNumber ASC")
    fun getLogsForDate(date: String): Flow<List<WorkoutLog>>

    @Query("SELECT * FROM workout_logs WHERE exerciseId = :exerciseId ORDER BY date DESC, setNumber ASC")
    fun getLogsForExercise(exerciseId: Long): Flow<List<WorkoutLog>>

    @Query("SELECT * FROM workout_logs WHERE exerciseId = :exerciseId ORDER BY date ASC, setNumber ASC")
    fun getExerciseHistory(exerciseId: Long): Flow<List<WorkoutLog>>

    @Query("SELECT * FROM workout_logs ORDER BY date DESC, id DESC LIMIT :limit")
    fun getRecentWorkouts(limit: Int = 20): Flow<List<WorkoutLog>>

    @Query("SELECT * FROM workout_logs WHERE exerciseId = :exerciseId AND isPersonalBest = 1 ORDER BY weight DESC LIMIT 1")
    fun getPersonalBest(exerciseId: Long): Flow<WorkoutLog?>

    @Query("SELECT * FROM workout_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getLogsForDateRange(startDate: String, endDate: String): Flow<List<WorkoutLog>>

    @Query("""
        SELECT e.muscleGroup as muscleGroup, COUNT(w.id) as totalSets, COALESCE(SUM(w.weight * w.reps), 0.0) as totalVolumeKg
        FROM workout_logs w
        INNER JOIN exercises e ON w.exerciseId = e.id
        WHERE w.date BETWEEN :startDate AND :endDate
        GROUP BY e.muscleGroup
    """)
    fun getMuscleVolumeForDateRange(startDate: String, endDate: String): Flow<List<MuscleVolumeSummary>>

    @Query("SELECT MAX(weight) FROM workout_logs WHERE exerciseId = :exerciseId")
    fun getMaxWeight(exerciseId: Long): Flow<Double?>

    @Query("SELECT DISTINCT date FROM workout_logs ORDER BY date DESC")
    fun getWorkoutDates(): Flow<List<String>>

    @Query("SELECT COUNT(DISTINCT date) FROM workout_logs WHERE date BETWEEN :startDate AND :endDate")
    fun getWorkoutDaysCount(startDate: String, endDate: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WorkoutLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<WorkoutLog>)

    @Update
    suspend fun updateLog(log: WorkoutLog)

    @Delete
    suspend fun deleteLog(log: WorkoutLog)

    @Query("SELECT * FROM workout_logs ORDER BY date ASC, id ASC")
    suspend fun getAllLogsSync(): List<WorkoutLog>

    @Query("DELETE FROM workout_logs WHERE date = :date")
    suspend fun deleteLogsForDate(date: String)
}

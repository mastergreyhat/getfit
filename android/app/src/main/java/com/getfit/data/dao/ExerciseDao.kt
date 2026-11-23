package com.getfit.data.dao

import androidx.room.*
import com.getfit.data.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY muscleGroup ASC, name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscleGroup(muscleGroup: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchExercises(query: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?

    @Query("SELECT DISTINCT muscleGroup FROM exercises ORDER BY muscleGroup ASC")
    fun getAllMuscleGroups(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("UPDATE exercises SET muscleGroup = :newMuscleGroup WHERE muscleGroup = :oldMuscleGroup")
    suspend fun renameMuscleGroup(oldMuscleGroup: String, newMuscleGroup: String)

    @Query("DELETE FROM exercises WHERE muscleGroup = :muscleGroup")
    suspend fun deleteExercisesByMuscleGroup(muscleGroup: String)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercisesSync(): List<Exercise>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup")
    suspend fun getExercisesByMuscleGroupSync(muscleGroup: String): List<Exercise>

    @Query("DELETE FROM exercises WHERE isCustom = 0")
    suspend fun deleteNonCustomExercises()
}

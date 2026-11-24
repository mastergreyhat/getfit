package com.getfit.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.getfit.data.AppDatabase
import com.getfit.data.ExerciseDatabase
import com.getfit.data.entity.FoodLog
import com.getfit.data.entity.UserProfile
import com.getfit.data.entity.WeightLog
import com.getfit.data.entity.WorkoutLog
import com.getfit.data.repository.GetFitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DailyCalorieStat(
    val date: String,
    val dayLabel: String,
    val calories: Double,
    val protein: Double,
    val targetCalories: Int,
    val targetProtein: Int
)

data class StrengthPoint(
    val date: String,
    val maxWeight: Double,
    val calculated1RM: Double
)

data class MuscleVolumeStat(
    val muscleGroup: String,
    val completedSets: Int,
    val targetSetsMin: Int = 12,
    val targetSetsMax: Int = 20
)

data class AnalyticsUiState(
    val userProfile: UserProfile = UserProfile(),
    val weeklyStats: List<DailyCalorieStat> = emptyList(),
    val proteinHitRatePercentage: Int = 0,
    val totalVolumePast30Days: Double = 0.0,
    val weightLogs: List<WeightLog> = emptyList(),
    val currentWeight: Double = 0.0,
    val strengthCurveExercises: List<String> = emptyList(),
    val selectedExercise: String = "Flat Barbell Bench Press",
    val exerciseHistory: List<StrengthPoint> = emptyList(),
    val personalBest: Double = 0.0,
    val muscleVolumes: List<MuscleVolumeStat> = emptyList()
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GetFitRepository(AppDatabase.getDatabase(application))

    private fun getTodayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalyticsData()
    }

    private fun loadAnalyticsData() {
        val today = getTodayDate()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        // 1 & 3. Observe Profile & Weight Logs together so fallback weight is always accurate
        viewModelScope.launch {
            combine(
                repository.getUserProfile(),
                repository.getAllWeightLogs()
            ) { profile, weights ->
                val user = profile ?: UserProfile()
                val latestWeight = weights.lastOrNull()?.weightKg ?: user.weight
                Pair(user, Pair(weights, latestWeight))
            }.collect { (user, weightPair) ->
                val (weights, latestWeight) = weightPair
                _uiState.update {
                    it.copy(
                        userProfile = user,
                        weightLogs = weights,
                        currentWeight = latestWeight
                    )
                }
            }
        }

        // 2. Observe 7-Day Food Logs for Calorie & Protein Hit Rate
        viewModelScope.launch(Dispatchers.Default) {
            repository.getFoodLogsForDateRange(startDate, today).collect { logs ->
                calculateWeeklyNutrition(logs)
            }
        }

        // 4. Observe Workout Logs for Strength Curves & Volume
        viewModelScope.launch(Dispatchers.Default) {
            val start30Cal = Calendar.getInstance()
            start30Cal.add(Calendar.DAY_OF_YEAR, -30)
            val start30 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(start30Cal.time)

            repository.getWorkoutLogsForDateRange(start30, today).collect { logs ->
                val exercises = logs.map { it.exerciseName }.distinct()
                val totalVol = logs.sumOf { it.weight * it.reps }

                // Calculate weekly hypertrophy volume per muscle group (last 7 days)
                val weeklyLogs = logs.filter { it.date >= startDate }
                val defaultExercises = ExerciseDatabase.getDefaultExercises()
                val muscleGroups = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs")
                val volumeStats = muscleGroups.map { mg ->
                    val setsCount = weeklyLogs.count { log ->
                        val matched = defaultExercises.find { it.name.equals(log.exerciseName, ignoreCase = true) }
                        matched?.muscleGroup?.contains(mg, ignoreCase = true) == true
                    }
                    MuscleVolumeStat(muscleGroup = mg, completedSets = setsCount)
                }

                _uiState.update { current ->
                    val defaultEx = if (exercises.contains(current.selectedExercise)) {
                        current.selectedExercise
                    } else {
                        exercises.firstOrNull() ?: "Flat Barbell Bench Press"
                    }
                    current.copy(
                        strengthCurveExercises = exercises,
                        selectedExercise = defaultEx,
                        totalVolumePast30Days = totalVol,
                        muscleVolumes = volumeStats
                    )
                }
                updateExerciseCurve(_uiState.value.selectedExercise, logs)
            }
        }
    }

    private fun calculateWeeklyNutrition(logs: List<FoodLog>) {
        val targetCal = _uiState.value.userProfile.dailyCalorieTarget.coerceAtLeast(1500)
        val targetPro = _uiState.value.userProfile.dailyProteinTarget.coerceAtLeast(100)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val stats = mutableListOf<DailyCalorieStat>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)

        var hitDays = 0

        for (i in 0..6) {
            val dateStr = dateFormat.format(cal.time)
            val dayName = dayNameFormat.format(cal.time)

            val logsForDay = logs.filter { it.date == dateStr }
            val totalCal = logsForDay.sumOf { it.totalCalories }
            val totalPro = logsForDay.sumOf { it.totalProtein }

            if (totalPro >= targetPro * 0.9) {
                hitDays++
            }

            stats.add(
                DailyCalorieStat(
                    date = dateStr,
                    dayLabel = dayName,
                    calories = totalCal,
                    protein = totalPro,
                    targetCalories = targetCal,
                    targetProtein = targetPro
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val hitRate = ((hitDays.toDouble() / 7.0) * 100).toInt()

        _uiState.update {
            it.copy(
                weeklyStats = stats,
                proteinHitRatePercentage = hitRate
            )
        }
    }

    fun selectExerciseForCurve(exerciseName: String) {
        _uiState.update { it.copy(selectedExercise = exerciseName) }
        viewModelScope.launch {
            val start30Cal = Calendar.getInstance()
            start30Cal.add(Calendar.DAY_OF_YEAR, -60)
            val start60 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(start30Cal.time)
            repository.getWorkoutLogsForDateRange(start60, getTodayDate()).firstOrNull()?.let { logs ->
                updateExerciseCurve(exerciseName, logs)
            }
        }
    }

    private fun updateExerciseCurve(exerciseName: String, allLogs: List<WorkoutLog>) {
        val exerciseLogs = allLogs.filter { it.exerciseName.equals(exerciseName, ignoreCase = true) }
        val points = exerciseLogs.groupBy { it.date }
            .map { (date, sets) ->
                val maxW = sets.maxOfOrNull { it.weight } ?: 0.0
                val bestSet = sets.maxByOrNull { it.weight }
                // Epley formula for 1RM: Weight * (1 + Reps / 30.0)
                val oneRM = if (bestSet != null && bestSet.reps > 0) {
                    bestSet.weight * (1.0 + bestSet.reps / 30.0)
                } else maxW

                StrengthPoint(
                    date = date,
                    maxWeight = maxW,
                    calculated1RM = (oneRM * 10).toInt() / 10.0
                )
            }
            .sortedBy { it.date }

        val pb = points.maxOfOrNull { it.maxWeight } ?: 0.0

        _uiState.update {
            it.copy(
                exerciseHistory = points,
                personalBest = pb
            )
        }
    }

    fun logBodyweight(weightKg: Double, notes: String = "") {
        viewModelScope.launch {
            repository.insertWeightLog(
                WeightLog(
                    weightKg = weightKg,
                    date = getTodayDate(),
                    notes = notes
                )
            )
        }
    }

    fun updateWeightLog(log: WeightLog) {
        viewModelScope.launch {
            repository.insertWeightLog(log)
        }
    }

    fun deleteWeightLog(log: WeightLog) {
        viewModelScope.launch {
            repository.deleteWeightLog(log)
        }
    }
}

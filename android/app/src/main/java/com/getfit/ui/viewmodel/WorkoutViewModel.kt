package com.getfit.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.getfit.data.AppDatabase
import com.getfit.data.PlannedExercise
import com.getfit.data.WorkoutDay
import com.getfit.data.WorkoutPlan
import com.getfit.data.WorkoutPlans
import com.getfit.data.entity.Exercise
import com.getfit.data.entity.StopwatchRecord
import com.getfit.data.entity.UserProfile
import com.getfit.data.entity.WorkoutLog
import com.getfit.data.repository.GetFitRepository
import com.getfit.ui.util.AppDateManager
import com.getfit.ui.util.CustomRoutinePreferences
import com.getfit.ui.util.StreakManager
import com.getfit.data.sync.CloudSyncManager
import org.json.JSONObject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ProgressiveOverloadInfo(
    val suggestedWeight: Double,
    val suggestedIncrease: Double,
    val reason: String
)

data class WorkoutUiState(
    val selectedDate: String = "",
    val dateDisplayLabel: String = "Today",
    val currentPlan: WorkoutPlan = WorkoutPlans.getPPLPlan(),
    val selectedDayIndex: Int = 0,
    val allPlans: List<WorkoutPlan> = WorkoutPlans.getAllPlans(),
    val exercises: List<Exercise> = emptyList(),
    val todayLogs: List<WorkoutLog> = emptyList(),
    val recentLogs: List<WorkoutLog> = emptyList(),
    val personalBests: Map<Long, Double> = emptyMap(),
    val overloadSuggestions: Map<Long, ProgressiveOverloadInfo> = emptyMap(),
    val userProfile: UserProfile = UserProfile(),
    val workoutDates: Set<String> = emptySet(),
    val stopwatchRecords: List<StopwatchRecord> = emptyList()
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GetFitRepository(AppDatabase.getDatabase(application))

    val selectedDate: StateFlow<String> = AppDateManager.selectedDate

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    fun resetToWorkoutTab() {
        _currentTab.value = 0
    }

    private var workoutLogsJob: kotlinx.coroutines.Job? = null

    private fun getTodayDate(): String = AppDateManager.getTodayDate()

    private val _uiState = MutableStateFlow(
        WorkoutUiState(
            selectedDate = AppDateManager.selectedDate.value,
            dateDisplayLabel = AppDateManager.getDateDisplayLabel(AppDateManager.selectedDate.value)
        )
    )
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val today = getTodayDate()

        // Observe UserProfile to set workout plan - defaults to index 0 ("Push A")
        viewModelScope.launch {
            repository.getUserProfile().collect { profile ->
                val user = profile ?: UserProfile()
                val plan = WorkoutPlans.getAllPlans().find {
                    it.name.equals(user.workoutPlan, ignoreCase = true)
                } ?: WorkoutPlans.getPPLPlan()
                val resolvedPlan = resolvePlanWithCustomRoutines(plan)

                _uiState.update {
                    it.copy(
                        userProfile = user,
                        currentPlan = resolvedPlan,
                        selectedDayIndex = 0 // Keep selection by default at Push A
                    )
                }
            }
        }

        // Observe Exercises
        viewModelScope.launch {
            repository.getAllExercises().collect { list ->
                _uiState.update { it.copy(exercises = list) }
                calculateOverloadSuggestions(list)
            }
        }

        // Observe AppDateManager.selectedDate changes and observe logs for that date
        viewModelScope.launch {
            AppDateManager.selectedDate.collect { date ->
                val label = AppDateManager.getDateDisplayLabel(date)
                _uiState.update { it.copy(selectedDate = date, dateDisplayLabel = label) }
                observeDateWorkoutLogs(date)
            }
        }

        // Observe dates with logged workouts
        viewModelScope.launch {
            repository.getWorkoutDates().collect { dates ->
                _uiState.update { it.copy(workoutDates = dates.toSet()) }
            }
        }

        // Observe saved stopwatch records (planks, wall sits, holds)
        viewModelScope.launch {
            repository.getAllStopwatchRecords().collect { records ->
                _uiState.update { it.copy(stopwatchRecords = records) }
            }
        }

        // Observe recent logs for progress tab (e.g. past 30 days)
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -30)
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            repository.getWorkoutLogsForDateRange(startDate, today).collect { logs ->
                // Calculate PBs
                val pbMap = mutableMapOf<Long, Double>()
                logs.forEach { log ->
                    val currentMax = pbMap[log.exerciseId] ?: 0.0
                    if (log.weight > currentMax) {
                        pbMap[log.exerciseId] = log.weight
                    }
                }
                _uiState.update { it.copy(recentLogs = logs, personalBests = pbMap) }
            }
        }
    }

    private fun observeDateWorkoutLogs(date: String) {
        workoutLogsJob?.cancel()
        workoutLogsJob = viewModelScope.launch {
            repository.getWorkoutLogsForDate(date).collect { logs ->
                _uiState.update { it.copy(todayLogs = logs) }
            }
        }
    }

    fun previousDate() {
        AppDateManager.previousDate()
    }

    fun nextDate() {
        AppDateManager.nextDate()
    }

    fun goToToday() {
        AppDateManager.goToToday()
    }

    fun selectDate(dateString: String) {
        AppDateManager.setDate(dateString)
    }

    fun selectDay(index: Int) {
        val daysCount = _uiState.value.currentPlan.days.size
        if (index in 0 until daysCount) {
            _uiState.update { it.copy(selectedDayIndex = index) }
        }
    }

    fun selectPlan(planName: String) {
        val plan = WorkoutPlans.getAllPlans().find { it.name.equals(planName, ignoreCase = true) }
            ?: return
        val resolvedPlan = resolvePlanWithCustomRoutines(plan)
        _uiState.update { it.copy(currentPlan = resolvedPlan, selectedDayIndex = 0) }
        viewModelScope.launch {
            repository.updateWorkoutPlan(plan.name)
        }
    }

    private fun resolvePlanWithCustomRoutines(plan: WorkoutPlan): WorkoutPlan {
        val context = getApplication<Application>().applicationContext
        val customizedDays = plan.days.map { day ->
            val custom = CustomRoutinePreferences.getCustomExercises(context, plan.name, day.dayName)
            if (custom != null) {
                day.copy(exercises = custom)
            } else {
                day
            }
        }
        return plan.copy(days = customizedDays)
    }

    fun addExerciseToTodayWorkout(
        exerciseName: String,
        sets: Int = 3,
        reps: String = "8-10",
        notes: String = ""
    ) {
        val currentPlan = _uiState.value.currentPlan
        val selectedDay = currentPlan.days.getOrNull(_uiState.value.selectedDayIndex) ?: return
        val basePlan = WorkoutPlans.getAllPlans().find { it.name.equals(currentPlan.name, ignoreCase = true) }
            ?: WorkoutPlans.getPPLPlan()
        val defaultExercises = basePlan.days.find { it.dayName.equals(selectedDay.dayName, ignoreCase = true) }?.exercises ?: emptyList()

        val context = getApplication<Application>().applicationContext
        val newPlanned = PlannedExercise(
            exerciseName = exerciseName.trim(),
            sets = sets,
            reps = reps.trim(),
            restSeconds = 90,
            notes = notes.trim()
        )
        CustomRoutinePreferences.addExerciseToDay(
            context = context,
            planName = currentPlan.name,
            dayName = selectedDay.dayName,
            defaultExercises = defaultExercises,
            newExercise = newPlanned
        )
        refreshCustomizedPlan()
    }

    fun removeExerciseFromTodayWorkout(exerciseName: String) {
        val currentPlan = _uiState.value.currentPlan
        val selectedDay = currentPlan.days.getOrNull(_uiState.value.selectedDayIndex) ?: return
        val basePlan = WorkoutPlans.getAllPlans().find { it.name.equals(currentPlan.name, ignoreCase = true) }
            ?: WorkoutPlans.getPPLPlan()
        val defaultExercises = basePlan.days.find { it.dayName.equals(selectedDay.dayName, ignoreCase = true) }?.exercises ?: emptyList()

        val context = getApplication<Application>().applicationContext
        val matchedPlanned = selectedDay.exercises.find { it.exerciseName.equals(exerciseName, ignoreCase = true) }

        CustomRoutinePreferences.removeExerciseFromDay(
            context = context,
            planName = currentPlan.name,
            dayName = selectedDay.dayName,
            defaultExercises = defaultExercises,
            exerciseName = exerciseName
        )

        // Move to Trash (10-day retention)
        viewModelScope.launch {
            try {
                val dataObj = org.json.JSONObject().apply {
                    put("planName", currentPlan.name)
                    put("dayName", selectedDay.dayName)
                    put("exerciseName", exerciseName)
                    put("sets", matchedPlanned?.sets ?: 3)
                    put("reps", matchedPlanned?.reps ?: "8-10")
                    put("notes", matchedPlanned?.notes ?: "")
                }
                repository.moveToTrash(
                    itemType = "Routine Exercise",
                    title = exerciseName,
                    subtitle = "${selectedDay.dayName} (${currentPlan.name})",
                    dataJson = dataObj.toString()
                )
            } catch (e: Exception) {
                // Ignore
            }
        }

        refreshCustomizedPlan()
    }

    fun modifyExerciseInTodayWorkout(
        exerciseName: String,
        sets: Int,
        reps: String,
        notes: String = ""
    ) {
        val currentPlan = _uiState.value.currentPlan
        val selectedDay = currentPlan.days.getOrNull(_uiState.value.selectedDayIndex) ?: return
        val basePlan = WorkoutPlans.getAllPlans().find { it.name.equals(currentPlan.name, ignoreCase = true) }
            ?: WorkoutPlans.getPPLPlan()
        val defaultExercises = basePlan.days.find { it.dayName.equals(selectedDay.dayName, ignoreCase = true) }?.exercises ?: emptyList()

        val context = getApplication<Application>().applicationContext
        val updated = PlannedExercise(
            exerciseName = exerciseName.trim(),
            sets = sets,
            reps = reps.trim(),
            restSeconds = 90,
            notes = notes.trim()
        )
        CustomRoutinePreferences.updateExerciseInDay(
            context = context,
            planName = currentPlan.name,
            dayName = selectedDay.dayName,
            defaultExercises = defaultExercises,
            updatedExercise = updated
        )
        refreshCustomizedPlan()
    }

    fun resetTodayWorkoutToDefault() {
        val currentPlan = _uiState.value.currentPlan
        val selectedDay = currentPlan.days.getOrNull(_uiState.value.selectedDayIndex) ?: return
        val context = getApplication<Application>().applicationContext
        CustomRoutinePreferences.resetDayToDefault(context, currentPlan.name, selectedDay.dayName)
        refreshCustomizedPlan()
    }

    private fun refreshCustomizedPlan() {
        val currentPlan = _uiState.value.currentPlan
        val basePlan = WorkoutPlans.getAllPlans().find { it.name.equals(currentPlan.name, ignoreCase = true) }
            ?: WorkoutPlans.getPPLPlan()
        val updatedPlan = resolvePlanWithCustomRoutines(basePlan)
        _uiState.update { it.copy(currentPlan = updatedPlan) }
    }

    private fun calculateOverloadSuggestions(exerciseList: List<Exercise>) {
        viewModelScope.launch {
            val suggestions = mutableMapOf<Long, ProgressiveOverloadInfo>()
            exerciseList.forEach { exercise ->
                // fetch logs for this exercise
                repository.getLogsForExercise(exercise.id).firstOrNull()?.let { logs ->
                    if (logs.isNotEmpty()) {
                        // Group by date, get most recent session
                        val lastSession = logs.groupBy { it.date }.entries.firstOrNull()?.value ?: emptyList()
                        if (lastSession.isNotEmpty()) {
                            val lastWeight = lastSession.maxOf { it.weight }
                            val minReps = lastSession.minOf { it.reps }
                            
                            // If user hit target reps (>= 8 for compound, >= 10 for isolation)
                            val targetRepsThreshold = if (exercise.isCompound) 8 else 10
                            val increase = if (minReps >= targetRepsThreshold) {
                                if (exercise.isCompound) 2.5 else 1.0
                            } else {
                                0.0
                            }

                            suggestions[exercise.id] = ProgressiveOverloadInfo(
                                suggestedWeight = lastWeight + increase,
                                suggestedIncrease = increase,
                                reason = if (increase > 0) {
                                    "Target reached! Increase by ${increase}kg"
                                } else {
                                    "Maintain ${lastWeight}kg until all reps completed cleanly"
                                }
                            )
                        } else {
                            val defaultWeight = if (exercise.isCompound) 40.0 else 10.0
                            suggestions[exercise.id] = ProgressiveOverloadInfo(
                                suggestedWeight = defaultWeight,
                                suggestedIncrease = 0.0,
                                reason = "Starting weight recommended"
                            )
                        }
                    } else {
                        val defaultWeight = if (exercise.isCompound) 40.0 else 10.0
                        suggestions[exercise.id] = ProgressiveOverloadInfo(
                            suggestedWeight = defaultWeight,
                            suggestedIncrease = 0.0,
                            reason = "Starting weight recommended"
                        )
                    }
                }
            }
            _uiState.update { it.copy(overloadSuggestions = suggestions) }
        }
    }

    fun logSet(
        exerciseId: Long,
        exerciseName: String,
        setNumber: Int,
        reps: Int,
        weight: Double,
        workoutPlanDay: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val currentPB = _uiState.value.personalBests[exerciseId] ?: 0.0
            val isPb = weight > currentPB && reps > 0
            val dateStr = AppDateManager.selectedDate.value
            val existing = _uiState.value.todayLogs.find {
                (it.exerciseId == exerciseId || it.exerciseName.equals(exerciseName, ignoreCase = true)) &&
                    it.setNumber == setNumber
            }

            val log = WorkoutLog(
                id = existing?.id ?: 0L,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                setNumber = setNumber,
                reps = reps,
                weight = weight,
                date = dateStr,
                workoutPlanDay = workoutPlanDay,
                notes = notes,
                isPersonalBest = isPb
            )
            repository.insertWorkoutLog(log)
            StreakManager.recordLogAction(getApplication(), log.date)
            CloudSyncManager.triggerSilentUpload(getApplication(), repository)
        }
    }

    /**
     * Replaces all logged sets for this exercise on the currently selected date so
     * deleted sets do not linger and reopening shows the exact saved state.
     */
    fun saveSessionSets(
        exerciseId: Long,
        exerciseName: String,
        workoutPlanDay: String,
        notes: String,
        setsData: List<Pair<Double, Int>>
    ) {
        viewModelScope.launch {
            val currentPB = _uiState.value.personalBests[exerciseId] ?: 0.0
            val dateStr = AppDateManager.selectedDate.value
            val existingForExercise = _uiState.value.todayLogs.filter {
                it.exerciseId == exerciseId || it.exerciseName.equals(exerciseName, ignoreCase = true)
            }

            // Remove any extra sets beyond the saved count
            existingForExercise
                .filter { it.setNumber > setsData.size }
                .forEach { repository.deleteWorkoutLog(it) }

            // Upsert exact sets 1..setsData.size
            setsData.forEachIndexed { idx, (weight, reps) ->
                val setNum = idx + 1
                val existing = existingForExercise.find { it.setNumber == setNum }
                val isPb = weight > currentPB && reps > 0
                repository.insertWorkoutLog(
                    WorkoutLog(
                        id = existing?.id ?: 0L,
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
                        setNumber = setNum,
                        reps = reps,
                        weight = weight,
                        date = dateStr,
                        workoutPlanDay = workoutPlanDay,
                        notes = notes,
                        isPersonalBest = isPb
                    )
                )
            }

            if (setsData.isNotEmpty()) {
                StreakManager.recordLogAction(getApplication(), dateStr)
            }
            CloudSyncManager.triggerSilentUpload(getApplication(), repository)
        }
    }

    fun getLogsForExercise(exerciseId: Long): Flow<List<WorkoutLog>> = repository.getLogsForExercise(exerciseId)

    fun deleteWorkoutLog(log: WorkoutLog) {
        viewModelScope.launch {
            repository.deleteWorkoutLog(log)
            CloudSyncManager.triggerSilentUpload(getApplication(), repository)
        }
    }

    /**
     * Deletes all logged sets for [exerciseId]/[exerciseName] on the currently selected date
     * (today or any previous date) so it returns to the unlogged state in Training, Diary, and Analytics.
     */
    fun resetExerciseSession(exerciseId: Long, exerciseName: String) {
        viewModelScope.launch {
            val dateStr = AppDateManager.selectedDate.value
            val logsForDate = repository.getWorkoutLogsForDate(dateStr).first()
            logsForDate
                .filter { it.exerciseId == exerciseId || it.exerciseName.equals(exerciseName, ignoreCase = true) }
                .forEach { repository.deleteWorkoutLog(it) }
            CloudSyncManager.triggerSilentUpload(getApplication(), repository)
        }
    }

    fun saveStopwatchRecord(title: String, elapsedMillis: Long, formattedTime: String) {
        viewModelScope.launch {
            val record = StopwatchRecord(
                title = title.trim().ifBlank { "Plank" },
                elapsedMillis = elapsedMillis,
                formattedTime = formattedTime,
                date = AppDateManager.getTodayDate() // Stopwatch is always logged for the current real-world date
            )
            repository.insertStopwatchRecord(record)
        }
    }

    fun deleteStopwatchRecord(record: StopwatchRecord) {
        viewModelScope.launch {
            repository.deleteStopwatchRecord(record)
        }
    }

    fun addCustomExercise(
        name: String,
        muscleGroup: String,
        equipment: String = "Other",
        description: String = "Custom exercise",
        isCompound: Boolean = false,
        onComplete: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val formattedName = name.trim().split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            val formattedCategory = muscleGroup.trim().split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }

            // Duplicate prevention check
            val existing = repository.getAllExercisesSync()
            if (existing.any { it.name.trim().equals(formattedName, ignoreCase = true) }) {
                // Exercise already exists; do not insert duplicate
                val existingEx = existing.first { it.name.trim().equals(formattedName, ignoreCase = true) }
                onComplete?.invoke(existingEx.id)
                return@launch
            }

            val exercise = Exercise(
                name = formattedName,
                muscleGroup = formattedCategory,
                equipment = equipment.ifBlank { "Other" },
                description = description.ifBlank { "Custom exercise added by user" },
                isCompound = isCompound,
                isCustom = true
            )
            val newId = repository.insertExercise(exercise)
            onComplete?.invoke(newId)
        }
    }

    fun updateExercise(
        exerciseId: Long,
        name: String,
        muscleGroup: String,
        equipment: String,
        description: String,
        isCompound: Boolean
    ) {
        viewModelScope.launch {
            val formattedName = name.trim().split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            val formattedCategory = muscleGroup.trim().split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            val existing = repository.getExerciseById(exerciseId)
            val updated = existing?.copy(
                name = formattedName,
                muscleGroup = formattedCategory,
                equipment = equipment.ifBlank { "Other" },
                description = description,
                isCompound = isCompound
            ) ?: Exercise(
                id = exerciseId,
                name = formattedName,
                muscleGroup = formattedCategory,
                equipment = equipment.ifBlank { "Other" },
                description = description,
                isCompound = isCompound
            )
            repository.updateExercise(updated)
        }
    }

    private fun exerciseToJson(e: Exercise): String {
        val obj = JSONObject()
        obj.put("name", e.name)
        obj.put("muscleGroup", e.muscleGroup)
        obj.put("equipment", e.equipment)
        obj.put("description", e.description)
        obj.put("isCompound", e.isCompound)
        obj.put("isCustom", e.isCustom)
        return obj.toString()
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            // Move to Trash (10-day retention) before deleting
            repository.moveToTrash(
                itemType = "Exercise",
                title = exercise.name,
                subtitle = "${exercise.muscleGroup} • ${exercise.equipment}",
                dataJson = exerciseToJson(exercise)
            )
            repository.deleteExercise(exercise)
        }
    }

    fun renameMuscleGroup(oldName: String, newName: String) {
        viewModelScope.launch {
            val formattedNew = newName.trim().split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            if (formattedNew.isNotBlank() && !formattedNew.equals(oldName, ignoreCase = true)) {
                repository.renameMuscleGroup(oldName, formattedNew)
            }
        }
    }

    fun deleteMuscleGroup(muscleGroup: String) {
        viewModelScope.launch {
            val exercises = repository.getExercisesByMuscleGroupSync(muscleGroup)
            exercises.forEach { ex ->
                repository.moveToTrash(
                    itemType = "Exercise",
                    title = ex.name,
                    subtitle = "${ex.muscleGroup} • ${ex.equipment}",
                    dataJson = exerciseToJson(ex)
                )
            }
            repository.deleteExercisesByMuscleGroup(muscleGroup)
        }
    }

    fun deleteCustomExercise(exercise: Exercise) {
        deleteExercise(exercise)
    }
}


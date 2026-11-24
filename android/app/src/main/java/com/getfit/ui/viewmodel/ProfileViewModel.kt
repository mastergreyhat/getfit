package com.getfit.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.getfit.data.AppDatabase
import com.getfit.data.MealPlanSuggestions
import com.getfit.data.entity.MealSchedule
import com.getfit.data.entity.TrashItem
import com.getfit.data.entity.UserProfile
import com.getfit.data.repository.GetFitRepository
import com.getfit.notification.AlarmScheduler
import com.getfit.notification.ReminderPreferences
import com.getfit.ui.util.CustomRoutinePreferences
import com.getfit.ui.util.AuthPreferences
import com.getfit.ui.util.OnboardingPreferences
import com.getfit.data.remote.AuthService
import com.getfit.data.sync.CloudSyncManager
import com.getfit.data.PlannedExercise
import com.getfit.data.WorkoutPlans
import com.getfit.data.entity.WeightLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CalculatedNutrition(
    val bmr: Int,
    val tdee: Int,
    val targetCalories: Int,
    val targetProtein: Int,
    val targetCarbs: Int,
    val targetFat: Int
)

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val calculatedNutrition: CalculatedNutrition = CalculatedNutrition(1700, 2400, 2700, 140, 350, 60),
    val trashItems: List<TrashItem> = emptyList(),
    val mealSchedules: List<MealSchedule> = emptyList(),
    val isSavedSuccess: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null,
    val userName: String? = null,
    val authProvider: String? = null,
    val lastSyncedMillis: Long = 0L,
    val isSyncing: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GetFitRepository(AppDatabase.getDatabase(application))

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadTrash()
        loadMealSchedules()
        refreshAuthState()
    }

    private fun loadMealSchedules() {
        viewModelScope.launch {
            repository.getAllMealSchedules().collect { schedules ->
                _uiState.update { it.copy(mealSchedules = schedules) }
            }
        }
    }

    private fun loadTrash() {
        viewModelScope.launch {
            repository.purgeExpiredTrash()
            repository.getActiveTrashItems().collect { items ->
                _uiState.update { it.copy(trashItems = items) }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            repository.getUserProfile().collect { userProfile ->
                val profile = userProfile ?: UserProfile()
                val calculated = calculateNutrition(
                    weight = profile.weight,
                    height = profile.height,
                    age = profile.age,
                    gender = profile.gender,
                    activityLevel = profile.activityLevel,
                    goal = profile.goal
                )
                _uiState.update {
                    it.copy(
                        profile = profile,
                        calculatedNutrition = calculated
                    )
                }
            }
        }
    }

    fun calculateNutrition(
        weight: Double,
        height: Double,
        age: Int,
        gender: String,
        activityLevel: String,
        goal: String
    ): CalculatedNutrition {
        // Mifflin-St Jeor formula
        val baseBmr = (10.0 * weight) + (6.25 * height) - (5.0 * age)
        val bmr = if (gender.equals("Female", ignoreCase = true)) {
            (baseBmr - 161.0).toInt()
        } else {
            (baseBmr + 5.0).toInt()
        }

        val multiplier = when (activityLevel.lowercase()) {
            "sedentary" -> 1.2
            "light", "lightly active" -> 1.375
            "moderate", "moderately active" -> 1.55
            "very active" -> 1.725
            "extremely active" -> 1.9
            else -> 1.55
        }

        val tdee = (bmr * multiplier).toInt()

        val targetCalories = when {
            goal.contains("Muscle", ignoreCase = true) || goal.contains("Bulk", ignoreCase = true) -> tdee + 350
            goal.contains("Fat", ignoreCase = true) || goal.contains("Cut", ignoreCase = true) -> (tdee - 450).coerceAtLeast(1500)
            else -> tdee
        }

        // Protein: 2.0g per kg of bodyweight
        val proteinGrams = (weight * 2.0).toInt()
        // Fat: 0.8g per kg of bodyweight
        val fatGrams = (weight * 0.8).toInt()
        // Carbs: remaining calories
        val remainingCal = (targetCalories - (proteinGrams * 4) - (fatGrams * 9)).coerceAtLeast(0)
        val carbsGrams = (remainingCal / 4)

        return CalculatedNutrition(
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCalories,
            targetProtein = proteinGrams,
            targetCarbs = carbsGrams,
            targetFat = fatGrams
        )
    }

    fun saveProfile(
        name: String,
        age: Int,
        weight: Double,
        height: Double,
        gender: String,
        activityLevel: String,
        goal: String,
        workoutPlan: String,
        customCalories: Int? = null,
        customProtein: Int? = null,
        customCarbs: Int? = null,
        customFat: Int? = null
    ) {
        viewModelScope.launch {
            val calc = calculateNutrition(weight, height, age, gender, activityLevel, goal)
            val updatedProfile = UserProfile(
                id = 1,
                name = name,
                age = age,
                weight = weight,
                height = height,
                gender = gender,
                activityLevel = activityLevel,
                goal = goal,
                workoutPlan = workoutPlan,
                dailyCalorieTarget = customCalories ?: calc.targetCalories,
                dailyProteinTarget = customProtein ?: calc.targetProtein,
                dailyCarbsTarget = customCarbs ?: calc.targetCarbs,
                dailyFatTarget = customFat ?: calc.targetFat,
                dailyWaterTarget = ((weight * 35) + 500).toInt()
            )
            repository.insertOrUpdateProfile(updatedProfile)

            // Store or update starting weight in weight_logs
            if (weight > 0.0) {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val existingWeights = repository.getAllWeightLogsSync()
                val startingLog = existingWeights.find { it.notes == "Starting weight" }
                    ?: if (existingWeights.size == 1 && (existingWeights[0].notes.isBlank() || existingWeights[0].notes == "Starting weight" || existingWeights[0].weightKg == 70.0)) existingWeights.first() else null

                if (startingLog != null) {
                    repository.insertWeightLog(startingLog.copy(weightKg = weight))
                } else if (existingWeights.isEmpty()) {
                    repository.insertWeightLog(
                        WeightLog(weightKg = weight, date = today, notes = "Starting weight")
                    )
                }
            }

            _uiState.update { it.copy(profile = updatedProfile, calculatedNutrition = calc, isSavedSuccess = true) }
        }
    }

    fun applyPresetDietPlan(calories: Int) {
        val preset = MealPlanSuggestions.getAllPlans().find { it.totalCalories == calories }
            ?: MealPlanSuggestions.get2500CalPlan()

        viewModelScope.launch {
            val current = _uiState.value.profile
            val protein = preset.totalProtein
            val fat = (current.weight * 0.8).toInt()
            val remainingCal = (preset.totalCalories - (protein * 4) - (fat * 9)).coerceAtLeast(0)
            val carbs = remainingCal / 4

            val updated = current.copy(
                dailyCalorieTarget = preset.totalCalories,
                dailyProteinTarget = protein,
                dailyCarbsTarget = carbs,
                dailyFatTarget = fat
            )
            repository.insertOrUpdateProfile(updated)
            _uiState.update { it.copy(profile = updated) }
        }
    }

    fun resetExercisesToDefault(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val restoredCount = repository.resetExercisesToDefault()
            onComplete?.invoke(restoredCount)
        }
    }

    fun restoreTrashItem(item: TrashItem, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            if (item.itemType == "Routine Exercise") {
                try {
                    val json = org.json.JSONObject(item.dataJson)
                    val planName = json.getString("planName")
                    val dayName = json.getString("dayName")
                    val exerciseName = json.getString("exerciseName")
                    val sets = json.optInt("sets", 3)
                    val reps = json.optString("reps", "8-10")
                    val notes = json.optString("notes", "")
                    val basePlan = WorkoutPlans.getAllPlans().find { it.name.equals(planName, ignoreCase = true) }
                        ?: WorkoutPlans.getPPLPlan()
                    val defaultExercises = basePlan.days.find { it.dayName.equals(dayName, ignoreCase = true) }?.exercises ?: emptyList()
                    val context = getApplication<Application>().applicationContext
                    CustomRoutinePreferences.addExerciseToDay(
                        context = context,
                        planName = planName,
                        dayName = dayName,
                        defaultExercises = defaultExercises,
                        newExercise = PlannedExercise(exerciseName, sets, reps, 90, notes)
                    )
                    repository.deleteTrashItem(item)
                    onComplete?.invoke(true)
                } catch (e: Exception) {
                    onComplete?.invoke(false)
                }
            } else {
                val success = repository.restoreTrashItem(item)
                onComplete?.invoke(success)
            }
        }
    }

    fun deleteTrashItem(item: TrashItem) {
        viewModelScope.launch {
            repository.deleteTrashItem(item)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun exportBackup(context: android.content.Context, onComplete: ((java.io.File?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val json = repository.exportBackupJson()
                val backupDir = java.io.File(context.cacheDir, "backups").apply { mkdirs() }
                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                val file = java.io.File(backupDir, "getfit_backup_$timeStamp.json")
                file.writeText(json)

                try {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                        val dlFile = java.io.File(downloadsDir, "GetFit_Full_Backup.json")
                        dlFile.writeText(json)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ProfileViewModel", "Could not copy to public downloads: ${e.message}")
                }

                onComplete?.invoke(file)
            } catch (e: Exception) {
                onComplete?.invoke(null)
            }
        }
    }


    fun importBackup(context: android.content.Context, uri: android.net.Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader().use { it?.readText() } ?: ""
                if (jsonString.isBlank()) {
                    onResult(false, "Selected backup file is empty.")
                    return@launch
                }
                val summary = repository.importBackupJson(jsonString)
                loadProfile()
                loadTrash()
                onResult(true, summary)
            } catch (e: Exception) {
                onResult(false, "Failed to restore backup: ${e.localizedMessage ?: "Invalid format"}")
            }
        }
    }

    fun updateMealSchedule(mealType: String, time: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val existing = repository.getScheduleByMealType(mealType).firstOrNull()
            val updated = if (existing != null) {
                existing.copy(scheduledTime = time, isEnabled = isEnabled)
            } else {
                MealSchedule(
                    mealType = mealType,
                    scheduledTime = time,
                    suggestedFoods = "",
                    targetCalories = 0,
                    targetProtein = 0,
                    isEnabled = isEnabled
                )
            }
            repository.insertMealSchedule(updated)

            val context = getApplication<Application>()
            if (isEnabled) {
                AlarmScheduler.scheduleMealReminder(
                    context = context,
                    mealType = mealType,
                    time12H = time,
                    suggestedFood = updated.suggestedFoods
                )
            } else {
                AlarmScheduler.cancelMealReminder(context, mealType)
            }
        }
    }

    fun updateWaterSettings(enabled: Boolean, times: List<String>) {
        val context = getApplication<Application>()
        ReminderPreferences.setWaterEnabled(context, enabled)
        ReminderPreferences.setWaterTimes(context, times)
        if (enabled) {
            for (time in times) {
                AlarmScheduler.scheduleWaterReminder(context, time)
            }
        } else {
            AlarmScheduler.cancelAllWaterReminders(context, times)
        }
    }

    fun refreshAuthState() {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                isLoggedIn = AuthPreferences.isLoggedIn(app),
                userEmail = AuthPreferences.getUserEmail(app),
                userName = AuthPreferences.getUserName(app),
                authProvider = AuthPreferences.getAuthProvider(app),
                lastSyncedMillis = AuthPreferences.getLastSyncedTime(app)
            )
        }
    }

    fun sendOtp(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val res = AuthService.sendOtp(email)
            _uiState.update { it.copy(isSyncing = false) }
            res.fold(
                onSuccess = { msg ->
                    onResult(true, msg)
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "Failed to send verification code.")
                }
            )
        }
    }

    fun register(email: String, password: String, name: String, otp: String, onResult: (Boolean, String) -> Unit) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val res = AuthService.register(email, password, name, otp)
            res.fold(
                onSuccess = { authRes ->
                    AuthPreferences.saveSession(
                        app,
                        authRes.token,
                        authRes.user.id,
                        authRes.user.email,
                        authRes.user.name,
                        authRes.user.authProvider
                    )
                    refreshAuthState()
                    val syncRes = CloudSyncManager.downloadAndRestore(app, repository)
                    loadProfile()
                    loadTrash()
                    loadMealSchedules()
                    _uiState.update { it.copy(isSyncing = false) }
                    onResult(true, "Account created! Welcome to GetFit.")
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSyncing = false) }
                    onResult(false, err.message ?: "Registration failed.")
                }
            )
        }
    }

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val res = AuthService.login(email, password)
            res.fold(
                onSuccess = { authRes ->
                    AuthPreferences.saveSession(
                        app,
                        authRes.token,
                        authRes.user.id,
                        authRes.user.email,
                        authRes.user.name,
                        authRes.user.authProvider
                    )
                    refreshAuthState()
                    val syncRes = CloudSyncManager.downloadAndRestore(app, repository)
                    loadProfile()
                    loadTrash()
                    loadMealSchedules()
                    OnboardingPreferences.setOnboardingCompleted(app, true)
                    _uiState.update { it.copy(isSyncing = false) }
                    onResult(true, "Welcome back!")
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSyncing = false) }
                    onResult(false, err.message ?: "Login failed.")
                }
            )
        }
    }

    fun logout(onResult: () -> Unit) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            CloudSyncManager.clearLocalUserData(app, repository)
            AuthPreferences.clearSession(app)
            OnboardingPreferences.setOnboardingCompleted(app, false)
            refreshAuthState()
            loadProfile()
            loadTrash()
            loadMealSchedules()
            onResult()
        }
    }

    fun syncNow(onResult: (Boolean, String) -> Unit) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val (success, msg) = CloudSyncManager.syncNow(app, repository)
            refreshAuthState()
            _uiState.update { it.copy(isSyncing = false) }
            onResult(success, msg)
        }
    }
}


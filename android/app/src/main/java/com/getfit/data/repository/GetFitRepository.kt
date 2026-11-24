package com.getfit.data.repository

import com.getfit.data.AppDatabase
import com.getfit.data.ExerciseDatabase
import com.getfit.data.dao.MuscleVolumeSummary
import com.getfit.data.entity.*
import com.getfit.data.remote.OpenFoodFactsService
import com.getfit.ui.util.AppDateManager
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GetFitRepository(private val database: AppDatabase) {
    // ===== Food =====
    private val foodDao = database.foodDao()
    fun getAllFoods(): Flow<List<FoodItem>> = foodDao.getAllFoods()
    fun getFoodsByCategory(category: String): Flow<List<FoodItem>> = foodDao.getFoodsByCategory(category)
    fun searchFoods(query: String): Flow<List<FoodItem>> = foodDao.searchFoods(query)
    suspend fun getAllFoodNames(): List<String> = foodDao.getAllFoodNames()
    suspend fun getFoodById(id: Long): FoodItem? = foodDao.getFoodById(id)
    fun getAllCategories(): Flow<List<String>> = foodDao.getAllCategories()
    suspend fun insertFood(food: FoodItem): Long = foodDao.insertFood(food)
    suspend fun insertAllFoods(foods: List<FoodItem>) = foodDao.insertAllFoods(foods)
    suspend fun updateFood(food: FoodItem) = foodDao.updateFood(food)
    suspend fun deleteFood(food: FoodItem) = foodDao.deleteFood(food)
    suspend fun getAllDefaultFoods(): List<FoodItem> = foodDao.getAllDefaultFoods()
    suspend fun getAllFoodsSync(): List<FoodItem> = foodDao.getAllFoodsSync()
    suspend fun getFoodCount(): Int = foodDao.getFoodCount()

    // ===== Online Food Search & Barcode =====
    suspend fun searchOnlineFood(query: String, pageSize: Int = 10): List<FoodItem> {
        return OpenFoodFactsService.searchFoods(query, pageSize)
    }

    suspend fun saveOnlineFood(foodItem: FoodItem): Long {
        return foodDao.insertFood(foodItem.copy(id = 0, isCustom = true))
    }

    suspend fun getProductByBarcode(barcode: String): FoodItem? {
        return OpenFoodFactsService.getProductByBarcode(barcode)
    }

    // ===== Food Log =====
    private val foodLogDao = database.foodLogDao()
    fun getLogsForDate(date: String): Flow<List<FoodLog>> = foodLogDao.getLogsForDate(date)
    fun getLogsForDateAndMeal(date: String, mealType: String): Flow<List<FoodLog>> = foodLogDao.getLogsForDateAndMeal(date, mealType)
    fun getTotalCaloriesForDate(date: String): Flow<Double?> = foodLogDao.getTotalCaloriesForDate(date)
    fun getTotalProteinForDate(date: String): Flow<Double?> = foodLogDao.getTotalProteinForDate(date)
    fun getTotalCarbsForDate(date: String): Flow<Double?> = foodLogDao.getTotalCarbsForDate(date)
    fun getTotalFatForDate(date: String): Flow<Double?> = foodLogDao.getTotalFatForDate(date)
    fun getTotalFiberForDate(date: String): Flow<Double?> = foodLogDao.getTotalFiberForDate(date)
    fun getFoodLogsForDateRange(startDate: String, endDate: String): Flow<List<FoodLog>> = foodLogDao.getLogsForDateRange(startDate, endDate)
    fun getFoodLogDates(): Flow<List<String>> = foodLogDao.getFoodLogDates()
    fun getWaterLogDates(): Flow<List<String>> = waterLogDao.getWaterLogDates()
    fun getAllActiveDates(): Flow<Set<String>> = kotlinx.coroutines.flow.combine(
        foodLogDao.getFoodLogDates(),
        workoutLogDao.getWorkoutDates(),
        waterLogDao.getWaterLogDates()
    ) { f, w, wa -> (f + w + wa).toSet() }
    suspend fun insertFoodLog(log: FoodLog): Long = foodLogDao.insertLog(log)
    suspend fun insertAllFoodLogs(logs: List<FoodLog>) = foodLogDao.insertAllLogs(logs)
    suspend fun getLogsForDateSync(date: String): List<FoodLog> = foodLogDao.getLogsForDateSync(date)
    suspend fun updateFoodLog(log: FoodLog) = foodLogDao.updateLog(log)
    suspend fun deleteFoodLog(log: FoodLog) = foodLogDao.deleteLog(log)
    suspend fun deleteFoodLogsForDate(date: String) = foodLogDao.deleteLogsForDate(date)

    // ===== Exercise =====
    private val exerciseDao = database.exerciseDao()
    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()
    fun getExercisesByMuscleGroup(muscleGroup: String): Flow<List<Exercise>> = exerciseDao.getExercisesByMuscleGroup(muscleGroup)
    fun searchExercises(query: String): Flow<List<Exercise>> = exerciseDao.searchExercises(query)
    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getExerciseById(id)
    fun getAllMuscleGroups(): Flow<List<String>> = exerciseDao.getAllMuscleGroups()
    suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.insertExercise(exercise)
    suspend fun insertAllExercises(exercises: List<Exercise>) = exerciseDao.insertAllExercises(exercises)
    suspend fun updateExercise(exercise: Exercise) = exerciseDao.updateExercise(exercise)
    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.deleteExercise(exercise)
    suspend fun renameMuscleGroup(oldMuscleGroup: String, newMuscleGroup: String) = exerciseDao.renameMuscleGroup(oldMuscleGroup, newMuscleGroup)
    suspend fun deleteExercisesByMuscleGroup(muscleGroup: String) = exerciseDao.deleteExercisesByMuscleGroup(muscleGroup)
    suspend fun getExerciseCount(): Int = exerciseDao.getExerciseCount()
    suspend fun getAllExercisesSync(): List<Exercise> = exerciseDao.getAllExercisesSync()
    suspend fun getExercisesByMuscleGroupSync(muscleGroup: String): List<Exercise> = exerciseDao.getExercisesByMuscleGroupSync(muscleGroup)

    // ===== Workout Log =====
    private val workoutLogDao = database.workoutLogDao()
    fun getWorkoutLogsForDate(date: String): Flow<List<WorkoutLog>> = workoutLogDao.getLogsForDate(date)
    fun getLogsForExercise(exerciseId: Long): Flow<List<WorkoutLog>> = workoutLogDao.getLogsForExercise(exerciseId)
    fun getExerciseHistory(exerciseId: Long): Flow<List<WorkoutLog>> = workoutLogDao.getExerciseHistory(exerciseId)
    fun getRecentWorkouts(limit: Int = 20): Flow<List<WorkoutLog>> = workoutLogDao.getRecentWorkouts(limit)
    fun getPersonalBest(exerciseId: Long): Flow<WorkoutLog?> = workoutLogDao.getPersonalBest(exerciseId)
    fun getWorkoutLogsForDateRange(startDate: String, endDate: String): Flow<List<WorkoutLog>> = workoutLogDao.getLogsForDateRange(startDate, endDate)
    fun getMuscleVolumeForDateRange(startDate: String, endDate: String): Flow<List<MuscleVolumeSummary>> = workoutLogDao.getMuscleVolumeForDateRange(startDate, endDate)
    fun getMaxWeight(exerciseId: Long): Flow<Double?> = workoutLogDao.getMaxWeight(exerciseId)
    fun getWorkoutDates(): Flow<List<String>> = workoutLogDao.getWorkoutDates()
    fun getWorkoutDaysCount(startDate: String, endDate: String): Flow<Int> = workoutLogDao.getWorkoutDaysCount(startDate, endDate)
    suspend fun insertWorkoutLog(log: WorkoutLog): Long = workoutLogDao.insertLog(log)
    suspend fun insertAllWorkoutLogs(logs: List<WorkoutLog>) = workoutLogDao.insertAllLogs(logs)
    suspend fun updateWorkoutLog(log: WorkoutLog) = workoutLogDao.updateLog(log)
    suspend fun deleteWorkoutLog(log: WorkoutLog) = workoutLogDao.deleteLog(log)
    suspend fun deleteWorkoutLogsForDate(date: String) = workoutLogDao.deleteLogsForDate(date)

    // ===== User Profile =====
    private val userProfileDao = database.userProfileDao()
    fun getUserProfile(): Flow<UserProfile?> = userProfileDao.getUserProfile()
    suspend fun insertOrUpdateProfile(profile: UserProfile) = userProfileDao.insertOrUpdate(profile)
    suspend fun updateProfile(profile: UserProfile) = userProfileDao.updateProfile(profile)
    suspend fun updateNutritionTargets(calories: Int, protein: Int, carbs: Int, fat: Int) = userProfileDao.updateNutritionTargets(calories, protein, carbs, fat)
    suspend fun updateWaterTarget(target: Int) = userProfileDao.updateWaterTarget(target)
    suspend fun updateWorkoutPlan(plan: String) = userProfileDao.updateWorkoutPlan(plan)

    // ===== Meal Schedule =====
    private val mealScheduleDao = database.mealScheduleDao()
    fun getAllMealSchedules(): Flow<List<MealSchedule>> = mealScheduleDao.getAllSchedules()
    fun getEnabledMealSchedules(): Flow<List<MealSchedule>> = mealScheduleDao.getEnabledSchedules()
    fun getScheduleByMealType(mealType: String): Flow<MealSchedule?> = mealScheduleDao.getScheduleByMealType(mealType)
    suspend fun getMealScheduleById(id: Long): MealSchedule? = mealScheduleDao.getScheduleById(id)
    suspend fun insertMealSchedule(schedule: MealSchedule): Long = mealScheduleDao.insertSchedule(schedule)
    suspend fun insertAllMealSchedules(schedules: List<MealSchedule>) = mealScheduleDao.insertAllSchedules(schedules)
    suspend fun updateMealSchedule(schedule: MealSchedule) = mealScheduleDao.updateSchedule(schedule)
    suspend fun deleteMealSchedule(schedule: MealSchedule) = mealScheduleDao.deleteSchedule(schedule)
    suspend fun deleteAllMealSchedules() = mealScheduleDao.deleteAllSchedules()

    // ===== Bodyweight Log =====
    private val weightLogDao = database.weightLogDao()
    fun getAllWeightLogs(): Flow<List<WeightLog>> = weightLogDao.getAllWeightLogs()
    fun getWeightLogsForDateRange(startDate: String, endDate: String): Flow<List<WeightLog>> = weightLogDao.getWeightLogsForDateRange(startDate, endDate)
    fun getLatestWeightLog(): Flow<WeightLog?> = weightLogDao.getLatestWeightLog()
    suspend fun insertWeightLog(log: WeightLog): Long = weightLogDao.insertWeightLog(log)
    suspend fun deleteWeightLog(log: WeightLog) = weightLogDao.deleteWeightLog(log)
    suspend fun deleteWeightLogById(id: Long) = weightLogDao.deleteWeightLogById(id)
    suspend fun getAllWeightLogsSync(): List<WeightLog> = weightLogDao.getAllWeightLogsSync()

    // ===== Trash / Recently Deleted (10-Day Retention) =====
    private val trashDao = database.trashDao()

    private fun getTrashCutoff(): Long = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000L) // 10 days

    fun getActiveTrashItems(): Flow<List<TrashItem>> {
        return trashDao.getActiveTrashItems(getTrashCutoff())
    }

    suspend fun purgeExpiredTrash() {
        trashDao.purgeExpiredTrash(getTrashCutoff())
    }

    suspend fun moveToTrash(itemType: String, title: String, subtitle: String, dataJson: String): Long {
        purgeExpiredTrash()
        return trashDao.insertTrashItem(
            TrashItem(
                itemType = itemType,
                title = title,
                subtitle = subtitle,
                dataJson = dataJson,
                deletedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteTrashItem(item: TrashItem) {
        trashDao.deleteTrashItem(item)
    }

    suspend fun emptyTrash() {
        trashDao.emptyTrash()
    }

    suspend fun restoreTrashItem(item: TrashItem): Boolean {
        return try {
            when (item.itemType) {
                "Exercise" -> {
                    val json = JSONObject(item.dataJson)
                    val ex = Exercise(
                        name = json.getString("name"),
                        muscleGroup = json.getString("muscleGroup"),
                        equipment = json.optString("equipment", "Other"),
                        description = json.optString("description", ""),
                        isCompound = json.optBoolean("isCompound", false),
                        isCustom = json.optBoolean("isCustom", true)
                    )
                    exerciseDao.insertExercise(ex)
                    trashDao.deleteTrashItem(item)
                    true
                }
                "Food Log" -> {
                    val json = JSONObject(item.dataJson)
                    val log = FoodLog(
                        foodItemId = json.optLong("foodItemId", 0L),
                        foodName = json.getString("foodName"),
                        quantity = json.optDouble("quantity", 1.0),
                        servingSize = json.optDouble("servingSize", 100.0),
                        totalCalories = json.optDouble("totalCalories", 0.0),
                        totalProtein = json.optDouble("totalProtein", 0.0),
                        totalCarbs = json.optDouble("totalCarbs", 0.0),
                        totalFat = json.optDouble("totalFat", 0.0),
                        totalFiber = json.optDouble("totalFiber", 0.0),
                        mealType = json.optString("mealType", "Snacks"),
                        date = json.optString("date", ""),
                        time = json.optString("time", "")
                    )
                    foodLogDao.insertLog(log)
                    trashDao.deleteTrashItem(item)
                    true
                }
                "Meal Plan" -> {
                    val arr = JSONArray(item.dataJson)
                    for (i in 0 until arr.length()) {
                        val json = arr.getJSONObject(i)
                        val log = FoodLog(
                            foodItemId = json.optLong("foodItemId", 0L),
                            foodName = json.getString("foodName"),
                            quantity = json.optDouble("quantity", 1.0),
                            servingSize = json.optDouble("servingSize", 100.0),
                            totalCalories = json.optDouble("totalCalories", 0.0),
                            totalProtein = json.optDouble("totalProtein", 0.0),
                            totalCarbs = json.optDouble("totalCarbs", 0.0),
                            totalFat = json.optDouble("totalFat", 0.0),
                            totalFiber = json.optDouble("totalFiber", 0.0),
                            mealType = json.optString("mealType", "Snacks"),
                            date = json.optString("date", ""),
                            time = json.optString("time", "")
                        )
                        foodLogDao.insertLog(log)
                    }
                    trashDao.deleteTrashItem(item)
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    // ===== Reset Exercises to Default =====
    suspend fun resetExercisesToDefault(): Int {
        val defaultList = ExerciseDatabase.getDefaultExercises()
        // Delete all non-custom exercises, keeping custom exercises intact
        exerciseDao.deleteNonCustomExercises()
        // Re-insert standard default exercises
        exerciseDao.insertAllExercises(defaultList)
        return defaultList.size
    }

    // ===== Water Intake =====
    private val waterLogDao = database.waterLogDao()
    fun getWaterLogForDate(date: String): Flow<WaterLog?> = waterLogDao.getWaterLogForDate(date)
    suspend fun getWaterAmountForDateSync(date: String): Int = waterLogDao.getWaterAmountForDateSync(date) ?: 0
    suspend fun logWater(date: String, deltaMl: Int): Int {
        val current = waterLogDao.getWaterAmountForDateSync(date) ?: 0
        val newAmount = (current + deltaMl).coerceAtLeast(0)
        waterLogDao.insertOrUpdateWaterLog(WaterLog(date = date, amountMl = newAmount))
        return newAmount
    }
    suspend fun setWaterIntake(date: String, amountMl: Int) {
        waterLogDao.insertOrUpdateWaterLog(WaterLog(date = date, amountMl = amountMl.coerceAtLeast(0)))
    }

    // ===== Stopwatch Records =====
    private val stopwatchRecordDao = database.stopwatchRecordDao()
    fun getAllStopwatchRecords(): Flow<List<StopwatchRecord>> = stopwatchRecordDao.getAllRecords()
    fun getStopwatchRecordsByTitle(title: String): Flow<List<StopwatchRecord>> = stopwatchRecordDao.getRecordsByTitle(title)
    suspend fun insertStopwatchRecord(record: StopwatchRecord): Long = stopwatchRecordDao.insertRecord(record)
    suspend fun deleteStopwatchRecord(record: StopwatchRecord) = stopwatchRecordDao.deleteRecord(record)
    suspend fun deleteStopwatchRecordById(id: Long) = stopwatchRecordDao.deleteRecordById(id)

    // ===== Data Backup & Migration =====
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "GetFit")
        root.put("exportedAtMillis", System.currentTimeMillis())

        // 1. User Profile
        val profile = userProfileDao.getUserProfileSync()
        if (profile != null) {
            val pObj = JSONObject().apply {
                put("name", profile.name)
                put("age", profile.age)
                put("weight", profile.weight)
                put("height", profile.height)
                put("gender", profile.gender)
                put("activityLevel", profile.activityLevel)
                put("goal", profile.goal)
                put("dailyCalorieTarget", profile.dailyCalorieTarget)
                put("dailyProteinTarget", profile.dailyProteinTarget)
                put("dailyCarbsTarget", profile.dailyCarbsTarget)
                put("dailyFatTarget", profile.dailyFatTarget)
                put("dailyWaterTarget", profile.dailyWaterTarget)
                put("workoutPlan", profile.workoutPlan)
            }
            root.put("profile", pObj)
        }

        // 2. Exercises (built-in + custom)
        val exercises = exerciseDao.getAllExercisesSync()
        val exArr = JSONArray()
        exercises.forEach { ex ->
            exArr.put(JSONObject().apply {
                put("id", ex.id)
                put("name", ex.name)
                put("muscleGroup", ex.muscleGroup)
                put("equipment", ex.equipment)
                put("description", ex.description)
                put("isCompound", ex.isCompound)
                put("isCustom", ex.isCustom)
            })
        }
        root.put("exercises", exArr)

        // 3. Workout Logs (3+ months full progress!)
        val workoutLogs = workoutLogDao.getAllLogsSync()
        val wArr = JSONArray()
        workoutLogs.forEach { wl ->
            wArr.put(JSONObject().apply {
                put("id", wl.id)
                put("exerciseId", wl.exerciseId)
                put("exerciseName", wl.exerciseName)
                put("setNumber", wl.setNumber)
                put("reps", wl.reps)
                put("weight", wl.weight)
                put("date", wl.date)
                put("workoutPlanDay", wl.workoutPlanDay)
                put("notes", wl.notes)
                put("isPersonalBest", wl.isPersonalBest)
            })
        }
        root.put("workoutLogs", wArr)

        // 4. Food Logs
        val foodLogs = foodLogDao.getAllFoodLogsSync()
        val fArr = JSONArray()
        foodLogs.forEach { fl ->
            fArr.put(JSONObject().apply {
                put("id", fl.id)
                put("foodItemId", fl.foodItemId)
                put("foodName", fl.foodName)
                put("quantity", fl.quantity)
                put("servingSize", fl.servingSize)
                put("totalCalories", fl.totalCalories)
                put("totalProtein", fl.totalProtein)
                put("totalCarbs", fl.totalCarbs)
                put("totalFat", fl.totalFat)
                put("totalFiber", fl.totalFiber)
                put("mealType", fl.mealType)
                put("date", fl.date)
                put("time", fl.time)
            })
        }
        root.put("foodLogs", fArr)

        // 5. Water Logs
        val waterLogs = waterLogDao.getAllWaterLogsSync()
        val waterArr = JSONArray()
        waterLogs.forEach { wtl ->
            waterArr.put(JSONObject().apply {
                put("id", wtl.id)
                put("date", wtl.date)
                put("amountMl", wtl.amountMl)
            })
        }
        root.put("waterLogs", waterArr)

        // 6. Weight Logs
        val weightLogs = weightLogDao.getAllWeightLogsSync()
        val wtArr = JSONArray()
        weightLogs.forEach { wtl ->
            wtArr.put(JSONObject().apply {
                put("id", wtl.id)
                put("weightKg", wtl.weightKg)
                put("date", wtl.date)
                put("notes", wtl.notes)
            })
        }
        root.put("weightLogs", wtArr)

        // 7. Custom Foods
        val customFoods = foodDao.getAllCustomFoodsSync()
        val cfArr = JSONArray()
        customFoods.forEach { cf ->
            cfArr.put(JSONObject().apply {
                put("id", cf.id)
                put("name", cf.name)
                put("category", cf.category)
                put("caloriesPer100g", cf.caloriesPer100g)
                put("proteinPer100g", cf.proteinPer100g)
                put("carbsPer100g", cf.carbsPer100g)
                put("fatPer100g", cf.fatPer100g)
                put("fiberPer100g", cf.fiberPer100g)
                put("defaultServingSize", cf.defaultServingSize)
                put("servingUnit", cf.servingUnit)
                put("isCustom", cf.isCustom)
                put("source", cf.source)
            })
        }
        root.put("customFoods", cfArr)

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): String = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        var restoredWorkouts = 0
        var restoredFoodLogs = 0
        var restoredExercises = 0
        var restoredWaterLogs = 0
        var restoredCustomFoods = 0
        var latestDate: String? = null

        // 1. User Profile
        if (root.has("profile")) {
            val pObj = root.getJSONObject("profile")
            val p = UserProfile(
                id = 1,
                name = pObj.optString("name", "User"),
                age = pObj.optInt("age", 25),
                weight = pObj.optDouble("weight", 70.0),
                height = pObj.optDouble("height", 170.0),
                gender = pObj.optString("gender", "Male"),
                activityLevel = pObj.optString("activityLevel", "Moderate"),
                goal = pObj.optString("goal", "Muscle Building"),
                dailyCalorieTarget = pObj.optInt("dailyCalorieTarget", 2500),
                dailyProteinTarget = pObj.optInt("dailyProteinTarget", 150),
                dailyCarbsTarget = pObj.optInt("dailyCarbsTarget", 300),
                dailyFatTarget = pObj.optInt("dailyFatTarget", 70),
                dailyWaterTarget = pObj.optInt("dailyWaterTarget", 3000),
                workoutPlan = pObj.optString("workoutPlan", "PPL")
            )
            userProfileDao.insertOrUpdate(p)
        }

        // 2. Custom Foods (Import BEFORE food logs so food logs can reference their IDs)
        if (root.has("customFoods")) {
            val cfArr = root.getJSONArray("customFoods")
            val list = mutableListOf<FoodItem>()
            val existingFoods = foodDao.getAllFoodsSync().associateBy { it.name.lowercase(Locale.ROOT) }
            for (i in 0 until cfArr.length()) {
                val cObj = cfArr.getJSONObject(i)
                val foodName = cObj.getString("name")
                val existing = existingFoods[foodName.lowercase(Locale.ROOT)]
                val targetId = existing?.id ?: cObj.optLong("id", 0L)
                list.add(FoodItem(
                    id = targetId,
                    name = foodName,
                    category = cObj.optString("category", "Custom"),
                    caloriesPer100g = cObj.optDouble("caloriesPer100g", 0.0),
                    proteinPer100g = cObj.optDouble("proteinPer100g", 0.0),
                    carbsPer100g = cObj.optDouble("carbsPer100g", 0.0),
                    fatPer100g = cObj.optDouble("fatPer100g", 0.0),
                    fiberPer100g = cObj.optDouble("fiberPer100g", 0.0),
                    defaultServingSize = cObj.optDouble("defaultServingSize", cObj.optDouble("defaultServingGrams", 100.0)),
                    servingUnit = cObj.optString("servingUnit", "g"),
                    isCustom = true,
                    source = cObj.optString("source", "Custom")
                ))
            }
            if (list.isNotEmpty()) {
                foodDao.insertAllFoods(list)
                restoredCustomFoods = list.size
            }
        }

        // 3. Exercises
        if (root.has("exercises")) {
            val exArr = root.getJSONArray("exercises")
            val list = mutableListOf<Exercise>()
            val existingExercises = exerciseDao.getAllExercisesSync().associateBy { it.name.lowercase(Locale.ROOT) }
            for (i in 0 until exArr.length()) {
                val exObj = exArr.getJSONObject(i)
                val exName = exObj.getString("name")
                val existing = existingExercises[exName.lowercase(Locale.ROOT)]
                val targetId = existing?.id ?: exObj.optLong("id", 0L)
                list.add(Exercise(
                    id = targetId,
                    name = exName,
                    muscleGroup = exObj.optString("muscleGroup", "Chest"),
                    equipment = exObj.optString("equipment", "Barbell"),
                    description = exObj.optString("description", ""),
                    isCompound = exObj.optBoolean("isCompound", true),
                    isCustom = exObj.optBoolean("isCustom", false)
                ))
            }
            if (list.isNotEmpty()) {
                exerciseDao.insertAllExercises(list)
                restoredExercises = list.size
            }
        }

        // 4. Workout Logs
        if (root.has("workoutLogs")) {
            val wArr = root.getJSONArray("workoutLogs")
            val existingLogs = workoutLogDao.getAllLogsSync()
            val list = mutableListOf<WorkoutLog>()
            for (i in 0 until wArr.length()) {
                val wObj = wArr.getJSONObject(i)
                val date = wObj.getString("date")
                if (latestDate == null || date > latestDate!!) latestDate = date
                val exName = wObj.getString("exerciseName")
                val setNum = wObj.optInt("setNumber", 1)
                val reps = wObj.optInt("reps", 8)
                val weight = wObj.optDouble("weight", 0.0)
                val day = wObj.optString("workoutPlanDay", "Push")

                val isDuplicate = existingLogs.any {
                    it.date == date && it.exerciseName.equals(exName, ignoreCase = true) &&
                    it.setNumber == setNum && it.reps == reps && it.weight == weight &&
                    it.workoutPlanDay == day
                }
                if (!isDuplicate) {
                    list.add(WorkoutLog(
                        id = 0L,
                        exerciseId = wObj.optLong("exerciseId", 0L),
                        exerciseName = exName,
                        setNumber = setNum,
                        reps = reps,
                        weight = weight,
                        date = date,
                        workoutPlanDay = day,
                        notes = wObj.optString("notes", ""),
                        isPersonalBest = wObj.optBoolean("isPersonalBest", false)
                    ))
                }
            }
            if (list.isNotEmpty()) {
                workoutLogDao.insertAllLogs(list)
                restoredWorkouts = list.size
            }
        }

        // 5. Food Logs
        if (root.has("foodLogs")) {
            val fArr = root.getJSONArray("foodLogs")
            val existingFoodLogs = foodLogDao.getAllFoodLogsSync()
            val list = mutableListOf<FoodLog>()
            for (i in 0 until fArr.length()) {
                val fObj = fArr.getJSONObject(i)
                val date = fObj.getString("date")
                if (latestDate == null || date > latestDate!!) latestDate = date
                val foodName = fObj.getString("foodName")
                val mealType = fObj.optString("mealType", "Snacks")
                val time = fObj.optString("time", "12:00")
                val qty = fObj.optDouble("quantity", 1.0)
                val cals = fObj.optDouble("totalCalories", 0.0)

                val isDuplicate = existingFoodLogs.any {
                    it.date == date && it.foodName.equals(foodName, ignoreCase = true) &&
                    it.mealType.equals(mealType, ignoreCase = true) &&
                    it.time == time
                }
                if (!isDuplicate) {
                    list.add(FoodLog(
                        id = 0L,
                        foodItemId = fObj.optLong("foodItemId", 0L),
                        foodName = foodName,
                        quantity = qty,
                        servingSize = fObj.optDouble("servingSize", 100.0),
                        totalCalories = cals,
                        totalProtein = fObj.optDouble("totalProtein", 0.0),
                        totalCarbs = fObj.optDouble("totalCarbs", 0.0),
                        totalFat = fObj.optDouble("totalFat", 0.0),
                        totalFiber = fObj.optDouble("totalFiber", 0.0),
                        mealType = mealType,
                        date = date,
                        time = time
                    ))
                }
            }
            if (list.isNotEmpty()) {
                foodLogDao.insertAllLogs(list)
                restoredFoodLogs = list.size
            }
        }

        // 6. Water Logs (collision-free update or insert)
        if (root.has("waterLogs")) {
            val waterArr = root.getJSONArray("waterLogs")
            for (i in 0 until waterArr.length()) {
                val wObj = waterArr.getJSONObject(i)
                val date = wObj.optString("date", "")
                val amount = wObj.optInt("amountMl", 0)
                if (date.isNotBlank()) {
                    if (latestDate == null || date > latestDate!!) latestDate = date
                    val existing = waterLogDao.getWaterAmountForDateSync(date)
                    if (existing != null) {
                        waterLogDao.updateWaterAmount(date, maxOf(existing, amount))
                    } else {
                        waterLogDao.insertOrUpdateWaterLog(WaterLog(date = date, amountMl = amount))
                    }
                    restoredWaterLogs++
                }
            }
        }

        // 7. Weight Logs
        if (root.has("weightLogs")) {
            val wtArr = root.getJSONArray("weightLogs")
            val list = mutableListOf<WeightLog>()
            for (i in 0 until wtArr.length()) {
                val wObj = wtArr.getJSONObject(i)
                val date = wObj.optString("date", "")
                if (date.isNotBlank()) {
                    list.add(WeightLog(
                        id = 0L,
                        weightKg = wObj.optDouble("weightKg", wObj.optDouble("weight", 70.0)),
                        date = date,
                        notes = wObj.optString("notes", "")
                    ))
                }
            }
            if (list.isNotEmpty()) {
                weightLogDao.insertAllWeightLogs(list)
            }
        }

        // Automatically switch date to the restored date so the user immediately sees their logs!
        if (latestDate != null) {
            AppDateManager.setDate(latestDate!!)
        }

        "Restored: $restoredWorkouts workouts, $restoredFoodLogs meals, $restoredWaterLogs water logs."
    }

    /**
     * Clears all user-specific tables and custom items while preserving built-in foods and exercises.
     * Guarantees strict isolation between Guest mode and logged-in accounts.
     */
    suspend fun clearUserScopedData() = withContext(Dispatchers.IO) {
        database.runInTransaction {
            val db = database.openHelper.writableDatabase
            db.execSQL("DELETE FROM food_logs")
            db.execSQL("DELETE FROM workout_logs")
            db.execSQL("DELETE FROM weight_logs")
            db.execSQL("DELETE FROM water_logs")
            db.execSQL("DELETE FROM meal_schedules")
            db.execSQL("DELETE FROM trash_items")
            db.execSQL("DELETE FROM stopwatch_records")
            db.execSQL("DELETE FROM user_profile")
            db.execSQL("DELETE FROM food_items WHERE isCustom = 1")
            db.execSQL("DELETE FROM exercises WHERE isCustom = 1")
        }
        userProfileDao.insertOrUpdate(UserProfile())
        database.invalidationTracker.refreshVersionsAsync()
    }
}

package com.getfit.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.getfit.data.AppDatabase
import com.getfit.data.MealPlanSuggestions
import com.getfit.data.WorkoutPlans
import com.getfit.data.entity.FoodLog
import com.getfit.data.entity.UserProfile
import com.getfit.data.entity.WaterLog
import com.getfit.data.entity.WeightLog
import com.getfit.data.entity.WorkoutLog
import com.getfit.data.repository.GetFitRepository
import com.getfit.ui.util.StreakManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DayStreakStatus(
    val dayLabel: String,
    val dayNumber: String,
    val dateStr: String,
    val isCompleted: Boolean,
    val isToday: Boolean
)

data class StreakData(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val isLoggedToday: Boolean = false,
    val weeklyDayStatus: List<DayStreakStatus> = emptyList(),
    val monthlyDaysCompleted: Int = 0,
    val monthlyDaysTotal: Int = 30
)

data class DayAdherence(
    val dayLabel: String,
    val dateStr: String,
    val calories: Double,
    val targetCalories: Double,
    val isMet: Boolean,
    val isToday: Boolean
)

data class MealStatus(
    val mealType: String,   // "Breakfast", "Lunch", "Dinner"
    val scheduledTime: String,
    val isLogged: Boolean,
    val loggedSummary: String = ""
)

data class DashboardUiState(
    val userProfile: UserProfile = UserProfile(),
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val todayMealLogs: List<FoodLog> = emptyList(),
    val todayWorkoutLogs: List<WorkoutLog> = emptyList(),
    val todayWorkoutTitle: String = "Push Day - Chest/Shoulders/Triceps",
    val todayWorkoutSubtitle: String = "6 exercises planned",
    val todayWorkoutExercises: List<String> = emptyList(),
    val nextMealTitle: String = "Post-Workout Snack",
    val nextMealTime: String = "17:30",
    val nextMealSuggestion: String = "Whey Protein + 1 Banana (35g Protein)",
    val mealStatusList: List<MealStatus> = emptyList(),
    val motivationalQuote: String = "Consistency is the key to building real muscle. Show up today!",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val isLoggedToday: Boolean = false,
    val weeklyDayStatus: List<DayStreakStatus> = emptyList(),
    val monthlyDaysCompleted: Int = 0,
    val monthlyDaysTotal: Int = 30,
    val monthlyBadgeUnlocked: Boolean = false,
    val monthlyBadgeName: String = "Monthly Consistency",
    val todayWaterMl: Int = 0,
    val dailyWaterTarget: Int = 3000,
    val past7DaysAdherence: List<DayAdherence> = emptyList(),
    val latestWeightKg: Double = 0.0,
    val weightDeltaKg: Double = 0.0,
    val hasWeightLog: Boolean = false,
    val allActiveDates: Set<String> = emptySet(),
    val streakDates: Set<String> = emptySet()
)

private data class DashboardExtraTotals(
    val waterLog: WaterLog?,
    val weightLogs: List<WeightLog>,
    val rangeFoodLogs: List<FoodLog>
)

private data class DashboardNutritionTotals(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

private data class DashboardActivityTotals(
    val profile: UserProfile,
    val mealLogs: List<FoodLog>,
    val workoutLogs: List<WorkoutLog>
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GetFitRepository(AppDatabase.getDatabase(application))

    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val sdfDayLetter = SimpleDateFormat("E", Locale.getDefault())

    private fun getTodayDate(): String = sdfDate.format(Date())
    private fun getCurrentTime(): String = sdfTime.format(Date())

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private fun parseTimeToMinutes(timeStr: String): Int? {
        val clean = timeStr.trim().uppercase()
        val isPm = clean.endsWith("PM")
        val isAm = clean.endsWith("AM")
        val rawTime = clean.removeSuffix("AM").removeSuffix("PM").trim()
        val parts = rawTime.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val hour24 = when {
            isPm && h < 12 -> h + 12
            isAm && h == 12 -> 0
            else -> h
        }
        return hour24 * 60 + m
    }

    private val quotes = listOf(
        "Consistency is the key to building real muscle. Show up today!",
        "The only bad workout is the one that didn't happen.",
        "Eat for the body you want, not the body you have.",
        "Small daily improvements over time lead to stunning results.",
        "Discipline is choosing between what you want now and what you want most."
    )

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val today = getTodayDate()
        val randomQuote = quotes[Random().nextInt(quotes.size)]
        val cal7 = Calendar.getInstance()
        cal7.add(Calendar.DAY_OF_YEAR, -6)
        val sevenDaysAgo = sdfDate.format(cal7.time)

        viewModelScope.launch {
            val nutritionFlow = combine(
                repository.getTotalCaloriesForDate(today),
                repository.getTotalProteinForDate(today),
                repository.getTotalCarbsForDate(today),
                repository.getTotalFatForDate(today)
            ) { cal, pro, carb, fat ->
                DashboardNutritionTotals(cal ?: 0.0, pro ?: 0.0, carb ?: 0.0, fat ?: 0.0)
            }

            val activityFlow = combine(
                repository.getUserProfile(),
                repository.getLogsForDate(today),
                repository.getWorkoutLogsForDate(today)
            ) { profile, mealLogs, workoutLogs ->
                DashboardActivityTotals(
                    profile ?: UserProfile(),
                    mealLogs,
                    workoutLogs
                )
            }

            val extraFlow = combine(
                repository.getWaterLogForDate(today),
                repository.getAllWeightLogs(),
                repository.getFoodLogsForDateRange(sevenDaysAgo, today),
                StreakManager.streakUpdates
            ) { waterLog, weightLogs, rangeFoodLogs, _ ->
                DashboardExtraTotals(waterLog, weightLogs, rangeFoodLogs)
            }

            combine(
                nutritionFlow,
                activityFlow,
                repository.getAllMealSchedules(),
                repository.getAllActiveDates(),
                extraFlow
            ) { nutrition, activity, mealSchedules, activeDates, extras ->
                val user = activity.profile
                val streak = calculateStreakData(activeDates)

                // 7-Day Adherence Sparkline
                val daySdf = sdfDayLetter
                val rangeByDate = extras.rangeFoodLogs.groupBy { it.date }
                val past7Days = mutableListOf<DayAdherence>()
                val sparkCal = Calendar.getInstance()
                sparkCal.add(Calendar.DAY_OF_YEAR, -6)
                for (i in 0..6) {
                    val dStr = sdfDate.format(sparkCal.time)
                    val label = daySdf.format(sparkCal.time).take(1)
                    val dailyCal = rangeByDate[dStr]?.sumOf { it.totalCalories } ?: 0.0
                    val isMet = dailyCal >= (user.dailyCalorieTarget * 0.85).coerceAtLeast(1000.0)
                    past7Days.add(
                        DayAdherence(
                            dayLabel = label,
                            dateStr = dStr,
                            calories = dailyCal,
                            targetCalories = user.dailyCalorieTarget.toDouble(),
                            isMet = isMet,
                            isToday = dStr == today
                        )
                    )
                    sparkCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                // Bodyweight delta
                val latestWeight = extras.weightLogs.lastOrNull()?.weightKg ?: user.weight
                val prevWeight = extras.weightLogs.dropLast(1).lastOrNull()?.weightKg
                val weightDelta = if (prevWeight != null) latestWeight - prevWeight else 0.0
                val hasWeightLog = extras.weightLogs.isNotEmpty()

                // Water
                val waterMl = extras.waterLog?.amountMl ?: 0
                val waterTarget = if (user.dailyWaterTarget > 0) user.dailyWaterTarget else 3000

                // Monthly Badge
                val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
                val monthlyBadgeName = "$monthName Consistency"
                val monthlyBadgeUnlocked = streak.monthlyDaysCompleted >= 20

                // Determine workout for today based on plan
                val plan = WorkoutPlans.getAllPlans().find { it.name.equals(user.workoutPlan, ignoreCase = true) }
                    ?: WorkoutPlans.getPPLPlan()
                val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
                val dayIndex = (dayOfWeek + 5) % 7 // Monday = 0
                val currentDay = if (dayIndex < plan.days.size) plan.days[dayIndex] else plan.days.first()

                // Dynamically calibrated meal plan from UserProfile
                val personalPlan = MealPlanSuggestions.getPersonalPlan(user)
                val scheduleMap = mealSchedules.associateBy { it.mealType.trim().lowercase() }

                // Map meals with user's customized schedule times
                val scheduledMeals = personalPlan.meals.map { meal ->
                    val custom = scheduleMap[meal.mealType.trim().lowercase()]
                        ?: scheduleMap[when {
                            meal.mealType.contains("Breakfast", ignoreCase = true) -> "breakfast"
                            meal.mealType.contains("Lunch", ignoreCase = true) -> "lunch"
                            meal.mealType.contains("Dinner", ignoreCase = true) -> "dinner"
                            else -> "snacks"
                        }]
                    if (custom != null && custom.scheduledTime.isNotBlank()) {
                        meal.copy(time = custom.scheduledTime)
                    } else {
                        meal
                    }
                }

                // Check which meal categories user has ALREADY logged today
                val loggedMealTypes = activity.mealLogs.map { it.mealType.trim().lowercase() }.toSet()

                // Build Breakfast/Lunch/Dinner status directly from diary logs + user schedule (snacks excluded)
                val defaultTimes = mapOf("Breakfast" to "08:00", "Lunch" to "13:00", "Dinner" to "20:00")
                val mealStatusList = listOf("Breakfast", "Lunch", "Dinner").map { category ->
                    val customTime = scheduleMap[category.lowercase()]?.scheduledTime?.takeIf { it.isNotBlank() }
                        ?: defaultTimes.getValue(category)
                    val logs = activity.mealLogs.filter { it.mealType.contains(category, ignoreCase = true) }
                    val summary = if (logs.isNotEmpty()) {
                        logs.joinToString(", ") { it.foodName.substringBefore(":").trim() }
                    } else ""
                    MealStatus(
                        mealType = category,
                        scheduledTime = customTime,
                        isLogged = logs.isNotEmpty(),
                        loggedSummary = summary
                    )
                }

                val unloggedMeals = scheduledMeals.filter { meal ->
                    val isLogged = loggedMealTypes.contains(meal.mealType.trim().lowercase()) ||
                        (meal.mealType.contains("Breakfast", ignoreCase = true) && loggedMealTypes.contains("breakfast")) ||
                        (meal.mealType.contains("Lunch", ignoreCase = true) && loggedMealTypes.contains("lunch")) ||
                        (meal.mealType.contains("Dinner", ignoreCase = true) && loggedMealTypes.contains("dinner")) ||
                        (meal.mealType.contains("Snack", ignoreCase = true) && loggedMealTypes.contains("snacks"))
                    !isLogged
                }

                val now = Calendar.getInstance()
                val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

                val nextMeal = if (unloggedMeals.isNotEmpty()) {
                    unloggedMeals.firstOrNull { meal ->
                        val mealMinutes = parseTimeToMinutes(meal.time) ?: 0
                        mealMinutes >= currentMinutes - 30
                    } ?: unloggedMeals.first()
                } else null

                val nextMealTitle = nextMeal?.mealType ?: "All Meals Logged!"
                val nextMealTime = nextMeal?.time ?: "Completed"
                val nextMealSuggestion = if (nextMeal != null) {
                    "${nextMeal.foods.joinToString(", ")} (${nextMeal.calories} kcal, ${nextMeal.protein}g protein)"
                } else {
                    "Great work! You've logged all meals for today. Tomorrow starts with Breakfast."
                }

                DashboardUiState(
                    userProfile = user,
                    totalCalories = nutrition.calories,
                    totalProtein = nutrition.protein,
                    totalCarbs = nutrition.carbs,
                    totalFat = nutrition.fat,
                    todayMealLogs = activity.mealLogs,
                    todayWorkoutLogs = activity.workoutLogs,
                    todayWorkoutTitle = currentDay.dayName + " - " + currentDay.muscleGroups,
                    todayWorkoutSubtitle = "${currentDay.exercises.size} exercises prescribed",
                    todayWorkoutExercises = currentDay.exercises.map { it.exerciseName },
                    nextMealTitle = nextMealTitle,
                    nextMealTime = nextMealTime,
                    nextMealSuggestion = nextMealSuggestion,
                    mealStatusList = mealStatusList,
                    motivationalQuote = randomQuote,
                    currentStreak = streak.currentStreak,
                    bestStreak = streak.bestStreak,
                    isLoggedToday = streak.isLoggedToday,
                    weeklyDayStatus = streak.weeklyDayStatus,
                    monthlyDaysCompleted = streak.monthlyDaysCompleted,
                    monthlyDaysTotal = streak.monthlyDaysTotal,
                    monthlyBadgeUnlocked = monthlyBadgeUnlocked,
                    monthlyBadgeName = monthlyBadgeName,
                    todayWaterMl = waterMl,
                    dailyWaterTarget = waterTarget,
                    past7DaysAdherence = past7Days,
                    latestWeightKg = latestWeight,
                    weightDeltaKg = weightDelta,
                    hasWeightLog = hasWeightLog,
                    allActiveDates = activeDates,
                    streakDates = StreakManager.getActiveStreakDates(getApplication())
                )
            }.flowOn(Dispatchers.Default).collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun quickAddWater(amountMl: Int = 250) {
        viewModelScope.launch {
            repository.logWater(getTodayDate(), amountMl)
            StreakManager.recordLogAction(getApplication(), getTodayDate())
        }
    }

    private fun calculateStreakData(activeDates: Set<String>): StreakData {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val todayStr = sdf.format(cal.time)

        val isLoggedToday = activeDates.contains(todayStr)
        val (currentStreak, bestStreak) = StreakManager.getStreak(getApplication(), isLoggedToday)

        val weekCal = Calendar.getInstance()
        weekCal.firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = weekCal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = (dayOfWeek + 5) % 7
        weekCal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
        val weeklyStatus = mutableListOf<DayStreakStatus>()
        for (i in 0..6) {
            val dateStr = sdf.format(weekCal.time)
            val dayNum = SimpleDateFormat("d", Locale.getDefault()).format(weekCal.time)
            val isDayToday = dateStr == todayStr
            val isCompleted = activeDates.contains(dateStr)
            weeklyStatus.add(
                DayStreakStatus(
                    dayLabel = dayLabels[i],
                    dayNumber = dayNum,
                    dateStr = dateStr,
                    isCompleted = isCompleted,
                    isToday = isDayToday
                )
            )
            weekCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val monthCal = Calendar.getInstance()
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(monthCal.time)
        val monthlyDaysCompleted = activeDates.count { it.startsWith(currentYearMonth) }
        val monthlyDaysTotal = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        return StreakData(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            isLoggedToday = isLoggedToday,
            weeklyDayStatus = weeklyStatus,
            monthlyDaysCompleted = monthlyDaysCompleted,
            monthlyDaysTotal = monthlyDaysTotal
        )
    }
}

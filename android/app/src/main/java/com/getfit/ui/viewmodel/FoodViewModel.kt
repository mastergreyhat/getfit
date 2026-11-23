package com.getfit.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.getfit.data.AppDatabase
import com.getfit.data.DietPlan
import com.getfit.data.MealPlanSuggestions
import com.getfit.data.MealSuggestion
import com.getfit.data.entity.FoodItem
import com.getfit.data.entity.FoodLog
import com.getfit.data.entity.UserProfile
import com.getfit.data.entity.WorkoutLog
import com.getfit.data.remote.GeminiNutritionService
import com.getfit.data.remote.OpenFoodFactsService
import com.getfit.data.repository.GetFitRepository
import com.getfit.data.util.FuzzySearchEngine
import com.getfit.ui.util.AppDateManager
import com.getfit.ui.util.StreakManager
import com.getfit.data.sync.CloudSyncManager
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class FoodUiState(
    val selectedDate: String = "",
    val dateDisplayLabel: String = "Today",
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val categories: List<String> = emptyList(),
    val searchResults: List<FoodItem> = emptyList(),
    val didYouMeanSuggestion: String? = null,
    val onlineSearchResults: List<FoodItem> = emptyList(),
    val isSearchingOnline: Boolean = false,
    val onlineSearchError: String? = null,
    val barcodeSearchResult: FoodItem? = null,
    val isSearchingBarcode: Boolean = false,
    val barcodeError: String? = null,
    val targetMealForAdd: String = "Breakfast",
    val todayLogs: List<FoodLog> = emptyList(),
    val todayWaterMl: Int = 0,
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalFiber: Double = 0.0,
    val userProfile: UserProfile = UserProfile(),
    val selectedDietPlanIndex: Int = 0, // 0 = Personal Plan!
    val loggedDates: Set<String> = emptySet(),
    val dateWorkoutLogs: List<WorkoutLog> = emptyList()
)

private data class FoodNutritionTotals(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class FoodViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GetFitRepository(AppDatabase.getDatabase(application))

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    val selectedDate: StateFlow<String> = AppDateManager.selectedDate

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    fun resetToDiary() {
        _currentTab.value = 0
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedDietPlanIndex = MutableStateFlow(0)
    val selectedDietPlanIndex: StateFlow<Int> = _selectedDietPlanIndex.asStateFlow()

    private val _uiState = MutableStateFlow(
        FoodUiState(
            selectedDate = AppDateManager.selectedDate.value,
            dateDisplayLabel = AppDateManager.getDateDisplayLabel(AppDateManager.selectedDate.value)
        )
    )
    val uiState: StateFlow<FoodUiState> = _uiState.asStateFlow()

    private var logsObservationJob: Job? = null

    init {
        loadData()
    }

    private fun getCurrentTime(): String = timeFormatter.format(Date())

    private fun loadData() {
        // Ensure all verified gold-standard staple foods (e.g. Carrots, Tomatoes, Whey 1 Scoop) are seeded/updated in SQLite
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val defaults = com.getfit.data.FoodDatabase.getDefaultFoods()
                val existingDefaults = repository.getAllDefaultFoods().associateBy { it.name }
                val toUpsert = mutableListOf<com.getfit.data.entity.FoodItem>()

                for (food in defaults) {
                    val existing = existingDefaults[food.name]
                    if (existing == null) {
                        toUpsert.add(food)
                    } else if (existing.caloriesPer100g != food.caloriesPer100g ||
                        existing.proteinPer100g != food.proteinPer100g ||
                        existing.carbsPer100g != food.carbsPer100g ||
                        existing.fatPer100g != food.fatPer100g ||
                        existing.servingUnit != food.servingUnit ||
                        existing.defaultServingSize != food.defaultServingSize ||
                        existing.source != food.source
                    ) {
                        toUpsert.add(food.copy(id = existing.id))
                    }
                }

                if (toUpsert.isNotEmpty()) {
                    repository.insertAllFoods(toUpsert)
                }

                // Clean up legacy bad-unit entries or duplicate obsolete entries
                val allExisting = repository.getAllFoodsSync()
                for (item in allExisting) {
                    if (item.servingUnit.equals("scoop", ignoreCase = true) ||
                        item.name.equals("Whey Protein (per scoop)", ignoreCase = true)
                    ) {
                        repository.deleteFood(item)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FoodViewModel", "Error syncing verified foods", e)
            }
        }

        // Categories
        viewModelScope.launch {
            repository.getAllCategories().collect { cats ->
                val list = mutableListOf("All")
                list.addAll(cats)
                _uiState.update { it.copy(categories = list) }
            }
        }

        // Search & Filter Foods with Typo-Tolerant Fuzzy Matching Engine (runs on background thread)
        viewModelScope.launch(Dispatchers.Default) {
            combine(_searchQuery, _selectedCategory, repository.getAllFoods()) { query, category, allFoods ->
                Triple(query, category, allFoods)
            }.catch { e ->
                android.util.Log.e("FoodViewModel", "Error in searchFoods flow", e)
            }.collect { (query, category, allFoods) ->
                val trimmed = query.trim()
                if (trimmed.isNotBlank()) {
                    val filteredByCategory = if (category != "All") {
                        allFoods.filter { it.category.equals(category, ignoreCase = true) }
                    } else {
                        allFoods
                    }
                    val matched = FuzzySearchEngine.search(trimmed, filteredByCategory)
                    val suggestion = FuzzySearchEngine.getDidYouMeanSuggestion(trimmed, filteredByCategory)
                    _uiState.update { it.copy(searchResults = matched, didYouMeanSuggestion = suggestion) }
                } else if (category != "All") {
                    val filtered = allFoods.filter { it.category.equals(category, ignoreCase = true) }
                    _uiState.update { it.copy(searchResults = filtered, didYouMeanSuggestion = null) }
                } else {
                    _uiState.update { it.copy(searchResults = allFoods, didYouMeanSuggestion = null) }
                }
            }
        }

        // Observe AppDateManager.selectedDate changes and observe logs/totals for that date
        viewModelScope.launch {
            AppDateManager.selectedDate.collect { date ->
                observeDateLogs(date)
            }
        }

        // Observe dates with logged food or training for the interactive Diary calendar
        viewModelScope.launch {
            repository.getAllActiveDates().collect { dates ->
                _uiState.update { it.copy(loggedDates = dates) }
            }
        }
    }

    private data class DateLogsAndDetails(
        val foodLogs: List<FoodLog>,
        val profile: UserProfile,
        val waterMl: Int,
        val workoutLogs: List<WorkoutLog>
    )

    private fun observeDateLogs(date: String) {
        logsObservationJob?.cancel()
        logsObservationJob = viewModelScope.launch {
            val label = AppDateManager.getDateDisplayLabel(date)

            val nutritionFlow = combine(
                repository.getTotalCaloriesForDate(date),
                repository.getTotalProteinForDate(date),
                repository.getTotalCarbsForDate(date),
                repository.getTotalFatForDate(date),
                repository.getTotalFiberForDate(date)
            ) { cal, pro, carb, fat, fib ->
                FoodNutritionTotals(
                    calories = cal ?: 0.0,
                    protein = pro ?: 0.0,
                    carbs = carb ?: 0.0,
                    fat = fat ?: 0.0,
                    fiber = fib ?: 0.0
                )
            }

            val logsAndProfile = combine(
                repository.getLogsForDate(date),
                repository.getUserProfile(),
                repository.getWaterLogForDate(date),
                repository.getWorkoutLogsForDate(date)
            ) { logs, profile, waterLog, workoutLogs ->
                DateLogsAndDetails(logs, profile ?: UserProfile(), waterLog?.amountMl ?: 0, workoutLogs)
            }

            combine(nutritionFlow, logsAndProfile) { nutrition, details ->
                _uiState.update { current ->
                    current.copy(
                        selectedDate = date,
                        dateDisplayLabel = label,
                        todayLogs = details.foodLogs,
                        todayWaterMl = details.waterMl,
                        dateWorkoutLogs = details.workoutLogs,
                        totalCalories = nutrition.calories,
                        totalProtein = nutrition.protein,
                        totalCarbs = nutrition.carbs,
                        totalFat = nutrition.fat,
                        totalFiber = nutrition.fiber,
                        userProfile = details.profile
                    )
                }
            }.collect()
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

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            clearOnlineResults()
        }
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setDietPlanIndex(index: Int) {
        _selectedDietPlanIndex.value = index
        _uiState.update { it.copy(selectedDietPlanIndex = index) }
    }

    private var searchOnlineJob: Job? = null

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    fun searchOnline(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        android.util.Log.d("FoodViewModel", "searchOnline initiated for '$trimmed'")

        searchOnlineJob?.cancel()
        searchOnlineJob = viewModelScope.launch {
            if (!isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        isSearchingOnline = false,
                        onlineSearchResults = emptyList(),
                        onlineSearchError = "No Internet Connection"
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isSearchingOnline = true, onlineSearchError = null) }
            try {
                val results = GeminiNutritionService.searchFoods(getApplication(), trimmed)
                android.util.Log.d("FoodViewModel", "searchOnline completed, found ${results.size} items")
                if (results.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isSearchingOnline = false,
                            onlineSearchResults = emptyList(),
                            onlineSearchError = "No nutritional data found for '$trimmed'"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSearchingOnline = false,
                            onlineSearchResults = results,
                            onlineSearchError = null
                        )
                    }
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isSearchingOnline = false,
                        onlineSearchError = if (!isNetworkAvailable()) "No Internet Connection" else "Search failed: ${t.localizedMessage ?: "Network error"}"
                    )
                }
            }
        }
    }

    fun searchBarcode(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingBarcode = true, barcodeError = null, barcodeSearchResult = null) }
            try {
                val result = OpenFoodFactsService.getProductByBarcode(trimmed)
                if (result != null) {
                    _uiState.update {
                        it.copy(
                            isSearchingBarcode = false,
                            barcodeSearchResult = result,
                            barcodeError = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSearchingBarcode = false,
                            barcodeError = "No product found for barcode: $trimmed"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearchingBarcode = false,
                        barcodeError = "Barcode lookup failed: ${e.localizedMessage ?: "Network error"}"
                    )
                }
            }
        }
    }

    fun clearBarcodeResult() {
        _uiState.update { it.copy(barcodeSearchResult = null, barcodeError = null) }
    }

    fun clearOnlineResults() {
        _uiState.update { it.copy(onlineSearchResults = emptyList(), onlineSearchError = null) }
    }

    fun logFood(
        foodItem: FoodItem,
        quantity: Double,
        mealType: String
    ) {
        viewModelScope.launch {
            // Auto-cache: If foodItem is from online/AI search (id == 0L), permanently save to local DB
            val effectiveFoodId = if (foodItem.id == 0L) {
                try {
                    repository.insertFood(foodItem.copy(id = 0, isCustom = true))
                } catch (e: Exception) {
                    0L
                }
            } else {
                foodItem.id
            }

            val isPiece = foodItem.servingUnit.equals("piece", ignoreCase = true) || foodItem.servingUnit.equals("scoop", ignoreCase = true)
            val scale = if (isPiece) {
                quantity
            } else {
                (quantity * foodItem.defaultServingSize) / 100.0
            }
            val totalCalories = foodItem.caloriesPer100g * scale
            val totalProtein = foodItem.proteinPer100g * scale
            val totalCarbs = foodItem.carbsPer100g * scale
            val totalFat = foodItem.fatPer100g * scale
            val totalFiber = foodItem.fiberPer100g * scale

            val foodLog = FoodLog(
                foodItemId = effectiveFoodId,
                foodName = foodItem.name,
                quantity = quantity,
                servingSize = if (isPiece) 1.0 else foodItem.defaultServingSize,
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat,
                totalFiber = totalFiber,
                mealType = mealType,
                date = AppDateManager.selectedDate.value,
                time = getCurrentTime()
            )
            repository.insertFoodLog(foodLog)
            StreakManager.recordLogAction(getApplication(), foodLog.date)
            CloudSyncManager.triggerSilentUpload(getApplication(), repository)
        }
    }

    fun quickAddCalories(
        name: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        mealType: String
    ) {
        viewModelScope.launch {
            val foodLog = FoodLog(
                foodItemId = 0L,
                foodName = if (name.isNotBlank()) name else "Quick Add ($mealType)",
                quantity = 1.0,
                servingSize = 1.0,
                totalCalories = calories,
                totalProtein = protein,
                totalCarbs = carbs,
                totalFat = fat,
                totalFiber = 0.0,
                mealType = mealType,
                date = AppDateManager.selectedDate.value,
                time = getCurrentTime()
            )
            repository.insertFoodLog(foodLog)
            StreakManager.recordLogAction(getApplication(), foodLog.date)
            CloudSyncManager.triggerSilentUpload(getApplication(), repository)
        }
    }

    fun logMealSuggestion(suggestion: MealSuggestion, targetMealType: String? = null) {
        viewModelScope.launch {
            val raw = targetMealType ?: suggestion.mealType
            val lower = raw.lowercase().trim()
            val effectiveMealType = when {
                lower.contains("breakfast") || lower.contains("morning") -> "Breakfast"
                lower.contains("lunch") -> "Lunch"
                lower.contains("dinner") || lower.contains("night") || lower.contains("bed") -> "Dinner"
                else -> "Snacks"
            }

            val foodLog = FoodLog(
                foodItemId = 0L,
                foodName = suggestion.mealType + ": " + suggestion.foods.joinToString(", "),
                quantity = 1.0,
                servingSize = 1.0,
                totalCalories = suggestion.calories.toDouble(),
                totalProtein = suggestion.protein.toDouble(),
                totalCarbs = suggestion.carbs.toDouble(),
                totalFat = suggestion.fat.toDouble(),
                totalFiber = 0.0,
                mealType = effectiveMealType,
                date = AppDateManager.selectedDate.value,
                time = getCurrentTime()
            )
            repository.insertFoodLog(foodLog)
            StreakManager.recordLogAction(getApplication(), foodLog.date)
            CloudSyncManager.triggerSilentUpload(getApplication(), repository)
        }
    }

    fun setTargetMealForAdd(mealType: String) {
        _uiState.update { it.copy(targetMealForAdd = mealType) }
    }

    fun saveAndLogOnlineFood(
        foodItem: FoodItem,
        quantity: Double,
        mealType: String
    ) {
        viewModelScope.launch {
            val savedId = repository.insertFood(foodItem.copy(isCustom = true))
            val savedFood = foodItem.copy(id = savedId, isCustom = true)
            logFood(savedFood, quantity, mealType)
            clearOnlineResults()
        }
    }

    private fun foodLogToJson(f: FoodLog): String {
        val obj = JSONObject()
        obj.put("foodItemId", f.foodItemId)
        obj.put("foodName", f.foodName)
        obj.put("quantity", f.quantity)
        obj.put("servingSize", f.servingSize)
        obj.put("totalCalories", f.totalCalories)
        obj.put("totalProtein", f.totalProtein)
        obj.put("totalCarbs", f.totalCarbs)
        obj.put("totalFat", f.totalFat)
        obj.put("totalFiber", f.totalFiber)
        obj.put("mealType", f.mealType)
        obj.put("date", f.date)
        obj.put("time", f.time)
        return obj.toString()
    }

    fun deleteFood(food: FoodItem) {
        viewModelScope.launch {
            repository.deleteFood(food)
        }
    }

    fun deleteFoodLog(log: FoodLog) {
        viewModelScope.launch {
            repository.moveToTrash(
                itemType = "Food Log",
                title = log.foodName,
                subtitle = "${log.mealType} • ${log.totalCalories.toInt()} kcal • P:${log.totalProtein.toInt()}g C:${log.totalCarbs.toInt()}g F:${log.totalFat.toInt()}g",
                dataJson = foodLogToJson(log)
            )
            repository.deleteFoodLog(log)
            CloudSyncManager.triggerSilentUpload(getApplication(), repository)
        }
    }

    fun addCustomFood(
        name: String,
        category: String,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        fiberPer100g: Double,
        servingSize: Double,
        servingUnit: String
    ) {
        viewModelScope.launch {
            val item = FoodItem(
                name = name,
                category = category,
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g,
                fatPer100g = fatPer100g,
                fiberPer100g = fiberPer100g,
                defaultServingSize = servingSize,
                servingUnit = servingUnit,
                isCustom = true
            )
            repository.insertFood(item)
        }
    }

    fun logWater(deltaMl: Int) {
        viewModelScope.launch {
            repository.logWater(AppDateManager.selectedDate.value, deltaMl)
            StreakManager.recordLogAction(getApplication(), AppDateManager.selectedDate.value)
        }
    }

    fun setWaterIntake(amountMl: Int) {
        viewModelScope.launch {
            repository.setWaterIntake(AppDateManager.selectedDate.value, amountMl)
            StreakManager.recordLogAction(getApplication(), AppDateManager.selectedDate.value)
        }
    }

    fun getYesterdayMealsSummary(onResult: (List<YesterdayMealSummary>) -> Unit) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val curDate = selectedDate.value
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsed = try { sdf.parse(curDate) ?: Date() } catch (_: Exception) { Date() }
            cal.time = parsed
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(cal.time)

            val yesterdayLogs = repository.getLogsForDateSync(yesterdayStr)
            val standardMeals = listOf("Breakfast", "Lunch", "Dinner", "Snacks")
            val grouped = yesterdayLogs.groupBy { it.mealType }
            val summaries = standardMeals.map { meal ->
                val logs = grouped[meal] ?: emptyList()
                YesterdayMealSummary(
                    mealType = meal,
                    itemsCount = logs.size,
                    totalCalories = logs.sumOf { it.totalCalories }.toInt(),
                    logs = logs
                )
            }
            onResult(summaries)
        }
    }

    fun copyYesterdaySelectedMeals(selectedMealTypes: Set<String>, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val curDate = selectedDate.value
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsed = try { sdf.parse(curDate) ?: Date() } catch (_: Exception) { Date() }
            cal.time = parsed
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(cal.time)

            val yesterdayLogs = repository.getLogsForDateSync(yesterdayStr)
            val filteredLogs = yesterdayLogs.filter { it.mealType in selectedMealTypes }
            if (filteredLogs.isEmpty()) {
                onResult(0)
                return@launch
            }

            val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val newLogs = filteredLogs.map { oldLog ->
                oldLog.copy(
                    id = 0L,
                    date = curDate,
                    time = nowTime
                )
            }
            repository.insertAllFoodLogs(newLogs)
            StreakManager.recordLogAction(getApplication(), curDate)
            onResult(newLogs.size)
        }
    }

    fun copyYesterdayMeals(onResult: (Int) -> Unit) {
        copyYesterdaySelectedMeals(setOf("Breakfast", "Lunch", "Dinner", "Snacks"), onResult)
    }
}

data class YesterdayMealSummary(
    val mealType: String,
    val itemsCount: Int,
    val totalCalories: Int,
    val logs: List<FoodLog>
)

package com.getfit.ui.screens

import com.getfit.ui.theme.GetFitTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.getfit.data.DietPlan
import com.getfit.data.MealPlanSuggestions
import com.getfit.data.MealSuggestion
import com.getfit.data.entity.FoodItem
import com.getfit.data.entity.FoodLog
import com.getfit.ui.components.FoodItemCard
import com.getfit.ui.components.MacroSummaryRow
import com.getfit.ui.components.ModifyPastDateDialog
import com.getfit.ui.theme.CaloriesOrange
import com.getfit.ui.theme.CarbsAmber
import com.getfit.ui.theme.FatRed
import com.getfit.ui.theme.ProteinBlue
import com.getfit.ui.theme.WaterBlue
import com.getfit.ui.util.AppDateManager
import com.getfit.ui.viewmodel.FoodUiState
import com.getfit.ui.viewmodel.FoodViewModel
import com.getfit.ui.viewmodel.YesterdayMealSummary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodScreen(
    viewModel: FoodViewModel,
    onNavigateToAddCustomFood: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.currentTab.collectAsState()
    val tabs = listOf("Diary", "Search & Add", "Meal Plans")

    var selectedFoodForDialog by remember { mutableStateOf<FoodItem?>(null) }
    var isSelectedFoodOnline by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var quickAddDefaultMeal by remember { mutableStateOf("Lunch") }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showCopyYesterdayDialog by remember { mutableStateOf(false) }
    var yesterdayMealSummaries by remember { mutableStateOf<List<YesterdayMealSummary>>(emptyList()) }

    var snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf(false) }

    var foodToDelete by remember { mutableStateOf<FoodItem?>(null) }
    var pendingPastAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPastDateConfirmDialog by remember { mutableStateOf(false) }

    val checkPastDateAndRun: (() -> Unit) -> Unit = { action ->
        if (AppDateManager.isPast() && !AppDateManager.isPastDateConfirmed()) {
            pendingPastAction = action
            showPastDateConfirmDialog = true
        } else {
            action()
        }
    }

    if (showPastDateConfirmDialog) {
        ModifyPastDateDialog(
            dateLabel = AppDateManager.getDateDisplayLabel(uiState.selectedDate),
            onContinue = {
                AppDateManager.confirmPastDate()
                val act = pendingPastAction
                pendingPastAction = null
                showPastDateConfirmDialog = false
                act?.invoke()
            },
            onGoToToday = {
                pendingPastAction = null
                showPastDateConfirmDialog = false
                viewModel.goToToday()
            }
        )
    }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar && snackbarMessage.isNotBlank()) {
            val job = launch {
                snackbarHostState.showSnackbar(
                    message = snackbarMessage,
                    duration = SnackbarDuration.Indefinite
                )
            }
            delay(1000L) // Exactly 1 second duration as requested
            snackbarHostState.currentSnackbarData?.dismiss()
            job.cancel()
            showSnackbar = false
        }
    }

    // Dialog for food details / logging
    if (selectedFoodForDialog != null) {
        FoodDetailDialog(
            foodItem = selectedFoodForDialog!!,
            isOnlineFood = isSelectedFoodOnline,
            defaultMealType = uiState.targetMealForAdd,
            onDismiss = { selectedFoodForDialog = null },
            onLogFood = { food, qty, mealType ->
                checkPastDateAndRun {
                    if (isSelectedFoodOnline) {
                        viewModel.saveAndLogOnlineFood(food, qty, mealType)
                    } else {
                        viewModel.logFood(food, qty, mealType)
                    }
                    selectedFoodForDialog = null
                    snackbarMessage = "Logged ${food.name} to $mealType"
                    showSnackbar = true
                }
            }
        )
    }

    // Confirmation Dialog for Deleting Food / Custom Food
    if (foodToDelete != null) {
        AlertDialog(
            onDismissRequest = { foodToDelete = null },
            title = {
                Text(
                    text = "Delete Food Item?",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            },
            text = {
                Text("Are you sure you want to remove '${foodToDelete?.name}' from your database?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val item = foodToDelete
                        if (item != null) {
                            viewModel.deleteFood(item)
                            snackbarMessage = "Removed ${item.name}"
                            showSnackbar = true
                        }
                        foodToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { foodToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Quick Add Raw Calories Dialog (MyFitnessPal style)
    if (showQuickAddDialog) {
        QuickAddDialog(
            initialMealType = quickAddDefaultMeal,
            onDismiss = { showQuickAddDialog = false },
            onConfirm = { name, cals, p, c, f, mealType ->
                checkPastDateAndRun {
                    viewModel.quickAddCalories(name, cals, p, c, f, mealType)
                    showQuickAddDialog = false
                    snackbarMessage = "Quick added ${cals.toInt()} kcal to $mealType"
                    showSnackbar = true
                }
            }
        )
    }

    // Historical Date Picker Dialog
    if (showDatePickerDialog) {
        DiaryDatePickerDialog(
            selectedDate = uiState.selectedDate,
            loggedDates = uiState.loggedDates,
            onDateSelected = { dateStr ->
                viewModel.selectDate(dateStr)
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    // Copy Yesterday's Meals Dialog with Checkboxes for Breakfast, Lunch, Dinner, Snacks
    if (showCopyYesterdayDialog) {
        CopyYesterdayMealsDialog(
            summaries = yesterdayMealSummaries,
            currentDateLabel = uiState.dateDisplayLabel,
            onDismiss = { showCopyYesterdayDialog = false },
            onConfirmCopy = { selectedMeals ->
                checkPastDateAndRun {
                    viewModel.copyYesterdaySelectedMeals(selectedMeals) { count ->
                        snackbarMessage = if (count > 0) {
                            "Logged $count meal items from yesterday (${selectedMeals.joinToString(", ")})!"
                        } else {
                            "No meals selected to copy."
                        }
                        showSnackbar = true
                        showCopyYesterdayDialog = false
                    }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, start = 20.dp, end = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.94f),
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            shadowElevation = 6.dp,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.bodySmall,

                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = onNavigateToAddCustomFood,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Custom Food")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = GetFitTheme.colors.surface1,
                contentColor = GetFitTheme.colors.accent,
                divider = { Divider(color = GetFitTheme.colors.hairline) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        selectedContentColor = GetFitTheme.colors.accent,
                        unselectedContentColor = GetFitTheme.colors.textSecondary,
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> MyFitnessPalDiaryTabContent(
                    uiState = uiState,
                    onPreviousDate = viewModel::previousDate,
                    onNextDate = viewModel::nextDate,
                    onGoToToday = viewModel::goToToday,
                    onOpenCalendarPicker = { showDatePickerDialog = true },
                    onDeleteLog = viewModel::deleteFoodLog,
                    onCopyYesterdayMeals = {
                        checkPastDateAndRun {
                            viewModel.getYesterdayMealsSummary { summaries ->
                                yesterdayMealSummaries = summaries
                                showCopyYesterdayDialog = true
                            }
                        }
                    },
                    onAddFoodClick = { mealType ->
                        checkPastDateAndRun {
                            viewModel.setTargetMealForAdd(mealType)
                            viewModel.selectTab(1)
                        }
                    },
                    onQuickAddClick = { mealType ->
                        checkPastDateAndRun {
                            quickAddDefaultMeal = mealType
                            showQuickAddDialog = true
                        }
                    },
                    onLogWater = { delta ->
                        checkPastDateAndRun {
                            viewModel.logWater(delta)
                            snackbarMessage = if (delta > 0) "Logged +${delta}ml water" else "Updated water intake"
                            showSnackbar = true
                        }
                    }
                )
                1 -> LogFoodTabContent(
                    uiState = uiState,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onCategorySelect = viewModel::onCategorySelect,
                    onSearchOnline = viewModel::searchOnline,
                    onFoodSelected = { food, isOnline ->
                        selectedFoodForDialog = food
                        isSelectedFoodOnline = isOnline
                    },
                    onDeleteFood = { foodToDelete = it }
                )
                2 -> MealPlanTabContent(
                    uiState = uiState,
                    selectedIndex = uiState.selectedDietPlanIndex,
                    onSelectPlanIndex = viewModel::setDietPlanIndex,
                    onLogMealToDiary = { mealSuggestion, targetMealType ->
                        checkPastDateAndRun {
                            viewModel.logMealSuggestion(mealSuggestion, targetMealType)
                            snackbarMessage = "Logged ${mealSuggestion.mealType} to diary"
                            showSnackbar = true
                        }
                    }
                )
            }
        }
    }
}

fun normalizeMealType(rawType: String?): String {
    val lower = (rawType ?: "").lowercase().trim()
    return when {
        lower.contains("breakfast") || lower.contains("morning") -> "Breakfast"
        lower.contains("lunch") -> "Lunch"
        lower.contains("dinner") || lower.contains("night") || lower.contains("bed") -> "Dinner"
        else -> "Snacks"
    }
}

/**
 * Tab 0: MyFitnessPal-Style Daily Food Diary
 */
@Composable
private fun MyFitnessPalDiaryTabContent(
    uiState: FoodUiState,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onGoToToday: () -> Unit,
    onOpenCalendarPicker: () -> Unit,
    onDeleteLog: (FoodLog) -> Unit,
    onCopyYesterdayMeals: () -> Unit,
    onAddFoodClick: (mealType: String) -> Unit,
    onQuickAddClick: (mealType: String) -> Unit,
    onLogWater: (Int) -> Unit
) {
    val calorieTarget = uiState.userProfile.dailyCalorieTarget.toDouble().coerceAtLeast(1000.0)
    val caloriesConsumed = uiState.totalCalories
    val remainingCalories = (calorieTarget - caloriesConsumed).coerceAtLeast(0.0)

    val standardMeals = remember { listOf("Breakfast", "Lunch", "Dinner", "Snacks") }
    val groupedLogs = remember(uiState.todayLogs) {
        uiState.todayLogs.groupBy { normalizeMealType(it.mealType) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Date Navigation Bar
        item(key = "diary_date_nav") {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousDate) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day", modifier = Modifier.size(18.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenCalendarPicker() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = uiState.dateDisplayLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onNextDate,
                        enabled = AppDateManager.canGoNext()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day",
                            modifier = Modifier.size(18.dp),
                            tint = if (AppDateManager.canGoNext()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // 2. MyFitnessPal Signature Calorie Equation Banner Card
        item(key = "diary_calorie_equation") {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "CALORIES REMAINING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${remainingCalories.toInt()}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${calorieTarget.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(text = "−", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${caloriesConsumed.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Food", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(text = "=", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${remainingCalories.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                    // Macro summary
                    MacroSummaryRow(
                        proteinCurrent = uiState.totalProtein.toFloat(),
                        proteinTarget = uiState.userProfile.dailyProteinTarget.toFloat().coerceAtLeast(100f),
                        carbsCurrent = uiState.totalCarbs.toFloat(),
                        carbsTarget = uiState.userProfile.dailyCarbsTarget.toFloat().coerceAtLeast(150f),
                        fatCurrent = uiState.totalFat.toFloat(),
                        fatTarget = uiState.userProfile.dailyFatTarget.toFloat().coerceAtLeast(40f)
                    )
                }
            }
        }

        // 2b. One-Click Copy Yesterday's Meals
        item(key = "diary_copy_yesterday") {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopyYesterdayMeals() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Log Yesterday's Same Plan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Copy yesterday's meals to this date",
                                style = MaterialTheme.typography.bodySmall,

                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Copy Yesterday",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Meal Category Cards (Breakfast, Lunch, Dinner, Snacks)
        standardMeals.forEach { mealType ->
            val logs = groupedLogs[mealType] ?: emptyList()
            val mealCals = logs.sumOf { it.totalCalories }.toInt()
            val mealP = logs.sumOf { it.totalProtein }.toInt()

            item(key = "diary_meal_$mealType") {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = mealType,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$mealCals kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.clickable { onQuickAddClick(mealType) }
                                ) {
                                    Text(
                                        text = "+ Quick",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onAddFoodClick(mealType) }
                                ) {
                                    Text(
                                        text = "+ ADD FOOD",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        // List of logged items in this meal
                        if (logs.isEmpty()) {
                            Text(
                                text = "No food logged for $mealType yet.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                            logs.forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.foodName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${log.totalCalories.toInt()} kcal • P:${log.totalProtein.toInt()}g • C:${log.totalCarbs.toInt()}g • F:${log.totalFat.toInt()}g",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteLog(log) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete item",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Workout / Training Completed Card (Tied to selected date)
        item(key = "diary_workout_section") {
            val workoutLogs = uiState.dateWorkoutLogs
            val groupedWorkouts = remember(workoutLogs) {
                workoutLogs.groupBy { it.exerciseName }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (groupedWorkouts.isNotEmpty()) GetFitTheme.colors.success.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = if (groupedWorkouts.isNotEmpty()) GetFitTheme.colors.success else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Exercise & Training",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (groupedWorkouts.isNotEmpty()) {
                                        "${groupedWorkouts.size} exercises • ${workoutLogs.size} sets completed"
                                    } else {
                                        "No workouts logged for this day"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (groupedWorkouts.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GetFitTheme.colors.success.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, GetFitTheme.colors.success.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = GetFitTheme.colors.success,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Trained",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GetFitTheme.colors.success
                                    )
                                }
                            }
                        }
                    }

                    if (groupedWorkouts.isEmpty()) {
                        Text(
                            text = "No workouts logged for this day.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                        groupedWorkouts.forEach { (exName, sets) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Completed",
                                            tint = GetFitTheme.colors.success,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = exName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    val setSummaries = remember(sets) {
                                        val allSameWeight = sets.all { it.weight == sets.first().weight }
                                        val allSameReps = sets.all { it.reps == sets.first().reps }
                                        if (allSameWeight && allSameReps && sets.isNotEmpty()) {
                                            "${sets.size} sets × ${sets.first().weight}kg (${sets.first().reps} reps)"
                                        } else {
                                            val maxWeight = sets.maxOfOrNull { it.weight } ?: 0.0
                                            "${sets.size} sets • Top ${maxWeight}kg"
                                        }
                                    }
                                    Text(
                                        text = setSummaries,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GetFitTheme.colors.success.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${sets.size} sets ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GetFitTheme.colors.success,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Water Intake Card (Clean SaaS style directly after Snacks & Training)
        item(key = "diary_water_intake") {
            val waterTarget = uiState.userProfile.dailyWaterTarget.coerceAtLeast(1000)
            val currentWater = uiState.todayWaterMl
            val waterPct = ((currentWater.toFloat() / waterTarget.toFloat()) * 100).toInt().coerceAtMost(100)
            val progressFraction = (currentWater.toFloat() / waterTarget.toFloat()).coerceIn(0f, 1f)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = WaterBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "WATER INTAKE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currentWater / $waterTarget ml ($waterPct%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WaterBlue.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, WaterBlue.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Goal: ${waterTarget}ml",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = WaterBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = progressFraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = WaterBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onLogWater(250) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("+250 ml", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { onLogWater(500) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("+500 ml", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { onLogWater(1000) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("+1000 ml", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { onLogWater(-250) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("-250", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Search & Database Food Tab
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogFoodTabContent(
    uiState: FoodUiState,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onSearchOnline: (String) -> Unit,
    onFoodSelected: (FoodItem, Boolean) -> Unit,
    onDeleteFood: (FoodItem) -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(uiState.searchQuery, TextRange(uiState.searchQuery.length)))
    }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.searchQuery) {
        if (uiState.searchQuery.isEmpty() && textFieldValue.text.isNotEmpty()) {
            textFieldValue = TextFieldValue("", TextRange.Zero)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar (Name Search)
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onSearchQueryChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                        val current = textFieldValue.text
                        val selection = textFieldValue.selection
                        if (current.isNotEmpty()) {
                            val (newText, newPos) = if (!selection.collapsed) {
                                val start = selection.min
                                val end = selection.max
                                Pair(current.removeRange(start, end), start)
                            } else if (selection.start > 0) {
                                val pos = selection.start
                                Pair(current.removeRange(pos - 1, pos), pos - 1)
                            } else {
                                Pair(current, 0)
                            }
                            if (newText != current) {
                                textFieldValue = TextFieldValue(newText, TextRange(newPos))
                                onSearchQueryChange(newText)
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                },
            placeholder = {
                Text(
                    text = "Search foods...",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (textFieldValue.text.isNotEmpty()) {
                    IconButton(onClick = {
                        textFieldValue = TextFieldValue("", TextRange.Zero)
                        onSearchQueryChange("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    if (textFieldValue.text.isNotBlank()) {
                        onSearchOnline(textFieldValue.text)
                    }
                }
            ),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        // Category Filter Chips
        if (uiState.categories.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.categories, key = { it }) { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { onCategorySelect(category) },
                        label = { Text(category, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Typo correction suggestion pill
        if (!uiState.didYouMeanSuggestion.isNullOrBlank()) {
            Surface(
                onClick = {
                    val suggestion = uiState.didYouMeanSuggestion
                    if (!suggestion.isNullOrBlank()) {
                        textFieldValue = TextFieldValue(suggestion, TextRange(suggestion.length))
                        onSearchQueryChange(suggestion)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Did you mean: ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.didYouMeanSuggestion,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Live AI Search action banner when query is present
        if (uiState.searchQuery.isNotBlank()) {
            Card(
                onClick = {
                    if (!uiState.isSearchingOnline) {
                        onSearchOnline(uiState.searchQuery)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isSearchingOnline) {
                        val infiniteTransition = rememberInfiniteTransition(label = "ai_fade")
                        val iconAlpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(700, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "ai_pulse_alpha"
                        )
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .alpha(iconAlpha),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isSearchingOnline) "Analyzing recipe & calculating macros with AI..." else "Search '${uiState.searchQuery}' with AI",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (uiState.isSearchingOnline) "Querying AI nutritionist & verified food data" else "Instant lab-grade macros for any dish, recipe, or brand",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Search Results List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (uiState.onlineSearchResults.isNotEmpty()) {
                item(key = "header_online_results") {
                    Text(
                        text = "AI & Live Online Results (${uiState.onlineSearchResults.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                itemsIndexed(uiState.onlineSearchResults, key = { idx, food -> "online_${food.name}_${food.source}_$idx" }) { _, food ->
                    FoodItemCard(
                        food = food,
                        onClick = { onFoodSelected(food, true) },
                        onQuickAdd = { onFoodSelected(food, true) }
                    )
                }
            }

            if (uiState.searchResults.isNotEmpty()) {
                if (uiState.onlineSearchResults.isNotEmpty()) {
                    item(key = "header_local_results") {
                        Text(
                            text = "Local Matches",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                itemsIndexed(uiState.searchResults, key = { idx, food -> "local_${food.id}_${food.name}_$idx" }) { _, food ->
                    FoodItemCard(
                        food = food,
                        onClick = { onFoodSelected(food, false) },
                        onQuickAdd = { onFoodSelected(food, false) },
                        onLongClick = { onDeleteFood(food) },
                        onDelete = if (food.isCustom || food.source != "Verified Nutrition") { { onDeleteFood(food) } } else null
                    )
                }
            } else if (uiState.onlineSearchResults.isEmpty()) {
                item(key = "no_search_results_msg") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillParentMaxHeight(0.6f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (uiState.onlineSearchError == "No Internet Connection") Icons.Default.WifiOff else Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = if (uiState.onlineSearchError == "No Internet Connection") MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (uiState.onlineSearchError != null) {
                                        uiState.onlineSearchError
                                    } else if (uiState.searchQuery.isNotBlank()) {
                                        "No local matches found for '${uiState.searchQuery}'"
                                    } else {
                                        "No food items found"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (uiState.onlineSearchError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (uiState.onlineSearchError == "No Internet Connection") {
                                        "Please check your network connection and try again."
                                    } else if (uiState.onlineSearchError != null) {
                                        "Try checking your spelling or search for standard ingredients."
                                    } else {
                                        "Tap 'Search with AI' above to look up instant verified macros live."
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Curated & Personalized Meal Plans
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealPlanTabContent(
    uiState: FoodUiState,
    selectedIndex: Int,
    onSelectPlanIndex: (Int) -> Unit,
    onLogMealToDiary: (MealSuggestion, String) -> Unit
) {
    val plans = MealPlanSuggestions.getAllPlans(uiState.userProfile)
    val currentPlan = if (selectedIndex in plans.indices) plans[selectedIndex] else plans[0]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Plan selector chips
        item(key = "meal_plans_selector") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                plans.forEachIndexed { index, plan ->
                    FilterChip(
                        selected = selectedIndex == index,
                        onClick = { onSelectPlanIndex(index) },
                        label = { Text("${plan.name} (${plan.totalCalories} kcal)", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Active Plan Overview Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentPlan.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "${currentPlan.totalCalories} kcal • ${currentPlan.totalProtein}g P",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = currentPlan.description,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Plan Meals List
        itemsIndexed(currentPlan.meals, key = { idx, meal -> "meal_${meal.mealType}_${meal.time}_$idx" }) { _, meal ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "SCHEDULED • ${meal.time}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = meal.mealType,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onLogMealToDiary(meal, meal.mealType)
                            }
                        ) {
                            Text(
                                text = "+ Log to Diary",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = if (meal.carbs > 0 || meal.fat > 0) {
                            "${meal.calories} kcal • ${meal.protein}g P • ${meal.carbs}g C • ${meal.fat}g F"
                        } else {
                            "${meal.calories} kcal • ${meal.protein}g Protein"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Food items in this meal
                    meal.foods.forEach { foodDesc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "•", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = foodDesc, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick Add Raw Calories Dialog (MyFitnessPal Feature)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddDialog(
    initialMealType: String = "Lunch",
    onDismiss: () -> Unit,
    onConfirm: (name: String, calories: Double, protein: Double, carbs: Double, fat: Double, mealType: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf(initialMealType) }
    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snacks")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Add Calories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Quickly log raw calories and macros into your food diary without searching:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Entry Name (optional)") },
                    placeholder = { Text("e.g. Homemade Protein Shake") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it },
                    label = { Text("Calories (kcal) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                Text(text = "Assign to Meal:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mealTypes.forEach { type ->
                        FilterChip(
                            selected = selectedMealType == type,
                            onClick = { selectedMealType = type },
                            label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Button(
                    onClick = {
                        val cals = caloriesText.toDoubleOrNull() ?: 0.0
                        val p = proteinText.toDoubleOrNull() ?: 0.0
                        val c = carbsText.toDoubleOrNull() ?: 0.0
                        val f = fatText.toDoubleOrNull() ?: 0.0
                        if (cals > 0) {
                            onConfirm(name, cals, p, c, f, selectedMealType)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    enabled = (caloriesText.toDoubleOrNull() ?: 0.0) > 0.0
                ) {
                    Text("Log to Diary", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/**
 * Historical Calendar Date Picker Dialog for Nutrition Diary
 * Allows jumping across past/future months with fixed 6-row layout and food logging indicators.
 */
@Composable
fun DiaryDatePickerDialog(
    selectedDate: String,
    loggedDates: Set<String>,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var monthOffset by remember { mutableIntStateOf(0) }
    val displayedCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
        }
    }
    val monthTitle = remember(displayedCal) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayedCal.time)
    }
    val todaySdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayStr = remember { todaySdf.format(Date()) }

    // First day of displayed month calculation (Monday = column 0)
    val firstDayCal = remember(displayedCal) {
        (displayedCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
    val leadingEmpty = (firstDayOfWeek + 5) % 7 // Monday = 0
    val daysInMonth = displayedCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Month Navigation (< Month Year >) and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Select Diary Date",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { monthOffset-- },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Month",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = monthTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { monthOffset++ },
                                    enabled = monthOffset < 0,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Next Month",
                                        tint = if (monthOffset < 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (monthOffset != 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { monthOffset = 0 }
                                    ) {
                                        Text(
                                            text = "Today",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Days of week header row (M T W T F S S)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayLabels.forEach { label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Fixed 6-week grid ensuring constant size across all months
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (row in 0 until 6) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - leadingEmpty + 1
                                if (dayNum in 1..daysInMonth) {
                                    val dayCal = (displayedCal.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, dayNum)
                                    }
                                    val dateStr = todaySdf.format(dayCal.time)
                                    val isSelected = (dateStr == selectedDate)
                                    val isLogged = loggedDates.contains(dateStr)
                                    val isToday = (dateStr == todayStr)
                                    val isFuture = dateStr > todayStr

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 1.dp else if (isToday) 0.8.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.outline else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable(enabled = !isFuture) {
                                                onDateSelected(dateStr)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    isToday -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            if (isLogged) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else GetFitTheme.colors.success)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Empty slot maintains equal height across all 6 rows
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Legend / Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GetFitTheme.colors.success)
                        )
                        Text(
                            text = "Logged Food",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedDate != todayStr) {
                        TextButton(
                            onClick = { onDateSelected(todayStr) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Jump to Today", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog enabling granular checkbox selection of yesterday's meals
 * (Breakfast, Lunch, Dinner, Snacks) before copying into the active diary date.
 */
@Composable
private fun CopyYesterdayMealsDialog(
    summaries: List<YesterdayMealSummary>,
    currentDateLabel: String,
    onDismiss: () -> Unit,
    onConfirmCopy: (Set<String>) -> Unit
) {
    val totalAvailableItems = summaries.sumOf { it.itemsCount }
    val selectedMeals = remember(summaries) {
        mutableStateMapOf<String, Boolean>().apply {
            summaries.forEach { summary ->
                put(summary.mealType, summary.itemsCount > 0)
            }
        }
    }

    val selectedCount = summaries.filter { selectedMeals[it.mealType] == true }.sumOf { it.itemsCount }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Copy Yesterday's Meals",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "To $currentDateLabel",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (totalAvailableItems == 0) {
                    // Empty state when yesterday had no food logged
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No Meals Logged Yesterday",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Yesterday has no recorded meals to copy. Log food today to easily duplicate it tomorrow!",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Got It", maxLines = 1, softWrap = false)
                    }
                } else {
                    // Quick Action Toggle: Select All / Deselect All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Choose meals to copy:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )

                        val allSelected = summaries.filter { it.itemsCount > 0 }.all { selectedMeals[it.mealType] == true }
                        TextButton(
                            onClick = {
                                val target = !allSelected
                                summaries.forEach {
                                    if (it.itemsCount > 0) {
                                        selectedMeals[it.mealType] = target
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (allSelected) "Deselect All" else "Select All",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Checkbox Meal Cards (Breakfast, Lunch, Dinner, Snacks)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        summaries.forEach { summary ->
                            val isAvailable = summary.itemsCount > 0
                            val isChecked = selectedMeals[summary.mealType] == true

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChecked && isAvailable) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isChecked && isAvailable) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isAvailable) {
                                        selectedMeals[summary.mealType] = !isChecked
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = isChecked && isAvailable,
                                            onCheckedChange = if (isAvailable) { isCheckedNew -> selectedMeals[summary.mealType] = isCheckedNew } else null,
                                            enabled = isAvailable
                                        )

                                        Column {
                                            Text(
                                                text = summary.mealType,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                softWrap = false,
                                                color = if (isAvailable) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = if (isAvailable) {
                                                    "${summary.itemsCount} item${if (summary.itemsCount > 1) "s" else ""} • ${summary.totalCalories} kcal"
                                                } else {
                                                    "No items logged yesterday"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                softWrap = false,
                                                color = if (isAvailable) MaterialTheme.colorScheme.onSurfaceVariant
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons (Cancel / Copy Selected)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = {
                                val chosen = summaries
                                    .filter { selectedMeals[it.mealType] == true && it.itemsCount > 0 }
                                    .map { it.mealType }
                                    .toSet()
                                onConfirmCopy(chosen)
                            },
                            enabled = selectedCount > 0,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (selectedCount > 0) "Copy ($selectedCount Items)" else "Copy Selected",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

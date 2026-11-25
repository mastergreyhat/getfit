package com.getfit.ui.screens

import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.theme.Dimens
import com.getfit.ui.components.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import com.getfit.data.entity.FoodItem
import com.getfit.data.entity.FoodLog
import com.getfit.data.remote.GeminiNutritionService
import com.getfit.ui.components.FoodItemCard
import com.getfit.ui.components.ModifyPastDateDialog
import com.getfit.ui.theme.WaterBlue
import com.getfit.ui.util.AppDateManager
import com.getfit.ui.util.AuthPreferences
import com.getfit.ui.viewmodel.FoodUiState
import com.getfit.ui.viewmodel.FoodViewModel
import com.getfit.ui.viewmodel.YesterdayMealSummary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodScreen(
    viewModel: FoodViewModel,
    onNavigateToAddCustomFood: () -> Unit,
    onNavigateToAuth: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val diaryScrollTarget by viewModel.diaryScrollTarget.collectAsStateWithLifecycle()
    val tabs = listOf("Diary", "Search & Add", "Meal Plans")

    androidx.activity.compose.BackHandler(enabled = selectedTab != 0) {
        viewModel.resetToDiary()
    }

    var selectedFoodForDialog by remember { mutableStateOf<FoodItem?>(null) }
    var isSelectedFoodOnline by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var quickAddDefaultMeal by remember { mutableStateOf("Lunch") }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showCopyYesterdayDialog by remember { mutableStateOf(false) }
    var showWaterTargetDialog by remember { mutableStateOf(false) }
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

    if (showWaterTargetDialog) {
        EditWaterTargetDialog(
            currentTarget = uiState.userProfile.dailyWaterTarget.coerceAtLeast(500),
            onDismiss = { showWaterTargetDialog = false },
            onConfirm = { newTarget ->
                viewModel.updateWaterTarget(newTarget)
                showWaterTargetDialog = false
                snackbarMessage = "Daily hydration target updated to $newTarget ml"
                showSnackbar = true
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
        containerColor = GetFitTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                            color = GetFitTheme.colors.surface3,
                            contentColor = GetFitTheme.colors.textPrimary,
                            shadowElevation = 6.dp,
                            border = BorderStroke(1.dp, GetFitTheme.colors.hairline)
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
                    containerColor = GetFitTheme.colors.accent,
                    contentColor = GetFitTheme.colors.onAccent,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 52.dp).navigationBarsPadding()
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
            // Pinned Segmented Control
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Dimens.screenGutter, end = Dimens.screenGutter, top = Dimens.xs, bottom = Dimens.sm)
            ) {
                GfSegmentedControl(
                    items = tabs,
                    selectedIndex = selectedTab,
                    onSelect = viewModel::selectTab
                )
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
                    },
                    onEditWaterTarget = { showWaterTargetDialog = true },
                    diaryScrollTarget = diaryScrollTarget,
                    onConsumeDiaryScrollTarget = viewModel::consumeDiaryScrollTarget
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
                    onPantryInputChange = viewModel::onPantryInputChange,
                    onGenerateMealPlan = viewModel::generateAiMealPlan,
                    onLogGeneratedMeal = { meal ->
                        checkPastDateAndRun {
                            viewModel.logGeneratedMeal(meal)
                            snackbarMessage = "Logged ${meal.mealType} to diary"
                            showSnackbar = true
                        }
                    },
                    onLogAllGeneratedMeals = { meals ->
                        checkPastDateAndRun {
                            viewModel.logAllGeneratedMeals(meals)
                            snackbarMessage = "Logged all ${meals.size} meals to diary"
                            showSnackbar = true
                        }
                    },
                    onNavigateToAuth = onNavigateToAuth
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
 * Tab 0: Daily Food Diary (CRED / Apple TV style)
 */
@OptIn(ExperimentalFoundationApi::class)
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
    onLogWater: (Int) -> Unit,
    onEditWaterTarget: () -> Unit,
    diaryScrollTarget: Int? = null,
    onConsumeDiaryScrollTarget: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    LaunchedEffect(diaryScrollTarget) {
        when (diaryScrollTarget) {
            0 -> {
                listState.scrollToItem(0)
                onConsumeDiaryScrollTarget()
            }
            1 -> {
                val lastIdx = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                listState.animateScrollToItem(lastIdx)
                onConsumeDiaryScrollTarget()
            }
        }
    }
    val calorieTarget = uiState.userProfile.dailyCalorieTarget.toDouble().coerceAtLeast(1000.0)
    val caloriesConsumed = uiState.totalCalories
    val isOverGoal = caloriesConsumed > calorieTarget
    val diffCalories = if (isOverGoal) (caloriesConsumed - calorieTarget) else (calorieTarget - caloriesConsumed)
    val heroNumber = diffCalories.toInt()
    val heroLabel = if (isOverGoal) "kcal over" else "kcal left"
    val heroColor = if (isOverGoal) GetFitTheme.colors.warning else GetFitTheme.colors.textPrimary

    val standardMeals = remember { listOf("Breakfast", "Lunch", "Dinner", "Snacks") }
    val groupedLogs = remember(uiState.todayLogs) {
        uiState.todayLogs.groupBy { normalizeMealType(it.mealType) }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.screenGutter,
            end = Dimens.screenGutter,
            top = Dimens.xs,
            bottom = com.getfit.ui.components.core.gfBottomBarContentPadding() + Dimens.xl
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.md)
    ) {
        // 1. Date Navigation Bar: compact centered row with chevrons, date in titleMedium, no surrounding box
        item(key = "diary_date_nav") {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousDate,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Day",
                        tint = GetFitTheme.colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                    modifier = Modifier
                        .pressable(onClick = onOpenCalendarPicker)
                        .padding(horizontal = Dimens.md, vertical = Dimens.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Pick Date",
                        tint = GetFitTheme.colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = uiState.dateDisplayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GetFitTheme.colors.textPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                IconButton(
                    onClick = onNextDate,
                    enabled = AppDateManager.canGoNext(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Day",
                        tint = if (AppDateManager.canGoNext()) GetFitTheme.colors.textSecondary else GetFitTheme.colors.textTertiary.copy(alpha = 0.35f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 2. Calories Hero Card: compact GfHeroCard with adaptive displayMedium, 3-col equation, and MacroStat row
        item(key = "diary_calorie_hero") {
            GfHeroCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(Dimens.lg)
            ) {
                GfOverline(text = "CALORIES REMAINING")
                Spacer(modifier = Modifier.height(Dimens.xs))
                GfBigNumber(
                    value = heroNumber,
                    unit = heroLabel,
                    hero = true,
                    color = heroColor,
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(Dimens.md))

                // Quiet three-column row: Goal, Food, Remaining/Over
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${calorieTarget.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GetFitTheme.colors.textPrimary
                        )
                        Text(
                            text = "Goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textSecondary
                        )
                    }
                    Text(
                        text = "−",
                        style = MaterialTheme.typography.titleMedium,
                        color = GetFitTheme.colors.textTertiary
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${caloriesConsumed.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GetFitTheme.colors.textPrimary
                        )
                        Text(
                            text = "Food",
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textSecondary
                        )
                    }
                    Text(
                        text = "=",
                        style = MaterialTheme.typography.titleMedium,
                        color = GetFitTheme.colors.textTertiary
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$heroNumber",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = heroColor
                        )
                        Text(
                            text = if (isOverGoal) "Over" else "Remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverGoal) GetFitTheme.colors.warning else GetFitTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.md))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(GetFitTheme.colors.hairline)
                )
                Spacer(modifier = Modifier.height(Dimens.md))

                // Shared MacroStat row (Protein, Carbs, Fat)
                val proteinTarget = uiState.userProfile.dailyProteinTarget.toFloat().coerceAtLeast(100f)
                val carbsTarget = uiState.userProfile.dailyCarbsTarget.toFloat().coerceAtLeast(150f)
                val fatTarget = uiState.userProfile.dailyFatTarget.toFloat().coerceAtLeast(40f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.lg)
                ) {
                    MacroStat(
                        modifier = Modifier.weight(1f),
                        label = "Protein",
                        current = uiState.totalProtein.toFloat(),
                        target = proteinTarget,
                        color = GetFitTheme.colors.protein
                    )
                    MacroStat(
                        modifier = Modifier.weight(1f),
                        label = "Carbs",
                        current = uiState.totalCarbs.toFloat(),
                        target = carbsTarget,
                        color = GetFitTheme.colors.carbs
                    )
                    MacroStat(
                        modifier = Modifier.weight(1f),
                        label = "Fat",
                        current = uiState.totalFat.toFloat(),
                        target = fatTarget,
                        color = GetFitTheme.colors.fat
                    )
                }
            }
        }

        // 3. One-Click Copy Yesterday's Meals: slim row styled like GfSecondaryButton, no white border
        item(key = "diary_copy_yesterday") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable(onClick = onCopyYesterdayMeals)
                    .clip(CircleShape)
                    .background(GetFitTheme.colors.surface2)
                    .border(1.dp, GetFitTheme.colors.hairline, CircleShape)
                    .padding(horizontal = Dimens.lg, vertical = Dimens.md),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = GetFitTheme.colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Log Yesterday's Same Plan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GetFitTheme.colors.textPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Copy Yesterday",
                        tint = GetFitTheme.colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 4. Meal Category Cards (Breakfast, Lunch, Dinner, Snacks)
        standardMeals.forEach { mealType ->
            val logs = groupedLogs[mealType] ?: emptyList()
            val mealCals = logs.sumOf { it.totalCalories }.toInt()

            item(key = "diary_meal_$mealType") {
                GfCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = Dimens.sm)
                        ) {
                            Text(
                                text = mealType,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = GetFitTheme.colors.textPrimary
                            )
                            Text(
                                text = "$mealCals kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = GetFitTheme.colors.textSecondary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GfTextButton(
                                text = "+ Quick",
                                onClick = { onQuickAddClick(mealType) },
                                color = GetFitTheme.colors.textSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .pressable(onClick = { onAddFoodClick(mealType) })
                                    .clip(CircleShape)
                                    .background(GetFitTheme.colors.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add food to $mealType",
                                    tint = GetFitTheme.colors.onAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (logs.isEmpty()) {
                        Spacer(modifier = Modifier.height(Dimens.md))
                        Text(
                            text = "No food logged for $mealType yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textTertiary
                        )
                    } else {
                        logs.forEach { log ->
                            Spacer(modifier = Modifier.height(Dimens.md))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(GetFitTheme.colors.hairline)
                            )
                            Spacer(modifier = Modifier.height(Dimens.md))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = Dimens.sm)
                                ) {
                                    Text(
                                        text = log.foodName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GetFitTheme.colors.textPrimary
                                    )
                                    val servingDesc = if (log.servingSize > 1.0) {
                                        "${(log.quantity * log.servingSize).toInt()}g • P:${log.totalProtein.toInt()}g C:${log.totalCarbs.toInt()}g F:${log.totalFat.toInt()}g"
                                    } else {
                                        "${log.quantity.toInt()} serving • P:${log.totalProtein.toInt()}g C:${log.totalCarbs.toInt()}g F:${log.totalFat.toInt()}g"
                                    }
                                    Text(
                                        text = servingDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GetFitTheme.colors.textSecondary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                                ) {
                                    Text(
                                        text = "${log.totalCalories.toInt()} kcal",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFeatureSettings = "tnum"
                                        ),
                                        fontWeight = FontWeight.SemiBold,
                                        color = GetFitTheme.colors.textPrimary
                                    )
                                    IconButton(
                                        onClick = { onDeleteLog(log) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete item",
                                            tint = GetFitTheme.colors.textTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Workout / Training Completed Card (Tied to selected date)
        item(key = "diary_workout_section") {
            val workoutLogs = uiState.dateWorkoutLogs
            val groupedWorkouts = remember(workoutLogs) {
                workoutLogs.groupBy { it.exerciseName }
            }

            GfCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Exercise & Training",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = GetFitTheme.colors.textPrimary
                        )
                        Text(
                            text = if (groupedWorkouts.isNotEmpty()) {
                                "${groupedWorkouts.size} exercises • ${workoutLogs.size} sets completed"
                            } else {
                                "No workouts logged for this day"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textSecondary
                        )
                    }
                }

                if (groupedWorkouts.isEmpty()) {
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    Text(
                        text = "No workouts logged for this day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GetFitTheme.colors.textTertiary
                    )
                } else {
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(GetFitTheme.colors.hairline)
                    )
                    Spacer(modifier = Modifier.height(Dimens.sm))

                    groupedWorkouts.entries.forEachIndexed { idx, (exName, sets) ->
                        if (idx > 0) {
                            Spacer(modifier = Modifier.height(Dimens.xs))
                        }

                        val setSummary = remember(sets) {
                            val allSameWeight = sets.all { it.weight == sets.first().weight }
                            val allSameReps = sets.all { it.reps == sets.first().reps }
                            fun fmtW(w: Double) = if (w % 1.0 == 0.0) "${w.toInt()}" else "$w"
                            if (allSameWeight && allSameReps && sets.isNotEmpty()) {
                                "${sets.size}×${fmtW(sets.first().weight)}kg · ${sets.first().reps} reps"
                            } else {
                                val maxWeight = sets.maxOfOrNull { it.weight } ?: 0.0
                                "${sets.size} sets · Top ${fmtW(maxWeight)}kg"
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = GetFitTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = Dimens.sm)
                            )
                            Text(
                                text = setSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = GetFitTheme.colors.textSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 6. Daily Hydration Card (matches Home screen style + quick-modify buttons; press & hold to set target)
        item(key = "diary_water_intake") {
            val waterTarget = uiState.userProfile.dailyWaterTarget.coerceAtLeast(500)
            val currentWater = uiState.todayWaterMl
            val progressFraction = (currentWater.toFloat() / waterTarget.toFloat()).coerceIn(0f, 1f)
            val haptic = LocalHapticFeedback.current

            GfCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.xxl))
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEditWaterTarget()
                        }
                    )
            ) {
                GfOverline(text = "Daily Hydration")
                Spacer(modifier = Modifier.height(Dimens.xs))
                GfBigNumber(
                    value = currentWater,
                    unit = "ml",
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = "Target: $waterTarget ml • Hold to edit",
                    style = MaterialTheme.typography.bodySmall,
                    color = GetFitTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(Dimens.md))

                GfProgressBar(
                    progress = progressFraction,
                    color = GetFitTheme.colors.water
                )

                Spacer(modifier = Modifier.height(Dimens.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    val waterActions = listOf(250 to "+250", 500 to "+500", 1000 to "+1 L", -250 to "-250")
                    waterActions.forEach { (delta, label) ->
                        GfChip(
                            text = label,
                            selected = false,
                            onClick = { onLogWater(delta) },
                            modifier = Modifier.weight(1f)
                        )
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
            .padding(horizontal = Dimens.screenGutter, vertical = Dimens.sm),
        verticalArrangement = Arrangement.spacedBy(Dimens.md)
    ) {
        // Search Bar with GfTextField
        GfTextField(
            value = textFieldValue.text,
            onValueChange = { newText ->
                textFieldValue = TextFieldValue(newText, TextRange(newText.length))
                onSearchQueryChange(newText)
            },
            placeholder = "Search foods...",
            leadingIcon = Icons.Default.Search,
            trailingIcon = if (textFieldValue.text.isNotEmpty()) {
                {
                    IconButton(onClick = {
                        textFieldValue = TextFieldValue("", TextRange.Zero)
                        onSearchQueryChange("")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = GetFitTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else null,
            imeAction = ImeAction.Search,
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    if (textFieldValue.text.isNotBlank()) {
                        onSearchOnline(textFieldValue.text)
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Category Filter Chips with GfChipRow
        if (uiState.categories.isNotEmpty()) {
            GfChipRow(
                items = uiState.categories,
                selectedItem = uiState.selectedCategory,
                onItemSelected = onCategorySelect,
                contentPadding = PaddingValues(0.dp)
            )
        }

        // Typo correction suggestion pill
        if (!uiState.didYouMeanSuggestion.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable {
                        val suggestion = uiState.didYouMeanSuggestion
                        if (!suggestion.isNullOrBlank()) {
                            textFieldValue = TextFieldValue(suggestion, TextRange(suggestion.length))
                            onSearchQueryChange(suggestion)
                        }
                    }
                    .clip(RoundedCornerShape(Dimens.sm))
                    .background(GetFitTheme.colors.surface2)
                    .border(1.dp, GetFitTheme.colors.accent.copy(alpha = 0.4f), RoundedCornerShape(Dimens.sm))
                    .padding(horizontal = Dimens.md, vertical = Dimens.sm)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = GetFitTheme.colors.accent
                    )
                    Spacer(modifier = Modifier.width(Dimens.sm))
                    Text(
                        text = "Did you mean: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = GetFitTheme.colors.textSecondary
                    )
                    Text(
                        text = uiState.didYouMeanSuggestion,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GetFitTheme.colors.accent,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Live AI Search action banner when query is present
        if (uiState.searchQuery.isNotBlank()) {
            GfCard(
                onClick = {
                    if (!uiState.isSearchingOnline) {
                        onSearchOnline(uiState.searchQuery)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(Dimens.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                .size(20.dp)
                                .graphicsLayer { alpha = iconAlpha },
                            tint = GetFitTheme.colors.accent
                        )
                    } else {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = GetFitTheme.colors.accent
                        )
                    }
                    Spacer(modifier = Modifier.width(Dimens.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isSearchingOnline) "Analyzing recipe & calculating macros with AI..." else "Search '${uiState.searchQuery}' with AI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GetFitTheme.colors.accent,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (uiState.isSearchingOnline) "Querying AI nutritionist & verified food data" else "Instant lab-grade macros for any dish, recipe, or brand",
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textSecondary,
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
            verticalArrangement = Arrangement.spacedBy(Dimens.sm),
            contentPadding = PaddingValues(bottom = com.getfit.ui.components.core.gfBottomBarContentPadding() + Dimens.lg)
        ) {
            if (uiState.onlineSearchResults.isNotEmpty()) {
                item(key = "header_online_results") {
                    Text(
                        text = "AI & Live Online Results (${uiState.onlineSearchResults.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GetFitTheme.colors.accent,
                        modifier = Modifier.padding(vertical = Dimens.xs),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                itemsIndexed(
                    uiState.onlineSearchResults,
                    key = { idx, food -> "online_${food.name}_${food.source}_$idx" },
                    contentType = { _, _ -> "food_item" }
                ) { _, food ->
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
                            fontWeight = FontWeight.SemiBold,
                            color = GetFitTheme.colors.textPrimary,
                            modifier = Modifier.padding(top = Dimens.sm, bottom = Dimens.xs),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                itemsIndexed(
                    uiState.searchResults,
                    key = { idx, food -> "local_${food.id}_${food.name}_$idx" },
                    contentType = { _, _ -> "food_item" }
                ) { _, food ->
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
                            .padding(horizontal = Dimens.xs),
                        contentAlignment = Alignment.Center
                    ) {
                        GfCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Dimens.lg),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (uiState.onlineSearchError == "No Internet Connection") Icons.Default.WifiOff else Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = if (uiState.onlineSearchError == "No Internet Connection") GetFitTheme.colors.danger else GetFitTheme.colors.textTertiary
                                )
                                Spacer(modifier = Modifier.height(Dimens.sm))
                                Text(
                                    text = if (uiState.onlineSearchError != null) {
                                        uiState.onlineSearchError
                                    } else if (uiState.searchQuery.isNotBlank()) {
                                        "No local matches found for '${uiState.searchQuery}'"
                                    } else {
                                        "No food items found"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (uiState.onlineSearchError != null) GetFitTheme.colors.danger else GetFitTheme.colors.textPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(Dimens.xs))
                                Text(
                                    text = if (uiState.onlineSearchError == "No Internet Connection") {
                                        "Please check your network connection and try again."
                                    } else if (uiState.onlineSearchError != null) {
                                        "Try checking your spelling or search for standard ingredients."
                                    } else {
                                        "Tap 'Search with AI' above to look up instant verified macros live."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GetFitTheme.colors.textSecondary,
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

private fun isGeneratedMealLogged(
    meal: GeminiNutritionService.GeneratedMeal,
    logs: List<FoodLog>
): Boolean {
    val expectedName = "${meal.title}: ${meal.items.joinToString(", ")}"
    return logs.any { it.foodName == expectedName }
}

/**
 * Tab 2: AI Pantry-to-Meal-Plan Generator (Logged-in users only)
 */
@Composable
private fun MealPlanTabContent(
    uiState: FoodUiState,
    onPantryInputChange: (String) -> Unit,
    onGenerateMealPlan: () -> Unit,
    onLogGeneratedMeal: (GeminiNutritionService.GeneratedMeal) -> Unit,
    onLogAllGeneratedMeals: (List<GeminiNutritionService.GeneratedMeal>) -> Unit,
    onNavigateToAuth: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLoggedIn = remember(uiState.userProfile) { AuthPreferences.isLoggedIn(context) }
    var duplicateMealToConfirm by remember { mutableStateOf<GeminiNutritionService.GeneratedMeal?>(null) }
    var pendingBulkLogMeals by remember { mutableStateOf<List<GeminiNutritionService.GeneratedMeal>?>(null) }
    var expandedHistoryId by remember { mutableStateOf<String?>(null) }
    val historyDateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    duplicateMealToConfirm?.let { meal ->
        AlertDialog(
            onDismissRequest = { duplicateMealToConfirm = null },
            containerColor = GetFitTheme.colors.surface1,
            titleContentColor = GetFitTheme.colors.textPrimary,
            textContentColor = GetFitTheme.colors.textSecondary,
            title = { Text("Already in Diary", fontWeight = FontWeight.Bold) },
            text = {
                Text("${meal.title} (${meal.mealType}) is already added for ${uiState.dateDisplayLabel}. Do you wish to continue?")
            },
            confirmButton = {
                GfTextButton(
                    text = "Continue",
                    onClick = {
                        duplicateMealToConfirm = null
                        onLogGeneratedMeal(meal)
                    },
                    color = GetFitTheme.colors.accent
                )
            },
            dismissButton = {
                GfTextButton(
                    text = "Cancel",
                    onClick = { duplicateMealToConfirm = null },
                    color = GetFitTheme.colors.textSecondary
                )
            }
        )
    }

    pendingBulkLogMeals?.let { mealsToLog ->
        AlertDialog(
            onDismissRequest = { pendingBulkLogMeals = null },
            containerColor = GetFitTheme.colors.surface1,
            titleContentColor = GetFitTheme.colors.textPrimary,
            textContentColor = GetFitTheme.colors.textSecondary,
            title = { Text("Already in Diary", fontWeight = FontWeight.Bold) },
            text = {
                Text("Some or all meals from this plan are already added for ${uiState.dateDisplayLabel}. Do you wish to continue?")
            },
            confirmButton = {
                GfTextButton(
                    text = "Continue",
                    onClick = {
                        pendingBulkLogMeals = null
                        onLogAllGeneratedMeals(mealsToLog)
                    },
                    color = GetFitTheme.colors.accent
                )
            },
            dismissButton = {
                GfTextButton(
                    text = "Cancel",
                    onClick = { pendingBulkLogMeals = null },
                    color = GetFitTheme.colors.textSecondary
                )
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.screenGutter,
            end = Dimens.screenGutter,
            top = Dimens.sm,
            bottom = com.getfit.ui.components.core.gfBottomBarContentPadding() + Dimens.xl
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.xxl)
    ) {
        if (!isLoggedIn) {
            item(key = "meal_plan_locked") {
                GfCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(GetFitTheme.colors.accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = GetFitTheme.colors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "AI Meal Planner",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GetFitTheme.colors.textPrimary
                        )
                        Text(
                            text = "Sign in to enter ingredients you have at home and let Gemini build a daily meal plan matching your calorie target.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GetFitTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        GfPrimaryButton(
                            text = "Sign In to Unlock",
                            onClick = onNavigateToAuth,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            item(key = "pantry_input_card") {
                GfCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                            GfOverline(text = "WHAT'S IN YOUR KITCHEN?")

                            GfTextField(
                                value = uiState.pantryInput,
                                onValueChange = onPantryInputChange,
                                placeholder = "e.g. eggs, oats, rice, chicken breast, paneer, curd, banana...",
                                maxLines = 4,
                                singleLine = false,
                                imeAction = ImeAction.Done,
                                unfocusedBorderColor = GetFitTheme.colors.accent.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!uiState.mealPlanError.isNullOrBlank()) {
                            Text(
                                text = uiState.mealPlanError,
                                style = MaterialTheme.typography.bodySmall,
                                color = GetFitTheme.colors.danger
                            )
                        }

                        GfSecondaryButton(
                            text = "Generate Meal Plan",
                            icon = Icons.Default.AutoAwesome,
                            isLoading = uiState.isGeneratingMealPlan,
                            enabled = uiState.pantryInput.isNotBlank() && !uiState.isGeneratingMealPlan,
                            onClick = onGenerateMealPlan,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            val plan = uiState.generatedMealPlan
            if (plan != null) {
                item(key = "ai_plan_overview") {
                    GfCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your AI Daily Plan",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = GetFitTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(Dimens.sm))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(GetFitTheme.colors.surface2)
                                    .border(1.dp, GetFitTheme.colors.hairline, CircleShape)
                                    .padding(horizontal = Dimens.md, vertical = Dimens.xs)
                            ) {
                                Text(
                                    text = "${plan.totalCalories} kcal • ${plan.totalProtein}g P",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GetFitTheme.colors.accent,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(Dimens.xs))
                        Text(
                            text = plan.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GetFitTheme.colors.textSecondary
                        )
                        if (plan.meals.isNotEmpty()) {
                            val allMealsLogged = plan.meals.all { isGeneratedMealLogged(it, uiState.todayLogs) }
                            val anyMealLogged = plan.meals.any { isGeneratedMealLogged(it, uiState.todayLogs) }
                            Spacer(modifier = Modifier.height(Dimens.md))
                            GfPrimaryButton(
                                text = if (allMealsLogged) "All Meals Logged" else "Log Entire Plan to Diary",
                                icon = if (allMealsLogged) Icons.Rounded.Check else Icons.Rounded.Add,
                                onClick = {
                                    if (anyMealLogged) {
                                        pendingBulkLogMeals = plan.meals
                                    } else {
                                        onLogAllGeneratedMeals(plan.meals)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                itemsIndexed(plan.meals, key = { idx, meal -> "ai_meal_${meal.mealType}_$idx" }) { _, meal ->
                    val isLogged = isGeneratedMealLogged(meal, uiState.todayLogs)
                    GeneratedMealCard(
                        meal = meal,
                        isLogged = isLogged,
                        onLogClick = {
                            if (isLogged) duplicateMealToConfirm = meal else onLogGeneratedMeal(meal)
                        }
                    )
                }
            }

            if (uiState.mealPlanHistory.isNotEmpty()) {
                item(key = "ai_plan_history_header") {
                    GfOverline(
                        text = "PREVIOUSLY GENERATED PLANS",
                        modifier = Modifier.padding(top = Dimens.sm)
                    )
                }

                items(uiState.mealPlanHistory, key = { "history_${it.id}" }) { entry ->
                    val isExpanded = expandedHistoryId == entry.id
                    val dateLabel = remember(entry.timestampMs) {
                        historyDateFormat.format(Date(entry.timestampMs))
                    }
                    val allMealsLogged = entry.plan.meals.isNotEmpty() &&
                        entry.plan.meals.all { isGeneratedMealLogged(it, uiState.todayLogs) }
                    val anyMealLogged = entry.plan.meals.any { isGeneratedMealLogged(it, uiState.todayLogs) }

                    GfCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            expandedHistoryId = if (isExpanded) null else entry.id
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = Dimens.sm)
                            ) {
                                Text(
                                    text = "$dateLabel • ${entry.goal}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GetFitTheme.colors.accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = entry.pantryItems,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GetFitTheme.colors.textPrimary,
                                    maxLines = if (isExpanded) 3 else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${entry.plan.totalCalories} kcal • ${entry.plan.totalProtein}g P • ${entry.plan.totalCarbs}g C • ${entry.plan.totalFat}g F",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GetFitTheme.colors.textSecondary
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse plan" else "Expand plan",
                                tint = GetFitTheme.colors.textSecondary
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(Dimens.md))
                            Text(
                                text = entry.plan.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GetFitTheme.colors.textSecondary
                            )
                            if (entry.plan.meals.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(Dimens.md))
                                GfPrimaryButton(
                                    text = if (allMealsLogged) "All Meals Logged" else "Log Entire Plan to Diary",
                                    icon = if (allMealsLogged) Icons.Rounded.Check else Icons.Rounded.Add,
                                    onClick = {
                                        if (anyMealLogged) {
                                            pendingBulkLogMeals = entry.plan.meals
                                        } else {
                                            onLogAllGeneratedMeals(entry.plan.meals)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(Dimens.md))
                                Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                                    entry.plan.meals.forEach { meal ->
                                        val isLogged = isGeneratedMealLogged(meal, uiState.todayLogs)
                                        GeneratedMealCard(
                                            meal = meal,
                                            isLogged = isLogged,
                                            onLogClick = {
                                                if (isLogged) duplicateMealToConfirm = meal else onLogGeneratedMeal(meal)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratedMealCard(
    meal: GeminiNutritionService.GeneratedMeal,
    isLogged: Boolean,
    onLogClick: () -> Unit
) {
    GfCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Dimens.sm)
            ) {
                GfOverline(text = meal.mealType.uppercase())
                Text(
                    text = meal.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GetFitTheme.colors.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLogged) {
                GfStatusPill(text = "Logged")
            } else {
                GfSecondaryButton(
                    text = "Log to Diary",
                    onClick = onLogClick,
                    icon = Icons.Rounded.Add,
                    compact = true
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.xs))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = GetFitTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)) {
                    append("${meal.calories}")
                }
                append(" kcal · P ${meal.protein}g · C ${meal.carbs}g · F ${meal.fat}g")
            },
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = GetFitTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(Dimens.sm))
        Column(modifier = Modifier.alpha(if (isLogged) 0.6f else 1f)) {
            meal.items.forEach { foodDesc ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(GetFitTheme.colors.textTertiary)
                    )
                    Text(
                        text = foodDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GetFitTheme.colors.textSecondary
                    )
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
        GfCard(
            cornerRadius = Dimens.xl,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Add Calories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = GetFitTheme.colors.textPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = GetFitTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.xs))
            Text(
                text = "Quickly log raw calories and macros into your food diary without searching:",
                style = MaterialTheme.typography.bodySmall,
                color = GetFitTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(Dimens.md))
            GfTextField(
                value = name,
                onValueChange = { name = it },
                label = "ENTRY NAME (OPTIONAL)",
                placeholder = "e.g. Homemade Protein Shake",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.sm))
            GfTextField(
                value = caloriesText,
                onValueChange = { caloriesText = it },
                label = "CALORIES (KCAL) *",
                placeholder = "0",
                suffix = "kcal",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                GfTextField(
                    value = proteinText,
                    onValueChange = { proteinText = it },
                    label = "PROTEIN",
                    placeholder = "0",
                    suffix = "g",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                GfTextField(
                    value = carbsText,
                    onValueChange = { carbsText = it },
                    label = "CARBS",
                    placeholder = "0",
                    suffix = "g",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                GfTextField(
                    value = fatText,
                    onValueChange = { fatText = it },
                    label = "FAT",
                    placeholder = "0",
                    suffix = "g",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.md))
            GfOverline(text = "ASSIGN TO MEAL")
            Spacer(modifier = Modifier.height(Dimens.xs))
            GfChipRow(
                items = mealTypes,
                selectedItem = selectedMealType,
                onItemSelected = { selectedMealType = it },
                contentPadding = PaddingValues(0.dp)
            )

            Spacer(modifier = Modifier.height(Dimens.lg))
            val cals = caloriesText.toDoubleOrNull() ?: 0.0
            GfPrimaryButton(
                text = "Log to Diary",
                enabled = cals > 0.0,
                onClick = {
                    val p = proteinText.toDoubleOrNull() ?: 0.0
                    val c = carbsText.toDoubleOrNull() ?: 0.0
                    val f = fatText.toDoubleOrNull() ?: 0.0
                    if (cals > 0) {
                        onConfirm(name, cals, p, c, f, selectedMealType)
                    }
                }
            )
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
        GfCard(
            cornerRadius = Dimens.xl,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.lg)
        ) {
            // Header with Month Navigation (< Month Year >) and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = GetFitTheme.colors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Select Diary Date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GetFitTheme.colors.textPrimary
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
                                    tint = GetFitTheme.colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GetFitTheme.colors.accent
                            )
                            IconButton(
                                onClick = { monthOffset++ },
                                enabled = monthOffset < 0,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next Month",
                                    tint = if (monthOffset < 0) GetFitTheme.colors.textSecondary else GetFitTheme.colors.textTertiary.copy(alpha = 0.25f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (monthOffset != 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(Dimens.xs))
                                        .background(GetFitTheme.colors.surface2)
                                        .pressable { monthOffset = 0 }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Today",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GetFitTheme.colors.textSecondary
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
                        tint = GetFitTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.md))

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
                            color = GetFitTheme.colors.textTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.xs))

            // Fixed 6-week grid ensuring constant size across all months
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.xs)
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
                                        .clip(RoundedCornerShape(Dimens.sm))
                                        .background(
                                            when {
                                                isSelected -> GetFitTheme.colors.accent
                                                isToday -> GetFitTheme.colors.surface2
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.dp else if (isToday) 1.dp else 0.dp,
                                            color = if (isSelected) GetFitTheme.colors.accent else if (isToday) GetFitTheme.colors.hairline else Color.Transparent,
                                            shape = RoundedCornerShape(Dimens.sm)
                                        )
                                        .pressable(enabled = !isFuture) {
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
                                                isFuture -> GetFitTheme.colors.textTertiary.copy(alpha = 0.3f)
                                                isSelected -> GetFitTheme.colors.onAccent
                                                isToday -> GetFitTheme.colors.accent
                                                else -> GetFitTheme.colors.textPrimary
                                            }
                                        )
                                        if (isLogged) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) GetFitTheme.colors.onAccent else GetFitTheme.colors.success)
                                            )
                                        }
                                    }
                                }
                            } else {
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

            Spacer(modifier = Modifier.height(Dimens.md))

            // Legend / Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
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
                        color = GetFitTheme.colors.textSecondary
                    )
                }

                if (selectedDate != todayStr) {
                    GfTextButton(
                        text = "Jump to Today",
                        onClick = { onDateSelected(todayStr) },
                        color = GetFitTheme.colors.accent
                    )
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

    val selectedCount = remember(summaries, selectedMeals.toMap()) {
        summaries.filter { selectedMeals[it.mealType] == true }.sumOf { it.itemsCount }
    }

    Dialog(onDismissRequest = onDismiss) {
        GfCard(
            cornerRadius = Dimens.xl,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.lg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GetFitTheme.colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = GetFitTheme.colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Copy Yesterday's Meals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            color = GetFitTheme.colors.textPrimary
                        )
                        Text(
                            text = "To $currentDateLabel",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            softWrap = false,
                            color = GetFitTheme.colors.textSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = GetFitTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.md))

            if (totalAvailableItems == 0) {
                // Empty state when yesterday had no food logged
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.md))
                        .background(GetFitTheme.colors.surface2)
                        .padding(Dimens.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.xs)
                    ) {
                        Text(
                            text = "No Meals Logged Yesterday",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            softWrap = false,
                            color = GetFitTheme.colors.textPrimary
                        )
                        Text(
                            text = "Yesterday has no recorded meals to copy. Log food today to easily duplicate it tomorrow!",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = GetFitTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.md))

                GfPrimaryButton(
                    text = "Got It",
                    onClick = onDismiss
                )
            } else {
                // Quick Action Toggle: Select All / Deselect All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose meals to copy:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GetFitTheme.colors.textSecondary,
                        maxLines = 1,
                        softWrap = false
                    )

                    val allSelected = remember(summaries, selectedMeals.toMap()) {
                        summaries.filter { it.itemsCount > 0 }.all { selectedMeals[it.mealType] == true }
                    }
                    GfTextButton(
                        text = if (allSelected) "Deselect All" else "Select All",
                        onClick = {
                            val target = !allSelected
                            summaries.forEach {
                                if (it.itemsCount > 0) {
                                    selectedMeals[it.mealType] = target
                                }
                            }
                        },
                        color = GetFitTheme.colors.accent
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.sm))

                // Checkbox Meal Cards (Breakfast, Lunch, Dinner, Snacks)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    summaries.forEach { summary ->
                        val isAvailable = summary.itemsCount > 0
                        val isChecked = selectedMeals[summary.mealType] == true

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Dimens.md))
                                .background(if (isChecked && isAvailable) GetFitTheme.colors.surface2 else GetFitTheme.colors.surface1)
                                .border(
                                    1.dp,
                                    if (isChecked && isAvailable) GetFitTheme.colors.accent.copy(alpha = 0.45f) else GetFitTheme.colors.hairline,
                                    RoundedCornerShape(Dimens.md)
                                )
                                .pressable(enabled = isAvailable) {
                                    selectedMeals[summary.mealType] = !isChecked
                                }
                                .padding(horizontal = Dimens.md, vertical = Dimens.sm)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isChecked && isAvailable,
                                        onCheckedChange = if (isAvailable) { isCheckedNew -> selectedMeals[summary.mealType] = isCheckedNew } else null,
                                        enabled = isAvailable,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = GetFitTheme.colors.accent,
                                            checkmarkColor = GetFitTheme.colors.onAccent,
                                            uncheckedColor = GetFitTheme.colors.hairline
                                        )
                                    )

                                    Column {
                                        Text(
                                            text = summary.mealType,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = if (isAvailable) GetFitTheme.colors.textPrimary else GetFitTheme.colors.textTertiary
                                        )
                                        Text(
                                            text = if (isAvailable) {
                                                "${summary.itemsCount} item${if (summary.itemsCount > 1) "s" else ""} • ${summary.totalCalories} kcal"
                                            } else {
                                                "No items logged yesterday"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = if (isAvailable) GetFitTheme.colors.textSecondary else GetFitTheme.colors.textTertiary.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.md))

                // Action Button (Copy Selected)
                GfPrimaryButton(
                    text = if (selectedCount > 0) "Copy ($selectedCount)" else "Copy Selected",
                    onClick = {
                        val chosen = summaries
                            .filter { selectedMeals[it.mealType] == true && it.itemsCount > 0 }
                            .map { it.mealType }
                            .toSet()
                        onConfirmCopy(chosen)
                    },
                    enabled = selectedCount > 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EditWaterTargetDialog(
    currentTarget: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var targetText by remember { mutableStateOf(currentTarget.toString()) }
    val parsed = targetText.toIntOrNull()
    val isValid = parsed != null && parsed in 500..10000

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GetFitTheme.colors.surface1,
        titleContentColor = GetFitTheme.colors.textPrimary,
        textContentColor = GetFitTheme.colors.textSecondary,
        title = { Text("Daily Hydration Target", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                Text(
                    text = "Set your daily water goal in ml.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GetFitTheme.colors.textSecondary
                )
                GfTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { ch -> ch.isDigit() } },
                    label = "Target (ml)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            GfTextButton(
                text = "Save Target",
                enabled = isValid,
                onClick = {
                    if (parsed != null) onConfirm(parsed)
                }
            )
        }
    )
}

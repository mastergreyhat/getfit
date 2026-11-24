package com.getfit.ui.screens

import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.getfit.data.PlannedExercise
import com.getfit.data.entity.Exercise
import com.getfit.data.entity.StopwatchRecord
import com.getfit.ui.components.AddCustomExerciseDialog
import com.getfit.ui.components.ExerciseCard
import com.getfit.ui.components.ModifyPastDateDialog
import com.getfit.ui.components.StopwatchDialog
import com.getfit.ui.components.StrengthCurveChart
import com.getfit.ui.components.StrengthCurvePoint
import com.getfit.ui.components.core.GfBigNumber
import com.getfit.ui.components.core.GfCard
import com.getfit.ui.components.core.GfCheckBadge
import com.getfit.ui.components.core.GfChip
import com.getfit.ui.components.core.GfChipRow
import com.getfit.ui.components.core.GfHeroCard
import com.getfit.ui.components.core.GfStatusPill
import com.getfit.ui.components.core.GfPrimaryButton
import com.getfit.ui.components.core.GfSecondaryButton
import com.getfit.ui.components.core.GfSectionHeader
import com.getfit.ui.components.core.GfSegmentedControl
import com.getfit.ui.components.core.GfTextButton
import com.getfit.ui.components.core.GfTextField
import com.getfit.ui.components.core.gfBottomBarPadding
import com.getfit.ui.components.core.pressable
import com.getfit.ui.util.AppDateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.getfit.ui.viewmodel.WorkoutViewModel

@Composable
fun PressableCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) GetFitTheme.colors.surface3 else GetFitTheme.colors.surface2,
        animationSpec = tween(durationMillis = 200),
        label = "cat_chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) GetFitTheme.colors.accent else GetFitTheme.colors.textSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "cat_chip_text"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) GetFitTheme.colors.accent.copy(alpha = 0.45f) else GetFitTheme.colors.hairline,
        animationSpec = tween(durationMillis = 200),
        label = "cat_chip_border"
    )

    Box(
        modifier = modifier
            .pressable(onClick = onClick, onLongClick = onLongClick)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .padding(horizontal = Dimens.lg, vertical = Dimens.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onNavigateToWorkoutDetail: (exerciseId: Long, exerciseName: String, muscleGroup: String, sets: Int, reps: String, dayName: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.currentTab.collectAsState()
    val tabs = listOf("Workout", "All Exercises", "Progress & PBs")

    var showStopwatch by remember { mutableStateOf(false) }
    var stopwatchMillis by remember { mutableLongStateOf(0L) }
    var isStopwatchRunning by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isStopwatchRunning) {
        if (isStopwatchRunning) {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                kotlinx.coroutines.delay(16L)
                val now = System.currentTimeMillis()
                stopwatchMillis += (now - lastTime)
                lastTime = now
            }
        }
    }

    var showAddCustomExerciseDialog by remember { mutableStateOf(false) }
    var customExerciseInitialName by remember { mutableStateOf("") }

    // Today's Workout Customization States (Temporary space)
    var showAddExerciseToTodayDialog by remember { mutableStateOf(false) }
    var plannedExerciseToEditInToday by remember { mutableStateOf<PlannedExercise?>(null) }
    var showFeedbackSnackbar by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }

    LaunchedEffect(showFeedbackSnackbar, feedbackMessage) {
        if (showFeedbackSnackbar) {
            delay(1800)
            showFeedbackSnackbar = false
        }
    }

    // Exercise & Category Management States (Press & Hold for Master Dataset)
    var selectedExerciseToManage by remember { mutableStateOf<Exercise?>(null) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }

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
            dateLabel = uiState.dateDisplayLabel,
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

    var categoryToManage by remember { mutableStateOf<String?>(null) }
    var categoryToRename by remember { mutableStateOf<String?>(null) }
    var renameCategoryInput by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    if (showStopwatch) {
        StopwatchDialog(
            elapsedMillis = stopwatchMillis,
            isRunning = isStopwatchRunning,
            onToggleRunning = { isStopwatchRunning = !isStopwatchRunning },
            onReset = {
                stopwatchMillis = 0L
                isStopwatchRunning = false
            },
            onDismiss = {
                isStopwatchRunning = false
                stopwatchMillis = 0L
                showStopwatch = false
            },
            savedRecords = uiState.stopwatchRecords,
            onSaveRecord = viewModel::saveStopwatchRecord,
            onDeleteRecord = viewModel::deleteStopwatchRecord
        )
    }

    if (showDatePickerDialog) {
        WorkoutDatePickerDialog(
            selectedDate = uiState.selectedDate,
            workoutDates = uiState.workoutDates,
            onDateSelected = { dateStr ->
                viewModel.selectDate(dateStr)
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    val muscleGroups = remember(uiState.exercises) {
        val list = mutableListOf("All")
        list.addAll(uiState.exercises.map { it.muscleGroup }.distinct().sorted())
        list
    }

    if (showAddCustomExerciseDialog) {
        AddCustomExerciseDialog(
            initialName = customExerciseInitialName,
            existingCategories = muscleGroups,
            existingExerciseNames = uiState.exercises.map { it.name },
            onDismiss = {
                showAddCustomExerciseDialog = false
                customExerciseInitialName = ""
            },
            onConfirm = { name, category, equipment, isCompound, description ->
                viewModel.addCustomExercise(
                    name = name,
                    muscleGroup = category,
                    equipment = equipment,
                    description = description,
                    isCompound = isCompound
                )
                showAddCustomExerciseDialog = false
                customExerciseInitialName = ""
            }
        )
    }

    // Exercise Action Dialog (Modify or Delete)
    if (selectedExerciseToManage != null) {
        val ex = selectedExerciseToManage!!
        AlertDialog(
            onDismissRequest = { selectedExerciseToManage = null },
            containerColor = GetFitTheme.colors.surface1,
            titleContentColor = GetFitTheme.colors.textPrimary,
            textContentColor = GetFitTheme.colors.textSecondary,
            title = { Text(ex.name, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Text(
                        text = "${ex.muscleGroup} • ${ex.equipment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GetFitTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(Dimens.xs))
                    Text(
                        text = "Select an action for this master catalog exercise:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GetFitTheme.colors.textSecondary
                    )
                }
            },
            confirmButton = {
                GfSecondaryButton(
                    text = "Modify Exercise",
                    icon = Icons.Default.Edit,
                    compact = true,
                    onClick = {
                        exerciseToEdit = ex
                        selectedExerciseToManage = null
                    }
                )
            },
            dismissButton = {
                GfTextButton(
                    text = "Delete from Catalog",
                    color = GetFitTheme.colors.danger,
                    onClick = {
                        exerciseToDelete = ex
                        selectedExerciseToManage = null
                    }
                )
            }
        )
    }

    // Edit Exercise Dialog
    if (exerciseToEdit != null) {
        val ex = exerciseToEdit!!
        AddCustomExerciseDialog(
            initialName = ex.name,
            initialCategory = ex.muscleGroup,
            initialEquipment = ex.equipment,
            initialIsCompound = ex.isCompound,
            initialDescription = ex.description,
            title = "Modify Exercise",
            confirmButtonText = "Save Changes",
            existingCategories = muscleGroups,
            existingExerciseNames = uiState.exercises.map { it.name },
            onDismiss = { exerciseToEdit = null },
            onConfirm = { name, category, equipment, isCompound, description ->
                viewModel.updateExercise(
                    exerciseId = ex.id,
                    name = name,
                    muscleGroup = category,
                    equipment = equipment,
                    description = description,
                    isCompound = isCompound
                )
                feedbackMessage = "Updated \"$name\" in Master Catalog"
                showFeedbackSnackbar = true
                exerciseToEdit = null
            }
        )
    }

    // Delete Exercise Confirmation Dialog (Master Dataset -> Trash)
    if (exerciseToDelete != null) {
        val ex = exerciseToDelete!!
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            containerColor = GetFitTheme.colors.surface1,
            titleContentColor = GetFitTheme.colors.textPrimary,
            textContentColor = GetFitTheme.colors.textSecondary,
            title = { Text("Delete from Master Catalog?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${ex.name}\"? It will be removed from your master exercise library and moved to Trash (where it can be restored within 10 days from Settings).",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                GfTextButton(
                    text = "Move to Trash",
                    color = GetFitTheme.colors.danger,
                    onClick = {
                        viewModel.deleteExercise(ex)
                        feedbackMessage = "\"${ex.name}\" moved to Trash"
                        showFeedbackSnackbar = true
                        exerciseToDelete = null
                    }
                )
            }
        )
    }

    // Category Action Dialog (Rename or Delete Category)
    if (categoryToManage != null) {
        val cat = categoryToManage!!
        AlertDialog(
            onDismissRequest = { categoryToManage = null },
            containerColor = GetFitTheme.colors.surface1,
            titleContentColor = GetFitTheme.colors.textPrimary,
            textContentColor = GetFitTheme.colors.textSecondary,
            title = { Text("Manage \"$cat\"", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text("Choose an action for this body part category:", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                GfSecondaryButton(
                    text = "Rename Category",
                    icon = Icons.Default.Edit,
                    compact = true,
                    onClick = {
                        categoryToRename = cat
                        renameCategoryInput = cat
                        categoryToManage = null
                    }
                )
            },
            dismissButton = {
                GfTextButton(
                    text = "Delete",
                    color = GetFitTheme.colors.danger,
                    onClick = {
                        categoryToDelete = cat
                        categoryToManage = null
                    }
                )
            }
        )
    }

    // Rename Category Dialog
    if (categoryToRename != null) {
        val oldCat = categoryToRename!!
        val isDuplicateCat = remember(renameCategoryInput, muscleGroups, oldCat) {
            val trimmed = renameCategoryInput.trim()
            trimmed.isNotBlank() &&
                !trimmed.equals(oldCat.trim(), ignoreCase = true) &&
                muscleGroups.any { it.trim().equals(trimmed, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { categoryToRename = null },
            containerColor = GetFitTheme.colors.surface1,
            titleContentColor = GetFitTheme.colors.textPrimary,
            textContentColor = GetFitTheme.colors.textSecondary,
            title = { Text("Rename Category", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Text("Enter new name for \"$oldCat\":", style = MaterialTheme.typography.bodyMedium)
                    GfTextField(
                        value = renameCategoryInput,
                        onValueChange = { renameCategoryInput = it },
                        label = "Category Name",
                        isError = isDuplicateCat,
                        supportingText = if (isDuplicateCat) "Category \"${renameCategoryInput.trim()}\" already exists." else null,
                        imeAction = ImeAction.Done,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                                    if (renameCategoryInput.isNotEmpty()) {
                                        renameCategoryInput = renameCategoryInput.dropLast(1)
                                        return@onPreviewKeyEvent true
                                    }
                                }
                                false
                            }
                    )
                }
            },
            confirmButton = {
                GfTextButton(
                    text = "Save",
                    enabled = renameCategoryInput.isNotBlank() && !isDuplicateCat,
                    onClick = {
                        if (renameCategoryInput.isNotBlank() && !isDuplicateCat) {
                            viewModel.renameMuscleGroup(oldCat, renameCategoryInput.trim())
                            categoryToRename = null
                        }
                    }
                )
            }
        )
    }

    // Delete Category Confirmation Dialog
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = GetFitTheme.colors.surface1,
            titleContentColor = GetFitTheme.colors.textPrimary,
            textContentColor = GetFitTheme.colors.textSecondary,
            title = { Text("Delete Category \"$cat\"?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "Are you sure you want to delete the \"$cat\" category and all exercises in it? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                GfTextButton(
                    text = "Delete Category",
                    color = GetFitTheme.colors.danger,
                    onClick = {
                        viewModel.deleteMuscleGroup(cat)
                        categoryToDelete = null
                    }
                )
            }
        )
    }

    val currentSelectedDay = uiState.currentPlan.days.getOrNull(uiState.selectedDayIndex) ?: uiState.currentPlan.days.firstOrNull()

    if (showAddExerciseToTodayDialog && currentSelectedDay != null) {
        AddExerciseToTodayDialog(
            dayName = currentSelectedDay.dayName,
            allExercises = uiState.exercises,
            existingPlannedNames = currentSelectedDay.exercises.map { it.exerciseName.trim().lowercase() }.toSet(),
            muscleGroups = muscleGroups,
            onDismiss = { showAddExerciseToTodayDialog = false },
            onConfirm = { name, sets, reps, notes ->
                viewModel.addExerciseToTodayWorkout(name, sets, reps, notes)
                feedbackMessage = "\"$name\" added to ${currentSelectedDay.dayName}"
                showFeedbackSnackbar = true
                showAddExerciseToTodayDialog = false
            }
        )
    }

    if (plannedExerciseToEditInToday != null && currentSelectedDay != null) {
        val planned = plannedExerciseToEditInToday!!
        ModifyTodayExerciseDialog(
            exerciseName = planned.exerciseName,
            dayName = currentSelectedDay.dayName,
            initialSets = planned.sets,
            initialReps = planned.reps,
            initialNotes = planned.notes,
            onDismiss = { plannedExerciseToEditInToday = null },
            onConfirm = { sets, reps, notes ->
                viewModel.modifyExerciseInTodayWorkout(planned.exerciseName, sets, reps, notes)
                feedbackMessage = "Updated \"${planned.exerciseName}\" in ${currentSelectedDay.dayName}"
                showFeedbackSnackbar = true
                plannedExerciseToEditInToday = null
            },
            onRemoveFromToday = {
                viewModel.removeExerciseFromTodayWorkout(planned.exerciseName)
                feedbackMessage = "\"${planned.exerciseName}\" moved to Trash"
                showFeedbackSnackbar = true
                plannedExerciseToEditInToday = null
            }
        )
    }

    Scaffold(
        containerColor = GetFitTheme.colors.background,
        snackbarHost = {
            AnimatedVisibility(
                visible = showFeedbackSnackbar,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .gfBottomBarPadding()
                        .padding(horizontal = Dimens.screenGutter, vertical = Dimens.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(Dimens.xl),
                        color = GetFitTheme.colors.surface3,
                        contentColor = GetFitTheme.colors.textPrimary,
                        border = BorderStroke(1.dp, GetFitTheme.colors.hairline),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = feedbackMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = Dimens.lg, vertical = Dimens.sm)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showStopwatch = true },
                    containerColor = GetFitTheme.colors.accent,
                    contentColor = GetFitTheme.colors.onAccent,
                    shape = CircleShape,
                    modifier = Modifier.gfBottomBarPadding()
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Stopwatch")
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
                    .padding(horizontal = Dimens.screenGutter, vertical = Dimens.sm)
            ) {
                GfSegmentedControl(
                    items = tabs,
                    selectedIndex = selectedTab,
                    onSelect = viewModel::selectTab
                )
            }

            when (selectedTab) {
                0 -> PrescribedWorkoutTab(
                    uiState = uiState,
                    onPreviousDate = viewModel::previousDate,
                    onNextDate = viewModel::nextDate,
                    onOpenCalendarPicker = { showDatePickerDialog = true },
                    onSelectDay = viewModel::selectDay,
                    onStartExercise = { exerciseId, name, muscle, sets, reps, dayName ->
                        checkPastDateAndRun {
                            onNavigateToWorkoutDetail(exerciseId, name, muscle, sets, reps, dayName)
                        }
                    },
                    onAddExerciseToToday = {
                        checkPastDateAndRun {
                            showAddExerciseToTodayDialog = true
                        }
                    },
                    onRemoveExerciseFromToday = { plannedEx ->
                        val dayName = uiState.currentPlan.days.getOrNull(uiState.selectedDayIndex)?.dayName ?: "Workout"
                        viewModel.removeExerciseFromTodayWorkout(plannedEx.exerciseName)
                        feedbackMessage = "\"${plannedEx.exerciseName}\" removed from $dayName"
                        showFeedbackSnackbar = true
                    },
                    onModifyExerciseInToday = { plannedEx ->
                        plannedExerciseToEditInToday = plannedEx
                    }
                )
                1 -> AllExercisesTab(
                    uiState = uiState,
                    onSelectExercise = { ex ->
                        checkPastDateAndRun {
                            onNavigateToWorkoutDetail(
                                ex.id,
                                ex.name,
                                ex.muscleGroup,
                                4,
                                "8-10",
                                uiState.currentPlan.days.getOrNull(uiState.selectedDayIndex)?.dayName ?: "Workout"
                            )
                        }
                    },
                    onOpenAddCustomExercise = {
                        customExerciseInitialName = ""
                        showAddCustomExerciseDialog = true
                    },
                    onManageExercise = { ex ->
                        selectedExerciseToManage = ex
                    },
                    onManageCategory = { cat ->
                        categoryToManage = cat
                    }
                )
                2 -> WorkoutProgressTab(
                    uiState = uiState,
                    onDeleteStopwatchRecord = viewModel::deleteStopwatchRecord
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrescribedWorkoutTab(
    uiState: com.getfit.ui.viewmodel.WorkoutUiState,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onOpenCalendarPicker: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onStartExercise: (Long, String, String, Int, String, String) -> Unit,
    onAddExerciseToToday: () -> Unit,
    onRemoveExerciseFromToday: (PlannedExercise) -> Unit,
    onModifyExerciseInToday: (PlannedExercise) -> Unit
) {
    val plan = uiState.currentPlan
    val selectedDay = plan.days.getOrNull(uiState.selectedDayIndex) ?: plan.days.firstOrNull()

    val exerciseMap = remember(uiState.exercises) {
        uiState.exercises.associateBy { it.name.lowercase().trim() }
    }
    val completedExerciseIds = remember(uiState.todayLogs) {
        uiState.todayLogs.map { it.exerciseId }.toSet()
    }
    val completedExerciseNames = remember(uiState.todayLogs) {
        uiState.todayLogs.map { it.exerciseName.lowercase().trim() }.toSet()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.md),
        contentPadding = PaddingValues(
            start = Dimens.screenGutter,
            end = Dimens.screenGutter,
            top = Dimens.md,
            bottom = com.getfit.ui.components.core.gfBottomBarContentPadding() + Dimens.lg
        )
    ) {
        // 1. Date Navigation Bar (Ties Training to Date)
        item(key = "training_date_nav") {
            GfCard(
                cornerRadius = Dimens.lg,
                contentPadding = PaddingValues(horizontal = Dimens.sm, vertical = Dimens.xs),
                useGradient = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousDate) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = GetFitTheme.colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.sm))
                            .pressable(onClick = onOpenCalendarPicker)
                            .padding(horizontal = Dimens.sm, vertical = Dimens.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick Date",
                            tint = GetFitTheme.colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = uiState.dateDisplayLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            color = GetFitTheme.colors.textPrimary
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
                            tint = if (AppDateManager.canGoNext()) GetFitTheme.colors.textPrimary else GetFitTheme.colors.textTertiary
                        )
                    }
                }
            }
        }

        // 2. Plan info header with Add Exercise button
        item(key = "training_plan_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = Dimens.sm)) {
                    Text(
                        text = "${plan.name} (${plan.daysPerWeek} Days/Week)",
                        style = MaterialTheme.typography.titleMedium,
                        color = GetFitTheme.colors.textPrimary
                    )
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = GetFitTheme.colors.textSecondary
                    )
                }
                GfSecondaryButton(
                    text = "Add Exercise",
                    icon = Icons.Default.Add,
                    compact = true,
                    onClick = onAddExerciseToToday
                )
            }
        }

        // 3. Day Selector Chips
        item(key = "training_day_selector") {
            GfChipRow(
                items = plan.days,
                selectedItem = selectedDay,
                onItemSelected = { day ->
                    val index = plan.days.indexOf(day)
                    if (index >= 0) onSelectDay(index)
                },
                label = { it.dayName },
                contentPadding = PaddingValues(0.dp)
            )
        }

        // 4. Planned Exercises List
        if (selectedDay != null) {
            itemsIndexed(
                items = selectedDay.exercises,
                key = { idx, plannedEx -> "plan_ex_${plannedEx.exerciseName}_$idx" }
            ) { _, plannedEx ->
                val matchedExercise = remember(plannedEx, exerciseMap) {
                    exerciseMap[plannedEx.exerciseName.lowercase().trim()] ?: Exercise(
                        id = plannedEx.exerciseName.hashCode().toLong(),
                        name = plannedEx.exerciseName,
                        muscleGroup = selectedDay.muscleGroups,
                        equipment = "Gym",
                        description = plannedEx.notes,
                        isCompound = true
                    )
                }

                val isCompleted = matchedExercise.id in completedExerciseIds ||
                        matchedExercise.name.lowercase().trim() in completedExerciseNames ||
                        plannedEx.exerciseName.lowercase().trim() in completedExerciseNames

                val overload = uiState.overloadSuggestions[matchedExercise.id]

                ExerciseCard(
                    exercise = matchedExercise,
                    targetSetsReps = "${plannedEx.sets} sets × ${plannedEx.reps} reps",
                    suggestedWeight = overload?.suggestedWeight,
                    suggestedIncrease = overload?.suggestedIncrease,
                    isCompleted = isCompleted,
                    onClick = {
                        onStartExercise(
                            matchedExercise.id,
                            matchedExercise.name,
                            matchedExercise.muscleGroup,
                            plannedEx.sets,
                            plannedEx.reps,
                            selectedDay.dayName
                        )
                    },
                    onLongClick = {
                        onModifyExerciseInToday(plannedEx)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllExercisesTab(
    uiState: com.getfit.ui.viewmodel.WorkoutUiState,
    onSelectExercise: (Exercise) -> Unit,
    onOpenAddCustomExercise: () -> Unit,
    onManageExercise: (Exercise) -> Unit,
    onManageCategory: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf("All") }

    val muscleGroups = remember(uiState.exercises) {
        val set = mutableListOf("All")
        set.addAll(uiState.exercises.map { it.muscleGroup }.distinct().sorted())
        set
    }

    val filteredExercises = remember(searchQuery, selectedMuscle, uiState.exercises) {
        uiState.exercises.filter { ex ->
            val matchQuery = searchQuery.isBlank() || ex.name.contains(searchQuery, ignoreCase = true)
            val matchMuscle = selectedMuscle == "All" || ex.muscleGroup.equals(selectedMuscle, ignoreCase = true)
            matchQuery && matchMuscle
        }
    }

    val completedExerciseIds = remember(uiState.todayLogs) {
        uiState.todayLogs.map { it.exerciseId }.toSet()
    }
    val completedExerciseNames = remember(uiState.todayLogs) {
        uiState.todayLogs.map { it.exerciseName.lowercase().trim() }.toSet()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.md),
        contentPadding = PaddingValues(
            start = Dimens.screenGutter,
            end = Dimens.screenGutter,
            top = Dimens.md,
            bottom = com.getfit.ui.components.core.gfBottomBarContentPadding() + Dimens.lg
        )
    ) {
        item(key = "all_ex_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = Dimens.sm)) {
                    Text(
                        text = "${filteredExercises.size} Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        color = GetFitTheme.colors.textPrimary
                    )
                    Text(
                        text = "Press & hold to edit",
                        style = MaterialTheme.typography.bodySmall,
                        color = GetFitTheme.colors.textSecondary
                    )
                }
                GfSecondaryButton(
                    text = "Create Exercise",
                    icon = Icons.Default.Add,
                    compact = true,
                    onClick = onOpenAddCustomExercise
                )
            }
        }

        item(key = "all_ex_search") {
            GfTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search 60+ exercises...",
                leadingIcon = Icons.Default.Search,
                imeAction = ImeAction.Search,
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = GetFitTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = searchQuery.dropLast(1)
                                return@onPreviewKeyEvent true
                            }
                        }
                        false
                    }
            )
        }

        item(key = "all_ex_categories") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(muscleGroups, key = { it }) { group ->
                    PressableCategoryChip(
                        label = group,
                        isSelected = selectedMuscle == group,
                        onClick = { selectedMuscle = group },
                        onLongClick = if (group != "All") { { onManageCategory(group) } } else null
                    )
                }
            }
        }

        if (filteredExercises.isEmpty()) {
            item(key = "all_ex_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.xxxl),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.md)
                    ) {
                        Text(
                            text = "No exercises found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GetFitTheme.colors.textSecondary
                        )
                        GfSecondaryButton(
                            text = "Add Custom Exercise",
                            icon = Icons.Default.Add,
                            compact = true,
                            onClick = onOpenAddCustomExercise
                        )
                    }
                }
            }
        } else {
            itemsIndexed(filteredExercises, key = { idx, exercise -> "filtered_ex_${exercise.id}_$idx" }) { _, exercise ->
                val overload = uiState.overloadSuggestions[exercise.id]
                val isCompleted = exercise.id in completedExerciseIds ||
                        exercise.name.lowercase().trim() in completedExerciseNames
                ExerciseCard(
                    exercise = exercise,
                    suggestedWeight = overload?.suggestedWeight,
                    suggestedIncrease = overload?.suggestedIncrease,
                    isCompleted = isCompleted,
                    onClick = { onSelectExercise(exercise) },
                    onLongClick = { onManageExercise(exercise) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutProgressTab(
    uiState: com.getfit.ui.viewmodel.WorkoutUiState,
    onDeleteStopwatchRecord: (StopwatchRecord) -> Unit = {}
) {
    // Group logs by exercise for strength curve
    val exercisesWithHistory = remember(uiState.recentLogs) {
        uiState.recentLogs.map { it.exerciseName }.distinct()
    }
    var selectedExerciseForCurve by remember(exercisesWithHistory) {
        mutableStateOf(exercisesWithHistory.firstOrNull() ?: "Flat Barbell Bench Press")
    }
    var selectedDateFilter by remember { mutableStateOf<String?>(null) }

    val historyForSelected = remember(selectedExerciseForCurve, uiState.recentLogs) {
        uiState.recentLogs
            .filter { it.exerciseName.equals(selectedExerciseForCurve, ignoreCase = true) }
            .groupBy { it.date }
            .map { (date, logs) ->
                Pair(date, logs.maxOfOrNull { it.weight } ?: 0.0)
            }
            .sortedBy { it.first }
    }

    val bestTimedPerTitle = remember(uiState.stopwatchRecords) {
        uiState.stopwatchRecords.groupBy { it.title.trim().lowercase() }
            .mapValues { (_, records) -> records.maxOf { it.elapsedMillis } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.lg),
        contentPadding = PaddingValues(
            start = Dimens.screenGutter,
            end = Dimens.screenGutter,
            top = Dimens.md,
            bottom = com.getfit.ui.components.core.gfBottomBarContentPadding() + Dimens.lg
        )
    ) {
        // Hero Summary Banner (One hero per screen)
        item {
            GfHeroCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GfBigNumber(
                            value = uiState.recentLogs.map { it.date }.distinct().size,
                            color = GetFitTheme.colors.success,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Days Trained",
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textSecondary
                        )
                    }
                    Divider(
                        modifier = Modifier.height(40.dp).width(1.dp),
                        color = GetFitTheme.colors.hairline
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GfBigNumber(
                            value = uiState.personalBests.size,
                            color = GetFitTheme.colors.calories,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Personal Records",
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textSecondary
                        )
                    }
                    Divider(
                        modifier = Modifier.height(40.dp).width(1.dp),
                        color = GetFitTheme.colors.hairline
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GfBigNumber(
                            value = 85,
                            unit = "%",
                            color = GetFitTheme.colors.textPrimary,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Protein Hit Rate",
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

        // Strength Curve Analytics Section
        item {
            GfCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    Text(
                        text = "Strength Progression Curve",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GetFitTheme.colors.textPrimary
                    )

                    if (exercisesWithHistory.isNotEmpty()) {
                        GfChipRow(
                            items = exercisesWithHistory,
                            selectedItem = selectedExerciseForCurve,
                            onItemSelected = { selectedExerciseForCurve = it },
                            contentPadding = PaddingValues(0.dp)
                        )

                        if (historyForSelected.isNotEmpty()) {
                            val chartPoints = remember(historyForSelected) {
                                historyForSelected.map { (date, weight) ->
                                    StrengthCurvePoint(date = date, weight = weight)
                                }
                            }
                            StrengthCurveChart(points = chartPoints)
                        }
                    } else {
                        Text(
                            text = "Log sets in workout sessions to track your strength curves over time!",
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textSecondary
                        )
                    }
                }
            }
        }

        // Personal Bests Section
        item {
            GfSectionHeader(
                title = "Personal Records (PBs)",
                topSpacing = Dimens.sm
            )
        }

        if (uiState.personalBests.isEmpty()) {
            item {
                Text(
                    text = "No personal bests logged yet. Start a workout session to log your weights!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GetFitTheme.colors.textSecondary
                )
            }
        } else {
            itemsIndexed(uiState.personalBests.entries.toList(), key = { idx, entry -> "pb_${entry.key}_$idx" }) { _, entry ->
                val exerciseName = uiState.exercises.find { it.id == entry.key }?.name
                    ?: uiState.recentLogs.find { it.exerciseId == entry.key }?.exerciseName
                    ?: "Exercise"

                GfCard(
                    cornerRadius = Dimens.lg,
                    contentPadding = PaddingValues(Dimens.lg),
                    useGradient = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = Dimens.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.md)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = GetFitTheme.colors.calories
                            )
                            Text(
                                text = exerciseName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = GetFitTheme.colors.textPrimary
                            )
                        }
                        Text(
                            text = "${entry.value} kg",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GetFitTheme.colors.success
                        )
                    }
                }
            }
        }

        // Timed Records (Planks & Holds) Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = GetFitTheme.colors.success
                    )
                    Text(
                        text = "Timed Records (Planks & Holds)",
                        style = MaterialTheme.typography.titleLarge,
                        color = GetFitTheme.colors.textPrimary
                    )
                }
                if (uiState.stopwatchRecords.isNotEmpty()) {
                    Text(
                        text = "${uiState.stopwatchRecords.size} logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = GetFitTheme.colors.textSecondary
                    )
                }
            }
        }

        if (uiState.stopwatchRecords.isEmpty()) {
            item {
                GfCard(
                    cornerRadius = Dimens.lg,
                    contentPadding = PaddingValues(Dimens.lg),
                    useGradient = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                        Text(
                            text = "No timed records saved yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = GetFitTheme.colors.textPrimary
                        )
                        Text(
                            text = "Use the stopwatch in workouts to time and save planks, wall sits, dead hangs, and other holds!",
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textSecondary
                        )
                    }
                }
            }
        } else {
            itemsIndexed(uiState.stopwatchRecords, key = { idx, record -> "stopwatch_${record.id}_$idx" }) { _, record ->
                val isBest = bestTimedPerTitle[record.title.trim().lowercase()] == record.elapsedMillis

                GfCard(
                    cornerRadius = Dimens.lg,
                    contentPadding = PaddingValues(Dimens.lg),
                    useGradient = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = Dimens.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.md)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GetFitTheme.colors.surface2),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (isBest) GetFitTheme.colors.calories else GetFitTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                                ) {
                                    Text(
                                        text = record.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GetFitTheme.colors.textPrimary
                                    )
                                    if (isBest) {
                                        Surface(
                                            shape = RoundedCornerShape(Dimens.xs),
                                            color = GetFitTheme.colors.calories.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Best",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GetFitTheme.colors.calories,
                                                modifier = Modifier.padding(horizontal = Dimens.xs, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = record.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GetFitTheme.colors.textSecondary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                        ) {
                            Text(
                                text = record.formattedTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GetFitTheme.colors.success
                            )
                            IconButton(
                                onClick = { onDeleteStopwatchRecord(record) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete record",
                                    tint = GetFitTheme.colors.danger.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Workout History Section
        val allGroupedLogs = uiState.recentLogs.groupBy { it.date }
        val availableDates = allGroupedLogs.keys.sortedDescending()

        item {
            GfSectionHeader(
                title = "Recent Logs",
                topSpacing = Dimens.sm,
                action = if (selectedDateFilter != null) "Show All Dates" else null,
                onActionClick = { selectedDateFilter = null }
            )
        }

        if (availableDates.size > 1) {
            item {
                val filterItems = listOf<String?>(null) + availableDates
                GfChipRow(
                    items = filterItems,
                    selectedItem = selectedDateFilter,
                    onItemSelected = { date ->
                        selectedDateFilter = if (date != null && selectedDateFilter == date) null else date
                    },
                    label = { date ->
                        if (date == null) {
                            "All Dates (${availableDates.size})"
                        } else {
                            val count = allGroupedLogs[date]?.size ?: 0
                            "$date ($count)"
                        }
                    },
                    contentPadding = PaddingValues(0.dp)
                )
            }
        }

        val displayedLogs = if (selectedDateFilter != null) {
            allGroupedLogs.filterKeys { it == selectedDateFilter }
        } else {
            allGroupedLogs
        }

        if (displayedLogs.isEmpty()) {
            item {
                Text(
                    text = if (selectedDateFilter != null) "No workout logs found for $selectedDateFilter." else "No recent workout logs found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GetFitTheme.colors.textSecondary
                )
            }
        } else {
            displayedLogs.forEach { (date, logs) ->
                item(key = "summary_date_$date") {
                    val groupedByExercise = remember(logs) {
                        logs.groupBy { it.exerciseName }
                    }
                    val totalVolume = remember(logs) {
                        logs.sumOf { it.weight * it.reps }
                    }
                    val totalSets = logs.size
                    val exerciseCount = groupedByExercise.size

                    GfCard(
                        cornerRadius = Dimens.lg,
                        contentPadding = PaddingValues(Dimens.lg),
                        useGradient = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                            // Date header row with summary pill badges
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
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = AppDateManager.getDateDisplayLabel(date),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GetFitTheme.colors.textPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(Dimens.xs),
                                    color = GetFitTheme.colors.success.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, GetFitTheme.colors.success.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "$exerciseCount Ex • $totalSets Sets",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GetFitTheme.colors.success,
                                        modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 3.dp)
                                    )
                                }
                            }

                            Divider(color = GetFitTheme.colors.hairline, thickness = 1.dp)

                            // Concise list of exercises completed on this date
                            groupedByExercise.forEach { (exName, exSets) ->
                                val maxWeight = exSets.maxOfOrNull { it.weight } ?: 0.0
                                val setsSummary = remember(exSets) {
                                    val allSameWeight = exSets.all { it.weight == exSets.first().weight }
                                    val allSameReps = exSets.all { it.reps == exSets.first().reps }
                                    if (allSameWeight && allSameReps && exSets.isNotEmpty()) {
                                        "${exSets.size} sets × ${exSets.first().weight.toInt()}kg (${exSets.first().reps} reps)"
                                    } else {
                                        "${exSets.size} sets • Top ${maxWeight.toInt()}kg"
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                                        ) {
                                            GfCheckBadge(checked = true, size = 18.dp)
                                            Text(
                                                text = exName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = GetFitTheme.colors.textPrimary
                                            )
                                        }
                                        Text(
                                            text = setsSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = GetFitTheme.colors.textSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(Dimens.sm))
                                    GfStatusPill(text = "${maxWeight.toInt()}kg")
                                }
                            }

                            if (totalVolume > 0) {
                                Text(
                                    text = "Total volume lifted: ${totalVolume.toInt()} kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = GetFitTheme.colors.textSecondary,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseToTodayDialog(
    dayName: String,
    allExercises: List<Exercise>,
    existingPlannedNames: Set<String>,
    muscleGroups: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (exerciseName: String, sets: Int, reps: String, notes: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf("All") }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var setsText by remember { mutableStateOf("3") }
    var repsText by remember { mutableStateOf("8-10") }
    var notesText by remember { mutableStateOf("") }

    val filteredExercises = remember(allExercises, searchQuery, selectedMuscle) {
        val q = searchQuery.trim().lowercase()
        allExercises.filter { ex ->
            val matchQ = q.isEmpty() || ex.name.lowercase().contains(q) || ex.muscleGroup.lowercase().contains(q)
            val matchM = selectedMuscle == "All" || ex.muscleGroup.equals(selectedMuscle, ignoreCase = true)
            matchQ && matchM
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GetFitTheme.colors.surface1,
        titleContentColor = GetFitTheme.colors.textPrimary,
        textContentColor = GetFitTheme.colors.textSecondary,
        title = {
            Text(
                text = "Add Exercise to $dayName",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                Text(
                    text = "Pick an exercise from your master exercise library:",
                    style = MaterialTheme.typography.bodySmall,
                    color = GetFitTheme.colors.textSecondary
                )

                // Search Box
                GfTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search exercises...",
                    leadingIcon = Icons.Default.Search,
                    imeAction = ImeAction.Search,
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = GetFitTheme.colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Muscle category chips
                GfChipRow(
                    items = muscleGroups,
                    selectedItem = selectedMuscle,
                    onItemSelected = { selectedMuscle = it },
                    contentPadding = PaddingValues(0.dp)
                )

                // Exercise Picker List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.xs)
                ) {
                    if (filteredExercises.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Dimens.lg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matching exercises found in library.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GetFitTheme.colors.textSecondary
                                )
                            }
                        }
                    } else {
                        itemsIndexed(filteredExercises, key = { idx, ex -> "add_custom_${ex.id}_$idx" }) { _, ex ->
                            val isSelected = selectedExercise?.id == ex.id
                            val isAlreadyInDay = existingPlannedNames.contains(ex.name.trim().lowercase())
                            Surface(
                                shape = RoundedCornerShape(Dimens.sm),
                                color = if (isSelected) GetFitTheme.colors.surface3 else GetFitTheme.colors.surface2,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) GetFitTheme.colors.accent else GetFitTheme.colors.hairline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressable(onClick = { selectedExercise = ex })
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Dimens.md, vertical = Dimens.sm),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ex.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isSelected) GetFitTheme.colors.accent else GetFitTheme.colors.textPrimary
                                        )
                                        Text(
                                            text = "${ex.muscleGroup} • ${ex.equipment}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GetFitTheme.colors.textSecondary
                                        )
                                    }
                                    if (isAlreadyInDay) {
                                        Surface(
                                            shape = RoundedCornerShape(Dimens.xs),
                                            color = GetFitTheme.colors.surface1
                                        ) {
                                            Text(
                                                text = "In Routine",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = GetFitTheme.colors.textSecondary,
                                                modifier = Modifier.padding(horizontal = Dimens.xs, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Target configuration for selected exercise
                if (selectedExercise != null) {
                    Divider(color = GetFitTheme.colors.hairline)
                    Text(
                        text = "Target for \"${selectedExercise!!.name}\":",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GetFitTheme.colors.accent
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                    ) {
                        GfTextField(
                            value = setsText,
                            onValueChange = { setsText = it.filter { ch -> ch.isDigit() } },
                            label = "Sets",
                            imeAction = ImeAction.Next,
                            modifier = Modifier.weight(1f)
                        )
                        GfTextField(
                            value = repsText,
                            onValueChange = { repsText = it },
                            label = "Reps",
                            imeAction = ImeAction.Next,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    GfTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = "Notes (optional)",
                        placeholder = "e.g. Focus on full stretch",
                        imeAction = ImeAction.Done,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            GfTextButton(
                text = "Add to $dayName",
                enabled = selectedExercise != null,
                onClick = {
                    val ex = selectedExercise
                    if (ex != null) {
                        val s = setsText.toIntOrNull() ?: 3
                        val r = repsText.ifBlank { "8-10" }
                        onConfirm(ex.name, s, r, notesText)
                    }
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModifyTodayExerciseDialog(
    exerciseName: String,
    dayName: String,
    initialSets: Int,
    initialReps: String,
    initialNotes: String,
    onDismiss: () -> Unit,
    onConfirm: (sets: Int, reps: String, notes: String) -> Unit,
    onRemoveFromToday: () -> Unit
) {
    var setsText by remember { mutableStateOf(initialSets.toString()) }
    var repsText by remember { mutableStateOf(initialReps) }
    var notesText by remember { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GetFitTheme.colors.surface1,
        titleContentColor = GetFitTheme.colors.textPrimary,
        textContentColor = GetFitTheme.colors.textSecondary,
        title = {
            Text(
                text = "Edit $exerciseName",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.md)
            ) {
                Text(
                    text = "Adjust prescription for $dayName without affecting the master catalog:",
                    style = MaterialTheme.typography.bodySmall,
                    color = GetFitTheme.colors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    GfTextField(
                        value = setsText,
                        onValueChange = { setsText = it.filter { ch -> ch.isDigit() } },
                        label = "Sets",
                        imeAction = ImeAction.Next,
                        modifier = Modifier.weight(1f)
                    )
                    GfTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = "Reps",
                        imeAction = ImeAction.Next,
                        modifier = Modifier.weight(1f)
                    )
                }
                GfTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = "Notes",
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            GfTextButton(
                text = "Save Changes",
                onClick = {
                    val s = setsText.toIntOrNull() ?: 3
                    val r = repsText.ifBlank { "8-10" }
                    onConfirm(s, r, notesText)
                }
            )
        },
        dismissButton = {
            GfTextButton(
                text = "Delete",
                color = GetFitTheme.colors.danger,
                onClick = onRemoveFromToday
            )
        }
    )
}

/**
 * Historical Calendar Date Picker Dialog for Workouts & Training
 * Allows navigating across past and future months to inspect or log workouts on specific dates.
 */
@Composable
fun WorkoutDatePickerDialog(
    selectedDate: String,
    workoutDates: Set<String>,
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
            contentPadding = PaddingValues(Dimens.xl),
            useGradient = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.lg)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.lg)
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
                                text = "Select Workout Date",
                                style = MaterialTheme.typography.titleMedium,
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
                                        tint = if (monthOffset < 0) GetFitTheme.colors.textSecondary else GetFitTheme.colors.textTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (monthOffset != 0) {
                                    Spacer(modifier = Modifier.width(Dimens.xs))
                                    Surface(
                                        shape = RoundedCornerShape(Dimens.xs),
                                        color = GetFitTheme.colors.surface2,
                                        modifier = Modifier.pressable(onClick = { monthOffset = 0 })
                                    ) {
                                        Text(
                                            text = "Today",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GetFitTheme.colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                                    val isLogged = workoutDates.contains(dateStr)
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
                                                width = if (isSelected) 1.dp else if (isToday) 0.8.dp else 0.dp,
                                                color = if (isSelected) GetFitTheme.colors.accent else if (isToday) GetFitTheme.colors.hairline else Color.Transparent,
                                                shape = RoundedCornerShape(Dimens.sm)
                                            )
                                            .pressable(enabled = !isFuture, onClick = {
                                                onDateSelected(dateStr)
                                            }),
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
                                                    isFuture -> GetFitTheme.colors.textTertiary
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
                        horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GetFitTheme.colors.success)
                        )
                        Text(
                            text = "Workout Logged",
                            style = MaterialTheme.typography.labelSmall,
                            color = GetFitTheme.colors.textSecondary
                        )
                    }

                    if (selectedDate != todayStr) {
                        GfTextButton(
                            text = "Jump to Today",
                            onClick = { onDateSelected(todayStr) }
                        )
                    }
                }
            }
        }
    }
}

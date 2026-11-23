package com.getfit.ui.screens

import com.getfit.ui.theme.GetFitTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.getfit.data.PlannedExercise
import com.getfit.data.WorkoutDay
import com.getfit.data.entity.Exercise
import com.getfit.data.entity.StopwatchRecord
import com.getfit.data.entity.WorkoutLog
import com.getfit.ui.components.AddCustomExerciseDialog
import com.getfit.ui.components.ExerciseCard
import com.getfit.ui.components.ModifyPastDateDialog
import com.getfit.ui.components.StopwatchDialog
import com.getfit.ui.components.StrengthCurveChart
import com.getfit.ui.components.StrengthCurvePoint
import com.getfit.ui.util.AppDateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.getfit.ui.theme.CaloriesOrange
import com.getfit.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PressableCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
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
            title = { Text(ex.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${ex.muscleGroup} • ${ex.equipment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Select an action for this master catalog exercise:")
                }
            },
            confirmButton = {
                Button(onClick = {
                    exerciseToEdit = ex
                    selectedExerciseToManage = null
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modify Exercise")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            exerciseToDelete = ex
                            selectedExerciseToManage = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete from Catalog")
                    }
                    TextButton(onClick = { selectedExerciseToManage = null }) {
                        Text("Cancel")
                    }
                }
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
            title = { Text("Delete from Master Catalog?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"${ex.name}\"? It will be removed from your master exercise library and moved to Trash (where it can be restored within 10 days from Settings).")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExercise(ex)
                        feedbackMessage = "\"${ex.name}\" moved to Trash"
                        showFeedbackSnackbar = true
                        exerciseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Move to Trash", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Category Action Dialog (Rename or Delete Category)
    if (categoryToManage != null) {
        val cat = categoryToManage!!
        AlertDialog(
            onDismissRequest = { categoryToManage = null },
            title = { Text("Manage \"$cat\"", fontWeight = FontWeight.Bold) },
            text = {
                Text("Choose an action for this body part category:")
            },
            confirmButton = {
                Button(onClick = {
                    categoryToRename = cat
                    renameCategoryInput = cat
                    categoryToManage = null
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rename Category")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            categoryToDelete = cat
                            categoryToManage = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                    TextButton(onClick = { categoryToManage = null }) {
                        Text("Cancel")
                    }
                }
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
            title = { Text("Rename Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter new name for \"$oldCat\":")
                    OutlinedTextField(
                        value = renameCategoryInput,
                        onValueChange = { renameCategoryInput = it },
                        label = { Text("Category Name") },
                        isError = isDuplicateCat,
                        supportingText = {
                            if (isDuplicateCat) {
                                Text(
                                    text = "Category \"${renameCategoryInput.trim()}\" already exists.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        singleLine = true,
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
                            },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameCategoryInput.isNotBlank() && !isDuplicateCat) {
                            viewModel.renameMuscleGroup(oldCat, renameCategoryInput.trim())
                            categoryToRename = null
                        }
                    },
                    enabled = renameCategoryInput.isNotBlank() && !isDuplicateCat
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Category Confirmation Dialog
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category \"$cat\"?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete the \"$cat\" category and all exercises in it? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMuscleGroup(cat)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Category", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
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
        snackbarHost = {
            AnimatedVisibility(
                visible = showFeedbackSnackbar,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Surface(
                    modifier = Modifier
                        .padding(bottom = 76.dp, start = 20.dp, end = 20.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = feedbackMessage,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showStopwatch = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // 1. Date Navigation Bar (Ties Training to Date)
        item(key = "training_date_nav") {
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

        // 2. Plan info header with Add Exercise button
        item(key = "training_plan_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "${plan.name} (${plan.daysPerWeek} Days/Week)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.labelSmall,
                        lineHeight = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Button(
                    onClick = onAddExerciseToToday,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Exercise",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 3. Day Selector Chips
        item(key = "training_day_selector") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(plan.days, key = { idx, day -> "day_${day.dayName}_$idx" }) { index, day ->
                    FilterChip(
                        selected = uiState.selectedDayIndex == index,
                        onClick = { onSelectDay(index) },
                        label = { Text(day.dayName) }
                    )
                }
            }
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
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item(key = "all_ex_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${filteredExercises.size} Exercises",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Press & hold to edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                OutlinedButton(
                    onClick = onOpenAddCustomExercise,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Create Exercise",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item(key = "all_ex_search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search 60+ exercises...",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
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
                    },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item(key = "all_ex_categories") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "No exercises found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onOpenAddCustomExercise,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Custom Exercise", fontWeight = FontWeight.Bold)
                        }
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
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Summary Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.recentLogs.map { it.date }.distinct().size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = GetFitTheme.colors.success
                        )
                        Text(text = "Days Trained", style = MaterialTheme.typography.bodySmall)
                    }
                    Divider(modifier = Modifier.height(40.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.personalBests.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = CaloriesOrange
                        )
                        Text(text = "Personal Records", style = MaterialTheme.typography.bodySmall)
                    }
                    Divider(modifier = Modifier.height(40.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "85%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "Protein Hit Rate", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Strength Curve Analytics Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Strength Progression Curve",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (exercisesWithHistory.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            exercisesWithHistory.forEach { exName ->
                                FilterChip(
                                    selected = selectedExerciseForCurve == exName,
                                    onClick = { selectedExerciseForCurve = exName },
                                    label = { Text(exName, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }

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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Personal Bests Section
        item {
            Text(
                text = "Personal Records (PBs)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.personalBests.isEmpty()) {
            item {
                Text(
                    text = "No personal bests logged yet. Start a workout session to log your weights!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            itemsIndexed(uiState.personalBests.entries.toList(), key = { idx, entry -> "pb_${entry.key}_$idx" }) { _, entry ->
                val exerciseName = uiState.exercises.find { it.id == entry.key }?.name
                    ?: uiState.recentLogs.find { it.exerciseId == entry.key }?.exerciseName
                    ?: "Exercise"

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
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CaloriesOrange)
                            Text(
                                text = exerciseName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = GetFitTheme.colors.success)
                    Text(
                        text = "Timed Records (Planks & Holds)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (uiState.stopwatchRecords.isNotEmpty()) {
                    Text(
                        text = "${uiState.stopwatchRecords.size} logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uiState.stopwatchRecords.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "No timed records saved yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Use the stopwatch in workouts to time and save planks, wall sits, dead hangs, and other holds!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(uiState.stopwatchRecords, key = { idx, record -> "stopwatch_${record.id}_$idx" }) { _, record ->
                val isBest = bestTimedPerTitle[record.title.trim().lowercase()] == record.elapsedMillis

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
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (isBest) CaloriesOrange else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = record.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isBest) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CaloriesOrange.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "★ Best",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = CaloriesOrange,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = record.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                    Icons.Default.Delete,
                                    contentDescription = "Delete record",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (selectedDateFilter != null) {
                    TextButton(onClick = { selectedDateFilter = null }) {
                        Text("Show All Dates", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (availableDates.size > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedDateFilter == null,
                        onClick = { selectedDateFilter = null },
                        label = { Text("All Dates (${availableDates.size})", style = MaterialTheme.typography.labelSmall) }
                    )
                    availableDates.forEach { date ->
                        val count = allGroupedLogs[date]?.size ?: 0
                        FilterChip(
                            selected = selectedDateFilter == date,
                            onClick = { selectedDateFilter = if (selectedDateFilter == date) null else date },
                            label = { Text("$date ($count)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            // Date header row with summary pill badges
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
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = AppDateManager.getDateDisplayLabel(date),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GetFitTheme.colors.success.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, GetFitTheme.colors.success.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "$exerciseCount Ex • $totalSets Sets",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GetFitTheme.colors.success,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.5.dp)

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
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = GetFitTheme.colors.success,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = exName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = setsSummary,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${maxWeight.toInt()}kg ✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = GetFitTheme.colors.success,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (totalVolume > 0) {
                                Text(
                                    text = "Total volume lifted: ${totalVolume.toInt()} kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
        title = {
            Text(
                text = "Add Exercise to $dayName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pick an exercise from your master exercise library:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search exercises...",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Muscle category chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(muscleGroups) { group ->
                        FilterChip(
                            selected = selectedMuscle.equals(group, ignoreCase = true),
                            onClick = { selectedMuscle = group },
                            label = { Text(group, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Exercise Picker List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredExercises.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matching exercises found in library.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        itemsIndexed(filteredExercises, key = { idx, ex -> "add_custom_${ex.id}_$idx" }) { _, ex ->
                            val isSelected = selectedExercise?.id == ex.id
                            val isAlreadyInDay = existingPlannedNames.contains(ex.name.trim().lowercase())
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedExercise = ex }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ex.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${ex.muscleGroup} • ${ex.equipment}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isAlreadyInDay) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "In Routine",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Text(
                        text = "Target for \"${selectedExercise!!.name}\":",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = setsText,
                            onValueChange = { setsText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Sets", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = repsText,
                            onValueChange = { repsText = it },
                            label = { Text("Reps", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes (optional)", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("e.g. Focus on full stretch") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ex = selectedExercise
                    if (ex != null) {
                        val s = setsText.toIntOrNull() ?: 3
                        val r = repsText.ifBlank { "8-10" }
                        onConfirm(ex.name, s, r, notesText)
                    }
                },
                enabled = selectedExercise != null
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add to $dayName", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
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
        title = {
            Text(
                text = "Edit $exerciseName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Adjust prescription for $dayName without affecting the master catalog:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = setsText,
                        onValueChange = { setsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Sets") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text("Reps") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val s = setsText.toIntOrNull() ?: 3
                val r = repsText.ifBlank { "8-10" }
                onConfirm(s, r, notesText)
            }) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onRemoveFromToday,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete (Move to Trash)")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
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
                                text = "Select Workout Date",
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
                                    val isLogged = workoutDates.contains(dateStr)
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
                            text = "Workout Logged",
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

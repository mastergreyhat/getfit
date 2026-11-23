package com.getfit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.core.content.FileProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import com.getfit.ui.theme.GetFitTheme
import com.getfit.data.MealPlanSuggestions
import com.getfit.data.WorkoutPlans
import com.getfit.data.entity.TrashItem
import com.getfit.notification.NotificationHelper
import com.getfit.notification.TimeValidator
import com.getfit.ui.components.AvatarPickerDialog
import com.getfit.ui.components.MacroSummaryRow
import com.getfit.ui.util.AvatarManager
import com.getfit.ui.util.ProfileAvatar
import com.getfit.ui.util.StreakManager
import com.getfit.ui.components.WheelTimePickerDialog
import com.getfit.notification.ReminderPreferences
import com.getfit.ui.components.CleanProgressSpinner
import com.getfit.ui.components.ThemeCapsuleToggle
import com.getfit.ui.theme.CaloriesOrange
import com.getfit.ui.util.ThemePreferences
import com.getfit.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToAuth: () -> Unit = {},
    onLogout: () -> Unit = onNavigateToAuth
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile

    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var ageText by remember(profile.age) { mutableStateOf(profile.age.toString()) }
    var weightText by remember(profile.weight) { mutableStateOf(profile.weight.toString()) }
    var heightText by remember(profile.height) { mutableStateOf(profile.height.toString()) }
    var selectedGender by remember(profile.gender) { mutableStateOf(profile.gender) }
    var selectedActivity by remember(profile.activityLevel) { mutableStateOf(profile.activityLevel) }
    var selectedGoal by remember(profile.goal) { mutableStateOf(profile.goal) }
    var selectedWorkoutPlan by remember(profile.workoutPlan) { mutableStateOf(profile.workoutPlan) }

    // Manual override state
    var isManualOverride by remember { mutableStateOf(false) }
    var customCalText by remember(profile.dailyCalorieTarget) { mutableStateOf(profile.dailyCalorieTarget.toString()) }
    var customProteinText by remember(profile.dailyProteinTarget) { mutableStateOf(profile.dailyProteinTarget.toString()) }
    var customCarbsText by remember(profile.dailyCarbsTarget) { mutableStateOf(profile.dailyCarbsTarget.toString()) }
    var customFatText by remember(profile.dailyFatTarget) { mutableStateOf(profile.dailyFatTarget.toString()) }

    val context = LocalContext.current
    val hasNotificationPermission = remember(context) { NotificationHelper.isNotificationPermissionGranted(context) }

    var breakfastReminderEnabled by remember(hasNotificationPermission) { mutableStateOf(hasNotificationPermission) }
    var breakfastTime by remember { mutableStateOf("08:30 AM") }
    var lunchReminderEnabled by remember(hasNotificationPermission) { mutableStateOf(hasNotificationPermission) }
    var lunchTime by remember { mutableStateOf("01:30 PM") }
    var dinnerReminderEnabled by remember(hasNotificationPermission) { mutableStateOf(hasNotificationPermission) }
    var dinnerTime by remember { mutableStateOf("08:30 PM") }

    LaunchedEffect(uiState.mealSchedules) {
        val bFast = uiState.mealSchedules.find { it.mealType.equals("Breakfast", ignoreCase = true) }
        if (bFast != null) {
            breakfastTime = bFast.scheduledTime
            breakfastReminderEnabled = bFast.isEnabled
        }
        val lunch = uiState.mealSchedules.find { it.mealType.equals("Lunch", ignoreCase = true) }
        if (lunch != null) {
            lunchTime = lunch.scheduledTime
            lunchReminderEnabled = lunch.isEnabled
        }
        val dinner = uiState.mealSchedules.find { it.mealType.equals("Dinner", ignoreCase = true) }
        if (dinner != null) {
            dinnerTime = dinner.scheduledTime
            dinnerReminderEnabled = dinner.isEnabled
        }
    }

    val defaultWaterEnabled = hasNotificationPermission && ReminderPreferences.isWaterEnabled(context)
    var waterRemindersEnabled by remember(hasNotificationPermission) { mutableStateOf(defaultWaterEnabled) }
    var waterReminderTimes by remember {
        mutableStateOf(ReminderPreferences.getWaterTimes(context))
    }
    var showAddWaterTimePicker by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var showResetExercisesDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var trashItemToDeletePermanently by remember { mutableStateOf<TrashItem?>(null) }
    var showEmptyTrashConfirmDialog by remember { mutableStateOf(false) }

    var showSavedSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("Profile and Targets Saved Successfully!") }

    var showAvatarPicker by remember { mutableStateOf(false) }
    val avatarUpdateTrigger by AvatarManager.avatarUpdates.collectAsState()
    val currentAvatar = remember(avatarUpdateTrigger) { AvatarManager.getSelectedAvatar(context) }
    val bestStreak = remember(avatarUpdateTrigger) { StreakManager.getBestStreak(context) }

    LaunchedEffect(showSavedSnackbar, snackbarMessage) {
        if (showSavedSnackbar) {
            delay(1800)
            showSavedSnackbar = false
        }
    }
    val currentThemeMode by ThemePreferences.themeMode.collectAsState()
    val isDark = when (currentThemeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            showSettingsDialog = false
            viewModel.importBackup(context, uri) { _, msg ->
                snackbarMessage = msg
                showSavedSnackbar = true
            }
        }
    }

    val genders = listOf("Male", "Female")
    val activities = listOf("Sedentary", "Light", "Moderate", "Very Active", "Extremely Active")
    val goals = listOf("Muscle Building", "Lose Fat", "Maintain")
    val workoutPlans = WorkoutPlans.getAllPlans()

    // Dynamic calculated values based on current inputs
    val currentWeight = weightText.toDoubleOrNull() ?: profile.weight
    val currentHeight = heightText.toDoubleOrNull() ?: profile.height
    val currentAge = ageText.toIntOrNull() ?: profile.age

    val liveCalc = remember(currentWeight, currentHeight, currentAge, selectedGender, selectedActivity, selectedGoal) {
        viewModel.calculateNutrition(
            weight = currentWeight,
            height = currentHeight,
            age = currentAge,
            gender = selectedGender,
            activityLevel = selectedActivity,
            goal = selectedGoal
        )
    }

    Scaffold(
        snackbarHost = {
            AnimatedVisibility(
                visible = showSavedSnackbar,
                enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
                    animationSpec = tween(180),
                    initialOffsetY = { it / 2 }
                ),
                exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(
                    animationSpec = tween(180),
                    targetOffsetY = { it / 2 }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.94f),
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shadowElevation = 6.dp,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = snackbarMessage,
                            style = MaterialTheme.typography.bodySmall,

                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val profileScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(profileScrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Profile Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GetFitTheme.colors.surface1),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, GetFitTheme.colors.hairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Avatar profile photo badge with streak tier glow
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = currentAvatar.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.5.dp, currentAvatar.accentColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(60.dp)
                            .clickable { showAvatarPicker = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = currentAvatar.emoji, style = MaterialTheme.typography.displaySmall)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showAvatarPicker = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (name.isNotBlank()) name else "User Profile",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = currentAvatar.accentColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = currentAvatar.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = currentAvatar.accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (uiState.isLoggedIn) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable { showSettingsDialog = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "Synced",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { onNavigateToAuth() }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "Cloud Sync",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = selectedGoal,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // User Info Inputs
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it },
                    label = { Text("Age (yrs)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = { Text("Height (cm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Gender Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Gender", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    genders.forEach { g ->
                        FilterChip(
                            selected = selectedGender.equals(g, ignoreCase = true),
                            onClick = { selectedGender = g },
                            label = { Text(g) }
                        )
                    }
                }
            }

            // Activity Level Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Activity Level", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activities.forEach { act ->
                        FilterChip(
                            selected = selectedActivity.equals(act, ignoreCase = true),
                            onClick = { selectedActivity = act },
                            label = { Text(act) }
                        )
                    }
                }
            }

            // Fitness Goal Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Fitness Goal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    goals.forEach { g ->
                        FilterChip(
                            selected = selectedGoal.equals(g, ignoreCase = true),
                            onClick = { selectedGoal = g },
                            label = { Text(g) }
                        )
                    }
                }
            }

            // Workout Split Selector
            Text("Workout Plan Split", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            workoutPlans.forEach { plan ->
                val isSelected = selectedWorkoutPlan.equals(plan.name, ignoreCase = true)

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${plan.name} (${plan.daysPerWeek} days/week)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = plan.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedWorkoutPlan = plan.name }
                        )
                    }
                }
            }

            // Auto-Calculated TDEE & Target Card
            Text("Calculated Energy & Macros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GetFitTheme.colors.surface1),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, GetFitTheme.colors.hairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${liveCalc.bmr} kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "BMR", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Divider(modifier = Modifier.height(30.dp).width(1.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${liveCalc.tdee} kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "TDEE", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Divider(modifier = Modifier.height(30.dp).width(1.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${liveCalc.targetCalories} kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CaloriesOrange)
                            Text(text = "Target Calories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    MacroSummaryRow(
                        proteinCurrent = liveCalc.targetProtein.toFloat(),
                        proteinTarget = liveCalc.targetProtein.toFloat(),
                        carbsCurrent = liveCalc.targetCarbs.toFloat(),
                        carbsTarget = liveCalc.targetCarbs.toFloat(),
                        fatCurrent = liveCalc.targetFat.toFloat(),
                        fatTarget = liveCalc.targetFat.toFloat()
                    )
                }
            }

            // Diet Plan Presets Quick Apply
            Text("Or Choose a Preset Diet Plan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2000, 2500, 3000, 3500).forEach { cal ->
                    OutlinedButton(
                        onClick = {
                            viewModel.applyPresetDietPlan(cal)
                            snackbarMessage = "Applied $cal kcal Diet Plan"
                            showSavedSnackbar = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GetFitTheme.colors.accent),
                        border = BorderStroke(1.dp, GetFitTheme.colors.hairline),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("$cal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Manual Override Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manual Target Override", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Switch(checked = isManualOverride, onCheckedChange = { isManualOverride = it })
            }

            if (isManualOverride) {
                OutlinedTextField(
                    value = customCalText,
                    onValueChange = { customCalText = it },
                    label = { Text("Target Calories (kcal)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customProteinText,
                        onValueChange = { customProteinText = it },
                        label = { Text("Protein (g)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = customCarbsText,
                        onValueChange = { customCarbsText = it },
                        label = { Text("Carbs (g)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = customFatText,
                        onValueChange = { customFatText = it },
                        label = { Text("Fat (g)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Save Profile Button
            Button(
                onClick = {
                    val finalAge = ageText.toIntOrNull() ?: 25
                    val finalWeight = weightText.toDoubleOrNull() ?: 70.0
                    val finalHeight = heightText.toDoubleOrNull() ?: 170.0

                    viewModel.saveProfile(
                        name = name.trim(),
                        age = finalAge,
                        weight = finalWeight,
                        height = finalHeight,
                        gender = selectedGender,
                        activityLevel = selectedActivity,
                        goal = selectedGoal,
                        workoutPlan = selectedWorkoutPlan,
                        customCalories = if (isManualOverride) customCalText.toIntOrNull() else null,
                        customProtein = if (isManualOverride) customProteinText.toIntOrNull() else null,
                        customCarbs = if (isManualOverride) customCarbsText.toIntOrNull() else null,
                        customFat = if (isManualOverride) customFatText.toIntOrNull() else null
                    )
                    viewModel.updateMealSchedule("Breakfast", breakfastTime, breakfastReminderEnabled)
                    viewModel.updateMealSchedule("Lunch", lunchTime, lunchReminderEnabled)
                    viewModel.updateMealSchedule("Dinner", dinnerTime, dinnerReminderEnabled)
                    snackbarMessage = "Profile and Targets Saved Successfully!"
                    showSavedSnackbar = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profile & Calculate Plan", fontWeight = FontWeight.Bold)
            }

            // Account & Sync Section at bottom of Profile Screen
            if (uiState.isLoggedIn) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.userEmail ?: "Signed In",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val lastSyncText = if (uiState.lastSyncedMillis > 0L) {
                                    val sdf = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
                                    "Synced: ${sdf.format(java.util.Date(uiState.lastSyncedMillis))}"
                                } else {
                                    "Ready to sync"
                                }
                                Text(
                                    text = lastSyncText,
                                    style = MaterialTheme.typography.bodySmall,

                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.syncNow { _, msg ->
                                        snackbarMessage = msg
                                        showSavedSnackbar = true
                                    }
                                },
                                enabled = !uiState.isSyncing,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (uiState.isSyncing) {
                                    CleanProgressSpinner(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Sync Now", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = {
                                    viewModel.logout {
                                        onLogout()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Log Out", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onNavigateToAuth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign In / Create Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Avatar Picker Dialog (1 month, 3 months, 6 months, 1 year streak unlocks)
    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentAvatar = currentAvatar,
            bestStreak = bestStreak,
            onSelectAvatar = { avatar ->
                AvatarManager.setSelectedAvatarId(context, avatar.id)
                showAvatarPicker = false
                snackbarMessage = "Profile photo updated to ${avatar.name}!"
                showSavedSnackbar = true
            },
            onDismiss = { showAvatarPicker = false }
        )
    }

    // Reset Exercises Confirmation Dialog
    if (showResetExercisesDialog) {
        AlertDialog(
            onDismissRequest = { showResetExercisesDialog = false },
            title = { Text("Reset Exercises to Default?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will restore all built-in exercises across all muscle groups. Any custom exercises you added will be preserved safely.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetExercisesToDefault { count ->
                        snackbarMessage = "Restored $count default exercises!"
                        showSavedSnackbar = true
                    }
                    showResetExercisesDialog = false
                }) {
                    Text("Reset Exercises", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetExercisesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Trash / Recently Deleted Dialog
    if (showTrashDialog) {
        Dialog(
            onDismissRequest = { showTrashDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Trash (${uiState.trashItems.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.trashItems.isNotEmpty()) {
                                TextButton(onClick = { showEmptyTrashConfirmDialog = true }) {
                                    Text("Empty Trash", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            IconButton(onClick = { showTrashDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }

                    Text(
                        text = "Items are retained for 10 days before automatic deletion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (uiState.trashItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Text("Trash is empty", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "Deleted exercises, categories, and logs will appear here for 10 days.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(uiState.trashItems, key = { idx, item -> "trash_${item.id}_${item.itemType}_$idx" }) { _, item ->
                                val daysElapsed = ((System.currentTimeMillis() - item.deletedAtMillis) / (24 * 3600 * 1000L)).toInt().coerceAtLeast(0)
                                val daysRemaining = (10 - daysElapsed).coerceAtLeast(1)

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = item.itemType,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "Auto-deletes in $daysRemaining d",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = item.subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.restoreTrashItem(item) { success ->
                                                        if (success) {
                                                            snackbarMessage = "Restored \"${item.title}\" successfully!"
                                                            showSavedSnackbar = true
                                                        }
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Restore", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }

                                            IconButton(
                                                onClick = { trashItemToDeletePermanently = item }
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Permanently",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
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
    }

    // Confirm Empty Trash Dialog
    if (showEmptyTrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirmDialog = false },
            title = { Text("Empty Trash?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete all items in trash? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashConfirmDialog = false
                        snackbarMessage = "Trash cleared permanently."
                        showSavedSnackbar = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty Trash", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Permanently Delete Single Item Dialog
    if (trashItemToDeletePermanently != null) {
        val target = trashItemToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { trashItemToDeletePermanently = null },
            title = { Text("Delete Permanently?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete \"${target.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTrashItem(target)
                        trashItemToDeletePermanently = null
                        snackbarMessage = "Deleted permanently."
                        showSavedSnackbar = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { trashItemToDeletePermanently = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settings & Preferences Dialog
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showSettingsDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

                    // Theme Appearance
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "APPEARANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Switch between Clean White light theme and OLED Inverted dark theme",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ThemeCapsuleToggle(
                            isDark = isDark,
                            onToggle = { dark ->
                                ThemePreferences.setThemeMode(context, if (dark) "DARK" else "LIGHT")
                            },
                            fullWidth = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

                    // Notifications & Reminders
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "NOTIFICATIONS & REMINDERS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Text(
                            text = "MEAL REMINDER SCHEDULE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Set or unset reminder alerts and customize delivery time for each meal:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Breakfast Row
                        MealReminderRow(
                            mealName = "Breakfast",
                            enabled = breakfastReminderEnabled,
                            time = breakfastTime,
                            onToggle = {
                                breakfastReminderEnabled = it
                                viewModel.updateMealSchedule("Breakfast", breakfastTime, it)
                            },
                            onTimeChange = {
                                breakfastTime = it
                                viewModel.updateMealSchedule("Breakfast", it, breakfastReminderEnabled)
                            }
                        )

                        // Lunch Row
                        MealReminderRow(
                            mealName = "Lunch",
                            enabled = lunchReminderEnabled,
                            time = lunchTime,
                            onToggle = {
                                lunchReminderEnabled = it
                                viewModel.updateMealSchedule("Lunch", lunchTime, it)
                            },
                            onTimeChange = {
                                lunchTime = it
                                viewModel.updateMealSchedule("Lunch", it, lunchReminderEnabled)
                            }
                        )

                        // Dinner Row
                        MealReminderRow(
                            mealName = "Dinner",
                            enabled = dinnerReminderEnabled,
                            time = dinnerTime,
                            onToggle = {
                                dinnerReminderEnabled = it
                                viewModel.updateMealSchedule("Dinner", dinnerTime, it)
                            },
                            onTimeChange = {
                                dinnerTime = it
                                viewModel.updateMealSchedule("Dinner", it, dinnerReminderEnabled)
                            }
                        )

                        Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

                        // Water Hydration Reminders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "WATER INTAKE ALERTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Alerts only at your chosen times. Add or remove multiple times:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = waterRemindersEnabled,
                                onCheckedChange = {
                                    waterRemindersEnabled = it
                                    viewModel.updateWaterSettings(it, waterReminderTimes)
                                }
                            )
                        }

                        if (waterRemindersEnabled) {
                            Text(
                                text = "Active Alert Times (${waterReminderTimes.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                waterReminderTimes.forEach { time ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = time,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            IconButton(
                                                onClick = {
                                                    val updated = waterReminderTimes.filter { it != time }
                                                    waterReminderTimes = updated
                                                    viewModel.updateWaterSettings(waterRemindersEnabled, updated)
                                                    snackbarMessage = "Removed $time alert"
                                                    showSavedSnackbar = true
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove $time",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { showAddWaterTimePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Alert Time", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

                    // Library & Recovery
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "LIBRARY & RECOVERY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        // Reset Exercises Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reset Exercises to Default",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Restores all standard exercises. Keeps custom exercises safe.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { showResetExercisesDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Reset", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Trash Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Trash & Deleted Items",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${uiState.trashItems.size} items in trash • 10-day retention",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = { showTrashDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Trash (${uiState.trashItems.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

                    // Data Backup & Portability
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "DATA BACKUP & DEVICE TRANSFER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "All your workouts, nutrition, and water history are permanently stored on this device. Create a backup file anytime to safely switch phones or prevent data loss.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.exportBackup(context) { file ->
                                        if (file != null) {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, "GetFit Full Backup")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Save or Share Backup"))
                                            snackbarMessage = "Backup created! Save to Google Drive, Files, or WhatsApp."
                                            showSavedSnackbar = true
                                        } else {
                                            snackbarMessage = "Could not export backup."
                                            showSavedSnackbar = true
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Export Backup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    restoreBackupLauncher.launch(
                                        arrayOf(
                                            "application/json",
                                            "application/octet-stream",
                                            "text/plain",
                                            "*/*"
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Restore Backup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Button(
                        onClick = { showSettingsDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }

    if (showAddWaterTimePicker) {
        WheelTimePickerDialog(
            title = "Add Water Alert Time",
            initialTime = "10:00 AM",
            onDismiss = { showAddWaterTimePicker = false },
            onConfirm = { selectedTime ->
                val updatedList = waterReminderTimes.toMutableList()
                if (updatedList.contains(selectedTime)) {
                    snackbarMessage = "$selectedTime is already added."
                    showSavedSnackbar = true
                } else {
                    updatedList.add(selectedTime)
                    waterReminderTimes = updatedList.sortedBy { TimeValidator.toMinutes(it) }
                    viewModel.updateWaterSettings(waterRemindersEnabled, waterReminderTimes)
                    snackbarMessage = "Added water alert for $selectedTime"
                    showSavedSnackbar = true
                }
                showAddWaterTimePicker = false
            }
        )
    }
}

@Composable
private fun MealReminderRow(
    mealName: String,
    enabled: Boolean,
    time: String,
    onToggle: (Boolean) -> Unit,
    onTimeChange: (String) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mealName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showTimePicker = true }
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "Time: $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }

    if (showTimePicker) {
        WheelTimePickerDialog(
            title = "Set $mealName Reminder Time",
            initialTime = time,
            onDismiss = { showTimePicker = false },
            onConfirm = { newTime ->
                onTimeChange(newTime)
                showTimePicker = false
            }
        )
    }
}

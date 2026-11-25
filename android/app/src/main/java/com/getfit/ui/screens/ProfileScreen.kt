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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme
import com.getfit.data.WorkoutPlans
import com.getfit.data.entity.TrashItem
import com.getfit.notification.NotificationHelper
import com.getfit.notification.TimeValidator
import com.getfit.ui.components.AvatarPickerDialog
import com.getfit.ui.components.MacroSummaryRow
import com.getfit.ui.util.AvatarManager
import com.getfit.ui.util.StreakManager
import com.getfit.ui.components.WheelTimePickerDialog
import com.getfit.notification.ReminderPreferences
import com.getfit.ui.components.core.GfBigNumber
import com.getfit.ui.components.core.GfCard
import com.getfit.ui.components.core.GfChipFlow
import com.getfit.ui.components.core.GfHeroCard
import com.getfit.ui.components.core.GfOverline
import com.getfit.ui.components.core.GfPrimaryButton
import com.getfit.ui.components.core.GfSecondaryButton
import com.getfit.ui.components.core.GfSectionHeader
import com.getfit.ui.components.core.GfTextButton
import com.getfit.ui.components.core.GfTextField
import com.getfit.ui.components.core.gfBottomBarPadding
import com.getfit.ui.components.core.pressable
import com.getfit.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToAuth: () -> Unit = {},
    onLogout: () -> Unit = onNavigateToAuth
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.profile

    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var ageText by remember(profile.age) { mutableStateOf(profile.age.toString()) }
    var weightText by remember(profile.weight) { mutableStateOf(profile.weight.toString()) }
    var heightText by remember(profile.height) { mutableStateOf(profile.height.toString()) }
    var selectedGender by remember(profile.gender) { mutableStateOf(profile.gender) }
    var selectedActivity by remember(profile.activityLevel) { mutableStateOf(profile.activityLevel) }
    var selectedGoal by remember(profile.goal) { mutableStateOf(profile.goal) }
    var selectedWorkoutPlan by remember(profile.workoutPlan) { mutableStateOf(profile.workoutPlan) }

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
    var hapticsEnabled by remember { mutableStateOf(com.getfit.ui.util.HapticsPreferences.isHapticsEnabled(context)) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteConfirmEmail by remember { mutableStateOf("") }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }

    var showResetExercisesDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var trashItemToDeletePermanently by remember { mutableStateOf<TrashItem?>(null) }
    var showEmptyTrashConfirmDialog by remember { mutableStateOf(false) }

    var showSavedSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("Profile and Targets Saved Successfully!") }

    var showAvatarPicker by remember { mutableStateOf(false) }
    val avatarUpdateTrigger by AvatarManager.avatarUpdates.collectAsStateWithLifecycle()
    val currentAvatar = remember(avatarUpdateTrigger) { AvatarManager.getSelectedAvatar(context) }
    val bestStreak = remember(avatarUpdateTrigger) { AvatarManager.getEffectiveBestStreak(context) }
    var newlyUnlockedAvatar by remember { mutableStateOf<com.getfit.ui.util.ProfileAvatar?>(null) }

    LaunchedEffect(bestStreak) {
        if (newlyUnlockedAvatar == null) {
            newlyUnlockedAvatar = AvatarManager.checkNewUnlock(context, bestStreak)
        }
    }

    LaunchedEffect(showSavedSnackbar, snackbarMessage) {
        if (showSavedSnackbar) {
            delay(1800)
            showSavedSnackbar = false
        }
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
        containerColor = GetFitTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    .gfBottomBarPadding()
                    .padding(horizontal = Dimens.screenGutter, vertical = Dimens.sm)
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(Dimens.xl),
                        color = GetFitTheme.colors.surface3,
                        contentColor = GetFitTheme.colors.textPrimary,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, GetFitTheme.colors.hairline)
                    ) {
                        Text(
                            text = snackbarMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = Dimens.lg, vertical = Dimens.sm)
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
                .background(GetFitTheme.colors.background)
                .verticalScroll(profileScrollState)
                .padding(start = Dimens.screenGutter, end = Dimens.screenGutter, top = Dimens.sm, bottom = Dimens.md),
            verticalArrangement = Arrangement.spacedBy(Dimens.lg)
        ) {
            // Header Profile Card
            GfCard(
                cornerRadius = Dimens.lg,
                contentPadding = PaddingValues(Dimens.lg),
                useGradient = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.md)
                ) {
                    // Avatar profile photo badge with streak tier ring
                    com.getfit.ui.components.AvatarImage(
                        avatar = currentAvatar,
                        size = 60.dp,
                        locked = false,
                        modifier = Modifier.pressable(onClick = { showAvatarPicker = true })
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .pressable(onClick = { showAvatarPicker = true })
                    ) {
                        Text(
                            text = if (name.isNotBlank()) name else "User Profile",
                            style = MaterialTheme.typography.titleLarge,
                            color = GetFitTheme.colors.textPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.xs),
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
                                    shape = RoundedCornerShape(Dimens.xs),
                                    color = GetFitTheme.colors.accent.copy(alpha = 0.15f),
                                    modifier = Modifier.pressable(
                                        enabled = !uiState.isSyncing,
                                        onClick = {
                                            viewModel.syncNow { _, msg ->
                                                snackbarMessage = msg
                                                showSavedSnackbar = true
                                            }
                                        }
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = GetFitTheme.colors.accent,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = if (uiState.isSyncing) "Syncing..." else "Sync",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = GetFitTheme.colors.accent
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
                            color = GetFitTheme.colors.textSecondary,
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
                            tint = GetFitTheme.colors.textSecondary
                        )
                    }
                }
            }

            // User Info Inputs
            GfSectionHeader(
                title = "Personal Information",
                topSpacing = Dimens.sm
            )

            GfTextField(
                value = name,
                onValueChange = { name = com.getfit.ui.util.InputSanitizer.sanitizeNameInput(it) },
                label = "Full Name",
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.md)
            ) {
                GfTextField(
                    value = ageText,
                    onValueChange = { ageText = it },
                    label = "Age (yrs)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )

                GfTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = "Weight (kg)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )

                GfTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = "Height (cm)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f)
                )
            }

            // Gender Selector
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                GfOverline(text = "Gender")
                GfChipFlow(
                    items = genders,
                    selectedItem = genders.firstOrNull { it.equals(selectedGender, ignoreCase = true) },
                    onItemSelected = { selectedGender = it }
                )
            }

            // Activity Level Selector
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                GfOverline(text = "Activity Level")
                GfChipFlow(
                    items = activities,
                    selectedItem = activities.firstOrNull { it.equals(selectedActivity, ignoreCase = true) },
                    onItemSelected = { selectedActivity = it }
                )
            }

            // Fitness Goal Selector
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                GfOverline(text = "Fitness Goal")
                GfChipFlow(
                    items = goals,
                    selectedItem = goals.firstOrNull { it.equals(selectedGoal, ignoreCase = true) },
                    onItemSelected = { selectedGoal = it }
                )
            }

            // Workout Split Selector
            GfSectionHeader(
                title = "Workout Plan Split",
                topSpacing = Dimens.sm
            )

            workoutPlans.forEach { plan ->
                val isSelected = selectedWorkoutPlan.equals(plan.name, ignoreCase = true)

                Surface(
                    shape = RoundedCornerShape(Dimens.lg),
                    color = if (isSelected) GetFitTheme.colors.surface3 else GetFitTheme.colors.surface1,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) GetFitTheme.colors.accent else GetFitTheme.colors.hairline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressable(onClick = { selectedWorkoutPlan = plan.name })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = Dimens.sm)) {
                            Text(
                                text = "${plan.name} (${plan.daysPerWeek} days/week)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) GetFitTheme.colors.accent else GetFitTheme.colors.textPrimary
                            )
                            Text(
                                text = plan.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = GetFitTheme.colors.textSecondary
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedWorkoutPlan = plan.name },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = GetFitTheme.colors.accent,
                                unselectedColor = GetFitTheme.colors.textTertiary
                            )
                        )
                    }
                }
            }

            // Auto-Calculated TDEE & Target Card (One hero per screen)
            GfSectionHeader(
                title = "Calculated Energy & Macros",
                topSpacing = Dimens.sm
            )

            GfHeroCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.lg)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.xs)
                    ) {
                        GfOverline(text = "Target Calories")
                        GfBigNumber(
                            value = liveCalc.targetCalories,
                            unit = "kcal",
                            hero = true,
                            color = GetFitTheme.colors.calories,
                            style = MaterialTheme.typography.displayMedium
                        )
                    }

                    Divider(color = GetFitTheme.colors.hairline, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${liveCalc.bmr} kcal",
                                style = MaterialTheme.typography.titleMedium,
                                color = GetFitTheme.colors.textPrimary
                            )
                            Text(
                                text = "BMR",
                                style = MaterialTheme.typography.bodySmall,
                                color = GetFitTheme.colors.textSecondary
                            )
                        }
                        Divider(
                            modifier = Modifier.height(30.dp).width(1.dp),
                            color = GetFitTheme.colors.hairline
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${liveCalc.tdee} kcal",
                                style = MaterialTheme.typography.titleMedium,
                                color = GetFitTheme.colors.textPrimary
                            )
                            Text(
                                text = "TDEE",
                                style = MaterialTheme.typography.bodySmall,
                                color = GetFitTheme.colors.textSecondary
                            )
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

            // Save Profile Button
            GfPrimaryButton(
                text = "Save Profile & Calculate Plan",
                icon = Icons.Default.Save,
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
                        customCalories = null,
                        customProtein = null,
                        customCarbs = null,
                        customFat = null
                    )
                    viewModel.updateMealSchedule("Breakfast", breakfastTime, breakfastReminderEnabled)
                    viewModel.updateMealSchedule("Lunch", lunchTime, lunchReminderEnabled)
                    viewModel.updateMealSchedule("Dinner", dinnerTime, dinnerReminderEnabled)
                    snackbarMessage = "Profile and Targets Saved Successfully!"
                    showSavedSnackbar = true
                }
            )

            // Account & Sync Section at bottom of Profile Screen
            if (uiState.isLoggedIn) {
                GfCard(
                    cornerRadius = Dimens.lg,
                    contentPadding = PaddingValues(Dimens.lg),
                    useGradient = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.sm),
                                color = GetFitTheme.colors.accent.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = GetFitTheme.colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.userEmail ?: "Signed In",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GetFitTheme.colors.textPrimary,
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
                                    color = GetFitTheme.colors.textSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                        ) {
                            GfSecondaryButton(
                                text = "Sync Now",
                                isLoading = uiState.isSyncing,
                                compact = true,
                                onClick = {
                                    viewModel.syncNow { _, msg ->
                                        snackbarMessage = msg
                                        showSavedSnackbar = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            GfTextButton(
                                text = "Log Out",
                                color = GetFitTheme.colors.danger,
                                onClick = {
                                    viewModel.logout {
                                        onLogout()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                GfSecondaryButton(
                    text = "Sign In / Create Account",
                    icon = Icons.Default.CloudSync,
                    onClick = onNavigateToAuth,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(com.getfit.ui.components.core.gfBottomBarContentPadding()))
        }
    }

    // Avatar Picker Sheet (1 month, 3 months, 6 months, 1 year streak unlocks)
    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentAvatar = currentAvatar,
            bestStreak = bestStreak,
            onSelectAvatar = { avatar ->
                if (AvatarManager.setSelectedAvatarId(context, avatar.id)) {
                    showAvatarPicker = false
                    snackbarMessage = "Profile photo updated to ${avatar.name}!"
                    showSavedSnackbar = true
                }
            },
            onDismiss = { showAvatarPicker = false }
        )
    }

    if (newlyUnlockedAvatar != null) {
        val unlocked = newlyUnlockedAvatar!!
        com.getfit.ui.components.AvatarUnlockSheet(
            avatar = unlocked,
            onEquipNow = {
                AvatarManager.markUnlockSeen(context, unlocked.id)
                AvatarManager.setSelectedAvatarId(context, unlocked.id)
                newlyUnlockedAvatar = null
                snackbarMessage = "Profile photo updated to ${unlocked.name}!"
                showSavedSnackbar = true
            },
            onLater = {
                AvatarManager.markUnlockSeen(context, unlocked.id)
                newlyUnlockedAvatar = null
            }
        )
    }

    // Reset Exercises Confirmation Dialog
    if (showResetExercisesDialog) {
        AlertDialog(
            onDismissRequest = { showResetExercisesDialog = false },
            containerColor = GetFitTheme.colors.surface1,
            titleContentColor = GetFitTheme.colors.textPrimary,
            textContentColor = GetFitTheme.colors.textSecondary,
            title = { Text("Reset Exercises to Default?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "This will restore all built-in exercises across all muscle groups. Any custom exercises you added will be preserved safely.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                GfTextButton(
                    text = "Reset Exercises",
                    onClick = {
                        viewModel.resetExercisesToDefault { count ->
                            snackbarMessage = "Restored $count default exercises!"
                            showSavedSnackbar = true
                        }
                        showResetExercisesDialog = false
                    }
                )
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
                shape = RoundedCornerShape(Dimens.xl),
                color = GetFitTheme.colors.surface1,
                border = BorderStroke(1.dp, GetFitTheme.colors.hairline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.xl)
                ) {
                    // Header
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
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = GetFitTheme.colors.accent
                            )
                            Text(
                                text = "Trash (${uiState.trashItems.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = GetFitTheme.colors.textPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.trashItems.isNotEmpty()) {
                                GfTextButton(
                                    text = "Empty Trash",
                                    color = GetFitTheme.colors.danger,
                                    onClick = { showEmptyTrashConfirmDialog = true }
                                )
                            }
                            IconButton(onClick = { showTrashDialog = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = GetFitTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    Text(
                        text = "Items are retained for 10 days before automatic deletion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GetFitTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = Dimens.md)
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
                                verticalArrangement = Arrangement.spacedBy(Dimens.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = GetFitTheme.colors.textTertiary
                                )
                                Text(
                                    text = "Trash is empty",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GetFitTheme.colors.textSecondary
                                )
                                Text(
                                    text = "Deleted exercises, categories, and logs will appear here for 10 days.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GetFitTheme.colors.textTertiary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Dimens.sm)
                        ) {
                            itemsIndexed(uiState.trashItems, key = { idx, item -> "trash_${item.id}_${item.itemType}_$idx" }) { _, item ->
                                val daysElapsed = ((System.currentTimeMillis() - item.deletedAtMillis) / (24 * 3600 * 1000L)).toInt().coerceAtLeast(0)
                                val daysRemaining = (10 - daysElapsed).coerceAtLeast(1)

                                GfCard(
                                    cornerRadius = Dimens.lg,
                                    contentPadding = PaddingValues(Dimens.md),
                                    useGradient = false,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(Dimens.xs),
                                                color = GetFitTheme.colors.surface2
                                            ) {
                                                Text(
                                                    text = item.itemType,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GetFitTheme.colors.accent,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "Auto-deletes in ${daysRemaining}d",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GetFitTheme.colors.textTertiary,
                                                maxLines = 1
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = GetFitTheme.colors.textPrimary,
                                                    maxLines = 2,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.subtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = GetFitTheme.colors.textSecondary,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                GfSecondaryButton(
                                                    text = "Restore",
                                                    icon = Icons.Default.Restore,
                                                    compact = true,
                                                    onClick = {
                                                        viewModel.restoreTrashItem(item) { success ->
                                                            if (success) {
                                                                snackbarMessage = "Restored \"${item.title}\" successfully!"
                                                                showSavedSnackbar = true
                                                            }
                                                        }
                                                    }
                                                )

                                                IconButton(
                                                    onClick = { trashItemToDeletePermanently = item }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete Permanently",
                                                        tint = GetFitTheme.colors.danger,
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
    }

    // Confirm Empty Trash Dialog
    if (showEmptyTrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirmDialog = false },
            containerColor = GetFitTheme.colors.surface1,
            titleContentColor = GetFitTheme.colors.textPrimary,
            textContentColor = GetFitTheme.colors.textSecondary,
            title = { Text("Empty Trash?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete all items in trash? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                GfTextButton(
                    text = "Empty Trash",
                    color = GetFitTheme.colors.danger,
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashConfirmDialog = false
                        snackbarMessage = "Trash cleared permanently."
                        showSavedSnackbar = true
                    }
                )
            }
        )
    }

    // Confirm Permanently Delete Single Item Dialog
    if (trashItemToDeletePermanently != null) {
        val target = trashItemToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { trashItemToDeletePermanently = null },
            containerColor = GetFitTheme.colors.surface1,
            titleContentColor = GetFitTheme.colors.textPrimary,
            textContentColor = GetFitTheme.colors.textSecondary,
            title = { Text("Delete Permanently?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${target.title}\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                GfTextButton(
                    text = "Delete",
                    color = GetFitTheme.colors.danger,
                    onClick = {
                        viewModel.deleteTrashItem(target)
                        trashItemToDeletePermanently = null
                        snackbarMessage = "Deleted permanently."
                        showSavedSnackbar = true
                    }
                )
            }
        )
    }

    // Settings & Preferences Dialog
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            GfCard(
                cornerRadius = Dimens.xl,
                contentPadding = PaddingValues(Dimens.xl),
                useGradient = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.lg)
                ) {
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
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = GetFitTheme.colors.accent
                            )
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleLarge,
                                color = GetFitTheme.colors.textPrimary
                            )
                        }
                        IconButton(onClick = { showSettingsDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = GetFitTheme.colors.textSecondary
                            )
                        }
                    }

                    Divider(color = GetFitTheme.colors.hairline, thickness = 1.dp)

                    // Notifications & Reminders
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                        GfOverline(text = "Notifications & Reminders")

                        Text(
                            text = "MEAL REMINDER SCHEDULE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GetFitTheme.colors.textPrimary
                        )
                        Text(
                            text = "Set or unset reminder alerts and customize delivery time for each meal:",
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textSecondary
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

                        Divider(color = GetFitTheme.colors.hairline, thickness = 1.dp)

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
                                    color = GetFitTheme.colors.textPrimary
                                )
                                Text(
                                    text = "Alerts only at your chosen times. Add or remove multiple times:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GetFitTheme.colors.textSecondary
                                )
                            }
                            Switch(
                                checked = waterRemindersEnabled,
                                onCheckedChange = {
                                    waterRemindersEnabled = it
                                    viewModel.updateWaterSettings(it, waterReminderTimes)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GetFitTheme.colors.onAccent,
                                    checkedTrackColor = GetFitTheme.colors.accent
                                )
                            )
                        }

                        if (waterRemindersEnabled) {
                            Text(
                                text = "Active Alert Times (${waterReminderTimes.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = GetFitTheme.colors.textSecondary
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                                verticalArrangement = Arrangement.spacedBy(Dimens.xs)
                            ) {
                                waterReminderTimes.forEach { time ->
                                    Surface(
                                        shape = RoundedCornerShape(Dimens.sm),
                                        color = GetFitTheme.colors.surface2,
                                        border = BorderStroke(1.dp, GetFitTheme.colors.hairline)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = Dimens.sm, end = Dimens.xs, top = Dimens.xs, bottom = Dimens.xs),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = time,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = GetFitTheme.colors.textPrimary
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
                                                    tint = GetFitTheme.colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            GfSecondaryButton(
                                text = "Add Alert Time",
                                icon = Icons.Default.Add,
                                compact = true,
                                onClick = { showAddWaterTimePicker = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Divider(color = GetFitTheme.colors.hairline, thickness = 1.dp)

                    // Haptics & Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Haptic Feedback",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GetFitTheme.colors.textPrimary
                            )
                            Text(
                                text = "Tactile vibration on taps and navigation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GetFitTheme.colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(Dimens.sm))
                        Switch(
                            checked = hapticsEnabled,
                            onCheckedChange = { enabled ->
                                hapticsEnabled = enabled
                                com.getfit.ui.util.HapticsPreferences.setHapticsEnabled(context, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GetFitTheme.colors.onAccent,
                                checkedTrackColor = GetFitTheme.colors.accent
                            )
                        )
                    }

                    Divider(color = GetFitTheme.colors.hairline, thickness = 1.dp)

                    // Library & Recovery
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                        GfOverline(text = "Library & Recovery")

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
                                    fontWeight = FontWeight.SemiBold,
                                    color = GetFitTheme.colors.textPrimary
                                )
                                Text(
                                    text = "Restores all standard exercises. Keeps custom exercises safe.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GetFitTheme.colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(Dimens.sm))
                            GfSecondaryButton(
                                text = "Reset",
                                compact = true,
                                onClick = { showResetExercisesDialog = true }
                            )
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
                                    fontWeight = FontWeight.SemiBold,
                                    color = GetFitTheme.colors.textPrimary
                                )
                                Text(
                                    text = "${uiState.trashItems.size} items in trash • 10-day retention",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GetFitTheme.colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(Dimens.sm))
                            GfSecondaryButton(
                                text = "Trash (${uiState.trashItems.size})",
                                compact = true,
                                onClick = { showTrashDialog = true }
                            )
                        }
                    }

                    Divider(color = GetFitTheme.colors.hairline, thickness = 1.dp)

                    // Data Backup & Portability
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        GfOverline(text = "Data Backup & Device Transfer")
                        Text(
                            text = "All your workouts, nutrition, and water history are permanently stored on this device. Create a backup file anytime to safely switch phones or prevent data loss.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                        ) {
                            GfSecondaryButton(
                                text = "Export Backup",
                                compact = true,
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
                                modifier = Modifier.weight(1f)
                            )

                            GfSecondaryButton(
                                text = "Restore Backup",
                                compact = true,
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
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (uiState.isLoggedIn) {
                        Divider(color = GetFitTheme.colors.hairline, thickness = 1.dp)

                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                            GfOverline(text = "Account")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Delete Account",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GetFitTheme.colors.danger
                                    )
                                    Text(
                                        text = "Permanently remove your account and cloud data.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GetFitTheme.colors.textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(Dimens.sm))
                                GfSecondaryButton(
                                    text = "Delete",
                                    compact = true,
                                    onClick = {
                                        deleteConfirmEmail = ""
                                        deleteAccountError = null
                                        showDeleteAccountDialog = true
                                    }
                                )
                            }
                        }
                    }

                    GfPrimaryButton(
                        text = "Done",
                        onClick = { showSettingsDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showDeleteAccountDialog) {
        val accountEmail = uiState.userEmail.orEmpty()
        val emailMatches = accountEmail.isNotBlank() &&
            deleteConfirmEmail.trim().equals(accountEmail.trim(), ignoreCase = true)
        AlertDialog(
            onDismissRequest = { if (!uiState.isSyncing) showDeleteAccountDialog = false },
            containerColor = GetFitTheme.colors.surface1,
            titleContentColor = GetFitTheme.colors.textPrimary,
            textContentColor = GetFitTheme.colors.textSecondary,
            title = { Text("Delete Account Permanently?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    Text(
                        text = "This will permanently delete your account and all synced data from Cloudflare. Enter your email ($accountEmail) to confirm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GetFitTheme.colors.textSecondary
                    )
                    GfTextField(
                        value = deleteConfirmEmail,
                        onValueChange = {
                            deleteConfirmEmail = com.getfit.ui.util.InputSanitizer.sanitizeEmailInput(it)
                            deleteAccountError = null
                        },
                        label = "Confirm Email Address",
                        placeholder = accountEmail,
                        imeAction = ImeAction.Done,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!deleteAccountError.isNullOrBlank()) {
                        Text(
                            text = deleteAccountError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.danger
                        )
                    }
                }
            },
            confirmButton = {
                GfTextButton(
                    text = if (uiState.isSyncing) "Deleting..." else "Delete Account",
                    color = if (emailMatches && !uiState.isSyncing) GetFitTheme.colors.danger else GetFitTheme.colors.textTertiary,
                    enabled = emailMatches && !uiState.isSyncing,
                    onClick = {
                        viewModel.deleteAccount(deleteConfirmEmail) { success, msg ->
                            if (success) {
                                showDeleteAccountDialog = false
                                showSettingsDialog = false
                                onLogout()
                            } else {
                                deleteAccountError = msg
                            }
                        }
                    }
                )
            },
            dismissButton = {
                GfTextButton(
                    text = "Cancel",
                    color = GetFitTheme.colors.textSecondary,
                    enabled = !uiState.isSyncing,
                    onClick = { showDeleteAccountDialog = false }
                )
            }
        )
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
                fontWeight = FontWeight.SemiBold,
                color = GetFitTheme.colors.textPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.xs))
                    .pressable(onClick = { showTimePicker = true })
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "Time: $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = GetFitTheme.colors.accent,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(Dimens.xs),
                    color = GetFitTheme.colors.surface2
                ) {
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = GetFitTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GetFitTheme.colors.onAccent,
                checkedTrackColor = GetFitTheme.colors.accent
            )
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

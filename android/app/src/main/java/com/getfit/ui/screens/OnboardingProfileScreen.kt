package com.getfit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.getfit.data.WorkoutPlans
import com.getfit.ui.components.core.GfChipFlow
import com.getfit.ui.components.core.GfChipRow
import com.getfit.ui.components.core.GfHeroCard
import com.getfit.ui.components.core.GfOverline
import com.getfit.ui.components.core.GfPrimaryButton
import com.getfit.ui.components.core.GfTextField
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.util.AuthPreferences
import com.getfit.ui.util.OnboardingPreferences
import com.getfit.ui.viewmodel.ProfileViewModel

@Composable
fun OnboardingProfileScreen(
    viewModel: ProfileViewModel,
    onSaveAndContinue: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val initialProfile = uiState.profile

    val defaultName = remember(initialProfile.name, uiState.userName) {
        initialProfile.name.ifBlank {
            uiState.userName?.takeIf { it.isNotBlank() } ?: AuthPreferences.getUserName(context) ?: ""
        }
    }
    var nameText by remember(defaultName) { mutableStateOf(defaultName) }
    var ageText by remember { mutableStateOf(if (initialProfile.age > 0) initialProfile.age.toString() else "25") }
    var weightText by remember { mutableStateOf(if (initialProfile.name.isNotBlank() && initialProfile.weight > 0) initialProfile.weight.toString() else "") }
    var heightText by remember { mutableStateOf(if (initialProfile.height > 0) initialProfile.height.toString() else "175.0") }

    var selectedGender by remember { mutableStateOf(initialProfile.gender.ifBlank { "Male" }) }
    var selectedActivity by remember { mutableStateOf(initialProfile.activityLevel.ifBlank { "Moderate" }) }
    var selectedGoal by remember { mutableStateOf(initialProfile.goal.ifBlank { "Muscle Building" }) }
    var selectedWorkoutPlan by remember { mutableStateOf(initialProfile.workoutPlan.ifBlank { "PPL" }) }

    val genders = listOf("Male", "Female")
    val activities = listOf("Sedentary", "Light", "Moderate", "Very Active", "Extremely Active")
    val goals = listOf("Muscle Building", "Lose Fat", "Maintain")
    val workoutPlans = WorkoutPlans.getAllPlans()

    val currentWeight = weightText.toDoubleOrNull() ?: 70.0
    val currentHeight = heightText.toDoubleOrNull() ?: 175.0
    val currentAge = ageText.toIntOrNull() ?: 25

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
        bottomBar = {
            Surface(
                color = GetFitTheme.colors.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Dimens.screenGutter, vertical = Dimens.lg)
            ) {
                GfPrimaryButton(
                    text = "Save & Continue",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = {
                        val finalName = if (nameText.isNotBlank()) nameText.trim() else "Athlete"
                        viewModel.saveProfile(
                            name = finalName,
                            age = currentAge,
                            weight = currentWeight,
                            height = currentHeight,
                            gender = selectedGender,
                            activityLevel = selectedActivity,
                            goal = selectedGoal,
                            workoutPlan = selectedWorkoutPlan
                        )
                        OnboardingPreferences.setOnboardingCompleted(context, true)
                        onSaveAndContinue()
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(GetFitTheme.colors.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenGutter, vertical = Dimens.xl),
            verticalArrangement = Arrangement.spacedBy(Dimens.lg)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                Text(
                    text = "Set Up Your Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = GetFitTheme.colors.textPrimary
                )
                Text(
                    text = "GetFit uses your metrics to automatically calibrate your calorie and macro targets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GetFitTheme.colors.textSecondary
                )
            }

            // Name
            GfTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = "Your Name",
                placeholder = "e.g. Alex",
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )

            // Age & Gender
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.md),
                verticalAlignment = Alignment.Top
            ) {
                GfTextField(
                    value = ageText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) ageText = it },
                    label = "Age",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )

                Column(
                    modifier = Modifier.weight(1.5f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.xs)
                ) {
                    GfOverline(text = "Gender")
                    GfChipFlow(
                        items = genders,
                        selectedItem = selectedGender,
                        onItemSelected = { selectedGender = it }
                    )
                }
            }

            // Weight & Height
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.md)
            ) {
                GfTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = "Weight",
                    suffix = "kg",
                    placeholder = "70.0",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )

                GfTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = "Height",
                    suffix = "cm",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f)
                )
            }

            // Goal
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                GfOverline(text = "Primary Fitness Goal")
                GfChipRow(
                    items = goals,
                    selectedItem = selectedGoal,
                    onItemSelected = { selectedGoal = it },
                    contentPadding = PaddingValues(0.dp)
                )
            }

            // Activity Level
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                GfOverline(text = "Activity Level")
                GfChipRow(
                    items = activities,
                    selectedItem = selectedActivity,
                    onItemSelected = { selectedActivity = it },
                    contentPadding = PaddingValues(0.dp)
                )
            }

            // Workout Split
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                GfOverline(text = "Starting Workout Routine")
                GfChipRow(
                    items = workoutPlans.map { it.name },
                    selectedItem = selectedWorkoutPlan,
                    onItemSelected = { selectedWorkoutPlan = it },
                    contentPadding = PaddingValues(0.dp)
                )
            }

            // Live Target Preview Card (One hero per screen)
            GfHeroCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GfOverline(text = "Calibrated Daily Targets")
                        Surface(
                            shape = RoundedCornerShape(Dimens.xs),
                            color = GetFitTheme.colors.success.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Mifflin-St Jeor",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GetFitTheme.colors.success,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${liveCalc.targetCalories}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = GetFitTheme.colors.textPrimary
                            )
                            Text(
                                text = "Calories",
                                style = MaterialTheme.typography.labelSmall,
                                color = GetFitTheme.colors.textSecondary
                            )
                        }
                        Column {
                            Text(
                                text = "${liveCalc.targetProtein}g",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = GetFitTheme.colors.protein
                            )
                            Text(
                                text = "Protein",
                                style = MaterialTheme.typography.labelSmall,
                                color = GetFitTheme.colors.textSecondary
                            )
                        }
                        Column {
                            Text(
                                text = "${liveCalc.targetCarbs}g",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = GetFitTheme.colors.carbs
                            )
                            Text(
                                text = "Carbs",
                                style = MaterialTheme.typography.labelSmall,
                                color = GetFitTheme.colors.textSecondary
                            )
                        }
                        Column {
                            Text(
                                text = "${liveCalc.targetFat}g",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = GetFitTheme.colors.fat
                            )
                            Text(
                                text = "Fat",
                                style = MaterialTheme.typography.labelSmall,
                                color = GetFitTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))
        }
    }
}

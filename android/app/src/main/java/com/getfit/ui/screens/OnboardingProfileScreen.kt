package com.getfit.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.data.WorkoutPlans
import com.getfit.ui.theme.CaloriesOrange
import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.theme.ProteinBlue
import com.getfit.ui.util.AuthPreferences
import com.getfit.ui.util.OnboardingPreferences
import com.getfit.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Button(
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
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Save & Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Set Up Your Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "GetFit uses your metrics to automatically calibrate your calorie and macro targets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Name
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text("Your Name") },
                placeholder = { Text("e.g. Alex") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Age & Gender
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) ageText = it },
                    label = { Text("Age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "Gender",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        genders.forEach { gender ->
                            FilterChip(
                                selected = selectedGender == gender,
                                onClick = { selectedGender = gender },
                                label = { Text(gender, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // Weight & Height
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    placeholder = { Text("e.g. 70.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Goal
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Primary Fitness Goal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goals.forEach { goal ->
                        FilterChip(
                            selected = selectedGoal == goal,
                            onClick = { selectedGoal = goal },
                            label = { Text(goal, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            // Activity Level
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Activity Level",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activities.forEach { act ->
                        FilterChip(
                            selected = selectedActivity == act,
                            onClick = { selectedActivity = act },
                            label = { Text(act, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            // Workout Split
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Starting Workout Routine",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    workoutPlans.forEach { plan ->
                        FilterChip(
                            selected = selectedWorkoutPlan == plan.name,
                            onClick = { selectedWorkoutPlan = plan.name },
                            label = { Text(plan.name, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            // Live Target Preview Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CALIBRATED DAILY TARGETS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
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
                            Text(text = "${liveCalc.targetCalories}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Calories", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text(text = "${liveCalc.targetProtein}g", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = ProteinBlue)
                            Text(text = "Protein", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text(text = "${liveCalc.targetCarbs}g", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = CaloriesOrange)
                            Text(text = "Carbs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text(text = "${liveCalc.targetFat}g", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                            Text(text = "Fat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

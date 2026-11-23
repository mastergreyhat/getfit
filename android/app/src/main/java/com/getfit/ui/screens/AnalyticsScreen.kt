package com.getfit.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.getfit.data.entity.WeightLog
import com.getfit.ui.components.StrengthCurveChart
import com.getfit.ui.components.StrengthCurvePoint
import com.getfit.ui.theme.CaloriesOrange
import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showWeightDialog by remember { mutableStateOf(false) }
    var selectedWeightLogToManage by remember { mutableStateOf<WeightLog?>(null) }
    var editingWeightLog by remember { mutableStateOf<WeightLog?>(null) }

    if (showWeightDialog) {
        WeightLogDialog(
            currentWeight = uiState.currentWeight,
            onDismiss = { showWeightDialog = false },
            onConfirm = { weight ->
                viewModel.logBodyweight(weight)
                showWeightDialog = false
            }
        )
    }

    if (selectedWeightLogToManage != null) {
        ManageWeightLogDialog(
            log = selectedWeightLogToManage!!,
            onEdit = {
                val target = selectedWeightLogToManage
                selectedWeightLogToManage = null
                editingWeightLog = target
            },
            onDelete = {
                viewModel.deleteWeightLog(selectedWeightLogToManage!!)
                selectedWeightLogToManage = null
            },
            onDismiss = { selectedWeightLogToManage = null }
        )
    }

    if (editingWeightLog != null) {
        EditWeightLogDialog(
            log = editingWeightLog!!,
            onDismiss = { editingWeightLog = null },
            onConfirm = { newWeight ->
                viewModel.updateWeightLog(editingWeightLog!!.copy(weightKg = newWeight))
                editingWeightLog = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress & Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Top Highlights Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HighlightCard(
                        title = "Protein Hit",
                        value = "${uiState.proteinHitRatePercentage}%",
                        subtitle = "7-Day Target",
                        modifier = Modifier.weight(1f)
                    )
                    HighlightCard(
                        title = "30-Day Vol",
                        value = "${(uiState.totalVolumePast30Days / 1000).toInt()}k kg",
                        subtitle = "Total Lifted",
                        modifier = Modifier.weight(1f)
                    )
                    HighlightCard(
                        title = "Bodyweight",
                        value = "${uiState.currentWeight} kg",
                        subtitle = "Current",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 7-Day Calorie & Macro Target Chart
            item {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "7-Day Calorie Adherence",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Goal: ${uiState.userProfile.dailyCalorieTarget} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        // Bar Chart Layout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val maxCal = uiState.weeklyStats.maxOfOrNull { it.calories }?.coerceAtLeast(2000.0) ?: 2500.0

                            uiState.weeklyStats.forEach { stat ->
                                val heightFraction = (stat.calories / maxCal).toFloat().coerceIn(0.06f, 1f)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    // Calorie value indicator at top
                                    Box(
                                        modifier = Modifier.height(18.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Text(
                                            text = "${stat.calories.toInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (stat.calories >= stat.targetCalories * 0.9) GetFitTheme.colors.success else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Bar area with fixed relative weight so labels never get pushed out
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .width(22.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        // Subtle background track
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        )
                                        // Colored calorie fill
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(heightFraction)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(
                                                    if (stat.calories >= stat.targetCalories * 0.9) GetFitTheme.colors.success
                                                    else CaloriesOrange.copy(alpha = 0.85f)
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Day label - guaranteed visible for all 7 days
                                    Text(
                                        text = stat.dayLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (stat.calories > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Strength Progression Curve
            item {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Strength Progression Curve",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.personalBest > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CaloriesOrange.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CaloriesOrange, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = "PB: ${uiState.personalBest.toInt()}kg",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CaloriesOrange
                                        )
                                    }
                                }
                            }
                        }

                        // Exercise selector chips
                        if (uiState.strengthCurveExercises.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.strengthCurveExercises.forEach { exercise ->
                                    FilterChip(
                                        selected = uiState.selectedExercise == exercise,
                                        onClick = { viewModel.selectExerciseForCurve(exercise) },
                                        label = { Text(exercise, style = MaterialTheme.typography.labelMedium) }
                                    )
                                }
                            }

                            if (uiState.exerciseHistory.isNotEmpty()) {
                                val chartPoints = remember(uiState.exerciseHistory) {
                                    uiState.exerciseHistory.map {
                                        StrengthCurvePoint(
                                            date = it.date,
                                            weight = it.maxWeight,
                                            calculated1RM = it.calculated1RM
                                        )
                                    }
                                }
                                StrengthCurveChart(points = chartPoints)
                            }
                        } else {
                            Text(
                                text = "Log workouts to visualize your strength curve and estimated 1-Rep Max over time!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Weekly Muscle Hypertrophy Volume Card
            item {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Weekly Hypertrophy",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "12-20 sets/wk",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (uiState.muscleVolumes.isNotEmpty()) {
                            uiState.muscleVolumes.forEach { stat ->
                                val progress = (stat.completedSets.toFloat() / stat.targetSetsMax.toFloat()).coerceIn(0f, 1f)
                                val statusText = when {
                                    stat.completedSets >= stat.targetSetsMin -> "Optimal"
                                    stat.completedSets > 0 -> "Maintaining"
                                    else -> "Untrained"
                                }
                                val statusColor = when {
                                    stat.completedSets >= stat.targetSetsMin -> GetFitTheme.colors.success
                                    stat.completedSets > 0 -> CaloriesOrange
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = stat.muscleGroup, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "${stat.completedSets}/${stat.targetSetsMax} sets", style = MaterialTheme.typography.bodySmall)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = statusColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = statusText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = statusColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Complete sets in your workout plan to track weekly volume for Chest, Back, Shoulders, Arms, and Legs!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bodyweight Tracking Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GetFitTheme.colors.surface1),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, GetFitTheme.colors.hairline),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Bodyweight Tracker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Press and hold to edit or delete",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                onClick = { showWeightDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = GetFitTheme.colors.accent)
                            ) {
                                Text("+ Log Weight", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                            }
                        }

                        if (uiState.weightLogs.isEmpty()) {
                            Text(
                                text = "No weight logged yet. Current base weight is ${uiState.currentWeight} kg.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.weightLogs.reversed().forEach { weightLog ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { /* Tap disabled: press and hold only */ },
                                            onLongClick = { selectedWeightLogToManage = weightLog }
                                        )
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = weightLog.date,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${weightLog.weightKg} kg",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
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
private fun HighlightCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GetFitTheme.colors.surface2),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, GetFitTheme.colors.hairline),
        modifier = modifier.height(86.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = GetFitTheme.colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeightLogDialog(
    currentWeight: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var weightText by remember { mutableStateOf(if (currentWeight > 0.0) currentWeight.toString() else "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Log Bodyweight (kg)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val w = weightText.toDoubleOrNull() ?: currentWeight
                            onConfirm(w)
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageWeightLogDialog(
    log: WeightLog,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Bodyweight Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Date: ${log.date} • Logged: ${log.weightKg} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Edit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditWeightLogDialog(
    log: WeightLog,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var weightText by remember { mutableStateOf(log.weightKg.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Edit Bodyweight",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Logged on ${log.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val w = weightText.toDoubleOrNull() ?: log.weightKg
                            onConfirm(w)
                        }
                    ) {
                        Text("Update")
                    }
                }
            }
        }
    }
}

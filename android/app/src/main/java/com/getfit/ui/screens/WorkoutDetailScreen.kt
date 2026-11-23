package com.getfit.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.ui.components.SetLogRow
import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.viewmodel.WorkoutViewModel

private fun Double.cleanFormat(): String =
    if (this % 1.0 == 0.0) this.toInt().toString() else "%.1f".format(this)

data class ActiveSetState(
    val setNumber: Int,
    var weight: String = "",
    var reps: String = "",
    var isDone: Boolean = false,
    var previous: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    exerciseId: Long,
    exerciseName: String,
    muscleGroup: String,
    targetSets: Int = 4,
    targetReps: String = "8-10",
    workoutPlanDay: String = "Push Day",
    viewModel: WorkoutViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val exerciseLogs by viewModel.getLogsForExercise(exerciseId).collectAsState(initial = emptyList())
    val overloadInfo = uiState.overloadSuggestions[exerciseId]
    val pbWeight = uiState.personalBests[exerciseId] ?: 0.0

    var showSummaryDialog by remember { mutableStateOf(false) }
    var totalVolumeCalculated by remember { mutableDoubleStateOf(0.0) }
    var completedSetsCount by remember { mutableIntStateOf(0) }
    var isNewPR by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    val defaultRepVal = targetReps.split("-").firstOrNull()?.trim() ?: "8"

    // Initialize sets
    val sets = remember(exerciseId) {
        mutableStateListOf<ActiveSetState>().apply {
            for (i in 1..targetSets) {
                add(
                    ActiveSetState(
                        setNumber = i,
                        weight = "",
                        reps = defaultRepVal,
                        previous = "—"
                    )
                )
            }
        }
    }

    LaunchedEffect(exerciseLogs, pbWeight, overloadInfo) {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val groupedByDate = exerciseLogs.groupBy { it.date }
        val lastSession = groupedByDate.entries.firstOrNull { it.key != todayStr }?.value
            ?: groupedByDate.values.firstOrNull()
            ?: emptyList()
        val prevMap = lastSession.associateBy { it.setNumber }
        val bestWeight = if (pbWeight > 0) pbWeight else (lastSession.maxOfOrNull { it.weight } ?: 0.0)
        val targetWeight = overloadInfo?.suggestedWeight ?: (if (bestWeight > 0) bestWeight else 50.0)

        sets.forEachIndexed { idx, item ->
            val setNum = item.setNumber
            val prevLog = prevMap[setNum]
            val prevStr = when {
                prevLog != null -> "${prevLog.weight.cleanFormat()}kg × ${prevLog.reps}"
                lastSession.isNotEmpty() -> "${lastSession.last().weight.cleanFormat()}kg × ${lastSession.last().reps}"
                bestWeight > 0 -> "${bestWeight.cleanFormat()}kg"
                else -> "—"
            }
            val currentWeight = if (item.weight.isBlank() || item.weight == "50" || item.weight == "0") {
                targetWeight.cleanFormat()
            } else {
                item.weight
            }
            sets[idx] = item.copy(
                previous = prevStr,
                weight = currentWeight
            )
        }
    }

    if (showSummaryDialog) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Workout Complete!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Awesome session on $exerciseName!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Total Volume Lifted: ${totalVolumeCalculated.toInt()} kg",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Completed Sets: $completedSetsCount sets",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (isNewPR) {
                                Text(
                                    text = "NEW PERSONAL RECORD ACHIEVED",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GetFitTheme.colors.success
                                )
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Recovery Tip: Drink 500ml water and consume 25-35g protein within 45 minutes for optimal muscle protein synthesis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onNavigateBack) {
                    Text("Done")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exerciseName, fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exercise Overview Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GetFitTheme.colors.surface1),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, GetFitTheme.colors.hairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = muscleGroup,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Target: $targetSets sets × $targetReps reps",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (pbWeight > 0) {
                        Text(
                            text = "Personal Best: ${pbWeight}kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Progressive overload card
                    if (overloadInfo != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GetFitTheme.colors.success.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = GetFitTheme.colors.success,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Suggested Weight: ${overloadInfo.suggestedWeight}kg",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GetFitTheme.colors.success
                                    )
                                    Text(
                                        text = overloadInfo.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GetFitTheme.colors.success
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sets Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SET LOG",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (sets.size > 1) {
                        TextButton(
                            onClick = {
                                if (sets.isNotEmpty()) {
                                    sets.removeAt(sets.lastIndex)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Remove Set",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remove Set",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    TextButton(onClick = {
                        val nextSetNumber = sets.size + 1
                        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                        val groupedByDate = exerciseLogs.groupBy { it.date }
                        val lastSession = groupedByDate.entries.firstOrNull { it.key != todayStr }?.value
                            ?: groupedByDate.values.firstOrNull()
                            ?: emptyList()
                        val prevLog = lastSession.find { it.setNumber == nextSetNumber }
                        val prevStr = when {
                            prevLog != null -> "${prevLog.weight.cleanFormat()}kg × ${prevLog.reps}"
                            lastSession.isNotEmpty() -> "${lastSession.last().weight.cleanFormat()}kg × ${lastSession.last().reps}"
                            pbWeight > 0 -> "${pbWeight.cleanFormat()}kg"
                            else -> sets.lastOrNull()?.previous ?: "—"
                        }
                        val defaultWeight = sets.lastOrNull()?.weight
                            ?: overloadInfo?.suggestedWeight?.cleanFormat()
                            ?: "50"
                        val defaultReps = sets.lastOrNull()?.reps ?: defaultRepVal

                        sets.add(
                            ActiveSetState(
                                setNumber = nextSetNumber,
                                weight = defaultWeight,
                                reps = defaultReps,
                                previous = prevStr
                            )
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Set",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Set", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (sets.isNotEmpty()) {
                // Column Headers Row directly above the fields: SET | PREVIOUS | WEIGHT (KG) | REPS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SET",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "PREVIOUS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = "WEIGHT (KG)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.width(76.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "REPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.width(66.dp),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier.width(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(28.dp))
                }
            }

            // Set Rows
            sets.forEachIndexed { index, setItem ->
                SetLogRow(
                    setNumber = setItem.setNumber,
                    previousLog = setItem.previous,
                    weight = setItem.weight,
                    reps = setItem.reps,
                    isCompleted = setItem.isDone,
                    onWeightChange = { newW ->
                        sets[index] = setItem.copy(weight = newW)
                    },
                    onRepsChange = { newR ->
                        sets[index] = setItem.copy(reps = newR)
                    },
                    onCompletedChange = { completed ->
                        sets[index] = setItem.copy(isDone = completed)
                    },
                    onDeleteSet = if (sets.size > 1) {
                        {
                            sets.removeAt(index)
                            // Renumber remaining sets
                            for (i in sets.indices) {
                                sets[i] = sets[i].copy(setNumber = i + 1)
                            }
                        }
                    } else null
                )
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Session Notes") },
                placeholder = { Text("e.g. Felt strong, good form on last 2 sets") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Workout Button
            Button(
                onClick = {
                    val completed = sets.filter { it.isDone }
                    var volume = 0.0
                    var hasPr = false

                    completed.forEach { completedSet ->
                        val w = completedSet.weight.toDoubleOrNull() ?: 0.0
                        val r = completedSet.reps.toIntOrNull() ?: 0
                        volume += (w * r)
                        if (w > pbWeight && pbWeight > 0) {
                            hasPr = true
                        }
                        viewModel.logSet(
                            exerciseId = exerciseId,
                            exerciseName = exerciseName,
                            setNumber = completedSet.setNumber,
                            reps = r,
                            weight = w,
                            workoutPlanDay = workoutPlanDay,
                            notes = notes
                        )
                    }

                    if (completed.isNotEmpty()) {
                        totalVolumeCalculated = volume
                        completedSetsCount = completed.size
                        isNewPR = hasPr
                        showSummaryDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete & Save Session", fontWeight = FontWeight.Bold)
            }
        }
    }
}

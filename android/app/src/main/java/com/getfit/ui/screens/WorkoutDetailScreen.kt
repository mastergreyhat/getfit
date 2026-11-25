package com.getfit.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.getfit.ui.components.SetLogRow
import com.getfit.ui.components.core.GfCheckBadge
import com.getfit.ui.components.core.GfHeroCard
import com.getfit.ui.components.core.GfOverline
import com.getfit.ui.components.core.GfPrimaryButton
import com.getfit.ui.components.core.GfTextButton
import com.getfit.ui.components.core.GfTextField
import com.getfit.ui.components.core.gfBottomBarPadding
import com.getfit.ui.theme.Dimens
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
    targetSets: Int = 3,
    targetReps: String = "8-10",
    workoutPlanDay: String = "Push Day",
    viewModel: WorkoutViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exerciseLogs by viewModel.getLogsForExercise(exerciseId).collectAsStateWithLifecycle(initialValue = emptyList())
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

    LaunchedEffect(exerciseLogs, uiState.todayLogs, uiState.selectedDate, pbWeight, overloadInfo) {
        val selectedDateStr = uiState.selectedDate
        val groupedByDate = exerciseLogs.groupBy { it.date }
        val savedTodayList = (exerciseLogs.filter { it.date == selectedDateStr } +
            uiState.todayLogs.filter {
                it.exerciseId == exerciseId || it.exerciseName.equals(exerciseName, ignoreCase = true)
            }).distinctBy { it.setNumber }

        if (notes.isBlank()) {
            savedTodayList.firstOrNull { it.notes.isNotBlank() }?.notes?.let { notes = it }
        }

        val lastSession = groupedByDate.entries
            .filter { it.key < selectedDateStr }
            .maxByOrNull { it.key }
            ?.value
            ?: groupedByDate.entries.firstOrNull { it.key != selectedDateStr }?.value
            ?: emptyList()
        val prevMap = lastSession.associateBy { it.setNumber }
        val bestWeight = if (pbWeight > 0) pbWeight else (lastSession.maxOfOrNull { it.weight } ?: 0.0)
        val targetWeight = overloadInfo?.suggestedWeight ?: (if (bestWeight > 0) bestWeight else 50.0)

        val prevStrFor: (Int) -> String = { setNum ->
            val prevLog = prevMap[setNum]
            when {
                prevLog != null -> "${prevLog.weight.cleanFormat()}kg × ${prevLog.reps}"
                lastSession.isNotEmpty() -> "${lastSession.last().weight.cleanFormat()}kg × ${lastSession.last().reps}"
                bestWeight > 0 -> "${bestWeight.cleanFormat()}kg"
                else -> "—"
            }
        }

        if (savedTodayList.isNotEmpty()) {
            // Exact state saved on this date — never add or pad extra sets
            val restored = savedTodayList
                .sortedBy { it.setNumber }
                .mapIndexed { idx, savedLog ->
                    val num = idx + 1
                    ActiveSetState(
                        setNumber = num,
                        weight = savedLog.weight.cleanFormat(),
                        reps = savedLog.reps.toString(),
                        isDone = true,
                        previous = prevStrFor(num)
                    )
                }
            sets.clear()
            sets.addAll(restored)
        } else {
            sets.forEachIndexed { idx, item ->
                val setNum = item.setNumber
                val currentWeight = if (item.weight.isBlank() || item.weight == "50" || item.weight == "0") {
                    targetWeight.cleanFormat()
                } else {
                    item.weight
                }
                sets[idx] = item.copy(
                    previous = prevStrFor(setNum),
                    weight = currentWeight
                )
            }
        }
    }

    if (showSummaryDialog) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
            containerColor = GetFitTheme.colors.surface1,
            titleContentColor = GetFitTheme.colors.textPrimary,
            textContentColor = GetFitTheme.colors.textSecondary,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    GfCheckBadge(checked = true, size = 24.dp)
                    Text("Workout Complete!", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    Text(
                        text = "Awesome session on $exerciseName!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GetFitTheme.colors.textPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(Dimens.md),
                        color = GetFitTheme.colors.surface2,
                        border = BorderStroke(1.dp, GetFitTheme.colors.hairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimens.lg),
                            verticalArrangement = Arrangement.spacedBy(Dimens.xs)
                        ) {
                            Text(
                                text = "Total Volume Lifted: ${totalVolumeCalculated.toInt()} kg",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GetFitTheme.colors.accent
                            )
                            Text(
                                text = "Completed Sets: $completedSetsCount sets",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GetFitTheme.colors.textSecondary
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

                    Surface(
                        shape = RoundedCornerShape(Dimens.md),
                        color = GetFitTheme.colors.surface2
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.md),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = GetFitTheme.colors.accent
                            )
                            Text(
                                text = "Recovery Tip: Drink 500ml water and consume 25-35g protein within 45 minutes for optimal muscle protein synthesis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GetFitTheme.colors.textSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                GfTextButton(
                    text = "Done",
                    onClick = onNavigateBack
                )
            }
        )
    }

    Scaffold(
        containerColor = GetFitTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleLarge,
                        color = GetFitTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GetFitTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GetFitTheme.colors.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenGutter, vertical = Dimens.md),
            verticalArrangement = Arrangement.spacedBy(Dimens.lg)
        ) {
            // Exercise Overview Card (One hero per screen)
            GfHeroCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GfOverline(text = muscleGroup)
                        Text(
                            text = "$targetSets sets × $targetReps reps",
                            style = MaterialTheme.typography.titleMedium,
                            color = GetFitTheme.colors.textPrimary
                        )
                    }

                    if (pbWeight > 0) {
                        Text(
                            text = "Personal Best: ${pbWeight}kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textSecondary
                        )
                    }

                    // Progressive overload card
                    if (overloadInfo != null) {
                        Surface(
                            shape = RoundedCornerShape(Dimens.md),
                            color = GetFitTheme.colors.success.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(Dimens.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
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
                    .padding(horizontal = Dimens.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GfOverline(text = "SET LOG")
                GfTextButton(
                    text = "+ Add Set",
                    onClick = {
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
                        }
                    )
            }

            if (sets.isNotEmpty()) {
                // Column Headers Row directly above the fields: SET | PREVIOUS | WEIGHT (KG) | REPS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    Text(
                        text = "SET",
                        style = MaterialTheme.typography.labelSmall,
                        color = GetFitTheme.colors.textTertiary,
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "PREVIOUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = GetFitTheme.colors.textTertiary,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = "WEIGHT (KG)",
                        style = MaterialTheme.typography.labelSmall,
                        color = GetFitTheme.colors.textTertiary,
                        modifier = Modifier.width(76.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "REPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = GetFitTheme.colors.textTertiary,
                        modifier = Modifier.width(66.dp),
                        textAlign = TextAlign.Center
                    )
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
            GfTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Session Notes",
                placeholder = "e.g. Felt strong, good form on last 2 sets",
                maxLines = 3,
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.sm))

            // Save Workout Button
            GfPrimaryButton(
                text = "Complete & Save Session",
                onClick = {
                    val completed = sets.filter { (it.reps.toIntOrNull() ?: 0) > 0 }
                    var volume = 0.0
                    var hasPr = false
                    val setsPayload = completed.map { completedSet ->
                        val w = completedSet.weight.toDoubleOrNull() ?: 0.0
                        val r = completedSet.reps.toIntOrNull() ?: 0
                        volume += (w * r)
                        if (w > pbWeight && pbWeight > 0) {
                            hasPr = true
                        }
                        w to r
                    }

                    viewModel.saveSessionSets(
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
                        workoutPlanDay = workoutPlanDay,
                        notes = notes,
                        setsData = setsPayload
                    )

                    if (completed.isNotEmpty()) {
                        totalVolumeCalculated = volume
                        completedSetsCount = completed.size
                        isNewPR = hasPr
                        showSummaryDialog = true
                    } else {
                        onNavigateBack()
                    }
                }
            )
        }
    }
}

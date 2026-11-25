package com.getfit.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.getfit.data.entity.WeightLog
import com.getfit.ui.components.StrengthCurveChart
import com.getfit.ui.components.StrengthCurvePoint
import com.getfit.ui.components.core.GfBigNumber
import com.getfit.ui.components.core.GfCard
import com.getfit.ui.components.core.GfChipRow
import com.getfit.ui.components.core.GfHeroCard
import com.getfit.ui.components.core.GfOverline
import com.getfit.ui.components.core.GfProgressBar
import com.getfit.ui.components.core.GfSecondaryButton
import com.getfit.ui.components.core.GfTextButton
import com.getfit.ui.components.core.GfTextField
import com.getfit.ui.components.core.gfBottomBarPadding
import com.getfit.ui.components.core.pressable
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
        containerColor = GetFitTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Progress & Analytics",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(GetFitTheme.colors.background),
            verticalArrangement = Arrangement.spacedBy(Dimens.lg),
            contentPadding = PaddingValues(
                start = Dimens.screenGutter,
                end = Dimens.screenGutter,
                top = Dimens.md,
                bottom = Dimens.lg
            )
        ) {
            // Top Highlights Hero (One hero per screen)
            item {
                GfHeroCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HighlightStatColumn(
                            title = "Protein Hit",
                            value = uiState.proteinHitRatePercentage.toDouble(),
                            unit = "%",
                            subtitle = "7-Day Target",
                            color = GetFitTheme.colors.success,
                            modifier = Modifier.weight(1f)
                        )
                        Divider(
                            modifier = Modifier.height(48.dp).width(1.dp),
                            color = GetFitTheme.colors.hairline
                        )
                        val volInThousands = uiState.totalVolumePast30Days >= 1000.0
                        HighlightStatColumn(
                            title = "30-Day Vol",
                            value = if (volInThousands) uiState.totalVolumePast30Days / 1000.0 else uiState.totalVolumePast30Days,
                            decimals = if (volInThousands) 1 else 0,
                            unit = if (volInThousands) "k kg" else "kg",
                            subtitle = "Total Lifted",
                            color = GetFitTheme.colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Divider(
                            modifier = Modifier.height(48.dp).width(1.dp),
                            color = GetFitTheme.colors.hairline
                        )
                        HighlightStatColumn(
                            title = "Bodyweight",
                            value = uiState.currentWeight,
                            decimals = 1,
                            unit = "kg",
                            subtitle = "Current",
                            color = GetFitTheme.colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 7-Day Calorie & Macro Target Chart
            item {
                GfCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "7-Day Calorie Adherence",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GetFitTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(Dimens.sm))
                            Surface(
                                shape = RoundedCornerShape(Dimens.xs),
                                color = GetFitTheme.colors.surface2
                            ) {
                                Text(
                                    text = "Goal: ${uiState.userProfile.dailyCalorieTarget} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GetFitTheme.colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = Dimens.sm, vertical = Dimens.xs),
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
                                val hitGoal = stat.calories >= stat.targetCalories * 0.9

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
                                            color = if (hitGoal) GetFitTheme.colors.success else GetFitTheme.colors.textSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(Dimens.xs))

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
                                                .background(GetFitTheme.colors.surface2)
                                        )
                                        // Colored calorie fill
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(heightFraction)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(
                                                    if (hitGoal) GetFitTheme.colors.success
                                                    else GetFitTheme.colors.calories.copy(alpha = 0.85f)
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Day label - guaranteed visible for all 7 days
                                    Text(
                                        text = stat.dayLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (stat.calories > 0) GetFitTheme.colors.textPrimary else GetFitTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Strength Progression Curve
            item {
                GfCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Strength Progression Curve",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GetFitTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.personalBest > 0) {
                                Spacer(modifier = Modifier.width(Dimens.sm))
                                Surface(
                                    shape = RoundedCornerShape(Dimens.xs),
                                    color = GetFitTheme.colors.calories.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = GetFitTheme.colors.calories,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "PB: ${uiState.personalBest.toInt()}kg",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GetFitTheme.colors.calories
                                        )
                                    }
                                }
                            }
                        }

                        // Exercise selector chips
                        if (uiState.strengthCurveExercises.isNotEmpty()) {
                            GfChipRow(
                                items = uiState.strengthCurveExercises,
                                selectedItem = uiState.selectedExercise,
                                onItemSelected = { viewModel.selectExerciseForCurve(it) },
                                contentPadding = PaddingValues(0.dp)
                            )

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
                                color = GetFitTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Weekly Muscle Hypertrophy Volume Card
            item {
                GfCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Weekly Hypertrophy",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GetFitTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(Dimens.sm))
                            Surface(
                                shape = RoundedCornerShape(Dimens.xs),
                                color = GetFitTheme.colors.surface2
                            ) {
                                Text(
                                    text = "12-20 sets/wk",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GetFitTheme.colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = Dimens.sm, vertical = Dimens.xs),
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
                                    stat.completedSets > 0 -> GetFitTheme.colors.calories
                                    else -> GetFitTheme.colors.textTertiary
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stat.muscleGroup,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GetFitTheme.colors.textPrimary
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${stat.completedSets}/${stat.targetSetsMax} sets",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GetFitTheme.colors.textSecondary
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(Dimens.xs),
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
                                    GfProgressBar(
                                        progress = progress,
                                        color = statusColor,
                                        trackColor = GetFitTheme.colors.surface2
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Complete sets in your workout plan to track weekly volume for Chest, Back, Shoulders, Arms, and Legs!",
                                style = MaterialTheme.typography.bodySmall,
                                color = GetFitTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Bodyweight Tracking Section
            item {
                GfCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Bodyweight Tracker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GetFitTheme.colors.textPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "Press and hold to edit or delete",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = GetFitTheme.colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(Dimens.xs))
                            GfTextButton(
                                text = "+ Log Weight",
                                onClick = { showWeightDialog = true }
                            )
                        }

                        if (uiState.weightLogs.isEmpty()) {
                            Text(
                                text = "No weight logged yet. Current base weight is ${uiState.currentWeight} kg.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GetFitTheme.colors.textSecondary
                            )
                        } else {
                            uiState.weightLogs.reversed().forEach { weightLog ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(Dimens.sm))
                                        .pressable(
                                            onClick = { /* Tap disabled: press and hold only */ },
                                            onLongClick = { selectedWeightLogToManage = weightLog }
                                        )
                                        .padding(vertical = Dimens.xs, horizontal = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = weightLog.date,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GetFitTheme.colors.textSecondary
                                        )
                                        Text(
                                            text = "${weightLog.weightKg} kg",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GetFitTheme.colors.textPrimary
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
private fun HighlightStatColumn(
    title: String,
    value: Double,
    unit: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    decimals: Int = 0
) {
    Column(
        modifier = modifier.padding(horizontal = Dimens.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.xs)
    ) {
        GfOverline(text = title)
        GfBigNumber(
            value = value,
            decimals = decimals,
            unit = unit,
            color = color,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = GetFitTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        GfCard(
            cornerRadius = Dimens.xl,
            contentPadding = PaddingValues(Dimens.xl),
            useGradient = false,
            modifier = Modifier.padding(Dimens.lg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                Text(
                    text = "Log Bodyweight (kg)",
                    style = MaterialTheme.typography.titleLarge,
                    color = GetFitTheme.colors.textPrimary
                )

                GfTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = "Weight",
                    suffix = "kg",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GfTextButton(
                        text = "Save",
                        onClick = {
                            val w = weightText.toDoubleOrNull() ?: currentWeight
                            onConfirm(w)
                        }
                    )
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
        GfCard(
            cornerRadius = Dimens.xl,
            contentPadding = PaddingValues(Dimens.xl),
            useGradient = false,
            modifier = Modifier.padding(Dimens.lg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                Text(
                    text = "Bodyweight Entry",
                    style = MaterialTheme.typography.titleLarge,
                    color = GetFitTheme.colors.textPrimary
                )
                Text(
                    text = "Date: ${log.date} • Logged: ${log.weightKg} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GetFitTheme.colors.textSecondary
                )

                Divider(color = GetFitTheme.colors.hairline)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    GfTextButton(
                        text = "Delete",
                        color = GetFitTheme.colors.danger,
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    )

                    GfSecondaryButton(
                        text = "Edit",
                        compact = true,
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    )
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
        GfCard(
            cornerRadius = Dimens.xl,
            contentPadding = PaddingValues(Dimens.xl),
            useGradient = false,
            modifier = Modifier.padding(Dimens.lg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                Text(
                    text = "Edit Bodyweight",
                    style = MaterialTheme.typography.titleLarge,
                    color = GetFitTheme.colors.textPrimary
                )
                Text(
                    text = "Logged on ${log.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GetFitTheme.colors.textSecondary
                )

                GfTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = "Weight",
                    suffix = "kg",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GfTextButton(
                        text = "Update",
                        onClick = {
                            val w = weightText.toDoubleOrNull() ?: log.weightKg
                            onConfirm(w)
                        }
                    )
                }
            }
        }
    }
}

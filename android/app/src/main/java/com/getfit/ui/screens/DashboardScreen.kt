package com.getfit.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.getfit.ui.components.core.*
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * CRED & Apple TV inspired Dashboard screen for GetFit.
 * Features:
 * - One hero per screen: [GfHeroCard] with calorie ring, remaining kcal big number, and thin macro progress bars.
 * - Quiet weight and percentage met chips placed beside the header overline.
 * - Compact streak card with animated lightning bolt and tokenized weekly consistency row.
 * - Slim hydration card with quick log "+250 ml" secondary button.
 * - Pressable next meal card with overline and chevron.
 * - Primary CTA button to open Food Diary.
 * - Staggered entrance animation on first composition.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToFood: () -> Unit,
    onNavigateToHydration: () -> Unit = onNavigateToFood,
    onNavigateToWorkout: () -> Unit,
    onScrollChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    var showCalendarDialog by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value > 0 }.collect { onScrollChanged(it) }
    }

    val streakAnimationStage = when {
        uiState.currentStreak >= 3 -> 3
        uiState.currentStreak == 2 -> 2
        else -> 1
    }

    if (showCalendarDialog) {
        ConsistencyCalendarDialog(
            activeDates = uiState.allActiveDates,
            streakDates = uiState.streakDates,
            currentStreak = uiState.currentStreak,
            bestStreak = uiState.bestStreak,
            currentStage = streakAnimationStage,
            onDismiss = { showCalendarDialog = false }
        )
    }

    val calorieTarget = uiState.userProfile.dailyCalorieTarget.toFloat().coerceAtLeast(1000f)
    val caloriesConsumed = uiState.totalCalories.toFloat()
    val isOverGoal = caloriesConsumed > calorieTarget
    val calProgress = if (calorieTarget > 0f) {
        if (isOverGoal) 1f else (caloriesConsumed / calorieTarget).coerceIn(0f, 1f)
    } else 0f
    val remainingKcal = if (isOverGoal) {
        (caloriesConsumed - calorieTarget).toInt()
    } else {
        (calorieTarget - caloriesConsumed).toInt()
    }
    val realPercent = if (calorieTarget > 0f) {
        ((caloriesConsumed / calorieTarget) * 100).toInt()
    } else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GetFitTheme.colors.background)
            .verticalScroll(scrollState)
            .padding(start = Dimens.screenGutter, end = Dimens.screenGutter, top = Dimens.sm, bottom = Dimens.xl),
        verticalArrangement = Arrangement.spacedBy(Dimens.xxl)
    ) {
        // ==========================================
        // 1. HERO: CALORIE RING & MACROS
        // ==========================================
        GfHeroCard(
            modifier = Modifier.gfEntrance(0)
        ) {
            // Overline + Quiet Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GfOverline(text = "Daily Energy")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    // Weight Chip (surface2, labelMedium)
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = GetFitTheme.colors.surface2
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", uiState.latestWeightKg)} kg",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GetFitTheme.colors.textPrimary
                            )
                            if (uiState.hasWeightLog && abs(uiState.weightDeltaKg) >= 0.1) {
                                val isDown = uiState.weightDeltaKg < 0
                                Text(
                                    text = if (isDown) "▼ ${String.format(Locale.US, "%.1f", -uiState.weightDeltaKg)}" else "▲ ${String.format(Locale.US, "%.1f", uiState.weightDeltaKg)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDown) GetFitTheme.colors.success else GetFitTheme.colors.carbs
                                )
                            }
                        }
                    }

                    // % Met Chip (surface2, labelMedium) - real percentage, can exceed 100%
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = GetFitTheme.colors.surface2
                    ) {
                        Text(
                            text = "$realPercent% Met",
                            modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.xs),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GetFitTheme.colors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.md))

            // Balanced Hero: fixed-size true-circle Ring on left, flexible Macros on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.xs),
                horizontalArrangement = Arrangement.spacedBy(Dimens.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(138.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.xs)
                ) {
                    GfRing(
                        progress = calProgress,
                        size = 138.dp,
                        strokeWidth = 11.dp,
                        overGoal = isOverGoal
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            GfBigNumber(
                                value = remainingKcal,
                                hero = true,
                                color = if (isOverGoal) GetFitTheme.colors.warning else GetFitTheme.colors.textPrimary,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 40.sp,
                                    lineHeight = 44.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = if (isOverGoal) "kcal over" else "kcal left",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverGoal) GetFitTheme.colors.warning else GetFitTheme.colors.textSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Text(
                        text = "${caloriesConsumed.toInt()} / ${calorieTarget.toInt()} kcal",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = GetFitTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.lg)
                ) {
                    val proteinTarget = uiState.userProfile.dailyProteinTarget.toFloat().coerceAtLeast(100f)
                    val carbsTarget = uiState.userProfile.dailyCarbsTarget.toFloat().coerceAtLeast(150f)
                    val fatTarget = uiState.userProfile.dailyFatTarget.toFloat().coerceAtLeast(40f)

                    MacroStat(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Protein",
                        current = uiState.totalProtein.toFloat(),
                        target = proteinTarget,
                        color = GetFitTheme.colors.protein,
                        inlineHeader = true
                    )
                    MacroStat(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Carbs",
                        current = uiState.totalCarbs.toFloat(),
                        target = carbsTarget,
                        color = GetFitTheme.colors.carbs,
                        inlineHeader = true
                    )
                    MacroStat(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Fat",
                        current = uiState.totalFat.toFloat(),
                        target = fatTarget,
                        color = GetFitTheme.colors.fat,
                        inlineHeader = true
                    )
                }
            }
        }

        // ==========================================
        // 2. STREAK CARD
        // ==========================================
        GfCard(
            modifier = Modifier.gfEntrance(1),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showCalendarDialog = true
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.md)
                ) {
                    AnimatedThunderbolt(
                        stage = streakAnimationStage,
                        isLoggedToday = uiState.isLoggedToday,
                        size = 40.dp
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                        ) {
                            Text(
                                text = if (uiState.currentStreak > 0) "${uiState.currentStreak}-Day Streak" else "Start Your Streak",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GetFitTheme.colors.textPrimary
                            )
                            if (uiState.isLoggedToday) {
                                Surface(
                                    shape = CircleShape,
                                    color = GetFitTheme.colors.accent.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                        color = GetFitTheme.colors.accent,
                                        modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 2.dp)
                                    )
                                }
                            }
                            if (uiState.monthlyBadgeUnlocked) {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = GetFitTheme.colors.surface2,
                                    border = BorderStroke(0.5.dp, GetFitTheme.colors.hairline)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = GetFitTheme.colors.carbs,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "BADGE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GetFitTheme.colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = if (uiState.isLoggedToday) {
                                "Logged today • Streak maintained!"
                            } else if (uiState.currentStreak > 0) {
                                "Log a meal or workout today to keep your streak!"
                            } else {
                                "Log food or workout today to begin!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = GetFitTheme.colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.md))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GetFitTheme.colors.hairline)
            )
            Spacer(modifier = Modifier.height(Dimens.md))

            // Weekly 7-Day Consistency Row (Mon to Sun)
            // Today = filled accent dot with onAccent number
            // Completed = accent outline
            // Future = hairline with textTertiary number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.weeklyDayStatus.forEach { day ->
                    val isStreak = uiState.streakDates.contains(day.dateStr)
                    val isDone = isStreak || day.isCompleted

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.xs)
                    ) {
                        Text(
                            text = day.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (day.isToday) GetFitTheme.colors.textPrimary else GetFitTheme.colors.textTertiary
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .then(
                                    when {
                                        day.isToday -> Modifier.background(GetFitTheme.colors.accent)
                                        isDone -> Modifier
                                            .background(Color.Transparent)
                                            .border(1.5.dp, GetFitTheme.colors.accent, CircleShape)
                                        else -> Modifier
                                            .background(Color.Transparent)
                                            .border(1.dp, GetFitTheme.colors.hairline, CircleShape)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.dayNumber,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (day.isToday || isDone) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    day.isToday -> GetFitTheme.colors.onAccent
                                    isDone -> GetFitTheme.colors.textPrimary
                                    else -> GetFitTheme.colors.textTertiary
                                }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. HYDRATION CARD (Tap -> Nutrition Diary Hydration)
        // ==========================================
        GfCard(
            modifier = Modifier.gfEntrance(2),
            onClick = onNavigateToHydration
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    GfOverline(text = "Daily Hydration")
                    Spacer(modifier = Modifier.height(Dimens.xs))
                    GfBigNumber(
                        value = uiState.todayWaterMl,
                        unit = "ml",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Text(
                        text = "Target: ${uiState.dailyWaterTarget} ml",
                        style = MaterialTheme.typography.bodySmall,
                        color = GetFitTheme.colors.textSecondary
                    )
                }

                GfSecondaryButton(
                    text = "+250 ml",
                    onClick = { viewModel.quickAddWater(250) },
                    compact = true
                )
            }

            Spacer(modifier = Modifier.height(Dimens.md))

            val waterProgress = if (uiState.dailyWaterTarget > 0) {
                (uiState.todayWaterMl.toFloat() / uiState.dailyWaterTarget).coerceIn(0f, 1f)
            } else 0f

            GfProgressBar(
                progress = waterProgress,
                color = GetFitTheme.colors.water
            )
        }

        // ==========================================
        // 4. MEAL STATUS CARD — dynamic per-meal logged state from diary
        // ==========================================
        GfCard(
            modifier = Modifier.gfEntrance(3),
            onClick = onNavigateToFood
        ) {
            GfOverline(text = "TODAY'S MEALS")
            Spacer(modifier = Modifier.height(Dimens.sm + 4.dp))

            if (uiState.mealStatusList.none { it.isLogged }) {
                Text(
                    text = "No meals logged today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GetFitTheme.colors.textSecondary
                )
            } else {
                Column {
                    uiState.mealStatusList.forEachIndexed { index, ms ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GfCheckBadge(checked = ms.isLogged)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ms.mealType,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (ms.isLogged) GetFitTheme.colors.textPrimary else GetFitTheme.colors.textTertiary
                                )
                                Text(
                                    text = if (ms.isLogged) ms.loggedSummary else "Not logged",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GetFitTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(Dimens.sm))
                            Text(
                                text = ms.scheduledTime,
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                color = GetFitTheme.colors.textTertiary
                            )
                        }
                        if (index < uiState.mealStatusList.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 34.dp)
                                    .height(1.dp)
                                    .background(GetFitTheme.colors.hairline)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. PRIMARY CTA: OPEN FOOD DIARY
        // ==========================================
        GfPrimaryButton(
            text = "Open Food Diary",
            onClick = onNavigateToFood,
            icon = Icons.Default.Restaurant,
            modifier = Modifier.gfEntrance(4)
        )

        Spacer(modifier = Modifier.height(com.getfit.ui.components.core.gfBottomBarContentPadding()))
    }
}

/**
 * Minimal streak bolt tile:
 * - 40.dp circle with surface2 fill and 1.dp hairline border (matching the Analytics icon button).
 * - 18.dp stroke-only (1.75.dp) bolt in accent when active today, or textTertiary when inactive.
 * - Subtle one-time entrance scale/fade; no infinite animations. Respects ANIMATOR_DURATION_SCALE == 0f.
 */
@Composable
fun AnimatedThunderbolt(
    stage: Int = 1,
    isLoggedToday: Boolean = true,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    val activeAnim = isLoggedToday && !reducedMotion

    val tileScale = remember(isLoggedToday, reducedMotion) {
        Animatable(if (activeAnim) 0.85f else 1f)
    }
    val boltAlpha = remember(isLoggedToday, reducedMotion) {
        Animatable(if (activeAnim) 0f else 1f)
    }

    LaunchedEffect(activeAnim) {
        if (activeAnim) {
            tileScale.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
            boltAlpha.animateTo(1f, tween(durationMillis = 200))
        }
    }

    val boltColor = if (isLoggedToday) GetFitTheme.colors.accent else GetFitTheme.colors.textTertiary

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = tileScale.value
                scaleY = tileScale.value
            }
            .clip(CircleShape)
            .background(GetFitTheme.colors.surface2)
            .border(1.dp, GetFitTheme.colors.hairline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tileW = this.size.width
            val tileH = this.size.height

            val boltSize = (18.dp.toPx()).coerceAtMost(tileW * 0.55f)
            val left = (tileW - boltSize) / 2f
            val top = (tileH - boltSize) / 2f

            val boltPath = Path().apply {
                moveTo(left + boltSize * 0.58f, top)
                lineTo(left + boltSize * 0.16f, top + boltSize * 0.54f)
                lineTo(left + boltSize * 0.48f, top + boltSize * 0.54f)
                lineTo(left + boltSize * 0.38f, top + boltSize * 1.0f)
                lineTo(left + boltSize * 0.84f, top + boltSize * 0.44f)
                lineTo(left + boltSize * 0.52f, top + boltSize * 0.44f)
                close()
            }

            drawPath(
                path = boltPath,
                color = boltColor.copy(alpha = boltColor.alpha * boltAlpha.value),
                style = Stroke(
                    width = 1.75.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Preview(name = "Streak Tile - 3 States Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun StreakTileStatesDarkPreview() {
    GetFitTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedThunderbolt(stage = 1, isLoggedToday = false)
            AnimatedThunderbolt(stage = 1, isLoggedToday = true)
            AnimatedThunderbolt(stage = 3, isLoggedToday = true)
        }
    }
}

/**
 * Interactive Consistency Calendar Modal Dialog.
 * Styled using GetFit design tokens (surface1, hairline, textPrimary, textSecondary).
 */
@Composable
private fun ConsistencyCalendarDialog(
    activeDates: Set<String>,
    streakDates: Set<String>,
    currentStreak: Int,
    bestStreak: Int,
    currentStage: Int,
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

    val firstDayCal = remember(displayedCal) {
        (displayedCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)
    val leadingEmpty = (firstDayOfWeek + 5) % 7
    val daysInMonth = displayedCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    val monthPrefix = remember(displayedCal) {
        SimpleDateFormat("yyyy-MM-", Locale.getDefault()).format(displayedCal.time)
    }
    val loggedDaysInDisplayedMonth = remember(monthPrefix, activeDates) {
        activeDates.count { it.startsWith(monthPrefix) }
    }

    Dialog(onDismissRequest = onDismiss) {
        GfCard(
            cornerRadius = Dimens.xxl,
            useGradient = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.lg)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.lg)
            ) {
                // Header with Month Navigation (< Month Year >)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                    ) {
                        AnimatedThunderbolt(stage = currentStage, size = 26.dp)
                        Column {
                            Text(
                                text = "Consistency Calendar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
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
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Next Month",
                                        tint = GetFitTheme.colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (monthOffset != 0) {
                                    Spacer(modifier = Modifier.width(Dimens.xs))
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = GetFitTheme.colors.surface2,
                                        modifier = Modifier.clickable { monthOffset = 0 }
                                    ) {
                                        Text(
                                            text = "Today",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GetFitTheme.colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 2.dp)
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

                // Summary Stats Pills Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    Surface(
                        shape = RoundedCornerShape(Dimens.sm),
                        color = GetFitTheme.colors.surface2,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = Dimens.sm, vertical = Dimens.xs),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (currentStreak == 1) "1 Day" else "$currentStreak Days",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GetFitTheme.colors.textPrimary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = GetFitTheme.colors.textSecondary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(Dimens.sm),
                        color = GetFitTheme.colors.surface2,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = Dimens.sm, vertical = Dimens.xs),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (bestStreak == 1) "1 Day" else "$bestStreak Days",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GetFitTheme.colors.textPrimary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Best",
                                style = MaterialTheme.typography.labelSmall,
                                color = GetFitTheme.colors.textSecondary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(Dimens.sm),
                        color = GetFitTheme.colors.surface2,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = Dimens.sm, vertical = Dimens.xs),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$loggedDaysInDisplayedMonth / $daysInMonth",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GetFitTheme.colors.textPrimary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = GetFitTheme.colors.textSecondary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GetFitTheme.colors.hairline)
                )

                // Days of week header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayLabels.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GetFitTheme.colors.textTertiary
                        )
                    }
                }

                // Fixed 6-week grid
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
                                    val isStreak = streakDates.contains(dateStr)
                                    val isLogged = activeDates.contains(dateStr)
                                    val isToday = (dateStr == todayStr)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .then(
                                                when {
                                                    isStreak -> Modifier.background(GetFitTheme.colors.accent)
                                                    isLogged -> Modifier
                                                        .background(GetFitTheme.colors.surface2)
                                                        .border(1.dp, GetFitTheme.colors.accent.copy(alpha = 0.6f), CircleShape)
                                                    isToday -> Modifier
                                                        .background(GetFitTheme.colors.surface3)
                                                        .border(1.dp, GetFitTheme.colors.textTertiary, CircleShape)
                                                    else -> Modifier.background(Color.Transparent)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isStreak || isLogged || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isStreak -> GetFitTheme.colors.onAccent
                                                isLogged -> GetFitTheme.colors.textPrimary
                                                isToday -> GetFitTheme.colors.accent
                                                else -> GetFitTheme.colors.textTertiary
                                            }
                                        )
                                    }
                                } else {
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

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GetFitTheme.colors.accent)
                        )
                        Text("Streak", style = MaterialTheme.typography.labelSmall, color = GetFitTheme.colors.textSecondary, maxLines = 1, softWrap = false)
                    }
                    Spacer(modifier = Modifier.width(Dimens.md))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GetFitTheme.colors.surface2)
                                .border(1.dp, GetFitTheme.colors.accent.copy(alpha = 0.6f), CircleShape)
                        )
                        Text("Logged", style = MaterialTheme.typography.labelSmall, color = GetFitTheme.colors.textSecondary, maxLines = 1, softWrap = false)
                    }
                    Spacer(modifier = Modifier.width(Dimens.md))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GetFitTheme.colors.surface3)
                                .border(1.dp, GetFitTheme.colors.textTertiary, CircleShape)
                        )
                        Text("Today", style = MaterialTheme.typography.labelSmall, color = GetFitTheme.colors.textSecondary, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

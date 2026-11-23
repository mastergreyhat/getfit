package com.getfit.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Check
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
    onNavigateToWorkout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showCalendarDialog by remember { mutableStateOf(false) }

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
            .gfBottomBarPadding()
            .padding(horizontal = Dimens.screenGutter, vertical = Dimens.xl),
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

            Spacer(modifier = Modifier.height(Dimens.lg))

            // Central Calorie Ring Hero
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GfRing(
                    progress = calProgress,
                    size = 208.dp,
                    strokeWidth = 12.dp,
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
                                fontSize = 52.sp,
                                lineHeight = 56.sp
                            )
                        )
                        Text(
                            text = if (isOverGoal) "kcal over" else "kcal left",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOverGoal) GetFitTheme.colors.warning else GetFitTheme.colors.textSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.sm))

            // Quiet secondary stats below ring: "{eaten} eaten · {goal} goal"
            Text(
                text = "${caloriesConsumed.toInt()} eaten · ${calorieTarget.toInt()} goal",
                style = MaterialTheme.typography.bodySmall,
                color = GetFitTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.lg))

            // Under the ring: row of three compact macro stats (Protein, Carbs, Fat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.lg)
            ) {
                val proteinTarget = uiState.userProfile.dailyProteinTarget.toFloat().coerceAtLeast(100f)
                val carbsTarget = uiState.userProfile.dailyCarbsTarget.toFloat().coerceAtLeast(150f)
                val fatTarget = uiState.userProfile.dailyFatTarget.toFloat().coerceAtLeast(40f)

                MacroStat(
                    modifier = Modifier.weight(1f),
                    label = "Protein",
                    current = uiState.totalProtein.toFloat(),
                    target = proteinTarget,
                    color = GetFitTheme.colors.protein
                )
                MacroStat(
                    modifier = Modifier.weight(1f),
                    label = "Carbs",
                    current = uiState.totalCarbs.toFloat(),
                    target = carbsTarget,
                    color = GetFitTheme.colors.carbs
                )
                MacroStat(
                    modifier = Modifier.weight(1f),
                    label = "Fat",
                    current = uiState.totalFat.toFloat(),
                    target = fatTarget,
                    color = GetFitTheme.colors.fat
                )
            }
        }

        // ==========================================
        // 2. STREAK CARD
        // ==========================================
        GfCard(
            modifier = Modifier.gfEntrance(1),
            onClick = { showCalendarDialog = true }
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
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedThunderbolt(
                            stage = streakAnimationStage,
                            size = 30.dp
                        )
                    }
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
                                    shape = RoundedCornerShape(100.dp),
                                    color = GetFitTheme.colors.surface2
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
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
                                            .background(GetFitTheme.colors.surface2)
                                            .border(1.dp, GetFitTheme.colors.accent, CircleShape)
                                        else -> Modifier
                                            .background(Color.Transparent)
                                            .border(1.dp, GetFitTheme.colors.hairline, CircleShape)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day.isToday) {
                                Text(
                                    text = day.dayNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GetFitTheme.colors.onAccent
                                )
                            } else if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = GetFitTheme.colors.accent,
                                    modifier = Modifier.size(13.dp)
                                )
                            } else {
                                Text(
                                    text = day.dayNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Normal,
                                    color = GetFitTheme.colors.textTertiary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. HYDRATION CARD
        // ==========================================
        GfCard(
            modifier = Modifier.gfEntrance(2)
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
        // 4. NEXT SCHEDULED MEAL CARD
        // ==========================================
        GfCard(
            modifier = Modifier.gfEntrance(3),
            onClick = onNavigateToFood
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    GfOverline(text = "NEXT • ${if (uiState.nextMealTime.isNotBlank()) uiState.nextMealTime else "07:30"}")
                    Spacer(modifier = Modifier.height(Dimens.xs))
                    Text(
                        text = if (uiState.nextMealTitle.isNotBlank()) uiState.nextMealTitle else "Next Meal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GetFitTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (uiState.nextMealSuggestion.isNotBlank()) uiState.nextMealSuggestion else "Tap to log this meal",
                        style = MaterialTheme.typography.bodySmall,
                        color = GetFitTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = GetFitTheme.colors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
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
    }
}

/**
 * Animated Thunderbolt Icon with progressive intensity stages:
 * - Stage 1 (Day 1): Small, gentle wave, warm golden-yellow flow.
 * - Stage 2 (Day 2): Faster wave, vibrant electric yellow into electric cyan.
 * - Stage 3 (Day 3+): Intense lightning surge with animated electric particle circling the bolt.
 *
 * Glow smoothly dissolves into the card surface without bounding boxes or sharp outlines.
 */
@Composable
fun AnimatedThunderbolt(
    stage: Int = 1,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thunderbolt_anim")

    val waveDuration = when (stage) {
        1 -> 1600
        2 -> 900
        else -> 450
    }

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(waveDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = when (stage) {
            1 -> 0.98f
            2 -> 0.96f
            else -> 0.94f
        },
        targetValue = when (stage) {
            1 -> 1.02f
            2 -> 1.04f
            else -> 1.06f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(when (stage) {
                1 -> 700
                2 -> 500
                else -> 320
            }, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = when (stage) {
            1 -> 0.18f
            2 -> 0.28f
            else -> 0.35f
        },
        targetValue = when (stage) {
            1 -> 0.42f
            2 -> 0.65f
            else -> 0.85f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(when (stage) {
                1 -> 650
                2 -> 500
                else -> 320
            }, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val boltAccent = GetFitTheme.colors.accent
    val boltGlow = GetFitTheme.colors.streakGlow
    val boltText = GetFitTheme.colors.textPrimary

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                this.scaleX = pulseScale
                this.scaleY = pulseScale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            val glowColors = listOf(
                boltAccent.copy(alpha = 0.25f * glowAlpha),
                boltGlow.copy(alpha = 0.12f * glowAlpha),
                Color.Transparent
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = glowColors,
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.85f
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.85f
            )

            val boltPath = Path().apply {
                moveTo(w * 0.58f, 0f)
                lineTo(w * 0.16f, h * 0.54f)
                lineTo(w * 0.48f, h * 0.54f)
                lineTo(w * 0.38f, h * 1.0f)
                lineTo(w * 0.84f, h * 0.44f)
                lineTo(w * 0.52f, h * 0.44f)
                close()
            }

            clipPath(boltPath) {
                val shiftY = waveOffset * h * 2f
                val gradientColors = listOf(
                    boltAccent,
                    boltAccent.copy(alpha = 0.85f),
                    boltText.copy(alpha = 0.9f),
                    boltAccent
                )

                val electricBrush = Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(w * 0.2f, -h + shiftY),
                    end = Offset(w * 0.8f, h + shiftY)
                )
                drawRect(brush = electricBrush)

                val waveY = (shiftY % (h * 1.6f)) - (h * 0.3f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            boltText.copy(alpha = if (stage == 1) 0.5f else 0.85f),
                            boltAccent.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, waveY),
                        radius = w * 0.65f
                    ),
                    center = Offset(w * 0.5f, waveY),
                    radius = w * 0.65f
                )
            }

            val strokeColor = boltAccent.copy(alpha = 0.75f)
            drawPath(
                path = boltPath,
                color = strokeColor,
                style = Stroke(
                    width = 1.1.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
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
                                text = "$currentStreak Days",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GetFitTheme.colors.textPrimary
                            )
                            Text(
                                text = "Current Streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = GetFitTheme.colors.textSecondary
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
                                text = "$bestStreak Days",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GetFitTheme.colors.textPrimary
                            )
                            Text(
                                text = "Best Streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = GetFitTheme.colors.textSecondary
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
                                color = GetFitTheme.colors.textPrimary
                            )
                            Text(
                                text = "Active Days",
                                style = MaterialTheme.typography.labelSmall,
                                color = GetFitTheme.colors.textSecondary
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
                                            .background(
                                                when {
                                                    isStreak -> GetFitTheme.colors.accent
                                                    isLogged -> GetFitTheme.colors.surface2
                                                    isToday -> GetFitTheme.colors.surface3
                                                    else -> Color.Transparent
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isStreak) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GetFitTheme.colors.onAccent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        } else if (isLogged) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GetFitTheme.colors.textPrimary
                                            )
                                        } else {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isToday) GetFitTheme.colors.accent else GetFitTheme.colors.textTertiary
                                            )
                                        }
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
                        Text("Active Streak", style = MaterialTheme.typography.labelSmall, color = GetFitTheme.colors.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(Dimens.lg))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GetFitTheme.colors.surface2)
                        )
                        Text("Logged Day", style = MaterialTheme.typography.labelSmall, color = GetFitTheme.colors.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(Dimens.lg))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GetFitTheme.colors.surface3)
                        )
                        Text("Today", style = MaterialTheme.typography.labelSmall, color = GetFitTheme.colors.textSecondary)
                    }
                }
            }
        }
    }
}

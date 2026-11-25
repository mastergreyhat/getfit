package com.getfit.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.getfit.ui.components.core.GfBottomSheet
import com.getfit.ui.components.core.GfCard
import com.getfit.ui.components.core.GfOverline
import com.getfit.ui.components.core.GfPrimaryButton
import com.getfit.ui.components.core.GfProgressBar
import com.getfit.ui.components.core.GfSecondaryButton
import com.getfit.ui.components.core.GfStatusPill
import com.getfit.ui.components.core.GfTextButton
import com.getfit.ui.components.core.pressable
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.util.AvatarManager
import com.getfit.ui.util.ProfileAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerDialog(
    currentAvatar: ProfileAvatar,
    bestStreak: Int,
    onSelectAvatar: (ProfileAvatar) -> Unit,
    onDismiss: () -> Unit,
    initialPreviewAvatar: ProfileAvatar? = null
) {
    val avatars = remember { AvatarManager.getAllAvatars() }
    var previewAvatar by remember(initialPreviewAvatar) { mutableStateOf(initialPreviewAvatar) }
    val haptic = LocalHapticFeedback.current

    GfBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenGutter)
                .padding(bottom = Dimens.xl),
            verticalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                GfOverline(text = "AVATARS")
                Text(
                    text = "Choose your avatar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GetFitTheme.colors.textPrimary
                )
                Text(
                    text = "Best streak: $bestStreak days",
                    style = MaterialTheme.typography.bodySmall,
                    color = GetFitTheme.colors.textSecondary
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.md),
                horizontalArrangement = Arrangement.spacedBy(Dimens.md),
                contentPadding = PaddingValues(bottom = Dimens.sm)
            ) {
                items(avatars, key = { it.id }) { avatar ->
                    val isUnlocked = AvatarManager.isUnlocked(avatar, bestStreak)
                    val isEquipped = currentAvatar.id == avatar.id
                    val daysToGo = AvatarManager.daysRemaining(avatar, bestStreak)
                    val progress = if (avatar.requiredStreakDays > 0) {
                        (bestStreak.toFloat() / avatar.requiredStreakDays.toFloat()).coerceIn(0f, 1f)
                    } else 1f

                    GfCard(
                        cornerRadius = Dimens.lg,
                        contentPadding = PaddingValues(Dimens.md),
                        useGradient = false,
                        onClick = { previewAvatar = avatar },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimens.xs)
                        ) {
                            AvatarImage(
                                avatar = avatar,
                                size = 88.dp,
                                locked = !isUnlocked,
                                daysToGo = daysToGo
                            )
                            Spacer(modifier = Modifier.height(Dimens.xs))
                            Text(
                                text = avatar.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = GetFitTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            GfOverline(text = avatar.tierLabel)
                            Spacer(modifier = Modifier.height(Dimens.xs))

                            when {
                                isEquipped -> {
                                    GfStatusPill(text = "Equipped")
                                }
                                isUnlocked -> {
                                    GfSecondaryButton(
                                        text = "Equip",
                                        compact = true,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectAvatar(avatar)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                else -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        GfProgressBar(
                                            progress = progress,
                                            color = GetFitTheme.colors.accent,
                                            modifier = Modifier.height(4.dp)
                                        )
                                        Text(
                                            text = "$bestStreak / ${avatar.requiredStreakDays} days",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                            color = GetFitTheme.colors.textPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "$daysToGo days to go",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GetFitTheme.colors.textSecondary,
                                            maxLines = 1
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

    if (previewAvatar != null) {
        AvatarPreviewSheet(
            initialAvatar = previewAvatar!!,
            currentAvatar = currentAvatar,
            bestStreak = bestStreak,
            onEquip = { selected ->
                onSelectAvatar(selected)
                previewAvatar = null
            },
            onDismiss = { previewAvatar = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AvatarPreviewSheet(
    initialAvatar: ProfileAvatar,
    currentAvatar: ProfileAvatar,
    bestStreak: Int,
    onEquip: (ProfileAvatar) -> Unit,
    onDismiss: () -> Unit
) {
    val avatars = remember { AvatarManager.getAllAvatars() }
    val initialPage = remember(initialAvatar) {
        avatars.indexOfFirst { it.id == initialAvatar.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { avatars.size })
    val haptic = LocalHapticFeedback.current

    GfBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenGutter)
                .padding(bottom = Dimens.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val avatar = avatars[page]
                val isUnlocked = AvatarManager.isUnlocked(avatar, bestStreak)
                val isEquipped = currentAvatar.id == avatar.id
                val daysToGo = AvatarManager.daysRemaining(avatar, bestStreak)
                val progress = if (avatar.requiredStreakDays > 0) {
                    (bestStreak.toFloat() / avatar.requiredStreakDays.toFloat()).coerceIn(0f, 1f)
                } else 1f
                val glowBrush = remember(avatar.accentColor) {
                    Brush.radialGradient(
                        colors = listOf(avatar.accentColor.copy(alpha = 0.08f), Color.Transparent)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(264.dp)
                            .drawBehind { drawCircle(brush = glowBrush) },
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarImage(
                            avatar = avatar,
                            size = 240.dp,
                            locked = !isUnlocked,
                            daysToGo = daysToGo
                        )
                    }

                    Text(
                        text = avatar.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GetFitTheme.colors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(Dimens.xs),
                        color = GetFitTheme.colors.surface2
                    ) {
                        GfOverline(
                            text = avatar.tierLabel,
                            modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = avatar.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GetFitTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (avatar.requiredStreakDays > 0) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Dimens.xs)
                        ) {
                            Text(
                                text = "Requires a ${avatar.requiredStreakDays}-day streak",
                                style = MaterialTheme.typography.labelMedium,
                                color = GetFitTheme.colors.textPrimary
                            )
                            GfProgressBar(
                                progress = progress,
                                color = GetFitTheme.colors.accent,
                                modifier = Modifier.height(6.dp)
                            )
                            Text(
                                text = if (isUnlocked) {
                                    "Your best: $bestStreak days • Unlocked"
                                } else {
                                    "Your best: $bestStreak days • $daysToGo to go"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = GetFitTheme.colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.xs))

                    if (isUnlocked) {
                        GfPrimaryButton(
                            text = if (isEquipped) "Equipped" else "Set as avatar",
                            enabled = !isEquipped,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEquip(avatar)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        GfSecondaryButton(
                            text = "Reach a ${avatar.requiredStreakDays}-day streak to unlock",
                            enabled = false,
                            onClick = {},
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Pager indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(avatars.size) { idx ->
                    val selected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) GetFitTheme.colors.accent
                                else GetFitTheme.colors.surface3
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarUnlockSheet(
    avatar: ProfileAvatar,
    onEquipNow: () -> Unit,
    onLater: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(avatar.id) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val glowBrush = remember(avatar.accentColor) {
        Brush.radialGradient(
            colors = listOf(avatar.accentColor.copy(alpha = 0.08f), Color.Transparent)
        )
    }

    GfBottomSheet(onDismissRequest = onLater) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenGutter)
                .padding(bottom = Dimens.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            GfOverline(text = "MILESTONE REACHED")
            Text(
                text = "New avatar unlocked",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GetFitTheme.colors.textPrimary
            )

            Box(
                modifier = Modifier
                    .size(164.dp)
                    .drawBehind { drawCircle(brush = glowBrush) },
                contentAlignment = Alignment.Center
            ) {
                AvatarImage(
                    avatar = avatar,
                    size = 140.dp,
                    locked = false
                )
            }

            Text(
                text = avatar.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GetFitTheme.colors.textPrimary
            )
            Text(
                text = avatar.description,
                style = MaterialTheme.typography.bodyMedium,
                color = GetFitTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )

            GfPrimaryButton(
                text = "Equip now",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEquipNow()
                },
                modifier = Modifier.fillMaxWidth()
            )
            GfTextButton(
                text = "Later",
                color = GetFitTheme.colors.textSecondary,
                onClick = onLater
            )
        }
    }
}

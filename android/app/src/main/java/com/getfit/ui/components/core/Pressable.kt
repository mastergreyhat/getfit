package com.getfit.ui.components.core

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.delay

/**
 * Tactical spring press feedback modifier built on top of Compose interaction source
 * and semantic clickable/combinedClickable.
 *
 * - Scales to 0.97 on press (dampingRatio 0.75, stiffness 500) inside graphicsLayer { scaleX = s; scaleY = s }
 *   via a State read in the lambda to prevent recomposition per frame.
 * - Reads press state via interactionSource.collectIsPressedAsState(), ensuring that when a list is scrolled,
 *   Android gesture cancellation resets isPressed without getting stuck scaled down.
 * - Accessibility: Sets role = Role.Button so TalkBack announces pressable elements as buttons.
 * - Haptic: Long press triggers HapticFeedbackType.LongPress; tap triggers optional light TextHandleMove.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressable(
    enabled: Boolean = true,
    haptic: Boolean = true,
    pressedScale: Float = 0.97f,
    pressDurationMs: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled || (onClick == null && onLongClick == null)) return@composed this

    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = if (pressDurationMs != null) {
            tween(durationMillis = pressDurationMs, easing = FastOutSlowInEasing)
        } else {
            spring(
                dampingRatio = 0.75f,
                stiffness = 500f
            )
        },
        label = "pressable_scale"
    )

    this
        .graphicsLayer {
            val s = scaleState.value
            scaleX = s
            scaleY = s
        }
        .then(
            if (onLongClick != null) {
                Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                    onClick = {
                        if (haptic) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onClick?.invoke()
                    }
                )
            } else {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = {
                        if (haptic) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onClick?.invoke()
                    }
                )
            }
        )
}

/**
 * Process-level gate so staggered entrance animations only run on first launch of the session.
 */
object EntranceGate {
    var homePlayed = false
}

/**
 * Entrance animation on first app launch only:
 * Fades in and slides up 12dp, with [staggerIndex] * [staggerMs] delay (capped at 150ms).
 * Once [EntranceGate.homePlayed] is true, renders immediately at final state.
 */
@Composable
fun Modifier.gfEntrance(
    staggerIndex: Int,
    staggerMs: Long = 30L,
    durationMs: Int = 150
): Modifier {
    val shouldAnimate = remember { !EntranceGate.homePlayed }
    if (!shouldAnimate) return this

    var hasAppeared by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasAppeared) {
            val clampedDelay = (staggerIndex.coerceIn(0, 5) * staggerMs).coerceAtMost(150L)
            delay(clampedDelay)
            hasAppeared = true
            delay(durationMs.toLong())
            EntranceGate.homePlayed = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
        label = "gf_entrance_alpha_$staggerIndex"
    )
    val translationY by animateFloatAsState(
        targetValue = if (hasAppeared) 0f else 12f,
        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
        label = "gf_entrance_trans_$staggerIndex"
    )

    return this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY * density
    }
}


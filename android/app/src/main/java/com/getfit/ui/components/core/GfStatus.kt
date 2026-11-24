package com.getfit.ui.components.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme

/**
 * Circular completion badge:
 * - checked = true: accent-tinted circle with rounded check icon.
 * - checked = false: hairline-bordered empty circle.
 */
@Composable
fun GfCheckBadge(
    checked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val bgColor by animateColorAsState(
        targetValue = if (checked) GetFitTheme.colors.accent.copy(alpha = 0.20f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "check_badge_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) GetFitTheme.colors.accent.copy(alpha = 0.35f) else GetFitTheme.colors.hairline,
        animationSpec = tween(durationMillis = 150),
        label = "check_badge_border"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(if (checked) 1.dp else 1.5.dp, borderColor, CircleShape)
            .semantics {
                contentDescription = if (checked) "Logged" else "Not logged"
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(animationSpec = tween(durationMillis = 150)) + fadeIn(animationSpec = tween(durationMillis = 150)),
            exit = scaleOut(animationSpec = tween(durationMillis = 150)) + fadeOut(animationSpec = tween(durationMillis = 150))
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Logged",
                tint = GetFitTheme.colors.accent,
                modifier = Modifier.size(size * 0.66f)
            )
        }
    }
}

/**
 * Compact status pill with accent-tinted background and leading rounded icon.
 */
@Composable
fun GfStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = Icons.Rounded.Check
) {
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(CircleShape)
            .background(GetFitTheme.colors.accent.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = GetFitTheme.colors.accent,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = GetFitTheme.colors.accent,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun GfStatusDarkPreview() {
    GetFitTheme {
        Row(
            modifier = Modifier.padding(Dimens.md),
            horizontalArrangement = Arrangement.spacedBy(Dimens.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GfCheckBadge(checked = true)
            GfCheckBadge(checked = false)
            GfStatusPill(text = "Logged")
        }
    }
}

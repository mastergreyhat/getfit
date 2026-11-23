package com.getfit.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme

/**
 * Primary CTA Button: 56dp tall, fully rounded, electric lime accent fill,
 * onAccent text, spring press interaction. Supports [isLoading] state.
 */
@Composable
fun GfPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressable(enabled = enabled && !isLoading, onClick = onClick)
            .clip(CircleShape)
            .background(if (enabled) GetFitTheme.colors.accent else GetFitTheme.colors.surface3),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.5.dp,
                color = GetFitTheme.colors.onAccent
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) GetFitTheme.colors.onAccent else GetFitTheme.colors.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) GetFitTheme.colors.onAccent else GetFitTheme.colors.textTertiary
                )
            }
        }
    }
}

/**
 * Secondary Button: Fully rounded, surface2 fill with 1dp hairline border,
 * spring press interaction. Supports [isLoading] state and [compact] mode (40dp vs 56dp).
 */
@Composable
fun GfSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    compact: Boolean = false
) {
    Box(
        modifier = modifier
            .then(if (compact) Modifier.height(40.dp) else Modifier.fillMaxWidth().height(56.dp))
            .pressable(enabled = enabled && !isLoading, onClick = onClick)
            .clip(CircleShape)
            .background(GetFitTheme.colors.surface2)
            .border(1.dp, GetFitTheme.colors.hairline, CircleShape)
            .padding(horizontal = if (compact) Dimens.lg else Dimens.xl),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(if (compact) 16.dp else 20.dp),
                strokeWidth = 2.dp,
                color = GetFitTheme.colors.textPrimary
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) Dimens.xs else Dimens.sm)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) GetFitTheme.colors.textPrimary else GetFitTheme.colors.textTertiary,
                        modifier = Modifier.size(if (compact) 16.dp else 20.dp)
                    )
                }
                Text(
                    text = text,
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) GetFitTheme.colors.textPrimary else GetFitTheme.colors.textTertiary
                )
            }
        }
    }
}

/**
 * Text Button: Quiet, borderless pressable text action with accent coloring.
 */
@Composable
fun GfTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = GetFitTheme.colors.accent
) {
    Box(
        modifier = modifier
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Dimens.md, vertical = Dimens.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) color else GetFitTheme.colors.textTertiary
        )
    }
}

@Preview(name = "GfButtons - Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GfButtonsDarkPreview() {
    GetFitTheme(themeMode = "DARK") {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GfPrimaryButton(text = "Log Workout", onClick = {})
            GfPrimaryButton(text = "Saving...", isLoading = true, onClick = {})
            GfSecondaryButton(text = "Cancel", onClick = {})
            GfSecondaryButton(text = "Loading", isLoading = true, onClick = {})
            GfTextButton(text = "Skip for Now", onClick = {})
        }
    }
}

@Preview(name = "GfButtons - Light", showBackground = true, backgroundColor = 0xFFF7F6F2)
@Composable
private fun GfButtonsLightPreview() {
    GetFitTheme(themeMode = "LIGHT") {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GfPrimaryButton(text = "Log Workout", onClick = {})
            GfPrimaryButton(text = "Saving...", isLoading = true, onClick = {})
            GfSecondaryButton(text = "Cancel", onClick = {})
            GfSecondaryButton(text = "Loading", isLoading = true, onClick = {})
            GfTextButton(text = "Skip for Now", onClick = {})
        }
    }
}

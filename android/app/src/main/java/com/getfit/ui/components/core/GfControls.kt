package com.getfit.ui.components.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme

/**
 * Filter Chip: Selected shows accent text on surface3 (or subtle accent fill),
 * unselected renders surface2 with hairline border. Animated 200ms transition.
 */
@Composable
fun GfChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) GetFitTheme.colors.surface3 else GetFitTheme.colors.surface2,
        animationSpec = tween(durationMillis = 200),
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) GetFitTheme.colors.accent else GetFitTheme.colors.textSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "chip_text"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) GetFitTheme.colors.accent.copy(alpha = 0.45f) else GetFitTheme.colors.hairline,
        animationSpec = tween(durationMillis = 200),
        label = "chip_border"
    )

    Box(
        modifier = modifier
            .pressable(onClick = onClick)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .padding(horizontal = Dimens.lg, vertical = Dimens.sm),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

/**
 * Segmented Control: Pill container in surface2 with animated selection pill,
 * 200ms animated transitions, hairline boundary.
 */
@Composable
fun GfSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(GetFitTheme.colors.surface2)
            .border(1.dp, GetFitTheme.colors.hairline, CircleShape)
            .padding(Dimens.xs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selectedIndex == index
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) GetFitTheme.colors.surface3 else Color.Transparent,
                animationSpec = tween(durationMillis = 200),
                label = "seg_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) GetFitTheme.colors.accent else GetFitTheme.colors.textSecondary,
                animationSpec = tween(durationMillis = 200),
                label = "seg_text"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(bgColor)
                    .pressable(onClick = { onSelect(index) })
                    .padding(vertical = Dimens.sm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Horizontally scrollable row of chips with 8dp spacing.
 */
@Composable
fun <T> GfChipRow(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    contentPadding: PaddingValues = PaddingValues(horizontal = Dimens.screenGutter)
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
        contentPadding = contentPadding
    ) {
        items(items) { item ->
            GfChip(
                text = label(item),
                selected = item == selectedItem,
                onClick = { onItemSelected(item) }
            )
        }
    }
}

/**
 * Wrapping multi-line flow of chips with 8dp horizontal and vertical spacing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> GfChipFlow(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() }
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm)
    ) {
        items.forEach { item ->
            GfChip(
                text = label(item),
                selected = item == selectedItem,
                onClick = { onItemSelected(item) }
            )
        }
    }
}

/**
 * Input field: surface2 background, 16dp rounded radius, no outline until focused,
 * 1dp accent border on focus, label rendered as uppercase overline above the field.
 *
 * Supports [isError] state (danger-colored border & supportingText), [suffix], [enabled],
 * [readOnly], and [imeAction].
 */
@Composable
fun GfTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    suffix: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    maxLines: Int = 1,
    imeAction: ImeAction = ImeAction.Default,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = maxLines == 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = imeAction),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
        if (!label.isNullOrBlank()) {
            GfOverline(text = label)
        }

        val borderColor by animateColorAsState(
            targetValue = when {
                isError -> GetFitTheme.colors.danger
                isFocused -> GetFitTheme.colors.accent
                else -> Color.Transparent
            },
            animationSpec = tween(durationMillis = 200),
            label = "textfield_border"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) GetFitTheme.colors.surface2 else GetFitTheme.colors.surface1)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = Dimens.lg, vertical = Dimens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = when {
                        isError -> GetFitTheme.colors.danger
                        isFocused -> GetFitTheme.colors.accent
                        else -> GetFitTheme.colors.textTertiary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && !placeholder.isNullOrBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GetFitTheme.colors.textTertiary
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    enabled = enabled,
                    readOnly = readOnly,
                    maxLines = maxLines,
                    singleLine = singleLine,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = if (enabled) GetFitTheme.colors.textPrimary else GetFitTheme.colors.textTertiary
                    ),
                    cursorBrush = SolidColor(if (isError) GetFitTheme.colors.danger else GetFitTheme.colors.accent),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation
                )
            }
            if (!suffix.isNullOrBlank()) {
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GetFitTheme.colors.textSecondary
                )
            }
            if (trailingIcon != null) {
                trailingIcon()
            }
        }

        if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) GetFitTheme.colors.danger else GetFitTheme.colors.textSecondary,
                modifier = Modifier.padding(start = Dimens.xs)
            )
        }
    }
}

@Preview(name = "GfControls - Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GfControlsDarkPreview() {
    GetFitTheme(themeMode = "DARK") {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val chips = listOf("Chest", "Back", "Shoulders", "Arms", "Legs", "Core")
            GfChipRow(items = chips, selectedItem = "Chest", onItemSelected = {})
            GfSegmentedControl(items = listOf("7 Days", "30 Days", "1 Year"), selectedIndex = 0, onSelect = {})
            GfTextField(
                value = "180",
                onValueChange = {},
                label = "Daily Protein",
                suffix = "g",
                placeholder = "Target in grams"
            )
            GfTextField(
                value = "Invalid",
                onValueChange = {},
                label = "Weight",
                isError = true,
                supportingText = "Please enter a valid numeric value"
            )
        }
    }
}

@Preview(name = "GfControls - Light", showBackground = true, backgroundColor = 0xFFF7F6F2)
@Composable
private fun GfControlsLightPreview() {
    GetFitTheme(themeMode = "LIGHT") {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val chips = listOf("Chest", "Back", "Shoulders", "Arms", "Legs", "Core")
            GfChipRow(items = chips, selectedItem = "Chest", onItemSelected = {})
            GfSegmentedControl(items = listOf("7 Days", "30 Days", "1 Year"), selectedIndex = 0, onSelect = {})
            GfTextField(
                value = "180",
                onValueChange = {},
                label = "Daily Protein",
                suffix = "g",
                placeholder = "Target in grams"
            )
            GfTextField(
                value = "Invalid",
                onValueChange = {},
                label = "Weight",
                isError = true,
                supportingText = "Please enter a valid numeric value"
            )
        }
    }
}

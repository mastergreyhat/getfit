package com.getfit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.data.entity.FoodItem
import com.getfit.ui.theme.GetFitTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoodItemCard(
    food: FoodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onQuickAdd: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(clickModifier),
        colors = CardDefaults.cardColors(
            containerColor = GetFitTheme.colors.surface1
        ),
        border = BorderStroke(1.dp, GetFitTheme.colors.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    val badgeLabel = when {
                        food.isCustom -> "Custom"
                        food.source.contains("Gemini", ignoreCase = true) || food.source.contains("AI", ignoreCase = true) -> "AI Verified"
                        food.source.contains("USDA", ignoreCase = true) -> "USDA Verified"
                        food.source.contains("OpenFoodFacts", ignoreCase = true) -> "OpenFoodFacts"
                        else -> "Verified"
                    }
                    val badgeColor = when (badgeLabel) {
                        "AI Verified" -> MaterialTheme.colorScheme.primaryContainer
                        "USDA Verified" -> MaterialTheme.colorScheme.secondaryContainer
                        "OpenFoodFacts" -> MaterialTheme.colorScheme.tertiaryContainer
                        "Custom" -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                    val textColor = when (badgeLabel) {
                        "AI Verified" -> MaterialTheme.colorScheme.onPrimaryContainer
                        "USDA Verified" -> MaterialTheme.colorScheme.onSecondaryContainer
                        "OpenFoodFacts" -> MaterialTheme.colorScheme.onTertiaryContainer
                        "Custom" -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor
                    ) {
                        Text(
                            text = badgeLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            color = textColor
                        )
                    }
                }

                val isPiece = food.servingUnit.equals("piece", ignoreCase = true)
                Text(
                    text = if (isPiece) {
                        "${food.category} • 1 piece"
                    } else {
                        "${food.category} • ${food.defaultServingSize.toInt()}g"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Nutritional Macros pill row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    val cal = food.caloriesPer100g.toInt()
                    Text(
                        text = if (isPiece) "$cal kcal/piece" else "$cal kcal",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        color = GetFitTheme.colors.textPrimary
                    )
                    Text(
                        text = "P: ${food.proteinPer100g}g",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        color = GetFitTheme.colors.protein
                    )
                    Text(
                        text = "C: ${food.carbsPer100g}g",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        color = GetFitTheme.colors.carbs
                    )
                    Text(
                        text = "F: ${food.fatPer100g}g",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        color = GetFitTheme.colors.fat
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete Food"
                        )
                    }
                }

                if (onQuickAdd != null) {
                    IconButton(
                        onClick = onQuickAdd,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Log Food"
                        )
                    }
                }
            }
        }
    }
}

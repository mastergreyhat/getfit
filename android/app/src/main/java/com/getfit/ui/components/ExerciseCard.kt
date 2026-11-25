package com.getfit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.data.entity.Exercise
import com.getfit.ui.components.core.GfCheckBadge
import com.getfit.ui.components.core.pressable
import com.getfit.ui.theme.GetFitTheme

@Composable
fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    suggestedWeight: Double? = null,
    suggestedIncrease: Double? = null,
    targetSetsReps: String? = null,
    isCompleted: Boolean = false
) {
    val shape = RoundedCornerShape(14.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .pressable(onClick = onClick, onLongClick = onLongClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCompleted) GetFitTheme.colors.success else GetFitTheme.colors.hairline
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${exercise.muscleGroup} • ${exercise.equipment}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (exercise.isCustom) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Custom",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    if (isCompleted) {
                        GfCheckBadge(checked = true, size = 20.dp)
                    }
                }
            }

            if (targetSetsReps != null) {
                val cleanTarget = targetSetsReps.removePrefix("Target:").trim()
                Text(
                    text = cleanTarget,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = GetFitTheme.colors.textPrimary
                )
            }

            // Progressive Overload Suggestion inline row
            if (suggestedWeight != null) {
                val hasIncrease = suggestedIncrease != null && suggestedIncrease > 0.0
                val weightStr = if (suggestedWeight % 1.0 == 0.0) "${suggestedWeight.toInt()}" else "$suggestedWeight"
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hasIncrease) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Progressive Overload",
                            tint = GetFitTheme.colors.success,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Suggested $weightStr kg",
                        style = MaterialTheme.typography.labelLarge,
                        color = GetFitTheme.colors.textPrimary
                    )
                    if (hasIncrease) {
                        val incStr = if (suggestedIncrease!! % 1.0 == 0.0) "${suggestedIncrease.toInt()}" else "$suggestedIncrease"
                        Text(
                            text = "+$incStr kg",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = GetFitTheme.colors.success
                        )
                    }
                }
            }

            if (exercise.description.isNotBlank()) {
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

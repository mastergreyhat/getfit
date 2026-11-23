package com.getfit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.ui.theme.GetFitTheme

@Composable
fun SetLogRow(
    setNumber: Int,
    previousLog: String,
    weight: String,
    reps: String,
    isCompleted: Boolean,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onDeleteSet: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rowBackground = if (isCompleted) {
        GetFitTheme.colors.success.copy(alpha = 0.08f)
    } else {
        GetFitTheme.colors.surface1
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowBackground)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Set number badge
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) GetFitTheme.colors.surface3
                    else GetFitTheme.colors.surface2
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$setNumber",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) GetFitTheme.colors.success else GetFitTheme.colors.textSecondary
            )
        }

        // Previous performance reference
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Previous",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (previousLog.isBlank() || previousLog.equals("None", ignoreCase = true)) "—" else previousLog,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Weight Input (kg)
        OutlinedTextField(
            value = weight,
            onValueChange = onWeightChange,
            modifier = Modifier
                .width(76.dp)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                        if (weight.isNotEmpty()) {
                            onWeightChange(weight.dropLast(1))
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                },
            placeholder = { Text("kg", style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            shape = RoundedCornerShape(8.dp)
        )

        // Reps Input
        OutlinedTextField(
            value = reps,
            onValueChange = onRepsChange,
            modifier = Modifier
                .width(66.dp)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                        if (reps.isNotEmpty()) {
                            onRepsChange(reps.dropLast(1))
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                },
            placeholder = { Text("reps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            shape = RoundedCornerShape(8.dp)
        )

        // Completed check button
        FilledIconToggleButton(
            checked = isCompleted,
            onCheckedChange = onCompletedChange,
            modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                checkedContainerColor = GetFitTheme.colors.success,
                checkedContentColor = GetFitTheme.colors.onAccent,
                containerColor = GetFitTheme.colors.surface2,
                contentColor = GetFitTheme.colors.textTertiary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Mark Set Done",
                modifier = Modifier.size(18.dp)
            )
        }

        // Remove Set button (if set can be removed)
        if (onDeleteSet != null) {
            IconButton(
                onClick = onDeleteSet,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove Set",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }
    }
}


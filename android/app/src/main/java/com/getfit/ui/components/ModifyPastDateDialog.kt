package com.getfit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.ui.theme.CaloriesOrange

@Composable
fun ModifyPastDateDialog(
    dateLabel: String,
    onContinue: () -> Unit,
    onGoToToday: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onGoToToday,
        icon = {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = CaloriesOrange,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Modifying Previous Date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "You are currently viewing a previous date ($dateLabel).",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Do you wish to continue logging for this past date? Logging past dates will not count toward your daily streak.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Yes, Continue", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onGoToToday,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("No, Go to Today")
            }
        }
    )
}

package com.getfit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThemeCapsuleToggle(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Row(
            modifier = if (fullWidth) Modifier.fillMaxWidth().padding(3.dp) else Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Light Option
            val lightBg by animateColorAsState(
                targetValue = if (!isDark) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(150),
                label = "lightBg"
            )
            val lightContentColor by animateColorAsState(
                targetValue = if (!isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                animationSpec = tween(150),
                label = "lightColor"
            )

            val lightModifier = if (fullWidth) {
                Modifier.weight(1f).clickable { onToggle(false) }
            } else {
                Modifier.clickable { onToggle(false) }
            }

            Surface(
                shape = RoundedCornerShape(100.dp),
                color = lightBg,
                shadowElevation = if (!isDark) 1.dp else 0.dp,
                modifier = lightModifier
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (fullWidth) 14.dp else 8.dp, vertical = if (fullWidth) 8.dp else 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = "Light Mode",
                        tint = lightContentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (fullWidth) "Light Mode" else "Light",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (!isDark) FontWeight.Bold else FontWeight.Medium,
                        color = lightContentColor
                    )
                }
            }

            // Dark Option
            val darkBg by animateColorAsState(
                targetValue = if (isDark) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(150),
                label = "darkBg"
            )
            val darkContentColor by animateColorAsState(
                targetValue = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                animationSpec = tween(150),
                label = "darkColor"
            )

            val darkModifier = if (fullWidth) {
                Modifier.weight(1f).clickable { onToggle(true) }
            } else {
                Modifier.clickable { onToggle(true) }
            }

            Surface(
                shape = RoundedCornerShape(100.dp),
                color = darkBg,
                shadowElevation = if (isDark) 1.dp else 0.dp,
                modifier = darkModifier
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (fullWidth) 14.dp else 8.dp, vertical = if (fullWidth) 8.dp else 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "Dark Mode",
                        tint = darkContentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (fullWidth) "Dark Mode" else "Dark",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isDark) FontWeight.Bold else FontWeight.Medium,
                        color = darkContentColor
                    )
                }
            }
        }
    }
}

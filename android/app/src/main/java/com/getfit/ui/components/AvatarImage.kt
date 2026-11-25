package com.getfit.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.getfit.ui.theme.GetFitTheme
import com.getfit.ui.util.ProfileAvatar

@Composable
fun AvatarImage(
    avatar: ProfileAvatar,
    size: Dp,
    locked: Boolean = false,
    daysToGo: Int = 0,
    modifier: Modifier = Modifier
) {
    val ringColor = if (locked) avatar.accentColor.copy(alpha = 0.35f) else avatar.accentColor
    val description = if (locked) {
        "${avatar.name} avatar, locked, $daysToGo days to go"
    } else {
        "${avatar.name} avatar"
    }
    val badgeSize = (size * (22f / 88f)).coerceIn(16.dp, 36.dp)
    val lockIconSize = badgeSize * 0.55f

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(2.dp, ringColor, CircleShape)
        ) {
            Image(
                painter = painterResource(id = avatar.artRes),
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (locked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.20f))
                )
            }
        }

        if (locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(GetFitTheme.colors.surface2)
                    .border(1.dp, GetFitTheme.colors.hairline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = GetFitTheme.colors.textSecondary,
                    modifier = Modifier.size(lockIconSize)
                )
            }
        }
    }
}

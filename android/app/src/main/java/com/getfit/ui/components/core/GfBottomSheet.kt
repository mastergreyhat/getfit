package com.getfit.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme

/**
 * Bottom Sheet wrapper styled for CRED/Apple TV aesthetic:
 * 32dp top corner radius, surface1 background, and subtle hairline drag handle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GfBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = GetFitTheme.colors.surface1,
        contentColor = GetFitTheme.colors.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = Dimens.md)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(GetFitTheme.colors.hairline.copy(alpha = 0.4f))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenGutter, vertical = Dimens.lg)
                .navigationBarsPadding()
                .imePadding(),
            content = content
        )
    }
}

@Preview(name = "GfBottomSheet - Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GfBottomSheetDarkPreview() {
    GetFitTheme(themeMode = "DARK") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(GetFitTheme.colors.surface1)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(GetFitTheme.colors.hairline.copy(alpha = 0.4f))
            )
            GfSectionHeader(title = "Log Set", action = null)
            GfTextField(value = "80", onValueChange = {}, label = "Weight (kg)")
            GfPrimaryButton(text = "Save Set", onClick = {})
        }
    }
}

@Preview(name = "GfBottomSheet - Light", showBackground = true, backgroundColor = 0xFFF7F6F2)
@Composable
private fun GfBottomSheetLightPreview() {
    GetFitTheme(themeMode = "LIGHT") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(GetFitTheme.colors.surface1)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(GetFitTheme.colors.hairline.copy(alpha = 0.4f))
            )
            GfSectionHeader(title = "Log Set", action = null)
            GfTextField(value = "80", onValueChange = {}, label = "Weight (kg)")
            GfPrimaryButton(text = "Save Set", onClick = {})
        }
    }
}

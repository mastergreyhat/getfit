package com.getfit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.getfit.R
import com.getfit.ui.components.core.GfBrandLogo
import com.getfit.ui.components.core.GfCard
import com.getfit.ui.components.core.GfPrimaryButton
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme

@Composable
fun OnboardingWelcomeScreen(
    onNext: () -> Unit
) {
    Scaffold(
        containerColor = GetFitTheme.colors.background,
        bottomBar = {
            Surface(
                color = GetFitTheme.colors.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Dimens.screenGutter, vertical = Dimens.lg)
            ) {
                GfPrimaryButton(
                    text = "Continue",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onNext
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(GetFitTheme.colors.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenGutter, vertical = Dimens.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.xxl)
        ) {
            Spacer(modifier = Modifier.height(Dimens.md))

            // GetFit Brand Mark + Wordmark
            GfBrandLogo(
                iconSize = 36.dp,
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = Dimens.xs)
            )

            // Headline & App Description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                Text(
                    text = "Welcome",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = GetFitTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Your private, focused companion for nutrition tracking, progressive workouts, and unbreakable daily consistency. No ads, no fluff — pure fitness progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GetFitTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Dimens.sm)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.xs))

            // 3 Clean Feature Highlight Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.md)
            ) {
                FeatureCard(
                    icon = Icons.Default.Bolt,
                    iconTint = GetFitTheme.colors.success,
                    title = "Daily Streaks & Consistency",
                    description = "Log your food, water, or workouts every day to intensify your active streak and unlock tier achievements."
                )

                FeatureCard(
                    icon = Icons.Default.Restaurant,
                    iconTint = GetFitTheme.colors.calories,
                    title = "Macro & Calorie Diary",
                    description = "Real-world equation tracking, curated meal plans, and verified OpenFoodFacts lookup."
                )

                FeatureCard(
                    icon = Icons.Default.FitnessCenter,
                    iconTint = GetFitTheme.colors.protein,
                    title = "Progressive Overload & Stopwatch",
                    description = "Curated workout splits, strength progression curves, estimated 1RM calculations, and hold timers."
                )
            }

            Spacer(modifier = Modifier.height(Dimens.lg))
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    GfCard(
        cornerRadius = Dimens.lg,
        contentPadding = PaddingValues(Dimens.lg),
        useGradient = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Dimens.md))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.xs),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GetFitTheme.colors.textPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = GetFitTheme.colors.textSecondary
                )
            }
        }
    }
}

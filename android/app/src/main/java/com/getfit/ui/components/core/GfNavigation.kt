package com.getfit.ui.components.core

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.getfit.ui.theme.Dimens
import com.getfit.ui.theme.GetFitTheme

/**
 * Total clearance height (bar height + floating margins = 96dp) for the floating bottom navigation bar.
 * Screens should use this value for list contentPadding (e.g. PaddingValues(bottom = GfBottomBarContentPadding))
 * so that list items never get obscured by the floating bar.
 */
val GfBottomBarContentPadding: Dp = 96.dp

/**
 * Total bottom clearance for scrollable lists/columns so items scroll smoothly under
 * the bottom fade gradient and the last item clears [GfBottomBar] when scrolled to end.
 */
@Composable
fun gfBottomBarContentPadding(): Dp =
    GfBottomBarContentPadding + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * Modifier helper that adds standard bottom clearance for [GfBottomBar] and accommodates
 * system navigation bar insets (for overlays like FABs and Snackbars).
 */
fun Modifier.gfBottomBarPadding(): Modifier = composed {
    this.padding(bottom = GfBottomBarContentPadding).navigationBarsPadding()
}

/**
 * Opaque screen root wrapper so no destination draws on a transparent root during transitions.
 */
@Composable
fun GfScreenSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GetFitTheme.colors.background)
    ) {
        content()
    }
}

/**
 * Shared 64.dp top bar for pushed screens (Analytics, Workout Detail, Add Custom Food).
 * Applies statusBarsPadding() once and matches CleanTopHeader horizontal padding.
 */
@Composable
fun GfTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Surface(
        color = GetFitTheme.colors.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = Dimens.screenGutter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.md),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .pressable(onClick = onBack),
                    shape = CircleShape,
                    color = GetFitTheme.colors.surface1,
                    border = BorderStroke(1.dp, GetFitTheme.colors.hairline)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GetFitTheme.colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GetFitTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (trailing != null) {
                trailing()
            }
        }
    }
}

/**
 * 140dp bottom fade scrim placed behind [GfBottomBar].
 */
@Composable
fun GfBottomFade(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.25f to GetFitTheme.colors.background.copy(alpha = 0.25f),
                    0.48f to GetFitTheme.colors.background.copy(alpha = 0.75f),
                    0.66f to GetFitTheme.colors.background,
                    1.0f to GetFitTheme.colors.background
                )
            )
    )
}

/**
 * Bottom navigation item model.
 */
data class GfBottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

/**
 * CRED / Apple TV style floating bottom bar pill.
 * Surface1 at 92% alpha, 32dp corner radius, 1dp hairline border, 16dp margin from edges
 * and situated above the system navigation bar insets.
 * Selected item animates with accent icon/label on a soft surface3 indicator.
 */
@Composable
fun GfBottomBar(
    items: List<GfBottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.xxl, vertical = Dimens.sm)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(32.dp),
        color = GetFitTheme.colors.surface1,
        border = BorderStroke(1.dp, GetFitTheme.colors.hairline),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index

                val itemBgColor by animateColorAsState(
                    targetValue = if (isSelected) GetFitTheme.colors.surface3 else Color.Transparent,
                    animationSpec = tween(durationMillis = 120),
                    label = "nav_item_bg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) GetFitTheme.colors.accent else GetFitTheme.colors.textSecondary,
                    animationSpec = tween(durationMillis = 120),
                    label = "nav_item_content"
                )

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(itemBgColor)
                        .pressable(
                            haptic = false,
                            pressedScale = 0.92f,
                            pressDurationMs = 80,
                            onClick = { onItemSelected(index) }
                        )
                        .padding(horizontal = Dimens.lg, vertical = Dimens.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(tween(200)) + expandHorizontally(tween(250)),
                        exit = fadeOut(tween(150)) + shrinkHorizontally(tween(200))
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "GfBottomBar - Dark", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GfBottomBarDarkPreview() {
    GetFitTheme {
        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.BottomCenter) {
            val sampleItems = listOf(
                GfBottomNavItem("home", "Home", Icons.Default.Home),
                GfBottomNavItem("food", "Nutrition", Icons.Default.Restaurant),
                GfBottomNavItem("workout", "Workout", Icons.Default.FitnessCenter),
                GfBottomNavItem("analytics", "Analytics", Icons.Default.Analytics),
                GfBottomNavItem("profile", "Profile", Icons.Default.Person)
            )
            GfBottomBar(items = sampleItems, selectedIndex = 0, onItemSelected = {})
        }
    }
}

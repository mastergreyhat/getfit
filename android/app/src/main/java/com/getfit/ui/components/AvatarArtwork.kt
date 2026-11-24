package com.getfit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Five HD canvas avatars, one per streak tier.
 * Drawn entirely on the GPU — zero bitmaps, zero memory overhead.
 */
@Composable
fun AvatarArtwork(
    avatarId: String,
    accentColor: Color,
    size: Dp = 46.dp,
    locked: Boolean = false
) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width
        if (locked) {
            drawLockedAvatar(s, accentColor)
        } else {
            when (avatarId) {
                "avatar_starter"  -> drawNoviceLifter(s, accentColor)
                "avatar_1_month"  -> drawIronDedicated(s, accentColor)
                "avatar_3_months" -> drawSilverBeast(s, accentColor)
                "avatar_6_months" -> drawGoldTitan(s, accentColor)
                "avatar_1_year"   -> drawDiamondLegend(s, accentColor)
                else              -> drawNoviceLifter(s, accentColor)
            }
        }
    }
}

// ─── Locked placeholder ──────────────────────────────────────────────────────

private fun DrawScope.drawLockedAvatar(s: Float, accent: Color) {
    val grey = Color(0xFF4A4A4A)
    val c = Offset(s / 2, s / 2)
    // shackle arc
    drawArc(
        color = grey,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(s * 0.3f, s * 0.15f),
        size = Size(s * 0.4f, s * 0.38f),
        style = Stroke(width = s * 0.09f, cap = StrokeCap.Round)
    )
    // body
    drawRoundRect(
        color = grey,
        topLeft = Offset(s * 0.22f, s * 0.44f),
        size = Size(s * 0.56f, s * 0.42f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.07f),
    )
    // keyhole circle
    drawCircle(color = Color(0xFF2A2A2A), radius = s * 0.09f, center = Offset(c.x, s * 0.60f))
    // keyhole stem
    drawLine(
        color = Color(0xFF2A2A2A),
        start = Offset(c.x, s * 0.60f),
        end = Offset(c.x, s * 0.76f),
        strokeWidth = s * 0.06f,
        cap = StrokeCap.Round
    )
}

// ─── Tier 0: Novice Lifter — stick figure doing overhead press ───────────────

private fun DrawScope.drawNoviceLifter(s: Float, accent: Color) {
    val bg = Brush.radialGradient(
        colors = listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.08f)),
        center = Offset(s / 2, s / 2), radius = s * 0.6f
    )
    drawCircle(brush = bg, radius = s * 0.47f, center = Offset(s / 2, s / 2))

    val c = s / 2
    // head
    drawCircle(color = accent, radius = s * 0.1f, center = Offset(c, s * 0.22f))
    // body
    drawLine(accent, Offset(c, s * 0.32f), Offset(c, s * 0.60f), s * 0.07f, StrokeCap.Round)
    // barbell
    drawLine(accent, Offset(s * 0.15f, s * 0.22f), Offset(s * 0.85f, s * 0.22f), s * 0.065f, StrokeCap.Round)
    // weight plates
    for (x in listOf(s * 0.15f, s * 0.85f)) {
        drawLine(Color.White.copy(alpha = 0.8f), Offset(x, s * 0.14f), Offset(x, s * 0.30f), s * 0.065f, StrokeCap.Round)
    }
    // arms up
    drawLine(accent, Offset(c, s * 0.38f), Offset(s * 0.20f, s * 0.24f), s * 0.065f, StrokeCap.Round)
    drawLine(accent, Offset(c, s * 0.38f), Offset(s * 0.80f, s * 0.24f), s * 0.065f, StrokeCap.Round)
    // legs
    drawLine(accent, Offset(c, s * 0.60f), Offset(s * 0.35f, s * 0.82f), s * 0.065f, StrokeCap.Round)
    drawLine(accent, Offset(c, s * 0.60f), Offset(s * 0.65f, s * 0.82f), s * 0.065f, StrokeCap.Round)
}

// ─── Tier 1: Iron Dedicated — flame shield ──────────────────────────────────

private fun DrawScope.drawIronDedicated(s: Float, accent: Color) {
    val c = s / 2
    val bg = Brush.radialGradient(
        colors = listOf(accent.copy(alpha = 0.4f), accent.copy(alpha = 0.05f)),
        center = Offset(c, c), radius = s * 0.6f
    )
    drawCircle(brush = bg, radius = s * 0.47f, center = Offset(c, c))

    // Shield body
    val shield = Path().apply {
        moveTo(c, s * 0.10f)
        lineTo(s * 0.82f, s * 0.28f)
        lineTo(s * 0.82f, s * 0.58f)
        cubicTo(s * 0.82f, s * 0.80f, c, s * 0.92f, c, s * 0.92f)
        cubicTo(c, s * 0.92f, s * 0.18f, s * 0.80f, s * 0.18f, s * 0.58f)
        lineTo(s * 0.18f, s * 0.28f)
        close()
    }
    drawPath(shield, brush = Brush.verticalGradient(
        colors = listOf(accent.copy(alpha = 0.6f), accent.copy(alpha = 0.25f)),
        startY = s * 0.1f, endY = s * 0.92f
    ))
    drawPath(shield, color = accent, style = Stroke(width = s * 0.055f, cap = StrokeCap.Round))

    // Flame inside
    val flame = Path().apply {
        moveTo(c, s * 0.75f)
        cubicTo(s * 0.32f, s * 0.65f, s * 0.30f, s * 0.48f, c, s * 0.38f)
        cubicTo(s * 0.52f, s * 0.44f, s * 0.58f, s * 0.36f, s * 0.55f, s * 0.28f)
        cubicTo(s * 0.65f, s * 0.40f, s * 0.70f, s * 0.55f, s * 0.65f, s * 0.65f)
        cubicTo(s * 0.75f, s * 0.55f, s * 0.72f, s * 0.44f, s * 0.65f, s * 0.38f)
        cubicTo(s * 0.78f, s * 0.50f, s * 0.72f, s * 0.65f, c, s * 0.75f)
        close()
    }
    drawPath(flame, color = Color.White.copy(alpha = 0.90f))
}

// ─── Tier 2: Silver Beast — lightning bolt ──────────────────────────────────

private fun DrawScope.drawSilverBeast(s: Float, accent: Color) {
    val c = s / 2
    val silver = Color(0xFFB0BEC5)
    val bg = Brush.radialGradient(
        colors = listOf(silver.copy(alpha = 0.5f), accent.copy(alpha = 0.08f)),
        center = Offset(c, c), radius = s * 0.6f
    )
    drawCircle(brush = bg, radius = s * 0.47f, center = Offset(c, c))

    // Outer ring
    drawCircle(color = silver.copy(alpha = 0.5f), radius = s * 0.42f, center = Offset(c, c), style = Stroke(width = s * 0.045f))

    // Bold lightning bolt
    val bolt = Path().apply {
        moveTo(s * 0.60f, s * 0.10f)
        lineTo(s * 0.35f, s * 0.50f)
        lineTo(s * 0.52f, s * 0.50f)
        lineTo(s * 0.40f, s * 0.90f)
        lineTo(s * 0.68f, s * 0.44f)
        lineTo(s * 0.50f, s * 0.44f)
        close()
    }
    drawPath(bolt, brush = Brush.verticalGradient(
        colors = listOf(Color.White, silver),
        startY = s * 0.1f, endY = s * 0.9f
    ))
    drawPath(bolt, color = accent.copy(alpha = 0.5f), style = Stroke(width = s * 0.035f, cap = StrokeCap.Round))
}

// ─── Tier 3: Gold Titan — trophy / crown ────────────────────────────────────

private fun DrawScope.drawGoldTitan(s: Float, accent: Color) {
    val c = s / 2
    val gold = Color(0xFFFFD700)
    val bg = Brush.radialGradient(
        colors = listOf(gold.copy(alpha = 0.45f), accent.copy(alpha = 0.08f)),
        center = Offset(c, c), radius = s * 0.6f
    )
    drawCircle(brush = bg, radius = s * 0.47f, center = Offset(c, c))

    // Crown band base
    drawRoundRect(
        color = gold,
        topLeft = Offset(s * 0.18f, s * 0.58f),
        size = Size(s * 0.64f, s * 0.17f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.05f)
    )

    // Three crown spires
    for ((x, h) in listOf(
        Pair(s * 0.20f, s * 0.24f),
        Pair(s * 0.46f, s * 0.14f),
        Pair(s * 0.72f, s * 0.24f)
    )) {
        val spire = Path().apply {
            moveTo(x, s * 0.60f)
            lineTo(x + s * 0.08f, s * 0.60f)
            lineTo(x + s * 0.04f, h)
            close()
        }
        drawPath(spire, color = gold)
    }

    // Jewels on crown
    for ((x, y, col) in listOf(
        Triple(s * 0.30f, s * 0.65f, Color(0xFF4FC3F7)),
        Triple(s * 0.50f, s * 0.65f, Color(0xFFEF5350)),
        Triple(s * 0.70f, s * 0.65f, Color(0xFF66BB6A))
    )) {
        drawCircle(color = col, radius = s * 0.045f, center = Offset(x, y))
        drawCircle(color = Color.White.copy(alpha = 0.5f), radius = s * 0.015f, center = Offset(x - s * 0.015f, y - s * 0.015f))
    }

    // Outline
    drawRoundRect(
        color = gold.copy(alpha = 0.8f),
        topLeft = Offset(s * 0.18f, s * 0.58f),
        size = Size(s * 0.64f, s * 0.17f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.05f),
        style = Stroke(width = s * 0.04f)
    )
}

// ─── Tier 4: Diamond Legend — gem with radiant beams ────────────────────────

private fun DrawScope.drawDiamondLegend(s: Float, accent: Color) {
    val c = s / 2
    val diamond = Color(0xFF80DEEA)
    val bg = Brush.radialGradient(
        colors = listOf(diamond.copy(alpha = 0.5f), accent.copy(alpha = 0.08f)),
        center = Offset(c, c), radius = s * 0.6f
    )
    drawCircle(brush = bg, radius = s * 0.47f, center = Offset(c, c))

    // Radiant beams (8 rays)
    for (angle in 0..315 step 45) {
        rotate(degrees = angle.toFloat(), pivot = Offset(c, c)) {
            drawLine(
                color = diamond.copy(alpha = 0.5f),
                start = Offset(c, s * 0.08f),
                end = Offset(c, s * 0.20f),
                strokeWidth = s * 0.04f,
                cap = StrokeCap.Round
            )
        }
    }

    // Diamond gem
    val gem = Path().apply {
        // top facet row
        moveTo(c, s * 0.18f)
        lineTo(s * 0.70f, s * 0.38f)
        lineTo(s * 0.60f, s * 0.38f)
        lineTo(c, s * 0.18f)
        moveTo(c, s * 0.18f)
        lineTo(s * 0.30f, s * 0.38f)
        lineTo(s * 0.40f, s * 0.38f)
        lineTo(c, s * 0.18f)
        // main body
        moveTo(s * 0.28f, s * 0.38f)
        lineTo(s * 0.72f, s * 0.38f)
        lineTo(c, s * 0.82f)
        close()
    }
    drawPath(gem, brush = Brush.linearGradient(
        colors = listOf(Color.White, diamond, accent),
        start = Offset(s * 0.28f, s * 0.18f),
        end = Offset(c, s * 0.82f)
    ))
    drawPath(gem, color = accent.copy(alpha = 0.6f), style = Stroke(width = s * 0.035f, cap = StrokeCap.Round))

    // Sparkle dot at top
    drawCircle(color = Color.White, radius = s * 0.045f, center = Offset(c, s * 0.18f))
}

package com.getfit.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.R

@OptIn(ExperimentalTextApi::class)
val Manrope = FontFamily(
    Font(
        R.font.manrope,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.manrope,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.manrope,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.manrope,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        R.font.manrope,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W800,
        fontSize = 64.sp,
        lineHeight = 72.sp,
        letterSpacing = (-2).sp,
        fontFeatureSettings = "tnum"
    ),
    displayMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W700,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum"
    ),
    displaySmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W700,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W700,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W700,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp
    )
)

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun TypographyPreview() {
    GetFitTheme {
        Column(
            modifier = Modifier
                .background(Color.Black)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Display Large 64sp (w800)", style = MaterialTheme.typography.displayLarge, color = Color.White)
            Text("Display Medium 44sp (w700)", style = MaterialTheme.typography.displayMedium, color = Color.White)
            Text("Display Small 32sp (w700)", style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text("Headline Large 28sp (w700)", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text("Headline Medium 22sp (w700)", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text("Title Large 20sp (w600)", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Title Medium 16sp (w600)", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text("Body Large 16sp (w400)", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Text("Body Medium 14sp (w400)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text("Body Small 12sp (w400)", style = MaterialTheme.typography.bodySmall, color = Color.White)
            Text("Label Large 14sp (w600)", style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text("Label Medium 12sp (w600)", style = MaterialTheme.typography.labelMedium, color = Color.White)
            Text("OVERLINE LABEL SMALL 11sp (w600)", style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}

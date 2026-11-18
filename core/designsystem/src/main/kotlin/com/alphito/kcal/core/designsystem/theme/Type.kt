package com.alphito.kcal.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.alphito.kcal.core.designsystem.R

/**
 * Inter ships as one subset variable font (≈100 KB instead of four static files). Each weight
 * is an instance of the `wght` axis, so the font is loaded once and shared.
 */
@OptIn(ExperimentalTextApi::class)
private fun interWeight(weight: FontWeight) = Font(
    resId = R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val InterFamily = FontFamily(
    interWeight(FontWeight.Normal),
    interWeight(FontWeight.Medium),
    interWeight(FontWeight.SemiBold),
    interWeight(FontWeight.Bold),
)

val PlayfairFamily = FontFamily(Font(R.font.playfair_display_semibold, FontWeight.SemiBold))

private val Trimmed = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun inter(size: Int, weight: FontWeight, lineHeight: Float = 1.22f, sizeHalf: Boolean = false) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = if (sizeHalf) (size + 0.5f).sp else size.sp,
    lineHeight = lineHeight.em,
    lineHeightStyle = Trimmed,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/** Type scale distilled from the Figma text styles (Inter 400/600/700, 10.5 → 34 sp). */
@Immutable
data class KcalTypography(
    val welcome: TextStyle = TextStyle(
        fontFamily = PlayfairFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
        lineHeight = 1.3.em, platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val hero: TextStyle = inter(34, FontWeight.Bold, 1.2f),
    val display: TextStyle = inter(30, FontWeight.Bold, 1.2f),
    val headline: TextStyle = inter(24, FontWeight.Bold, 1.2f),
    val headlineSmall: TextStyle = inter(22, FontWeight.Bold, 1.22f),
    val title: TextStyle = inter(20, FontWeight.SemiBold, 1.2f),
    val titleMedium: TextStyle = inter(17, FontWeight.SemiBold, 1.24f),
    val titleSmall: TextStyle = inter(15, FontWeight.SemiBold, 1.2f),
    val label: TextStyle = inter(14, FontWeight.SemiBold, 1.2f),
    val labelBold: TextStyle = inter(14, FontWeight.Bold, 1.2f),
    val labelSmall: TextStyle = inter(12, FontWeight.SemiBold, 1.22f, sizeHalf = true),
    val button: TextStyle = inter(16, FontWeight.Bold, 1.2f),
    val body: TextStyle = inter(14, FontWeight.Normal, 1.4f),
    val bodySmall: TextStyle = inter(12, FontWeight.Normal, 1.3f),
    val caption: TextStyle = inter(11, FontWeight.Normal, 1.22f),
    val overline: TextStyle = inter(11, FontWeight.SemiBold, 1.2f).copy(letterSpacing = 0.04.em),
)

val LocalKcalTypography = staticCompositionLocalOf { KcalTypography() }

/** Material slots mapped onto the kCal scale so M3 components inherit Inter. */
internal fun KcalTypography.toMaterial() = Typography(
    displayLarge = hero, displayMedium = display, displaySmall = headline,
    headlineLarge = headline, headlineMedium = headlineSmall, headlineSmall = title,
    titleLarge = title, titleMedium = titleMedium, titleSmall = titleSmall,
    bodyLarge = body.copy(fontSize = 15.sp), bodyMedium = body, bodySmall = bodySmall,
    labelLarge = label, labelMedium = labelSmall, labelSmall = caption,
)

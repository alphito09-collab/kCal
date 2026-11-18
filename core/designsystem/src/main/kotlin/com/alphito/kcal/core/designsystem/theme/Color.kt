package com.alphito.kcal.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Raw palette from the Figma file "kCal – UI/UX (Plum, Blush, Ivory)". */
object KcalPalette {
    val Plum = Color(0xFF684A68)
    val PlumDeep = Color(0xFF3D1F3D)
    val PlumVivid = Color(0xFF6B2F6B)
    val Blush = Color(0xFFD9A6B3)
    val Mist = Color(0xFFE9DCE2)
    val MistLine = Color(0xFFE8DFE3)
    val Ivory = Color(0xFFFBF7F5)
    val Ink = Color(0xFF302830)
    val Mauve = Color(0xFF8A7B85)
    val Rose = Color(0xFFF2C6C2)
    val RoseLine = Color(0xFFE8969A)
    val White = Color(0xFFFFFFFF)

    // Dark-theme counterparts (not in Figma – derived to keep the same hue family).
    val NightBackground = Color(0xFF171217)
    val NightSurface = Color(0xFF241D24)
    val NightSurfaceHigh = Color(0xFF2F262F)
    val NightInk = Color(0xFFF4ECF1)
    val NightPlum = Color(0xFFD7B6D7)
}

/**
 * Semantic colour roles used by kCal components. Exposed through [LocalKcalColors] so screens
 * never reference raw hex values and dark mode is a single swap.
 */
@Immutable
data class KcalColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val accentContainer: Color,
    val selectedContainer: Color,
    val selectedBorder: Color,
    val track: Color,
    val outline: Color,
    val textPrimary: Color,
    val textBrand: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val chartFill: Color,
    val scrim: Color,
    val shadow: Color,
    val isDark: Boolean,
)

val LightKcalColors = KcalColors(
    background = KcalPalette.Ivory,
    surface = KcalPalette.White,
    surfaceMuted = KcalPalette.Mist.copy(alpha = 0.5f),
    primary = KcalPalette.Plum,
    onPrimary = KcalPalette.White,
    accent = KcalPalette.Blush,
    accentContainer = KcalPalette.Blush.copy(alpha = 0.32f),
    selectedContainer = KcalPalette.Rose,
    selectedBorder = KcalPalette.RoseLine,
    track = KcalPalette.Mist,
    outline = KcalPalette.MistLine,
    textPrimary = KcalPalette.Ink,
    textBrand = KcalPalette.Plum,
    textSecondary = KcalPalette.Ink.copy(alpha = 0.55f),
    textTertiary = KcalPalette.Plum.copy(alpha = 0.45f),
    chartFill = KcalPalette.Blush.copy(alpha = 0.18f),
    scrim = KcalPalette.Ink.copy(alpha = 0.40f),
    shadow = KcalPalette.Plum.copy(alpha = 0.10f),
    isDark = false,
)

val DarkKcalColors = KcalColors(
    background = KcalPalette.NightBackground,
    surface = KcalPalette.NightSurface,
    surfaceMuted = KcalPalette.NightSurfaceHigh,
    primary = KcalPalette.NightPlum,
    onPrimary = KcalPalette.PlumDeep,
    accent = KcalPalette.Blush,
    accentContainer = KcalPalette.Blush.copy(alpha = 0.18f),
    selectedContainer = KcalPalette.Plum.copy(alpha = 0.55f),
    selectedBorder = KcalPalette.Blush,
    track = KcalPalette.NightSurfaceHigh,
    outline = KcalPalette.NightInk.copy(alpha = 0.12f),
    textPrimary = KcalPalette.NightInk,
    textBrand = KcalPalette.NightPlum,
    textSecondary = KcalPalette.NightInk.copy(alpha = 0.62f),
    textTertiary = KcalPalette.NightPlum.copy(alpha = 0.55f),
    chartFill = KcalPalette.Blush.copy(alpha = 0.14f),
    scrim = Color.Black.copy(alpha = 0.55f),
    shadow = Color.Black.copy(alpha = 0.35f),
    isDark = true,
)

val LocalKcalColors = staticCompositionLocalOf { LightKcalColors }

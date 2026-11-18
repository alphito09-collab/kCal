package com.alphito.kcal.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

/** Brand ripple: plum tinted, slightly stronger pressed alpha than M3 default for the ivory UI. */
private val KcalRippleAlpha = RippleAlpha(
    pressedAlpha = 0.14f, focusedAlpha = 0.12f, draggedAlpha = 0.10f, hoveredAlpha = 0.06f,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KcalTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkKcalColors else LightKcalColors
    val typography = remember { KcalTypography() }
    val material = remember(colors) {
        if (colors.isDark) {
            darkColorScheme(
                primary = colors.primary, onPrimary = colors.onPrimary,
                secondary = colors.accent, background = colors.background,
                surface = colors.surface, onSurface = colors.textPrimary,
                onBackground = colors.textPrimary, outline = colors.outline,
                surfaceContainerLow = colors.surface, surfaceContainer = colors.surface,
                surfaceContainerHigh = colors.surfaceMuted, scrim = colors.scrim,
            )
        } else {
            lightColorScheme(
                primary = colors.primary, onPrimary = colors.onPrimary,
                secondary = colors.accent, background = colors.background,
                surface = colors.surface, onSurface = colors.textPrimary,
                onBackground = colors.textPrimary, outline = colors.outline,
                surfaceContainerLow = colors.surface, surfaceContainer = colors.surface,
                surfaceContainerHigh = colors.surface, scrim = colors.scrim,
            )
        }
    }
    val ripple = remember(colors) { RippleConfiguration(color = colors.primary, rippleAlpha = KcalRippleAlpha) }

    CompositionLocalProvider(
        LocalKcalColors provides colors,
        LocalKcalTypography provides typography,
        LocalRippleConfiguration provides ripple,
    ) {
        MaterialTheme(colorScheme = material, typography = typography.toMaterial(), content = content)
    }
}

/** Accessor mirroring `MaterialTheme.*` – e.g. `KcalTheme.colors.primary`. */
object KcalTheme {
    val colors: KcalColors
        @Composable @ReadOnlyComposable get() = LocalKcalColors.current
    val typography: KcalTypography
        @Composable @ReadOnlyComposable get() = LocalKcalTypography.current
}

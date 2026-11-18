package com.alphito.kcal.core.designsystem.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalTheme

/** Rounded linear progress (steps goal). Fill springs to the new value in the draw phase. */
@Composable
fun KcalLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = KcalTheme.colors.primary,
    trackColor: Color = KcalTheme.colors.track,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) { anim.animateTo(progress.coerceIn(0f, 1f), KcalMotion.chartSpring()) }
    Canvas(modifier) {
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(trackColor, cornerRadius = r)
        val w = size.width * anim.value
        if (w > 0f) {
            // Left end rounded, right edge straight – matches the Figma steps bar.
            clipRect(right = w) { drawRoundRect(color, size = Size(size.width, size.height), cornerRadius = r) }
        }
    }
}

/** Two-tone split bar: steps share vs diet share of the calorie deficit. */
@Composable
fun SplitBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    leading: Color = KcalTheme.colors.primary,
    trailing: Color = KcalTheme.colors.accent,
) {
    val anim = remember { Animatable(fraction.coerceIn(0f, 1f)) }
    LaunchedEffect(fraction) { anim.animateTo(fraction.coerceIn(0f, 1f), KcalMotion.chartSpring()) }
    Canvas(modifier) {
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(trailing, cornerRadius = r)
        val w = size.width * anim.value
        clipRect(right = w) { drawRoundRect(leading, Offset.Zero, size, cornerRadius = r) }
    }
}

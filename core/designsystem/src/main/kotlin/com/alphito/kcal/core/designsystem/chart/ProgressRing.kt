package com.alphito.kcal.core.designsystem.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalTheme

/**
 * Circular progress (Daily Calorie Tracking, Water Intake).
 * Sweeps from 0 on first display, then springs between values. The animated sweep is read
 * only inside `drawBehind`, so progress updates redraw without recomposing the centre content.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 18.dp,
    color: Color = KcalTheme.colors.primary,
    trackColor: Color = KcalTheme.colors.track,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        val target = progress.coerceIn(0f, 1f)
        if (sweep.value == 0f) {
            sweep.animateTo(target, tween(KcalMotion.DurationChart + 300, easing = KcalMotion.EmphasizedDecelerate))
        } else {
            sweep.animateTo(target, KcalMotion.chartSpring())
        }
    }
    Box(
        modifier
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f) }
            .drawBehind {
                val strokePx = strokeWidth.toPx()
                val arcSize = Size(size.minDimension - strokePx, size.minDimension - strokePx)
                val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)
                drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokePx))
                val s = sweep.value
                if (s > 0f) {
                    drawArc(color, -90f, 360f * s, false, topLeft, arcSize, style = Stroke(strokePx, cap = StrokeCap.Round))
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

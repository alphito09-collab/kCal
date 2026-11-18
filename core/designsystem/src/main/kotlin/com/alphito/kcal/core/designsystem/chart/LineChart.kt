package com.alphito.kcal.core.designsystem.chart

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * Trend line with area fill (weight trend, sugar trend, symptom severity).
 *
 * - Geometry (line + area paths, point positions) is built in `drawWithCache` and only
 *   rebuilt when size or data change.
 * - The line "draws in" left→right via a clip whose width is an animated value read in draw.
 * - The selected point springs to its new position; tap anywhere to select the nearest point.
 */
@Composable
fun KcalLineChart(
    values: ImmutableList<Float>,
    labels: ImmutableList<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 120.dp,
    labelStyleBoldOnly: Boolean = false,
    description: String = "",
) {
    val colors = KcalTheme.colors
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(values) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(KcalMotion.DurationChart, easing = KcalMotion.EmphasizedDecelerate))
    }
    val selX = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) { selX.animateTo(selectedIndex.toFloat(), KcalMotion.indicatorSpring()) }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val lineColor = colors.primary
    val pointColor = colors.accent
    val fillTop = colors.accent.copy(alpha = 0.32f)
    val fillBottom = colors.accent.copy(alpha = 0.06f)
    val surface = colors.surface

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .semantics { contentDescription = description }
                .pointerInput(values.size) {
                    detectTapGestures { tap ->
                        if (values.isEmpty()) return@detectTapGestures
                        val slot = size.width.toFloat() / values.size
                        currentOnSelect((tap.x / slot).toInt().coerceIn(0, values.lastIndex))
                    }
                }
                .drawWithCache {
                    val pad = 8.dp.toPx()
                    val min = values.minOrNull() ?: 0f
                    val max = values.maxOrNull() ?: 1f
                    val range = (max - min).takeIf { it > 0f } ?: 1f
                    // Each point sits in the centre of its label slot so dots and labels line up.
                    val slot = if (values.isNotEmpty()) size.width / values.size else 0f
                    val points = values.mapIndexed { i, v ->
                        Offset(slot * (i + 0.5f), pad + (1f - (v - min) / range) * (size.height - pad * 2))
                    }
                    val line = Path().apply {
                        points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                    }
                    val area = Path().apply {
                        addPath(line)
                        if (points.isNotEmpty()) {
                            lineTo(points.last().x, size.height); lineTo(points.first().x, size.height); close()
                        }
                    }
                    val fill = Brush.verticalGradient(listOf(fillTop, fillBottom))
                    val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val dot = 3.5.dp.toPx()
                    val selRadius = 6.5.dp.toPx()
                    onDrawBehind {
                        clipRect(right = size.width * reveal.value + selRadius) {
                            drawPath(area, fill)
                            drawPath(line, lineColor, style = stroke)
                            points.forEach { drawCircle(pointColor, dot, it) }
                        }
                        if (points.isNotEmpty()) {
                            val f = selX.value.coerceIn(0f, points.lastIndex.toFloat())
                            val i0 = f.toInt(); val i1 = (i0 + 1).coerceAtMost(points.lastIndex)
                            val t = f - i0
                            val p = Offset(
                                points[i0].x + (points[i1].x - points[i0].x) * t,
                                points[i0].y + (points[i1].y - points[i0].y) * t,
                            )
                            drawCircle(surface, selRadius + 2.dp.toPx(), p)
                            drawCircle(lineColor, selRadius, p)
                        }
                    }
                },
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val bg by animateColorAsState(
                    if (selected && !labelStyleBoldOnly) colors.accentContainer else colors.surface.copy(alpha = 0f),
                    KcalMotion.fadeTween(), label = "lbl",
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        style = if (selected) KcalTheme.typography.labelSmall else KcalTheme.typography.bodySmall,
                        color = if (selected) colors.textBrand else colors.textTertiary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.background(bg, KcalShapes.pill).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

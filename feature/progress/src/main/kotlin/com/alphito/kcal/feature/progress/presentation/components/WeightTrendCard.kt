package com.alphito.kcal.feature.progress.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.modifier.kcalClickable
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.feature.progress.R
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.abs

/**
 * "Weight Trend" card from the Figma Progress screens: straight plum segments over a flat blush
 * fill, small mist dots, one larger plum dot (white ring) for the selected period, and labels in a
 * space-between row with a blush pill on the selected one. No grid, no y-axis.
 *
 * @param values one per slot, in the user's unit; null = the period hasn't started (label, no point).
 * @param selectedSlot slot of the selected period, or -1 when it's outside the visible window.
 */
@Composable
internal fun WeightTrendCard(
    title: String,
    values: ImmutableList<Float?>,
    labels: ImmutableList<String>,
    selectedSlot: Int,
    hasData: Boolean,
    emptyMessage: String,
    onSelect: (Int) -> Unit,
    canPageBack: Boolean,
    canPageForward: Boolean,
    onPageBack: () -> Unit,
    onPageForward: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = KcalTheme.colors
    KcalCard(
        modifier.fillMaxWidth(), shape = KcalShapes.xl,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = KcalTheme.typography.titleSmall, color = colors.textBrand, modifier = Modifier.weight(1f))
            // Only once there are more periods than slots: new users see the plain Figma header.
            if (canPageBack || canPageForward) {
                PageArrow(enabled = canPageBack, back = true, onClick = onPageBack)
                PageArrow(enabled = canPageForward, back = false, onClick = onPageForward)
            }
        }
        Spacer(Modifier.height(14.dp))
        // Time runs left → right in every language, so the chart and its labels stay LTR.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column {
                if (hasData) {
                    TrendChart(values, selectedSlot, onSelect, onPageBack, onPageForward, canPageBack, canPageForward, description)
                } else {
                    Box(Modifier.fillMaxWidth().height(ChartHeight), contentAlignment = Alignment.Center) {
                        Text(emptyMessage, style = KcalTheme.typography.body, color = colors.textSecondary, textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    labels.forEachIndexed { i, label -> SlotLabel(label, selected = i == selectedSlot, enabled = values.getOrNull(i) != null) { onSelect(i) } }
                }
            }
        }
    }
}

private val ChartHeight = 100.dp

@Composable
private fun TrendChart(
    values: ImmutableList<Float?>,
    selectedSlot: Int,
    onSelect: (Int) -> Unit,
    onPageBack: () -> Unit,
    onPageForward: () -> Unit,
    canPageBack: Boolean,
    canPageForward: Boolean,
    description: String,
) {
    val colors = KcalTheme.colors
    val line = colors.primary
    val fill = colors.accent.copy(alpha = 0.15f)
    val dot = colors.track
    val ring = colors.surface

    // Draw-in from the left whenever the data set changes.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(values) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(KcalMotion.DurationChart, easing = KcalMotion.EmphasizedDecelerate))
    }
    // The selected dot glides between points and grows in when a period is (re)selected.
    val selX = remember { Animatable(selectedSlot.coerceAtLeast(0).toFloat()) }
    LaunchedEffect(selectedSlot) { if (selectedSlot >= 0) selX.animateTo(selectedSlot.toFloat(), KcalMotion.indicatorSpring()) }
    val selScale by animateFloatAsState(if (selectedSlot >= 0) 1f else 0f, KcalMotion.bouncySpring(), label = "sel-dot")

    val onSelectNow by rememberUpdatedState(onSelect)
    val pageBack by rememberUpdatedState(if (canPageBack) onPageBack else null)
    val pageForward by rememberUpdatedState(if (canPageForward) onPageForward else null)

    Box(
        Modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .semantics { contentDescription = description }
            .pointerInput(values) {
                detectTapGestures { tap ->
                    val step = size.width / (values.size - 1).coerceAtLeast(1).toFloat()
                    values.indices.filter { values[it] != null }
                        .minByOrNull { abs(it * step - tap.x) }
                        ?.let(onSelectNow)
                }
            }
            .pointerInput(Unit) {
                // Swipe right for earlier periods, left for later ones.
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = {
                        val threshold = 48.dp.toPx()
                        if (drag > threshold) pageBack?.invoke() else if (drag < -threshold) pageForward?.invoke()
                    },
                ) { change, amount -> change.consume(); drag += amount }
            }
            .drawWithCache {
                val padY = 5.dp.toPx()
                val present = values.filterNotNull()
                val min = present.minOrNull() ?: 0f
                val max = present.maxOrNull() ?: 0f
                // ~10% headroom so small changes stay visible; a flat series sits in the middle.
                val span = max - min
                val lo = if (span > 0f) min - span * 0.1f else min - 1f
                val hi = if (span > 0f) max + span * 0.1f else max + 1f
                val step = size.width / (values.size - 1).coerceAtLeast(1)
                val points = values.mapIndexed { i, v ->
                    v?.let { Offset(i * step, padY + (1f - (it - lo) / (hi - lo)) * (size.height - 2 * padY)) }
                }
                val drawn = points.filterNotNull()
                val path = Path().apply { drawn.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
                val area = Path().apply {
                    if (drawn.size > 1) {
                        addPath(path)
                        lineTo(drawn.last().x, size.height); lineTo(drawn.first().x, size.height); close()
                    }
                }
                val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val dotRadius = 4.dp.toPx()
                val selRadius = 7.dp.toPx()
                val ringWidth = 2.5.dp.toPx()
                onDrawBehind {
                    clipRect(left = -selRadius, top = -selRadius, right = size.width * reveal.value + selRadius, bottom = size.height + selRadius) {
                        drawPath(area, fill)
                        drawPath(path, line, style = stroke)
                        drawn.forEach { drawCircle(dot, dotRadius, it) }
                    }
                    if (selectedSlot >= 0 && drawn.isNotEmpty()) {
                        // Interpolate along started points only, so the dot never lands on an empty slot.
                        val f = selX.value.coerceIn(0f, (drawn.size - 1).toFloat())
                        val i0 = f.toInt()
                        val i1 = (i0 + 1).coerceAtMost(drawn.lastIndex)
                        val t = f - i0
                        val p = Offset(drawn[i0].x + (drawn[i1].x - drawn[i0].x) * t, drawn[i0].y + (drawn[i1].y - drawn[i0].y) * t)
                        drawCircle(line, selRadius * selScale, p)
                        drawCircle(ring, selRadius * selScale, p, style = Stroke(ringWidth))
                    }
                }
            },
    )
}

@Composable
private fun SlotLabel(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = KcalTheme.colors
    val bg by animateColorAsState(if (selected) colors.accent.copy(alpha = 0.5f) else colors.accent.copy(alpha = 0f), KcalMotion.fadeTween(), label = "slot-pill")
    val style = if (selected) KcalTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold) else KcalTheme.typography.caption
    Text(
        label, style = style, color = if (selected) colors.textBrand else colors.textTertiary, maxLines = 1,
        modifier = Modifier
            .background(bg, KcalShapes.pill)
            .then(if (enabled) Modifier.kcalClickable(shape = KcalShapes.pill, onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

@Composable
private fun PageArrow(enabled: Boolean, back: Boolean, onClick: () -> Unit) {
    val colors = KcalTheme.colors
    Box(
        Modifier.size(32.dp).kcalClickable(shape = KcalShapes.pill, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(KcalIcons.ChevronRight),
            contentDescription = stringResource(if (back) R.string.progress_earlier else R.string.progress_later),
            tint = if (enabled) colors.textBrand else colors.textTertiary,
            modifier = Modifier.size(16.dp).then(if (back) Modifier.rotate(180f) else Modifier),
        )
    }
}

package com.alphito.kcal.core.designsystem.chart

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.clip
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay

@Immutable
data class BarEntry(val label: String, val value: Float, val description: String = label)

/**
 * Steps history bars. Each bar springs to its height with a small stagger; switching between
 * Months / Weeks / Days morphs heights in place. Height is applied in the *layout* phase and
 * colour in the *draw* phase, so the animation never recomposes the chart.
 */
@Composable
fun KcalBarChart(
    entries: ImmutableList<BarEntry>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 76.dp,
    maxBarWidth: Dp = 26.dp,
) {
    val colors = KcalTheme.colors
    val max = remember(entries) { entries.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEachIndexed { index, entry ->
            val selected = index == selectedIndex
            val height = remember(entries.size) { Animatable(0f) }
            LaunchedEffect(entry.value, max) {
                delay(index * 45L)
                height.animateTo((entry.value / max).coerceIn(0.08f, 1f), KcalMotion.chartSpring())
            }
            val barColor by animateColorAsState(if (selected) colors.primary else colors.track, KcalMotion.fadeTween(), label = "bar")
            val labelBg by animateColorAsState(if (selected) colors.accentContainer else colors.surface.copy(alpha = 0f), KcalMotion.fadeTween(), label = "bar-label")
            val source = remember { MutableInteractionSource() }
            Column(
                Modifier
                    .weight(1f)
                    .clip(KcalShapes.sm)
                    .selectable(selected, interactionSource = source, indication = ripple(), role = Role.Tab) { onSelect(index) }
                    .semantics { contentDescription = entry.description },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.height(chartHeight).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .widthIn(max = maxBarWidth)
                            .fillMaxWidth()
                            .layout { measurable, constraints ->
                                val h = (constraints.maxHeight * height.value).toInt()
                                val p = measurable.measure(Constraints.fixed(constraints.maxWidth, h))
                                layout(p.width, h) { p.place(0, 0) }
                            }
                            .drawBehind { drawRoundRect(barColor, cornerRadius = CornerRadius(8.dp.toPx())) },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    entry.label,
                    style = if (selected) KcalTheme.typography.labelSmall else KcalTheme.typography.bodySmall,
                    color = if (selected) colors.textBrand else colors.textTertiary,
                    maxLines = 1,
                    modifier = Modifier.background(labelBg, KcalShapes.pill).padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
    }
}

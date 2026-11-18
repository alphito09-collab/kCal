package com.alphito.kcal.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import kotlinx.collections.immutable.ImmutableList

/** Card-style selection: ripple only (clipped to the card) + correct a11y role. */
@Composable
private fun Modifier.kcalSelectable(selected: Boolean, shape: androidx.compose.ui.graphics.Shape, role: Role, onClick: () -> Unit): Modifier =
    clip(shape).selectable(selected = selected, interactionSource = null, indication = ripple(), role = role, onClick = onClick)

/** Goal / budget option card: radio dot, title, subtitle; rose fill + border when selected. */
@Composable
fun SelectableOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KcalTheme.colors
    val container by animateColorAsState(if (selected) colors.selectedContainer else colors.surface, KcalMotion.fadeTween(), label = "opt-bg")
    val border by animateColorAsState(if (selected) colors.selectedBorder else colors.outline, KcalMotion.fadeTween(), label = "opt-border")
    Row(
        modifier
            .fillMaxWidth()
            .kcalSelectable(selected, KcalShapes.md, Role.RadioButton, onClick)
            .background(container, KcalShapes.md)
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, border), KcalShapes.md)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioDot(selected = selected, filledStyle = true)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = KcalTheme.typography.titleSmall, color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

/**
 * Radio indicator. `filledStyle` = solid plum dot (Figma option cards);
 * otherwise a ring with an inner dot (language / swap lists). The dot springs in.
 */
@Composable
fun RadioDot(selected: Boolean, modifier: Modifier = Modifier, size: Dp = 20.dp, filledStyle: Boolean = false) {
    val colors = KcalTheme.colors
    val dotScale by animateFloatAsState(if (selected) 1f else 0f, KcalMotion.bouncySpring(), label = "radio")
    if (filledStyle) {
        Box(modifier.size(size).background(if (selected) colors.primary else colors.track, KcalShapes.pill))
        return
    }
    Box(
        modifier
            .size(size)
            .background(if (selected) colors.primary else colors.surface, KcalShapes.pill)
            .border(1.5.dp, if (selected) colors.primary else colors.outline, KcalShapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size * 0.36f)
                .graphicsLayer { scaleX = dotScale; scaleY = dotScale }
                .background(colors.onPrimary, KcalShapes.pill),
        )
    }
}

enum class ChipStyle {
    /** Selected = blush fill, plum text (health conditions, gender). */
    Soft,
    /** Selected = solid plum, white text (symptoms, severity, yes/no). */
    Solid,
}

@Composable
fun KcalChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChipStyle = ChipStyle.Soft,
    multiSelect: Boolean = true,
    fill: Boolean = false,
) {
    val colors = KcalTheme.colors
    val (selBg, selFg) = when (style) {
        ChipStyle.Soft -> colors.accentContainer to colors.textBrand
        ChipStyle.Solid -> colors.primary to colors.onPrimary
    }
    val bg by animateColorAsState(if (selected) selBg else colors.surface, KcalMotion.fadeTween(), label = "chip-bg")
    val fg by animateColorAsState(if (selected) selFg else colors.textTertiary, KcalMotion.fadeTween(), label = "chip-fg")
    val borderColor by animateColorAsState(if (selected) selBg else colors.outline, KcalMotion.fadeTween(), label = "chip-border")
    val source = remember { MutableInteractionSource() }
    val selection = if (multiSelect) {
        Modifier.toggleable(selected, source, ripple(), role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        Modifier.selectable(selected, interactionSource = source, indication = ripple(), role = Role.RadioButton, onClick = onClick)
    }
    Box(
        modifier
            .clip(KcalShapes.pill)
            .then(selection)
            .background(bg, KcalShapes.pill)
            .border(1.5.dp, borderColor, KcalShapes.pill)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = KcalTheme.typography.label, color = fg, textAlign = TextAlign.Center,
            modifier = if (fill) Modifier.fillMaxWidth() else Modifier,
        )
    }
}

/**
 * Pill segmented control ("Weekly | Monthly", "Months | Weeks | Days").
 * The thumb slides with a spring; its offset is read in the layout phase (`offset { }`)
 * so dragging the selection never recomposes the labels.
 */
@Composable
fun SegmentedControl(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
) {
    val colors = KcalTheme.colors
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(colors.track.copy(alpha = if (colors.isDark) 1f else 0.7f), KcalShapes.md)
            .padding(4.dp),
    ) {
        val segment = maxWidth / options.size
        val thumbOffset by animateDpAsState(segment * selectedIndex, KcalMotion.dpSpring, label = "seg-thumb")
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(thumbOffset.roundToPx(), 0) }
                .width(segment)
                .fillMaxHeight()
                .background(colors.primary, KcalShapes.sm),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val fg by animateColorAsState(if (selected) colors.onPrimary else colors.textTertiary, KcalMotion.fadeTween(), label = "seg-fg")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(KcalShapes.sm)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = KcalTheme.typography.label, color = fg)
                }
            }
        }
    }
}

/** Rounded square checkbox from the Meal Checklist (check mark springs in). */
@Composable
fun KcalCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val colors = KcalTheme.colors
    val bg by animateColorAsState(if (checked) colors.primary else colors.surface, KcalMotion.fadeTween(), label = "cb-bg")
    val markScale by animateFloatAsState(if (checked) 1f else 0f, KcalMotion.bouncySpring(), label = "cb-mark")
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(size + 16.dp)
            .clip(KcalShapes.sm)
            .toggleable(checked, source, ripple(bounded = false, radius = 20.dp), role = Role.Checkbox, onValueChange = onCheckedChange),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .background(bg, KcalShapes.xs)
                .border(1.5.dp, if (checked) colors.primary else colors.track, KcalShapes.xs),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(KcalIcons.Check), null, tint = colors.onPrimary,
                modifier = Modifier.size(size * 0.6f).graphicsLayer { scaleX = markScale; scaleY = markScale; alpha = markScale },
            )
        }
    }
}

@Composable
fun KcalSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = KcalTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onPrimary,
            checkedTrackColor = colors.primary,
            checkedBorderColor = colors.primary,
            uncheckedThumbColor = colors.surface,
            uncheckedTrackColor = colors.track,
            uncheckedBorderColor = colors.track,
        ),
    )
}

/** Onboarding wizard progress: N rounded segments, filled ones animate their colour. */
@Composable
fun StepProgress(total: Int, current: Int, modifier: Modifier = Modifier) {
    val colors = KcalTheme.colors
    Row(modifier.fillMaxWidth().height(4.dp)) {
        repeat(total) { index ->
            val filled = index < current
            val c by animateColorAsState(if (filled) colors.primary else colors.track, KcalMotion.fadeTween(KcalMotion.DurationMedium), label = "step")
            Box(Modifier.weight(1f).fillMaxHeight().background(c, KcalShapes.pill))
            if (index != total - 1) Spacer(Modifier.width(8.dp))
        }
    }
}

/**
 * Pager dots – the active dot stretches into a pill that follows the pager's fractional offset.
 * [position] is read only in the layout (width) and draw (colour) phases, so scrolling the
 * pager never recomposes this row.
 */
@Composable
fun PagerDots(count: Int, position: () -> Float, modifier: Modifier = Modifier) {
    val colors = KcalTheme.colors
    val active = colors.primary
    val inactive = colors.track
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .layout { measurable, _ ->
                        val f = activeFraction(index, position())
                        val w = lerpDp(8.dp, 22.dp, f).roundToPx()
                        val h = 8.dp.roundToPx()
                        val placeable = measurable.measure(Constraints.fixed(w, h))
                        layout(w, h) { placeable.place(0, 0) }
                    }
                    .drawBehind {
                        val f = activeFraction(index, position())
                        drawRoundRect(lerp(inactive, active, f), cornerRadius = CornerRadius(size.height / 2f))
                    },
            )
        }
    }
}

private fun activeFraction(index: Int, position: Float): Float =
    (1f - kotlin.math.abs(position - index)).coerceIn(0f, 1f)

private fun lerpDp(start: Dp, end: Dp, fraction: Float): Dp = start + (end - start) * fraction

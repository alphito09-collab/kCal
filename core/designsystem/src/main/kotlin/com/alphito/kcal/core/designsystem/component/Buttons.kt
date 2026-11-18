package com.alphito.kcal.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.modifier.kcalClickable
import com.alphito.kcal.core.designsystem.modifier.softShadow
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme

enum class KcalButtonStyle { Primary, Inverse, Tonal, Outline }

/** 51dp primary CTA from Figma ("Button/Primary"), with spring press + clipped ripple. */
@Composable
fun KcalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: KcalButtonStyle = KcalButtonStyle.Primary,
    enabled: Boolean = true,
    height: Dp = 51.dp,
    shape: Shape = KcalShapes.md,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
    /** Morph instead of swap: the label slides to the new text while the width springs. */
    animateLabel: Boolean = false,
) {
    val colors = KcalTheme.colors
    val (container, content) = when (style) {
        KcalButtonStyle.Primary -> colors.primary to colors.onPrimary
        KcalButtonStyle.Inverse -> colors.surface to colors.textBrand
        KcalButtonStyle.Tonal -> colors.accentContainer to colors.textBrand
        KcalButtonStyle.Outline -> colors.surface to colors.textTertiary
    }
    val animatedContainer by animateColorAsState(
        if (enabled) container else container.copy(alpha = 0.45f), KcalMotion.fadeTween(), label = "btn-container",
    )
    Box(
        modifier = modifier
            .then(
                if (style == KcalButtonStyle.Primary && enabled) {
                    Modifier.softShadow(colors.primary.copy(alpha = 0.22f), cornerRadius = 16.dp, blur = 14.dp, offsetY = 5.dp)
                } else Modifier,
            )
            .then(if (animateLabel) Modifier.animateContentSize(KcalMotion.sizeSpring) else Modifier)
            .defaultMinSize(minHeight = height)
            .kcalClickable(shape = shape, enabled = enabled, onClick = onClick)
            .background(animatedContainer, shape)
            .then(if (style == KcalButtonStyle.Outline) Modifier.border(BorderStroke(1.5.dp, colors.outline), shape) else Modifier)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (leadingIcon != null) {
                Icon(painterResource(leadingIcon), null, tint = content, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
            }
            if (animateLabel) {
                AnimatedContent(
                    targetState = text,
                    transitionSpec = {
                        (slideInVertically(KcalMotion.slideSpring) { it } + fadeIn(KcalMotion.fadeTween())) togetherWith
                            (slideOutVertically(KcalMotion.slideSpring) { -it } + fadeOut(KcalMotion.fadeTween(120))) using
                            SizeTransform(clip = false) { _, _ -> KcalMotion.sizeSpring }
                    },
                    label = "button-label",
                ) { label -> Text(label, style = KcalTheme.typography.button, color = content, maxLines = 1) }
            } else {
                Text(text, style = KcalTheme.typography.button, color = content)
            }
            if (trailingIcon != null) {
                Spacer(Modifier.width(10.dp))
                Icon(painterResource(trailingIcon), null, tint = content, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun KcalTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = KcalTheme.colors.textSecondary,
    underline: Boolean = false,
) {
    Box(
        modifier
            .kcalClickable(shape = KcalShapes.xs, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = KcalTheme.typography.label.copy(textDecoration = if (underline) TextDecoration.Underline else null),
            color = color,
        )
    }
}

/** Round icon action (Steps "Start", +/- steppers, logout). */
@Composable
fun KcalIconButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 18.dp,
    container: Color = KcalTheme.colors.track,
    tint: Color = KcalTheme.colors.primary,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .kcalClickable(shape = KcalShapes.pill, enabled = enabled, onClick = onClick)
            .background(container, KcalShapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Fills the remaining width; convenience for full-width CTAs. */
fun Modifier.fullWidthButton(): Modifier = fillMaxWidth().height(51.dp)

package com.alphito.kcal.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.core.designsystem.util.appLocale

/** Number that counts up/down to its new value (kcal totals, steps). */
@Composable
fun AnimatedNumber(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = KcalTheme.colors.textBrand,
    prefix: String = "",
    suffix: String = "",
) {
    val animated by animateIntAsState(value, tween(KcalMotion.DurationChart, easing = KcalMotion.EmphasizedDecelerate), label = "number")
    Text(prefix + KcalFormat.int(animated, appLocale()) + suffix, style = style, color = color, modifier = modifier)
}

/** 5-star rating; the tapped star does a spring "pop". */
@Composable
fun StarRating(rating: Int, onRate: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = KcalTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        for (star in 1..5) {
            val filled = star <= rating
            val pop = remember { Animatable(1f) }
            LaunchedEffect(filled) {
                if (filled && star == rating) {
                    pop.snapTo(0.6f)
                    pop.animateTo(1f, KcalMotion.bouncySpring())
                }
            }
            Icon(
                painterResource(if (filled) KcalIcons.StarFilled else KcalIcons.StarOutline),
                contentDescription = androidx.compose.ui.res.pluralStringResource(com.alphito.kcal.core.designsystem.R.plurals.ds_stars, star, star),
                tint = if (filled) colors.primary else colors.textTertiary,
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                    .selectable(
                        selected = filled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 22.dp),
                        role = Role.RadioButton,
                    ) { onRate(star) },
            )
        }
    }
}

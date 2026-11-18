package com.alphito.kcal.feature.steps.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.modifier.kcalClickable
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme

/**
 * Round Start/Stop button. While tracking, two soft rings "breathe" outward. The ripple
 * values are read inside `drawBehind`, so the infinite animation only redraws – it never
 * recomposes the button or the screen.
 */
@Composable
internal fun StartStopButton(tracking: Boolean, startLabel: String, stopLabel: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = KcalTheme.colors
    val pulse = rememberInfiniteTransition(label = "pulse")
    val wave by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1_600, easing = LinearEasing), RepeatMode.Restart), label = "wave")
    val container by animateColorAsState(
        when {
            !enabled -> colors.primary.copy(alpha = 0.35f)
            tracking -> colors.textPrimary
            else -> colors.primary
        },
        KcalMotion.fadeTween(), label = "start-bg",
    )
    val ringColor = colors.accent
    Box(
        Modifier
            .size(84.dp)
            .drawBehind {
                if (tracking) {
                    for (i in 0..1) {
                        val p = (wave + i * 0.5f) % 1f
                        drawCircle(ringColor.copy(alpha = (1f - p) * 0.45f), radius = size.minDimension / 2f * (1f + p * 0.45f))
                    }
                }
            }
            .kcalClickable(shape = KcalShapes.pill, enabled = enabled, onClick = onClick)
            .background(container, KcalShapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = tracking,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.7f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.7f)) },
            label = "start-content",
        ) { isTracking ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isTracking) {
                    Box(Modifier.size(14.dp).background(colors.onPrimary, KcalShapes.xs.copy(all = androidx.compose.foundation.shape.CornerSize(3.dp))))
                } else {
                    Icon(painterResource(KcalIcons.Play), null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(if (isTracking) stopLabel else startLabel, style = KcalTheme.typography.labelBold, color = colors.onPrimary)
            }
        }
    }
}

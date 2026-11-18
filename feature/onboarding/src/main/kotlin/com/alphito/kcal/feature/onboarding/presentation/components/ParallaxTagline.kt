package com.alphito.kcal.feature.onboarding.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalTheme

private const val SLIDE_MS = 520

/**
 * Tagline ticker: the whole sentence slides up and fades out while the next sentence slides up
 * from below and fades in. Both use the same decelerating curve and duration, and the outgoing
 * line travels a bit less than the incoming one, so the swap reads as one smooth upward motion.
 * The container has a fixed height (set by the caller), so nothing around it moves.
 */
@Composable
internal fun ParallaxTagline(text: String, modifier: Modifier = Modifier, style: TextStyle = KcalTheme.typography.body) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(tween(SLIDE_MS, easing = KcalMotion.EmphasizedDecelerate)) { it } +
                fadeIn(tween(SLIDE_MS, delayMillis = 60, easing = KcalMotion.Standard))) togetherWith
                (slideOutVertically(tween(SLIDE_MS, easing = KcalMotion.EmphasizedDecelerate)) { -it * 3 / 4 } +
                    fadeOut(tween(SLIDE_MS / 2, easing = KcalMotion.Standard))) using
                SizeTransform(clip = false)
        },
        contentAlignment = Alignment.Center,
        modifier = modifier,
        label = "tagline",
    ) { line ->
        Text(line, style = style, color = KcalTheme.colors.textSecondary, textAlign = TextAlign.Center, maxLines = 1)
    }
}

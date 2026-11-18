package com.alphito.kcal.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Soft blush/plum "light blobs" behind onboarding screens.
 *
 * Depth comes from parallax: each blob moves by a different factor of [parallax] (pager
 * position, scroll offset…), plus a slow independent drift. Every animated value is read
 * inside `drawBehind`, so the background animates in the draw phase only – it never
 * recomposes or relayouts the content on top.
 */
@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    parallax: () -> Float = { 0f },
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = KcalTheme.colors
    val drift by rememberInfiniteTransition(label = "ambient").animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift",
    )
    val blush = colors.accent.copy(alpha = if (colors.isDark) 0.16f else 0.30f)
    val plum = colors.primary.copy(alpha = if (colors.isDark) 0.14f else 0.10f)
    val rose = colors.selectedContainer.copy(alpha = if (colors.isDark) 0.18f else 0.45f)
    Box(
        modifier.drawBehind {
            val p = parallax()
            blob(blush, Offset(size.width * 0.12f + cos(drift) * 30f - p * 90f, size.height * 0.16f + sin(drift) * 24f), size.width * 0.55f)
            blob(rose, Offset(size.width * 0.92f + sin(drift) * 26f - p * 180f, size.height * 0.42f + cos(drift) * 30f), size.width * 0.50f)
            blob(plum, Offset(size.width * 0.30f + cos(drift + 1f) * 22f - p * 40f, size.height * 0.92f + sin(drift) * 18f), size.width * 0.60f)
        },
        content = content,
    )
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float) {
    drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, radius), radius, center)
}

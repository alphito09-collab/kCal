package com.alphito.kcal.core.designsystem.modifier

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Diffuse, colour-tinted shadow (the Figma file is flat; this adds the soft plum depth the
 * cards need without the grey "elevation" look of `Modifier.shadow`).
 *
 * Paint + blur filter are created once in `drawWithCache` and only rebuilt when size or
 * parameters change; drawing happens in the draw phase so it never triggers recomposition.
 */
@Stable
fun Modifier.softShadow(
    color: Color,
    cornerRadius: Dp = 22.dp,
    blur: Dp = 18.dp,
    offsetY: Dp = 6.dp,
    spread: Dp = 0.dp,
): Modifier = drawWithCache {
    val blurPx = blur.toPx()
    val spreadPx = spread.toPx()
    val radius = cornerRadius.toPx()
    val dy = offsetY.toPx()
    val paint = Paint().also { p ->
        p.asFrameworkPaint().apply {
            isAntiAlias = true
            this.color = color.toArgb()
            if (blurPx > 0f) maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    onDrawBehind {
        drawIntoCanvas { canvas ->
            canvas.drawRoundRect(
                left = -spreadPx, top = -spreadPx + dy,
                right = size.width + spreadPx, bottom = size.height + spreadPx + dy,
                radiusX = radius, radiusY = radius, paint = paint,
            )
        }
    }
}

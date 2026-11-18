package com.alphito.kcal.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalPalette
import kotlinx.coroutines.launch

/** Geometry of the Figma "kCal Logo" (viewport 238×194), parsed once. */
private object LogoGeometry {
    const val W = 238f
    const val H = 194f
    val bowl: Path = PathParser().parsePathString(
        "M47.6001 81.9905C47.6001 139.884 78.5401 167.759 119 167.759C159.46 167.759 190.4 139.884 190.4 81.9905",
    ).toPath()
    val rim: Path = PathParser().parsePathString(
        "M190.4 81.9905C190.4 92.6485 158.433 101.288 119 101.288C79.567 101.288 47.6001 92.6485 47.6001 81.9905C47.6001 71.3326 79.567 62.6926 119 62.6926C158.433 62.6926 190.4 71.3326 190.4 81.9905Z",
    ).toPath()
    val plumLeaf: Path = PathParser().parsePathString(
        "M119 81.9905C99.9599 60.5484 80.9199 39.1063 80.9199 13.3757C109.48 26.241 128.52 51.9715 119 81.9905Z",
    ).toPath()
    val blushLeaf: Path = PathParser().parsePathString(
        "M119.745 81C134.025 55.2695 157.825 29.539 186.385 16.6737C162.585 46.6926 138.785 68.1348 119.745 81Z",
    ).toPath()
    val leafPivot = Offset(119f, 82f)
}

/**
 * The kCal bowl, drawn on Canvas so it can be animated:
 * the bowl outline "draws" itself (PathMeasure trim), then both leaves spring out of the bowl.
 * With [animate] = false it renders the final state (used as the shared-element target).
 */
@Composable
fun KcalLogo(
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    strokeColor: Color = KcalPalette.Plum,
    leafColor: Color = KcalPalette.Plum,
    accentLeafColor: Color = KcalPalette.Blush,
    strokeWidth: Dp = 2.5.dp,
) {
    val draw = remember { Animatable(if (animate) 0f else 1f) }
    val leaves = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        launch { draw.animateTo(1f, tween(KcalMotion.DurationLong + 200, easing = KcalMotion.EmphasizedDecelerate)) }
        launch {
            kotlinx.coroutines.delay(320)
            leaves.animateTo(1f, KcalMotion.bouncySpring())
        }
    }
    val measure = remember { PathMeasure() }
    val trimmedBowl = remember { Path() }
    val trimmedRim = remember { Path() }

    Canvas(modifier) {
        val s = minOf(size.width / LogoGeometry.W, size.height / LogoGeometry.H)
        val dx = (size.width - LogoGeometry.W * s) / 2f
        val dy = (size.height - LogoGeometry.H * s) / 2f
        // Constant on-screen stroke (like the Figma small & large logos), independent of scale.
        val stroke = Stroke(width = strokeWidth.toPx() / s, cap = StrokeCap.Round)
        withTransform({
            translate(dx, dy)
            scale(s, s, pivot = Offset.Zero)
        }) {
            val p = draw.value
            if (p >= 1f) {
                drawPath(LogoGeometry.bowl, strokeColor, style = stroke)
                drawPath(LogoGeometry.rim, strokeColor, style = stroke)
            } else {
                trimmedBowl.reset(); trimmedRim.reset()
                measure.setPath(LogoGeometry.rim, false)
                measure.getSegment(0f, measure.length * p, trimmedRim, true)
                measure.setPath(LogoGeometry.bowl, false)
                measure.getSegment(0f, measure.length * p, trimmedBowl, true)
                drawPath(trimmedRim, strokeColor, style = stroke)
                drawPath(trimmedBowl, strokeColor, style = stroke)
            }
            val l = leaves.value
            if (l > 0f) {
                scale(l, l, pivot = LogoGeometry.leafPivot) {
                    rotateAround(-14f * (1f - l)) { drawPath(LogoGeometry.plumLeaf, leafColor) }
                    rotateAround(14f * (1f - l)) { drawPath(LogoGeometry.blushLeaf, accentLeafColor) }
                }
            }
        }
    }
}

private inline fun androidx.compose.ui.graphics.drawscope.DrawScope.rotateAround(
    degrees: Float,
    block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
) = withTransform({ rotate(degrees, LogoGeometry.leafPivot) }, block)

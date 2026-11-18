package com.alphito.kcal.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** Motion tokens (Material 3 expressive easing + kCal springs). */
object KcalMotion {
    const val DurationShort = 180
    const val DurationMedium = 320
    const val DurationLong = 480
    const val DurationChart = 900

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Press feedback – quick, slightly under-damped so release "pops" back. */
    fun <T> pressSpring(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)

    /** Selection indicators (segmented control, bottom bar pill). */
    fun <T> indicatorSpring(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.78f, stiffness = 380f)

    /** Playful bounce for check marks, stars and success states. */
    fun <T> bouncySpring(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Charts grow with a soft spring so bars settle naturally. */
    fun <T> chartSpring(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow)

    val sharedBoundsSpring: SpringSpec<Rect> = spring(dampingRatio = 0.86f, stiffness = 320f, visibilityThreshold = Rect.VisibilityThreshold)

    fun <T> fadeTween(duration: Int = DurationShort): FiniteAnimationSpec<T> = tween(duration, easing = Standard)

    val slideSpring: SpringSpec<IntOffset> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f, visibilityThreshold = IntOffset.VisibilityThreshold)
    val sizeSpring: SpringSpec<IntSize> = spring(dampingRatio = 0.85f, stiffness = 400f, visibilityThreshold = IntSize.VisibilityThreshold)
    val dpSpring: SpringSpec<Dp> = spring(dampingRatio = 0.8f, stiffness = 380f, visibilityThreshold = Dp.VisibilityThreshold)
}

package com.alphito.kcal.core.designsystem.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** Provided once by the app around NavDisplay. Null in previews → shared modifiers are no-ops. */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** The AnimatedContent scope of the current navigation entry (bridged from Navigation 3). */
val LocalEntryAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

private val KcalBoundsTransform = BoundsTransform { _, _ -> KcalMotion.sharedBoundsSpring }

/**
 * Shared element for content that looks identical on both screens (logo, food photo, crown).
 * Falls back to a plain modifier when not inside a shared transition (previews, dialogs).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.kcalSharedElement(key: String): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalEntryAnimatedScope.current ?: return this
    return with(shared) {
        this@kcalSharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
            boundsTransform = KcalBoundsTransform,
        )
    }
}

/**
 * Container transform for content that changes between screens (card → screen header).
 * The two contents cross-fade while the bounds morph with a spring.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.kcalSharedBounds(
    key: String,
    enter: EnterTransition = fadeIn(KcalMotion.fadeTween(KcalMotion.DurationMedium)),
    exit: ExitTransition = fadeOut(KcalMotion.fadeTween(KcalMotion.DurationShort)),
): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalEntryAnimatedScope.current ?: return this
    return with(shared) {
        this@kcalSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
            enter = enter,
            exit = exit,
            boundsTransform = KcalBoundsTransform,
            resizeMode = RemeasureToBounds,
        )
    }
}

/** Keeps persistent chrome (bottom bar) above content that is mid shared-transition. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.kcalRenderAboveSharedTransition(zIndex: Float = 1f): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    return with(shared) { this@kcalRenderAboveSharedTransition.renderInSharedTransitionScopeOverlay(zIndexInOverlay = zIndex) }
}

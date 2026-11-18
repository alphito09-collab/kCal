package com.alphito.kcal.mealplanner.padometer.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.navigation.NavMotion

/*
 * Material motion for Navigation 3, chosen per destination via NavMotion metadata:
 *  - shared axis X for regular push/pop (spring-driven slide + staggered fade),
 *  - fade-through for bottom-bar tabs,
 *  - plain cross-fade where a shared-bounds container transform is the hero.
 */

private fun Scene<*>.motion(): Any? = metadata[NavMotion.KEY]

/** In RTL the reading direction flips, so "forward" enters from the left. */
private fun sharedAxisX(forward: Boolean, rtl: Boolean): ContentTransform {
    val dir = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
    return (slideInHorizontally(KcalMotion.slideSpring) { (it * 0.18f).toInt() * dir } +
        fadeIn(tween(KcalMotion.DurationMedium - 60, delayMillis = 60, easing = KcalMotion.EmphasizedDecelerate))) togetherWith
        (slideOutHorizontally(KcalMotion.slideSpring) { -(it * 0.08f).toInt() * dir } +
            fadeOut(tween(KcalMotion.DurationShort - 40, easing = KcalMotion.EmphasizedAccelerate)))
}

private fun fadeThrough(): ContentTransform =
    (fadeIn(tween(220, delayMillis = 90, easing = KcalMotion.EmphasizedDecelerate)) +
        scaleIn(tween(260, delayMillis = 90, easing = KcalMotion.EmphasizedDecelerate), initialScale = 0.94f)) togetherWith
        fadeOut(tween(90, easing = KcalMotion.EmphasizedAccelerate))

private fun crossFade(): ContentTransform =
    fadeIn(tween(KcalMotion.DurationMedium, easing = KcalMotion.Standard)) togetherWith
        fadeOut(tween(KcalMotion.DurationShort, easing = KcalMotion.Standard))

fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.kcalPushTransition(rtl: Boolean): ContentTransform =
    when (targetState.motion()) {
        "topLevel" -> fadeThrough()
        "container", "fade" -> crossFade()
        else -> sharedAxisX(forward = true, rtl = rtl)
    }.apply { targetContentZIndex = 1f }

fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.kcalPopTransition(rtl: Boolean): ContentTransform =
    when (initialState.motion()) {
        "topLevel" -> fadeThrough()
        "container", "fade" -> crossFade()
        else -> sharedAxisX(forward = false, rtl = rtl)
    }

/** Predictive back: the leaving screen shrinks toward the swipe edge while the previous one fades up. */
@Suppress("UNUSED_PARAMETER")
fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.kcalPredictivePopTransition(
    @NavigationEvent.SwipeEdge swipeEdge: Int,
): ContentTransform =
    if (initialState.motion() == "container") crossFade()
    else (fadeIn(tween(KcalMotion.DurationMedium)) + scaleIn(tween(KcalMotion.DurationMedium), initialScale = 0.96f)) togetherWith
        (fadeOut(tween(KcalMotion.DurationMedium)) + scaleOut(tween(KcalMotion.DurationMedium), targetScale = 0.9f))

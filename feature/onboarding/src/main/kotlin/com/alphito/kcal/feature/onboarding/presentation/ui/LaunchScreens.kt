package com.alphito.kcal.feature.onboarding.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.FeatureFlags
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.component.AmbientBackground
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalLogo
import com.alphito.kcal.core.designsystem.component.KcalTextButton
import com.alphito.kcal.core.designsystem.modifier.softShadow
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.motion.kcalSharedElement
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.OnboardingImages
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.core.navigation.SharedKeys
import com.alphito.kcal.feature.onboarding.R
import com.alphito.kcal.feature.onboarding.presentation.components.ParallaxTagline
import com.alphito.kcal.feature.onboarding.presentation.mvi.LaunchMode
import com.alphito.kcal.feature.onboarding.presentation.mvi.SplashEffect
import com.alphito.kcal.feature.onboarding.presentation.mvi.SplashIntent
import com.alphito.kcal.feature.onboarding.presentation.mvi.SplashState
import com.alphito.kcal.feature.onboarding.presentation.mvi.SplashViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private val Taglines = listOf(R.string.onb_tagline, R.string.onb_tagline_habits, R.string.onb_tagline_smarter)

// ================================================================== Splash

@Composable
internal fun SplashRoute() {
    val viewModel: SplashViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            SplashEffect.OpenHome -> navigator.resetTo(Route.Home)
            SplashEffect.OpenAboutYou -> navigator.resetTo(Route.AboutYou())
            SplashEffect.OpenPremiumIntro -> navigator.replace(
                if (FeatureFlags.PREMIUM_ENABLED) Route.PremiumIntro(fromOnboarding = true) else Route.Language(onboarding = true),
            )
        }
    }
    BackHandler { /* first screen of the app – back is intentionally disabled */ }
    SplashScreen(state) { viewModel.onIntent(SplashIntent.GetStarted) }
}

/**
 * Splash: the bowl draws itself, then the three taglines slide up one after another
 * (random order, one per second, stopping on the last) while the background blobs drift.
 * First session → "Get Started" rises in. Returning → 4 s progress + ads disclosure.
 */
@Composable
internal fun SplashScreen(state: SplashState, onGetStarted: () -> Unit) {
    AmbientBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            // The logo never moves while taglines change – only the text line animates.
            KcalLogo(
                Modifier.kcalSharedElement(SharedKeys.LOGO).size(width = 150.dp, height = 122.dp),
                animate = true,
            )
            Spacer(Modifier.height(36.dp))
            ParallaxTagline(
                text = stringResource(Taglines[state.taglineIndex]),
                style = KcalTheme.typography.body.copy(fontSize = KcalTheme.typography.titleSmall.fontSize),
                modifier = Modifier.fillMaxWidth().height(28.dp),
            )
            Spacer(Modifier.weight(1.25f))
            // Fixed-height slot: the CTA / progress appear without shifting the logo.
            Box(Modifier.fillMaxWidth().height(102.dp), contentAlignment = Alignment.BottomCenter) {
            when (state.mode) {
                LaunchMode.FirstLaunch -> androidx.compose.animation.AnimatedVisibility(
                    visible = state.showGetStarted,
                    enter = fadeIn(KcalMotion.fadeTween(KcalMotion.DurationMedium)) + slideInVertically(KcalMotion.slideSpring) { it },
                ) {
                    KcalButton(
                        stringResource(R.string.onb_get_started), onGetStarted,
                        Modifier.fillMaxWidth().padding(bottom = 24.dp), height = 54.dp,
                    )
                }
                LaunchMode.Returning -> ReturningProgress()
                LaunchMode.Loading -> Unit
            }
            }
        }
    }
}

@Composable
private fun ReturningProgress() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SplashViewModel.RETURNING_SPLASH_MS.toInt(), easing = LinearEasing))
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val colors = KcalTheme.colors
        Canvas(Modifier.fillMaxWidth().height(6.dp)) {
            val r = CornerRadius(size.height / 2f)
            drawRoundRect(colors.track, cornerRadius = r)
            drawRoundRect(colors.primary, size = Size(size.width * progress.value, size.height), cornerRadius = r)
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.onb_may_contain_ads), style = KcalTheme.typography.caption, color = colors.textTertiary)
    }
}

// ================================================================== Intro (depth parallax)

private data class IntroPage(val image: Int, val title: Int, val body: Int)

private val introPages = listOf(
    IntroPage(OnboardingImages.NoManualLogging, R.string.onb_intro_1_title, R.string.onb_intro_1_body),
    IntroPage(OnboardingImages.BuiltForRegion, R.string.onb_intro_2_title, R.string.onb_intro_2_body),
    IntroPage(OnboardingImages.FitsBudget, R.string.onb_intro_3_title, R.string.onb_intro_3_body),
)

/** No bounce: page changes glide in and settle without overshoot (an overshoot flashes the neighbour page). */
private val IntroSpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 170f)

/**
 * Intro story. Pages are layered (not a sliding carousel); one [position] (0 → 2) drives three
 * depths with linear cross-fades:
 *   background blobs ×0.6 (far) · illustration card – soft rise + slight scale (mid) ·
 *   title and body – horizontal parallax at two speeds (near).
 * A critically damped spring and no rotations keep it calm and glitch-free. Everything is applied
 * in graphicsLayer / draw / placement, so dragging never recomposes. Back is disabled here.
 * Portrait stacks image over text; landscape puts them side by side.
 */
@Composable
internal fun IntroScreen() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val position = remember { Animatable(0f) }
    val last = introPages.lastIndex
    val page by remember { derivedStateOf { position.value.roundToInt().coerceIn(0, last) } }
    val goTo: (Int) -> Unit = { target -> scope.launch { position.animateTo(target.toFloat(), IntroSpring) } }
    var dragStart by remember { mutableFloatStateOf(0f) }
    val settle: () -> Unit = {
        val delta = position.value - dragStart
        val target = when {
            delta > 0.15f -> ceil(position.value).toInt()
            delta < -0.15f -> floor(position.value).toInt()
            else -> position.value.roundToInt()
        }
        goTo(target.coerceIn(0, last))
    }
    val finish: () -> Unit = { navigator.navigate(Route.AboutYou()) }
    val next: () -> Unit = { if (page < last) goTo(page + 1) else finish() }

    BackHandler { /* intro is not a back-stack step */ }
    val isRtl = LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl

    AmbientBackground(Modifier.fillMaxSize(), parallax = { position.value * 0.6f }) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(isRtl) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragStart = position.value },
                        onDragEnd = settle,
                        onDragCancel = settle,
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val d = if (isRtl) -dragAmount else dragAmount
                            position.snapTo((position.value - d / size.width).coerceIn(-0.15f, last + 0.15f))
                        }
                    }
                },
        ) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IntroImages({ position.value }, Modifier.weight(1f).fillMaxHeight().padding(vertical = 8.dp))
                    Spacer(Modifier.width(28.dp))
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        IntroTopBar(page, { position.value }, finish)
                        Spacer(Modifier.weight(1f))
                        IntroTexts({ position.value }, Modifier.fillMaxWidth().height(170.dp))
                        Spacer(Modifier.weight(1f))
                        IntroBottomBar(page, next)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.padding(start = 24.dp, end = 12.dp, top = 12.dp)) { IntroTopBar(page, { position.value }, finish) }
                    Spacer(Modifier.height(28.dp))
                    IntroImages({ position.value }, Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 32.dp))
                    Spacer(Modifier.height(36.dp))
                    IntroTexts({ position.value }, Modifier.fillMaxWidth().height(170.dp).padding(horizontal = 28.dp))
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.padding(horizontal = 28.dp).padding(bottom = 24.dp)) { IntroBottomBar(page, next) }
                }
            }
        }
    }
}

@Composable
private fun IntroTopBar(page: Int, position: () -> Float, onSkip: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        StoryProgress(introPages.size, position, Modifier.weight(1f))
        // Skip is offered on the first step only.
        AnimatedVisibility(visible = page == 0, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
            Row {
                Spacer(Modifier.size(12.dp))
                KcalTextButton(stringResource(R.string.onb_skip), onSkip)
            }
        }
    }
}

@Composable
private fun IntroImages(position: () -> Float, modifier: Modifier) {
    val colors = KcalTheme.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        introPages.forEachIndexed { i, item ->
            Image(
                painter = painterResource(item.image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0, zIndex = -abs(i - position()))
                        }
                    }
                    .graphicsLayer {
                        val d = i - position()
                        val ad = abs(d).coerceAtMost(1f)
                        alpha = 1f - ad
                        translationY = d * 40.dp.toPx()
                        val sc = 1f - ad * 0.06f
                        scaleX = sc; scaleY = sc
                    }
                    .softShadow(colors.primary.copy(alpha = 0.16f), cornerRadius = 28.dp, blur = 26.dp, offsetY = 12.dp)
                    .clip(KcalShapes.xl),
            )
        }
    }
}

@Composable
private fun IntroTexts(position: () -> Float, modifier: Modifier) {
    val colors = KcalTheme.colors
    Box(modifier) {
        introPages.forEachIndexed { i, item ->
            Column(
                Modifier.fillMaxWidth().graphicsLayer {
                    val d = i - position()
                    alpha = (1f - abs(d) * 1.4f).coerceIn(0f, 1f)
                    translationX = d * 36.dp.toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(item.title), style = KcalTheme.typography.headlineSmall, color = colors.textPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(item.body),
                    style = KcalTheme.typography.body.copy(fontSize = KcalTheme.typography.titleSmall.fontSize),
                    color = colors.textSecondary, textAlign = TextAlign.Center,
                    // the body trails the title a little – a second, nearer layer
                    modifier = Modifier.graphicsLayer { translationX = (i - position()) * 24.dp.toPx() },
                )
            }
        }
    }
}

@Composable
private fun IntroBottomBar(page: Int, onNext: () -> Unit) {
    val colors = KcalTheme.colors
    val last = introPages.lastIndex
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        AnimatedContent(page, transitionSpec = {
            (slideInVertically(KcalMotion.slideSpring) { it } + fadeIn()) togetherWith (slideOutVertically(KcalMotion.slideSpring) { -it } + fadeOut())
        }, label = "counter") { p ->
            Text(stringResource(R.string.onb_page_counter, p + 1, introPages.size), style = KcalTheme.typography.label, color = colors.textTertiary)
        }
        // One button that morphs: the label slides and the width springs ("Next" → "Let's begin").
        KcalButton(
            text = stringResource(if (page == last) R.string.onb_lets_begin else R.string.onb_next),
            onClick = onNext,
            trailingIcon = com.alphito.kcal.core.designsystem.icon.KcalIcons.ViewAll,
            animateLabel = true,
        )
    }
}

/** Instagram-style segments that fill continuously with the scrub position (draw phase only). */
@Composable
private fun StoryProgress(count: Int, position: () -> Float, modifier: Modifier = Modifier) {
    val colors = KcalTheme.colors
    Canvas(modifier.height(4.dp)) {
        val gap = 6.dp.toPx()
        val w = (size.width - gap * (count - 1)) / count
        val r = CornerRadius(size.height / 2f)
        val rtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
        for (i in 0 until count) {
            val x = if (rtl) size.width - (i + 1) * w - i * gap else i * (w + gap)
            drawRoundRect(colors.track, Offset(x, 0f), Size(w, size.height), r)
            val fill = (position() - i + 1f).coerceIn(0f, 1f)
            if (fill > 0f) {
                val fx = if (rtl) x + w * (1f - fill) else x
                drawRoundRect(colors.primary, Offset(fx, 0f), Size(w * fill, size.height), r)
            }
        }
    }
}

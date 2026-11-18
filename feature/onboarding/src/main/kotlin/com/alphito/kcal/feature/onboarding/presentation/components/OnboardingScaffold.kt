package com.alphito.kcal.feature.onboarding.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.component.AmbientBackground
import com.alphito.kcal.core.designsystem.component.KcalTopBar
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.feature.onboarding.R
import kotlinx.coroutines.delay

const val WIZARD_STEPS = 5

/**
 * Shared layout for the profile wizard.
 *  - "Step N of 5" + a continuous progress bar that springs from the previous step to this one;
 *  - ambient blobs that parallax against the scroll position (draw phase only);
 *  - title / subtitle / CTA cascade in; screens stagger their own fields with [staggerIn].
 * In edit mode (from Settings) it becomes a regular back-bar screen.
 */
@Composable
internal fun OnboardingScaffold(
    title: String,
    subtitle: String,
    step: Int?,
    onBack: (() -> Unit)?,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = KcalTheme.colors
    val scroll = rememberScrollState()
    AmbientBackground(Modifier.fillMaxSize(), parallax = { scroll.value / 900f }) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp),
        ) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) {
                if (step != null) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.onb_step_of, step, WIZARD_STEPS).uppercase(),
                        style = KcalTheme.typography.overline, color = colors.textTertiary,
                        modifier = Modifier.staggerIn(0),
                    )
                    Spacer(Modifier.height(10.dp))
                    WizardProgress(step)
                    Spacer(Modifier.height(26.dp))
                    Text(title, style = KcalTheme.typography.headline, color = colors.textPrimary, modifier = Modifier.staggerIn(1))
                } else {
                    KcalTopBar(title = title, onBack = onBack)
                }
                Spacer(Modifier.height(10.dp))
                Text(subtitle, style = KcalTheme.typography.body, color = colors.textSecondary, modifier = Modifier.staggerIn(2))
                Spacer(Modifier.height(22.dp))
                content()
                Spacer(Modifier.height(24.dp))
            }
            Column(Modifier.fillMaxWidth().padding(bottom = 20.dp, top = 8.dp).staggerIn(6)) { bottom() }
        }
    }
}

/** Continuous bar: animates from the previous step's fill to this step's fill on entry. */
@Composable
private fun WizardProgress(step: Int) {
    val colors = KcalTheme.colors
    val fill = remember { Animatable((step - 1f) / WIZARD_STEPS) }
    LaunchedEffect(step) { fill.animateTo(step / WIZARD_STEPS.toFloat(), KcalMotion.chartSpring()) }
    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(colors.track, cornerRadius = r)
        drawRoundRect(colors.primary, size = Size(size.width * fill.value, size.height), cornerRadius = r)
    }
}

/**
 * Entrance cascade: element [index] rises 24dp and fades in after `index × 60ms` on a spring.
 * Runs once per screen; the value is read in graphicsLayer (no recomposition per frame).
 */
@Composable
internal fun Modifier.staggerIn(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(80L + index * 60L)
        progress.animateTo(1f, KcalMotion.chartSpring())
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 24.dp.toPx()
    }
}

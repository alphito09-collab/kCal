package com.alphito.kcal.feature.premium.presentation.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.LegalLinks
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.modifier.kcalClickable
import com.alphito.kcal.core.designsystem.component.IconChip
import com.alphito.kcal.core.designsystem.component.KcalBadge
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalButtonStyle
import com.alphito.kcal.core.designsystem.component.KcalIconButton
import com.alphito.kcal.core.designsystem.component.KcalLogo
import com.alphito.kcal.core.designsystem.component.KcalTextButton
import com.alphito.kcal.core.designsystem.component.KcalTopBar
import com.alphito.kcal.core.designsystem.component.SegmentedControl
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.modifier.softShadow
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.motion.kcalSharedElement
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.model.PremiumPlan
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.core.navigation.SharedKeys
import com.alphito.kcal.feature.premium.R
import com.alphito.kcal.feature.premium.presentation.mvi.PremiumEffect
import com.alphito.kcal.feature.premium.presentation.mvi.PremiumIntent
import com.alphito.kcal.feature.premium.presentation.mvi.PremiumState
import com.alphito.kcal.feature.premium.presentation.mvi.PremiumViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private const val CLOSE_DELAY_MS = 3_000L

private data class Benefit(@DrawableRes val icon: Int, val title: Int, val sub: Int)

private val benefits = listOf(
    Benefit(KcalIcons.Utensils, R.string.premium_b1, R.string.premium_b1_sub),
    Benefit(KcalIcons.Swap, R.string.premium_b2, R.string.premium_b2_sub),
    Benefit(KcalIcons.Budget, R.string.premium_b3, R.string.premium_b3_sub),
    Benefit(KcalIcons.Chart, R.string.premium_b4, R.string.premium_b4_sub),
    Benefit(KcalIcons.Shield, R.string.premium_b5, R.string.premium_b5_sub),
)

/** Premium intro – benefits cascade in one after another (staggered spring). */
@Composable
internal fun PremiumIntroScreen(fromOnboarding: Boolean) {
    val navigator = LocalNavigator.current
    val colors = KcalTheme.colors
    // First session: Premium → Language → intro. Otherwise simply close.
    val leave: () -> Unit = { if (fromOnboarding) navigator.navigate(Route.Language(onboarding = true)) else navigator.back() }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
        // Close appears after 3 s (gives the offer a moment) and springs in; it continues the flow.
        var showClose by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(CLOSE_DELAY_MS); showClose = true }
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterStart) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showClose,
                enter = fadeIn(KcalMotion.fadeTween()) + scaleIn(KcalMotion.bouncySpring(), initialScale = 0.4f),
            ) {
                KcalIconButton(KcalIcons.Close, stringResource(R.string.premium_close), leave, container = Color.Transparent, size = 44.dp, iconSize = 22.dp)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            KcalLogo(Modifier.kcalSharedElement(SharedKeys.PREMIUM_CROWN).size(width = 76.dp, height = 62.dp), animate = true)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.premium_unlock_title), style = KcalTheme.typography.headline, color = colors.textBrand)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.premium_intro_body), style = KcalTheme.typography.body.copy(fontSize = KcalTheme.typography.titleSmall.fontSize),
                color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(20.dp))
            benefits.forEachIndexed { index, b -> BenefitRow(b, index) }
        }
        KcalButton(stringResource(R.string.premium_unlock), { navigator.navigate(Route.Premium(fromOnboarding)) }, Modifier.fillMaxWidth().padding(top = 12.dp),
            trailingIcon = KcalIcons.ViewAll)
        LegalLinksRow()
    }
}

@Composable
private fun BenefitRow(benefit: Benefit, index: Int) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(180L + index * 70L)
        appear.animateTo(1f, KcalMotion.bouncySpring())
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                translationX = (1f - appear.value) * 40.dp.toPx()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconChip(benefit.icon, size = 38.dp, iconSize = 18.dp)
        Spacer(Modifier.width(18.dp))
        Column {
            Text(stringResource(benefit.title), style = KcalTheme.typography.titleSmall, color = KcalTheme.colors.textPrimary)
            Text(stringResource(benefit.sub), style = KcalTheme.typography.bodySmall, color = KcalTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun LegalLinksRow() {
    val uriHandler = LocalUriHandler.current
    val open: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } } // no browser → ignore, never crash
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        LegalLink(stringResource(R.string.premium_terms)) { open(LegalLinks.TERMS) }
        Text("·", style = KcalTheme.typography.label, color = KcalTheme.colors.textTertiary)
        LegalLink(stringResource(R.string.premium_privacy)) { open(LegalLinks.PRIVACY_POLICY) }
    }
}

@Composable
private fun LegalLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = KcalTheme.typography.label.copy(textDecoration = TextDecoration.Underline),
        color = KcalTheme.colors.textTertiary,
        modifier = Modifier
            .kcalClickable(shape = KcalShapes.xs, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

// ---------------------------------------------------------------- Plans

@Composable
internal fun PremiumRoute(fromOnboarding: Boolean) {
    val viewModel: PremiumViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) {
        when (it) {
            // In the first session continue the flow; otherwise the card flips to "Active ✓".
            PremiumEffect.Purchased -> if (fromOnboarding) navigator.navigate(Route.Language(onboarding = true))
            PremiumEffect.Close -> navigator.back()
        }
    }
    PremiumScreen(state, viewModel::onIntent)
}

@Composable
internal fun PremiumScreen(state: PremiumState, onIntent: (PremiumIntent) -> Unit) {
    val colors = KcalTheme.colors
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 12.dp)) {
        KcalTopBar(stringResource(R.string.premium_title), onBack = { onIntent(PremiumIntent.Back) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.premium_unlock_title), style = KcalTheme.typography.headline, color = colors.textPrimary)
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(KcalIcons.Heart), null, tint = colors.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.premium_plans_body), style = KcalTheme.typography.body.copy(fontSize = KcalTheme.typography.titleSmall.fontSize), color = colors.textSecondary)
            Spacer(Modifier.height(20.dp))
            SegmentedControl(
                persistentListOf(stringResource(R.string.premium_weekly), stringResource(R.string.premium_monthly)),
                state.selected.ordinal, { onIntent(PremiumIntent.Select(PremiumPlan.entries[it])) }, height = 52.dp,
            )
            Spacer(Modifier.height(24.dp))
            // The plan card morphs size and colour between Weekly (white) and Monthly (plum).
            AnimatedContent(
                targetState = state.selected,
                transitionSpec = {
                    (fadeIn(KcalMotion.fadeTween(KcalMotion.DurationMedium)) + scaleIn(KcalMotion.bouncySpring(), initialScale = 0.94f)) togetherWith
                        fadeOut(KcalMotion.fadeTween()) using SizeTransform(clip = false) { _, _ -> KcalMotion.sizeSpring }
                },
                label = "plan-card",
            ) { plan -> PlanCard(plan, active = state.active == plan, purchasing = state.purchasing) { onIntent(PremiumIntent.Purchase) } }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.premium_cancel_note), style = KcalTheme.typography.bodySmall, color = colors.textSecondary,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            LegalLinksRow()
        }
    }
}

@Composable
private fun PlanCard(plan: PremiumPlan, active: Boolean, purchasing: Boolean, onStart: () -> Unit) {
    val colors = KcalTheme.colors
    val monthly = plan == PremiumPlan.Monthly
    val container = if (monthly) colors.primary else colors.surface
    val content = if (monthly) colors.onPrimary else colors.textBrand
    val features = if (monthly) {
        listOf(R.string.premium_m1, R.string.premium_m2, R.string.premium_m3, R.string.premium_m4, R.string.premium_m5, R.string.premium_m6)
    } else {
        listOf(R.string.premium_w1, R.string.premium_w2, R.string.premium_w3, R.string.premium_w4, R.string.premium_w5, R.string.premium_w6, R.string.premium_w7, R.string.premium_w8)
    }
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .softShadow(colors.shadow.copy(alpha = if (monthly) 0.3f else 0.12f), cornerRadius = 24.dp, blur = 24.dp, offsetY = 10.dp)
                .background(container, KcalShapes.xl)
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            if (monthly) Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (monthly) R.string.premium_monthly_name else R.string.premium_weekly_name),
                    style = KcalTheme.typography.titleMedium.copy(fontSize = KcalTheme.typography.title.fontSize), color = content, modifier = Modifier.weight(1f))
                Text(stringResource(if (monthly) R.string.premium_monthly_price else R.string.premium_weekly_price),
                    style = KcalTheme.typography.titleSmall.copy(fontSize = KcalTheme.typography.titleMedium.fontSize), color = content)
            }
            Spacer(Modifier.height(12.dp))
            features.forEach {
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(KcalIcons.CheckThin), null, tint = content, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(it), style = KcalTheme.typography.body.copy(fontSize = KcalTheme.typography.titleSmall.fontSize),
                        color = if (monthly) content else colors.textPrimary.copy(alpha = 0.8f))
                }
            }
            Spacer(Modifier.height(16.dp))
            KcalButton(
                text = stringResource(if (active) R.string.premium_active else if (monthly) R.string.premium_start_monthly else R.string.premium_start_weekly),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                style = if (monthly) KcalButtonStyle.Inverse else KcalButtonStyle.Primary,
                enabled = !purchasing,
            )
        }
        if (monthly) {
            KcalBadge(
                stringResource(R.string.premium_best_value), uppercase = true,
                container = colors.accent, content = colors.textBrand.let { if (colors.isDark) colors.onPrimary else it },
                modifier = Modifier.padding(start = 24.dp).offset(y = (-12).dp),
            )
        }
    }
}

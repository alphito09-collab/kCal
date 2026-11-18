package com.alphito.kcal.feature.onboarding.presentation.ui

import com.alphito.kcal.core.designsystem.util.appLocale
import com.alphito.kcal.core.designsystem.util.valueWithUnit
import com.alphito.kcal.core.designsystem.util.kcalPerDayUnit
import com.alphito.kcal.core.designsystem.util.kcalUnit
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alphito.kcal.core.designsystem.component.KcalKeyboard
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.chart.SplitBar
import com.alphito.kcal.core.designsystem.component.AmbientBackground
import com.alphito.kcal.core.designsystem.component.AnimatedNumber
import com.alphito.kcal.core.designsystem.component.KcalBadge
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalButtonStyle
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalDialog
import com.alphito.kcal.core.designsystem.component.KcalDivider
import com.alphito.kcal.core.designsystem.component.KcalTextField
import com.alphito.kcal.core.designsystem.modifier.kcalClickable
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.onboarding.R
import com.alphito.kcal.feature.onboarding.presentation.components.staggerIn
import com.alphito.kcal.feature.onboarding.presentation.mvi.DeficitEffect
import com.alphito.kcal.feature.onboarding.presentation.mvi.DeficitIntent
import com.alphito.kcal.feature.onboarding.presentation.mvi.DeficitState
import com.alphito.kcal.feature.onboarding.presentation.mvi.DeficitViewModel
import com.alphito.kcal.feature.onboarding.presentation.mvi.EnergyEffect
import com.alphito.kcal.feature.onboarding.presentation.mvi.EnergyIntent
import com.alphito.kcal.feature.onboarding.presentation.mvi.EnergyState
import com.alphito.kcal.feature.onboarding.presentation.mvi.EnergyViewModel
import org.koin.compose.viewmodel.koinViewModel

// ---------------------------------------------------------------- Your daily energy needs

@Composable
internal fun EnergyRoute() {
    val viewModel: EnergyViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) {
        when (it) {
            EnergyEffect.OpenDeficitSetup -> navigator.navigate(Route.DeficitSetup)
            EnergyEffect.Finished -> navigator.resetTo(Route.Home)
        }
    }
    EnergyScreen(state) { viewModel.onIntent(EnergyIntent.Next) }
}

@Composable
internal fun EnergyScreen(state: EnergyState, onNext: () -> Unit) {
    val colors = KcalTheme.colors
    AmbientBackground(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.onb_energy_title), style = KcalTheme.typography.headlineSmall, color = colors.textBrand)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.onb_energy_subtitle), style = KcalTheme.typography.body, color = colors.textSecondary)
            Spacer(Modifier.height(20.dp))
            KcalCard(Modifier.staggerIn(2).fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp)) {
                Text(stringResource(R.string.onb_bmr), style = KcalTheme.typography.label, color = colors.textSecondary)
                Spacer(Modifier.height(8.dp))
                AnimatedNumber(state.bmr, KcalTheme.typography.display, suffix = " " + kcalPerDayUnit(), color = colors.textBrand)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.onb_bmr_name), style = KcalTheme.typography.label, color = colors.textSecondary)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.onb_bmr_body), style = KcalTheme.typography.body, color = colors.textSecondary)
            }
            Spacer(Modifier.height(16.dp))
            KcalCard(
                Modifier.staggerIn(3).fillMaxWidth(),
                border = BorderStroke(1.5.dp, colors.track),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 22.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.onb_tdee), style = KcalTheme.typography.titleSmall, color = colors.textSecondary, modifier = Modifier.weight(1f))
                    KcalBadge(stringResource(R.string.onb_daily_target), container = colors.primary, content = colors.onPrimary, uppercase = true)
                }
                Spacer(Modifier.height(10.dp))
                AnimatedNumber(state.tdee, KcalTheme.typography.display.copy(fontSize = KcalTheme.typography.hero.fontSize), suffix = " " + kcalPerDayUnit(), color = colors.textBrand)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.onb_tdee_name), style = KcalTheme.typography.titleSmall, color = colors.textSecondary)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.onb_tdee_body), style = KcalTheme.typography.body, color = colors.textSecondary)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.onb_energy_note), style = KcalTheme.typography.bodySmall, color = colors.textSecondary,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
        KcalButton(stringResource(R.string.onb_next), onNext, Modifier.staggerIn(5).fillMaxWidth().padding(vertical = 20.dp))
    }
    }
}

// ---------------------------------------------------------------- Calorie deficit setup

@Composable
internal fun DeficitRoute() {
    val viewModel: DeficitViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { if (it == DeficitEffect.Finished) navigator.resetTo(Route.Home) }
    DeficitScreen(state, viewModel::onIntent)
}

@Composable
internal fun DeficitScreen(state: DeficitState, onIntent: (DeficitIntent) -> Unit) {
    val colors = KcalTheme.colors
    AmbientBackground(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            KcalCard(Modifier.staggerIn(0).fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                Text(
                    stringResource(R.string.onb_deficit_question), style = KcalTheme.typography.title,
                    color = colors.textBrand, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KcalButton(
                        stringResource(R.string.onb_yes), { onIntent(DeficitIntent.AddSteps(true)) }, Modifier.weight(1f),
                        style = if (state.addSteps) KcalButtonStyle.Primary else KcalButtonStyle.Outline, height = 46.dp,
                    )
                    KcalButton(
                        stringResource(R.string.onb_no), { onIntent(DeficitIntent.AddSteps(false)) }, Modifier.weight(1f),
                        style = if (!state.addSteps) KcalButtonStyle.Primary else KcalButtonStyle.Outline, height = 46.dp,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.onb_divide_title), style = KcalTheme.typography.title, color = colors.textBrand)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.onb_divide_body), style = KcalTheme.typography.body, color = colors.textSecondary)
            Spacer(Modifier.height(16.dp))
            KcalCard(Modifier.staggerIn(2).fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                Text(stringResource(R.string.onb_daily_deficit), style = KcalTheme.typography.body, color = colors.textSecondary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AnimatedNumber(state.totalDeficit, KcalTheme.typography.hero, suffix = " " + kcalUnit(),
                    modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(16.dp))
                SplitBar(state.stepsFraction, Modifier.fillMaxWidth().height(12.dp))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(colors.primary, stringResource(R.string.onb_steps_legend), state.effectiveSteps)
                    Spacer(Modifier.width(20.dp))
                    LegendDot(colors.accent, stringResource(R.string.onb_diet_legend), state.dietKcal)
                }
            }
            AnimatedVisibility(state.addSteps, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                StepsAllocationCard(state, onIntent)
            }
        }
        KcalButton(
            stringResource(R.string.onb_get_started), { onIntent(DeficitIntent.Finish) },
            Modifier.fillMaxWidth().padding(vertical = 20.dp), enabled = !state.finishing,
        )
    }
    }
    if (state.editingExact) ExactValueDialog(state.stepsKcal, onIntent)
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String, kcal: Int) {
    Box(Modifier.size(8.dp).background(color, KcalShapes.pill))
    Spacer(Modifier.width(6.dp))
    Text("$label  " + valueWithUnit(KcalFormat.int(kcal, appLocale()), kcalUnit()), style = KcalTheme.typography.label, color = KcalTheme.colors.textBrand)
}

@Composable
private fun StepsAllocationCard(state: DeficitState, onIntent: (DeficitIntent) -> Unit) {
    val colors = KcalTheme.colors
    KcalCard(Modifier.fillMaxWidth().padding(top = 16.dp), contentPadding = PaddingValues(20.dp)) {
        Text(stringResource(R.string.onb_through_steps), style = KcalTheme.typography.titleMedium, color = colors.textBrand)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton("–", primary = false, enabled = state.stepsKcal > 0) { onIntent(DeficitIntent.Decrease) }
            Column(
                Modifier.weight(1f).kcalClickable(shape = KcalShapes.md) { onIntent(DeficitIntent.EditExact) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedNumber(state.stepsKcal, KcalTheme.typography.display, suffix = " " + kcalUnit())
                Text(stringResource(R.string.onb_tap_exact), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
            }
            StepperButton("+", primary = true, enabled = state.stepsKcal < state.totalDeficit) { onIntent(DeficitIntent.Increase) }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onb_steps_equivalent, KcalFormat.int(state.stepsEquivalent)),
            style = KcalTheme.typography.bodySmall, color = colors.textTertiary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        KcalDivider()
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().background(colors.surfaceMuted, KcalShapes.md).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.onb_through_diet), style = KcalTheme.typography.titleSmall, color = colors.textBrand)
                Text(stringResource(R.string.onb_calculated), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
            }
            AnimatedNumber(state.dietKcal, KcalTheme.typography.title.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), suffix = " " + kcalUnit())
        }
    }
}

@Composable
private fun StepperButton(symbol: String, primary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = KcalTheme.colors
    Box(
        Modifier
            .size(56.dp)
            .kcalClickable(shape = KcalShapes.pill, enabled = enabled, onClick = onClick)
            .background(if (primary) colors.primary else colors.track, KcalShapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = KcalTheme.typography.title, color = if (primary) colors.onPrimary else colors.textBrand)
    }
}

@Composable
private fun ExactValueDialog(current: Int, onIntent: (DeficitIntent) -> Unit) {
    var text by rememberSaveable { mutableStateOf(current.toString()) }
    KcalDialog(onDismiss = { onIntent(DeficitIntent.DismissExact) }) { controller ->
        Text(stringResource(R.string.onb_enter_steps_kcal), style = KcalTheme.typography.title, color = KcalTheme.colors.textBrand)
        Spacer(Modifier.height(16.dp))
        KcalTextField(text, { text = it.filter(Char::isDigit).take(4) }, keyboard = KcalKeyboard.Number, suffix = kcalUnit(), placeholder = stringResource(R.string.onb_exact_hint))
        Spacer(Modifier.height(16.dp))
        KcalButton(stringResource(R.string.onb_done), {
            val value = text.toIntOrNull() ?: current
            controller.dismiss { onIntent(DeficitIntent.ExactValue(value)) }
        }, Modifier.fillMaxWidth())
    }
}

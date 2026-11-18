package com.alphito.kcal.feature.onboarding.presentation.ui

import com.alphito.kcal.core.designsystem.util.regionLabel
import com.alphito.kcal.core.designsystem.util.KcalUnit
import com.alphito.kcal.core.designsystem.util.unitLabel
import com.alphito.kcal.core.designsystem.util.weightUnitLabel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.alphito.kcal.core.designsystem.component.UnitToggle
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alphito.kcal.core.designsystem.component.KcalKeyboard
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.component.ChipStyle
import com.alphito.kcal.core.designsystem.component.KcalBottomSheet
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalChip
import com.alphito.kcal.core.designsystem.component.KcalListRow
import com.alphito.kcal.core.designsystem.component.KcalTextField
import com.alphito.kcal.core.designsystem.component.SelectableOptionCard
import com.alphito.kcal.core.designsystem.modifier.kcalClickable
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.core.model.BudgetTier
import com.alphito.kcal.core.model.Gender
import com.alphito.kcal.core.model.Goal
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.onboarding.R
import com.alphito.kcal.feature.onboarding.presentation.components.OnboardingScaffold
import com.alphito.kcal.feature.onboarding.presentation.components.staggerIn
import com.alphito.kcal.feature.onboarding.presentation.mvi.AboutYouEffect
import com.alphito.kcal.feature.onboarding.presentation.mvi.AboutYouIntent
import com.alphito.kcal.feature.onboarding.presentation.mvi.AboutYouState
import com.alphito.kcal.feature.onboarding.presentation.mvi.AboutYouViewModel
import com.alphito.kcal.feature.onboarding.presentation.mvi.GoalEffect
import com.alphito.kcal.feature.onboarding.presentation.mvi.GoalIntent
import com.alphito.kcal.feature.onboarding.presentation.mvi.GoalState
import com.alphito.kcal.feature.onboarding.presentation.mvi.GoalViewModel
import com.alphito.kcal.feature.onboarding.presentation.mvi.HealthConditionsEffect
import com.alphito.kcal.feature.onboarding.presentation.mvi.HealthConditionsIntent
import com.alphito.kcal.feature.onboarding.presentation.mvi.HealthConditionsState
import com.alphito.kcal.feature.onboarding.presentation.mvi.HealthConditionsViewModel
import com.alphito.kcal.feature.onboarding.presentation.mvi.RegionBudgetEffect
import com.alphito.kcal.feature.onboarding.presentation.mvi.RegionBudgetIntent
import com.alphito.kcal.feature.onboarding.presentation.mvi.RegionBudgetState
import com.alphito.kcal.feature.onboarding.presentation.mvi.RegionBudgetViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// ---------------------------------------------------------------- Tell us about you (step 1)

@Composable
internal fun AboutYouRoute(edit: Boolean) {
    val viewModel: AboutYouViewModel = koinViewModel { parametersOf(edit) }
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) {
        when (it) {
            AboutYouEffect.Next -> navigator.navigate(Route.HealthConditions())
            AboutYouEffect.Close -> navigator.back()
        }
    }
    AboutYouScreen(state, viewModel::onIntent)
}

@Composable
internal fun AboutYouScreen(state: AboutYouState, onIntent: (AboutYouIntent) -> Unit) {
    OnboardingScaffold(
        title = stringResource(R.string.onb_about_title),
        subtitle = stringResource(R.string.onb_about_subtitle),
        step = if (state.isEdit) null else 1,
        onBack = { onIntent(AboutYouIntent.Back) },
        bottom = {
            ErrorHint(visible = state.showError, text = stringResource(if (state.name.isBlank()) R.string.onb_error_name else R.string.onb_error_range))
            KcalButton(
                stringResource(if (state.isEdit) R.string.onb_save else R.string.onb_continue),
                onClick = { onIntent(AboutYouIntent.Submit) }, modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        KcalTextField(state.name, { onIntent(AboutYouIntent.Name(it)) }, Modifier.staggerIn(3), label = stringResource(R.string.onb_name), placeholder = stringResource(R.string.onb_name_hint))
        Spacer(Modifier.height(16.dp))
        KcalTextField(state.age, { onIntent(AboutYouIntent.Age(it)) }, Modifier.staggerIn(4), label = stringResource(R.string.onb_age), keyboard = KcalKeyboard.Number, placeholder = stringResource(R.string.onb_age_hint), suffix = stringResource(R.string.onb_years))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.staggerIn(5), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Gender.Female to R.string.onb_female, Gender.Male to R.string.onb_male, Gender.Other to R.string.onb_other).forEach { (g, label) ->
                KcalChip(stringResource(label), selected = state.gender == g, onClick = { onIntent(AboutYouIntent.GenderSelected(g)) }, multiSelect = false)
            }
        }
        Spacer(Modifier.height(16.dp))
        HeightInput(state, onIntent, Modifier.staggerIn(6))
        Spacer(Modifier.height(16.dp))
        WeightLabel(state.weightInLb, { onIntent(AboutYouIntent.WeightUnit(it)) }, Modifier.staggerIn(7))
        KcalTextField(
            state.weight, { onIntent(AboutYouIntent.Weight(it)) }, Modifier.staggerIn(7),
            placeholder = stringResource(if (state.weightInLb) R.string.onb_weight_hint_lb else R.string.onb_weight_hint),
            suffix = weightUnitLabel(state.weightInLb),
            keyboard = KcalKeyboard.Decimal, imeDone = true, onImeAction = { onIntent(AboutYouIntent.Submit) },
        )
    }
}

/** Height in cm, or feet + inches – a compact unit toggle sits next to the label. */
@Composable
private fun HeightInput(state: AboutYouState, onIntent: (AboutYouIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = KcalTheme.colors
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.onb_height), style = KcalTheme.typography.body, color = colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            UnitToggle(
                stringResource(R.string.onb_unit_cm), stringResource(R.string.onb_unit_ft_in),
                state.heightInFeet, { onIntent(AboutYouIntent.HeightUnit(feet = it)) },
            )
        }
        Spacer(Modifier.height(8.dp))
        AnimatedContent(
            state.heightInFeet,
            transitionSpec = {
                val dir = if (targetState) 1 else -1
                (slideInHorizontally(KcalMotion.slideSpring) { it / 3 * dir } + fadeIn()) togetherWith
                    (slideOutHorizontally(KcalMotion.slideSpring) { -it / 3 * dir } + fadeOut())
            },
            label = "height-unit",
        ) { inFeet ->
            if (inFeet) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KcalTextField(state.feet, { onIntent(AboutYouIntent.Feet(it)) }, Modifier.weight(1f),
                        keyboard = KcalKeyboard.Number, placeholder = "5", suffix = unitLabel(KcalUnit.Ft))
                    KcalTextField(state.inches, { onIntent(AboutYouIntent.Inches(it)) }, Modifier.weight(1f),
                        keyboard = KcalKeyboard.Number, placeholder = "5", suffix = unitLabel(KcalUnit.In))
                }
            } else {
                KcalTextField(state.height, { onIntent(AboutYouIntent.Height(it)) }, keyboard = KcalKeyboard.Number,
                    placeholder = stringResource(R.string.onb_height_hint), suffix = unitLabel(KcalUnit.Cm))
            }
        }
    }
}

/** "Current weight" label with the kg | lb switch, mirroring the height row. */
@Composable
private fun WeightLabel(inLb: Boolean, onUnit: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.onb_weight), style = KcalTheme.typography.body, color = KcalTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        UnitToggle(stringResource(R.string.onb_unit_kg), stringResource(R.string.onb_unit_lb), inLb, onUnit)
    }
}

@Composable
private fun ErrorHint(visible: Boolean, text: String) {
    AnimatedVisibility(visible, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Text(
            text, style = KcalTheme.typography.bodySmall, color = KcalTheme.colors.textBrand,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        )
    }
}

// ---------------------------------------------------------------- Health conditions (step 2)

@Composable
internal fun HealthConditionsRoute(edit: Boolean) {
    val viewModel: HealthConditionsViewModel = koinViewModel { parametersOf(edit) }
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) {
        when (it) {
            HealthConditionsEffect.Next -> navigator.navigate(Route.GoalSelection)
            HealthConditionsEffect.Close -> navigator.back()
        }
    }
    HealthConditionsScreen(state, viewModel::onIntent)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HealthConditionsScreen(state: HealthConditionsState, onIntent: (HealthConditionsIntent) -> Unit) {
    OnboardingScaffold(
        title = stringResource(R.string.onb_health_title),
        subtitle = stringResource(R.string.onb_health_subtitle),
        step = if (state.isEdit) null else 2,
        onBack = { onIntent(HealthConditionsIntent.Back) },
        bottom = {
            KcalButton(
                stringResource(if (state.isEdit) R.string.onb_save else R.string.onb_continue),
                onClick = { onIntent(HealthConditionsIntent.Submit) }, modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Text(stringResource(R.string.onb_health_hint), style = KcalTheme.typography.body, color = KcalTheme.colors.textSecondary, modifier = Modifier.staggerIn(3))
        Spacer(Modifier.height(16.dp))
        FlowRow(Modifier.staggerIn(4), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(
                HealthCondition.Diabetes to R.string.onb_diabetes,
                HealthCondition.Ibs to R.string.onb_ibs,
                HealthCondition.LactoseIntolerant to R.string.onb_lactose,
            ).forEach { (condition, label) ->
                KcalChip(stringResource(label), condition in state.selected, { onIntent(HealthConditionsIntent.Toggle(condition)) })
            }
            KcalChip(stringResource(R.string.onb_none), state.noneSelected, { onIntent(HealthConditionsIntent.ToggleNone) })
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.onb_disclaimer),
            style = KcalTheme.typography.body, color = KcalTheme.colors.textSecondary,
            modifier = Modifier.staggerIn(5).fillMaxWidth().background(KcalTheme.colors.accentContainer, KcalShapes.md).padding(16.dp),
        )
    }
}

// ---------------------------------------------------------------- Goal (step 3)

@Composable
internal fun GoalRoute() {
    val viewModel: GoalViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { if (it == GoalEffect.Next) navigator.navigate(Route.RegionBudget()) }
    GoalScreen(state, viewModel::onIntent)
}

@Composable
internal fun GoalScreen(state: GoalState, onIntent: (GoalIntent) -> Unit) {
    OnboardingScaffold(
        title = stringResource(R.string.onb_goal_title),
        subtitle = stringResource(R.string.onb_goal_subtitle),
        step = 3,
        onBack = null,
        bottom = { KcalButton(stringResource(R.string.onb_continue), { onIntent(GoalIntent.Submit) }, Modifier.fillMaxWidth()) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                Triple(Goal.LoseWeight, R.string.onb_goal_lose, R.string.onb_goal_lose_sub),
                Triple(Goal.GainWeight, R.string.onb_goal_gain, R.string.onb_goal_gain_sub),
                Triple(Goal.MaintainWeight, R.string.onb_goal_maintain, R.string.onb_goal_maintain_sub),
            ).forEachIndexed { i, (goal, title, sub) ->
                SelectableOptionCard(stringResource(title), stringResource(sub), state.goal == goal, { onIntent(GoalIntent.Select(goal)) }, Modifier.staggerIn(3 + i))
            }
        }
        AnimatedVisibility(
            visible = state.needsTarget,
            enter = fadeIn(KcalMotion.fadeTween()) + expandVertically(KcalMotion.sizeSpring),
            exit = fadeOut(KcalMotion.fadeTween()) + shrinkVertically(KcalMotion.sizeSpring),
        ) {
            KcalTextField(
                state.targetWeight, { onIntent(GoalIntent.Target(it)) },
                label = stringResource(R.string.onb_target_weight), keyboard = KcalKeyboard.Decimal,
                placeholder = stringResource(if (state.weightInLb) R.string.onb_target_hint_lb else R.string.onb_target_hint),
                suffix = weightUnitLabel(state.weightInLb),
                imeDone = true, modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- Region & budget (step 4)

@Composable
internal fun RegionBudgetRoute(edit: Boolean) {
    val viewModel: RegionBudgetViewModel = koinViewModel { parametersOf(edit) }
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) {
        when (it) {
            RegionBudgetEffect.Next -> navigator.navigate(Route.EnergyNeeds)
            RegionBudgetEffect.Close -> navigator.back()
        }
    }
    RegionBudgetScreen(state, viewModel::onIntent)
}

@Composable
internal fun RegionBudgetScreen(state: RegionBudgetState, onIntent: (RegionBudgetIntent) -> Unit) {
    val colors = KcalTheme.colors
    OnboardingScaffold(
        title = stringResource(R.string.onb_region_title),
        subtitle = stringResource(R.string.onb_region_subtitle),
        step = if (state.isEdit) null else 4,
        onBack = { onIntent(RegionBudgetIntent.Back) },
        bottom = {
            KcalButton(
                stringResource(if (state.isEdit) R.string.onb_save else R.string.onb_see_plan),
                { onIntent(RegionBudgetIntent.Submit) }, Modifier.fillMaxWidth(),
            )
        },
    ) {
        Row(
            Modifier
                .staggerIn(3)
                .fillMaxWidth()
                .kcalClickable(shape = KcalShapes.md) { onIntent(RegionBudgetIntent.OpenRegionPicker) }
                .background(colors.surface, KcalShapes.md)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.onb_region_label), style = KcalTheme.typography.body, color = colors.textSecondary, modifier = Modifier.weight(1f))
            Text(regionLabel(state.region), style = KcalTheme.typography.labelBold, color = colors.textPrimary)
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.onb_budget_label), style = KcalTheme.typography.body, color = colors.textSecondary)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BudgetTier.entries.forEachIndexed { i, tier ->
                val title = stringResource(
                    when (tier) {
                        BudgetTier.Low -> R.string.onb_budget_low
                        BudgetTier.Medium -> R.string.onb_budget_medium
                        BudgetTier.High -> R.string.onb_budget_high
                    },
                )
                val range = tier.maxPkrPerWeek?.let {
                    stringResource(R.string.onb_budget_range, KcalFormat.int(tier.minPkrPerWeek), KcalFormat.int(it))
                } ?: stringResource(R.string.onb_budget_open, KcalFormat.int(tier.minPkrPerWeek))
                SelectableOptionCard(title, range, state.budget == tier, { onIntent(RegionBudgetIntent.BudgetSelected(tier)) }, Modifier.staggerIn(4 + i))
            }
        }
    }
    if (state.showRegionPicker) {
        KcalBottomSheet(onDismiss = { onIntent(RegionBudgetIntent.DismissRegionPicker) }) { _ ->
            Text(stringResource(R.string.onb_choose_region), style = KcalTheme.typography.title, color = colors.textBrand)
            Spacer(Modifier.height(12.dp))
            state.regions.forEach { region ->
                KcalListRow(
                    title = regionLabel(region),
                    onClick = { onIntent(RegionBudgetIntent.RegionSelected(region)) },
                    modifier = Modifier.fillMaxWidth(),
                    titleColor = if (region == state.region) colors.textBrand else colors.textPrimary,
                    trailing = { com.alphito.kcal.core.designsystem.component.RadioDot(region == state.region) },
                )
            }
        }
    }
}

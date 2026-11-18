package com.alphito.kcal.feature.dashboard.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.component.IconChip
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalDialog
import com.alphito.kcal.core.designsystem.component.KcalTextButton
import com.alphito.kcal.core.designsystem.component.SectionHeader
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.motion.kcalSharedElement
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.core.navigation.SharedKeys
import com.alphito.kcal.feature.dashboard.R
import com.alphito.kcal.feature.dashboard.presentation.components.ActionCard
import com.alphito.kcal.feature.dashboard.presentation.components.CalorieTrackingCard
import com.alphito.kcal.feature.dashboard.presentation.components.NextMealCard
import com.alphito.kcal.feature.dashboard.presentation.components.ViewAllLink
import com.alphito.kcal.feature.dashboard.presentation.mvi.DashboardEffect
import com.alphito.kcal.feature.dashboard.presentation.mvi.DashboardIntent
import com.alphito.kcal.feature.dashboard.presentation.mvi.DashboardState
import com.alphito.kcal.feature.dashboard.presentation.mvi.DashboardViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DashboardRoute() {
    val viewModel: DashboardViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) {
        when (it) {
            is DashboardEffect.Navigate -> navigator.navigate(it.route)
        }
    }
    DashboardScreen(state, viewModel::onIntent)
}

@Composable
internal fun DashboardScreen(state: DashboardState, onIntent: (DashboardIntent) -> Unit) {
    val colors = KcalTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.dash_welcome, state.name), style = KcalTheme.typography.welcome, color = colors.textBrand)
                Text(stringResource(R.string.dash_subtitle), style = KcalTheme.typography.body, color = colors.textSecondary)
            }
            Image(
                painterResource(KcalIcons.LogoSmall), contentDescription = null,
                modifier = Modifier.kcalSharedElement(SharedKeys.LOGO).size(width = 32.dp, height = 29.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        CalorieTrackingCard(state.eatenKcal, state.targetKcal, state.ringProgress, state.stepsKcal, state.streakDays)
        Spacer(Modifier.height(22.dp))
        SectionHeader(stringResource(R.string.dash_todays_meal)) {
            ViewAllLink { onIntent(DashboardIntent.Open(Route.MealChecklist)) }
        }
        Spacer(Modifier.height(10.dp))
        NextMealCard(
            slot = state.nextMeal,
            onCheck = { onIntent(DashboardIntent.CheckMeal(it)) },
            onOpen = { onIntent(DashboardIntent.Open(Route.MealChecklist)) },
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(KcalIcons.Water, stringResource(R.string.dash_water), "water", { onIntent(DashboardIntent.Open(Route.Water)) }, Modifier.weight(1f))
            ActionCard(KcalIcons.Steps, stringResource(com.alphito.kcal.feature.dashboard.R.string.dash_steps), "steps", { onIntent(DashboardIntent.Open(Route.Steps)) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(KcalIcons.Weight, stringResource(R.string.dash_weight), "weight", { onIntent(DashboardIntent.Open(Route.WeightCheckIn)) }, Modifier.weight(1f))
            ActionCard(KcalIcons.Health, stringResource(R.string.dash_health), "health", { onIntent(DashboardIntent.Open(Route.HealthLog)) }, Modifier.weight(1f))
        }
        com.alphito.kcal.core.designsystem.component.BottomBarSpacer(extra = 24.dp)
    }

    if (state.showFreeTrial) FreeTrialDialog(onIntent)
}

@Composable
private fun FreeTrialDialog(onIntent: (DashboardIntent) -> Unit) {
    val colors = KcalTheme.colors
    KcalDialog(onDismiss = { onIntent(DashboardIntent.DismissTrial) }) { controller ->
        IconChip(KcalIcons.Crown, size = 64.dp, iconSize = 28.dp, modifier = Modifier.kcalSharedElement(SharedKeys.PREMIUM_CROWN))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.dash_trial_title), style = KcalTheme.typography.title, color = colors.textBrand, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.dash_trial_body), style = KcalTheme.typography.body, color = colors.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(R.string.dash_trial_1, R.string.dash_trial_2, R.string.dash_trial_3, R.string.dash_trial_4).forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(KcalIcons.CheckThin), null, tint = colors.primary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(it), style = KcalTheme.typography.body, color = colors.textPrimary)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        KcalButton(stringResource(R.string.dash_get_premium), { controller.dismiss { onIntent(DashboardIntent.GetPremium) } }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        KcalTextButton(stringResource(R.string.dash_maybe_later), { controller.dismiss() }, underline = true)
    }
}

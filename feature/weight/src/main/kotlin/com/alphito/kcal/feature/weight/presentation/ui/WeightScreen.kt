package com.alphito.kcal.feature.weight.presentation.ui

import com.alphito.kcal.core.designsystem.util.weightChangeText
import com.alphito.kcal.core.designsystem.util.weightText
import com.alphito.kcal.core.designsystem.util.weightUnitLabel
import com.alphito.kcal.core.designsystem.util.KcalUnit
import com.alphito.kcal.core.designsystem.util.unitLabel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.component.BigNumberField
import com.alphito.kcal.core.designsystem.component.KcalBadge
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalDetailScaffold
import com.alphito.kcal.core.designsystem.component.KcalDivider
import com.alphito.kcal.core.designsystem.component.UnitToggle
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.SharedKeys
import com.alphito.kcal.feature.weight.R
import com.alphito.kcal.feature.weight.presentation.mvi.WeightEffect
import com.alphito.kcal.feature.weight.presentation.mvi.WeightIntent
import com.alphito.kcal.feature.weight.presentation.mvi.WeightState
import com.alphito.kcal.feature.weight.presentation.mvi.WeightViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun WeightRoute() {
    val viewModel: WeightViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { if (it == WeightEffect.Close) navigator.back() }
    WeightScreen(state, viewModel::onIntent)
}

@Composable
internal fun WeightScreen(state: WeightState, onIntent: (WeightIntent) -> Unit) {
    val colors = KcalTheme.colors
    KcalDetailScaffold(
        title = stringResource(R.string.weight_title),
        onBack = { onIntent(WeightIntent.Back) },
        sharedBoundsKey = SharedKeys.actionCard("weight"),
    ) {
        KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.weight_this_week), style = KcalTheme.typography.titleMedium, color = colors.textBrand, modifier = Modifier.weight(1f))
                UnitToggle(unitLabel(KcalUnit.Kg), unitLabel(KcalUnit.Lb), state.weightInLb, { onIntent(WeightIntent.ChangeUnit(it)) })
            }
            Spacer(Modifier.height(16.dp))
            BigNumberField(state.input, { onIntent(WeightIntent.Input(it)) }, unit = weightUnitLabel(state.weightInLb), placeholder = "0.0")
            Spacer(Modifier.height(16.dp))
            AnimatedContent(
                state.justSaved,
                transitionSpec = { (fadeIn() + scaleIn(KcalMotion.bouncySpring(), initialScale = 0.9f)) togetherWith fadeOut() },
                label = "save",
            ) { saved ->
                KcalButton(stringResource(if (saved) R.string.weight_saved else R.string.weight_save), { onIntent(WeightIntent.Save) }, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.weight_hint), style = KcalTheme.typography.bodySmall, color = colors.textSecondary,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(16.dp))
        KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
            Text(stringResource(R.string.weight_progress), style = KcalTheme.typography.titleMedium, color = colors.textBrand)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Metric(stringResource(R.string.weight_current), state.current?.let { weightText(it, state.weightInLb) } ?: "–", brand = true)
                Metric(stringResource(R.string.weight_previous), state.previous?.let { weightText(it, state.weightInLb) } ?: "–", brand = false)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.weight_change), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    KcalBadge(state.change?.let { weightChangeText(it, state.weightInLb) } ?: "–")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
            Text(stringResource(R.string.weight_history), style = KcalTheme.typography.titleMedium, color = colors.textBrand)
            Spacer(Modifier.height(8.dp))
            if (state.recent.isEmpty()) {
                Text(stringResource(R.string.weight_empty), style = KcalTheme.typography.body, color = colors.textSecondary)
            }
            state.recent.forEachIndexed { i, entry ->
                if (i > 0) KcalDivider()
                val isLast = i == state.recent.lastIndex
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(
                        stringResource(if (isLast) R.string.weight_week_current else R.string.weight_week, i + 1),
                        style = if (isLast) KcalTheme.typography.titleSmall else KcalTheme.typography.body,
                        color = if (isLast) colors.textBrand else colors.textPrimary, modifier = Modifier.weight(1f),
                    )
                    Text(weightText(entry.weightKg, state.weightInLb), style = KcalTheme.typography.titleSmall, color = colors.textBrand)
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, brand: Boolean) {
    Column {
        Text(label, style = KcalTheme.typography.bodySmall, color = KcalTheme.colors.textSecondary)
        Spacer(Modifier.height(6.dp))
        Text(value, style = KcalTheme.typography.title.copy(fontWeight = FontWeight.Bold),
            color = if (brand) KcalTheme.colors.textBrand else KcalTheme.colors.textPrimary)
    }
}

package com.alphito.kcal.feature.water.presentation.ui

import com.alphito.kcal.core.designsystem.util.appLocale
import com.alphito.kcal.core.designsystem.util.valueWithUnit
import com.alphito.kcal.core.designsystem.util.KcalUnit
import com.alphito.kcal.core.designsystem.util.unitLabel
import com.alphito.kcal.core.designsystem.util.fractionText
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.alphito.kcal.core.designsystem.component.KcalKeyboard
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.chart.ProgressRing
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalButtonStyle
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalDetailScaffold
import com.alphito.kcal.core.designsystem.component.KcalTextField
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.modifier.kcalClickable
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.SharedKeys
import com.alphito.kcal.feature.water.R
import com.alphito.kcal.feature.water.presentation.mvi.WaterEffect
import com.alphito.kcal.feature.water.presentation.mvi.WaterIntent
import com.alphito.kcal.feature.water.presentation.mvi.WaterState
import com.alphito.kcal.feature.water.presentation.mvi.WaterViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun WaterRoute() {
    val viewModel: WaterViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { if (it == WaterEffect.Close) navigator.back() }
    WaterScreen(state, viewModel::onIntent)
}

private val Amounts = listOf(250, 500, 750)

@Composable
internal fun WaterScreen(state: WaterState, onIntent: (WaterIntent) -> Unit) {
    val colors = KcalTheme.colors
    val day = state.day
    KcalDetailScaffold(
        title = stringResource(R.string.water_title),
        onBack = { onIntent(WaterIntent.Back) },
        sharedBoundsKey = SharedKeys.actionCard("water"),
    ) {
        KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
            Text(stringResource(R.string.water_today), style = KcalTheme.typography.titleSmall, color = colors.textBrand)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(day.progress, Modifier.size(84.dp), strokeWidth = 8.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(KcalIcons.Drop), null, tint = colors.primary, modifier = Modifier.size(12.dp))
                        Text(fractionText(day.glasses, day.goalGlasses), style = KcalTheme.typography.titleSmall, color = colors.textBrand)
                    }
                }
                Spacer(Modifier.width(22.dp))
                Column {
                    Text(stringResource(R.string.water_glasses, day.glasses, day.goalGlasses), style = KcalTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.textBrand)
                    Text(stringResource(R.string.water_ml, KcalFormat.int(day.consumedMl), KcalFormat.int(day.goalMl)), style = KcalTheme.typography.body, color = colors.textSecondary)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
            Text(stringResource(R.string.water_add_title), style = KcalTheme.typography.titleSmall, color = colors.textBrand)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Amounts.forEach { ml ->
                    val selected = ml == state.selectedAmount
                    val bg by animateColorAsState(if (selected) colors.accent.copy(alpha = 0.55f) else colors.track, KcalMotion.fadeTween(), label = "amt")
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .kcalClickable(shape = KcalShapes.md) { onIntent(WaterIntent.SelectAmount(ml)) }
                            .background(bg, KcalShapes.md),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.water_amount, ml), style = KcalTheme.typography.labelBold, color = colors.textBrand) }
                }
            }
            Spacer(Modifier.height(12.dp))
            KcalButton(stringResource(R.string.water_add), { onIntent(WaterIntent.Add) }, Modifier.fillMaxWidth(), height = 48.dp)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.water_glass_hint), style = KcalTheme.typography.bodySmall, color = colors.textSecondary,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(14.dp))
        KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
            Text(stringResource(R.string.water_goal), style = KcalTheme.typography.body, color = colors.textSecondary)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.editingGoal) {
                    KcalTextField(
                        state.goalInput, { onIntent(WaterIntent.GoalInput(it)) }, Modifier.weight(1f),
                        keyboard = KcalKeyboard.Number, imeDone = true, suffix = unitLabel(KcalUnit.Ml), placeholder = stringResource(R.string.water_goal_hint),
                        onImeAction = { onIntent(WaterIntent.ToggleEditGoal) },
                        textStyle = KcalTheme.typography.title,
                    )
                } else {
                    Box(
                        Modifier.weight(1f).height(51.dp).background(colors.surface, KcalShapes.md)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) { Text(valueWithUnit(KcalFormat.int(day.goalMl, appLocale()), unitLabel(KcalUnit.Ml)), style = KcalTheme.typography.title, color = colors.textBrand) }
                }
                Spacer(Modifier.width(10.dp))
                KcalButton(
                    stringResource(if (state.editingGoal) R.string.water_save_goal else R.string.water_edit_goal),
                    { onIntent(WaterIntent.ToggleEditGoal) }, style = KcalButtonStyle.Tonal, height = 46.dp,
                )
            }
        }
    }
}

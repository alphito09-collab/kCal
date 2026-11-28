package com.alphito.kcal.feature.progress.presentation.ui

import com.alphito.kcal.core.designsystem.util.weightUnitLabel
import com.alphito.kcal.core.designsystem.util.valueWithUnit
import com.alphito.kcal.core.designsystem.util.weightChangeText
import com.alphito.kcal.core.designsystem.util.localizedDate
import com.alphito.kcal.core.designsystem.util.rangeText
import com.alphito.kcal.core.designsystem.util.appLocale
import com.alphito.kcal.core.designsystem.util.kcalPerDayUnit
import com.alphito.kcal.core.designsystem.util.kcalUnit
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.alphito.kcal.core.designsystem.component.AnimatedNumber
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.designsystem.component.KcalBadge
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.SegmentedControl
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.core.model.WeightUnits
import com.alphito.kcal.feature.progress.R
import com.alphito.kcal.feature.progress.domain.ProgressPeriod
import com.alphito.kcal.feature.progress.presentation.components.MetricTile
import com.alphito.kcal.feature.progress.presentation.components.StatLine
import com.alphito.kcal.feature.progress.presentation.components.WeightTrendCard
import com.alphito.kcal.feature.progress.domain.ProgressCalculator
import com.alphito.kcal.feature.progress.presentation.mvi.ProgressIntent
import com.alphito.kcal.feature.progress.presentation.mvi.ProgressState
import com.alphito.kcal.feature.progress.presentation.mvi.ProgressViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

@Composable
internal fun ProgressRoute() {
    val viewModel: ProgressViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgressScreen(state, viewModel::onIntent)
}

@Composable
internal fun ProgressScreen(state: ProgressState, onIntent: (ProgressIntent) -> Unit) {
    val colors = KcalTheme.colors
    val locale = appLocale()
    val weekLabel = stringResource(R.string.progress_week_label)
    val slots = state.slots
    // Upcoming periods keep their label (Wk 3 / Dec) but have no point.
    val labels = remember(slots, state.period, state.startDate, locale) {
        slots.map { index ->
            if (state.period == ProgressPeriod.Weekly) String.format(locale, weekLabel, index + 1)
            else ProgressCalculator.bounds(state.period, state.startDate, index).first.month.getDisplayName(DateTextStyle.SHORT, locale)
        }.toImmutableList()
    }
    val values = remember(slots, state.stats, state.weightInLb) {
        slots.map { index -> state.stats.getOrNull(index)?.weightKg?.let { WeightUnits.display(it, state.weightInLb).toFloat() } }.toImmutableList()
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.progress_title), style = KcalTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.textBrand)
        Spacer(Modifier.height(14.dp))
        SegmentedControl(
            persistentListOf(stringResource(R.string.progress_weekly), stringResource(R.string.progress_monthly)),
            state.period.ordinal, { onIntent(ProgressIntent.SelectPeriod(ProgressPeriod.entries[it])) }, height = 52.dp,
        )
        Spacer(Modifier.height(14.dp))
        if (state.loaded) {
            WeightTrendCard(
                title = stringResource(R.string.progress_weight_trend),
                values = values,
                labels = labels,
                selectedSlot = slots.indexOf(state.selected),
                hasData = state.hasWeighIns,
                emptyMessage = stringResource(R.string.progress_no_weight),
                onSelect = { slot -> onIntent(ProgressIntent.SelectBucket(slots[slot])) },
                canPageBack = state.canPageBack,
                canPageForward = state.canPageForward,
                onPageBack = { onIntent(ProgressIntent.PageBack) },
                onPageForward = { onIntent(ProgressIntent.PageForward) },
                description = stringResource(R.string.progress_weight_trend),
            )
        }
        Spacer(Modifier.height(14.dp))
        val current = state.current
        PeriodSummaryCard(state)
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.progress_key_metrics), style = KcalTheme.typography.titleMedium, color = colors.textBrand)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(KcalIcons.Flame, stringResource(R.string.progress_burned), Modifier.weight(1f)) {
                AnimatedNumber(current?.burnedKcal ?: 0, MetricValueStyle)
            }
            MetricTile(KcalIcons.Utensils, stringResource(R.string.progress_intake), Modifier.weight(1f)) {
                AnimatedNumber(current?.intakeKcal ?: 0, MetricValueStyle)
            }
            MetricTile(KcalIcons.Weight, stringResource(R.string.progress_weight), Modifier.weight(1f)) {
                AnimatedWeight(current?.weightKg, state.weightInLb, MetricValueStyle)
            }
        }
        com.alphito.kcal.core.designsystem.component.BottomBarSpacer(extra = 24.dp)
    }
}

private val MetricValueStyle: TextStyle
    @Composable get() = KcalTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)

/**
 * The card stays put while the user taps through the chart; only its content animates:
 *  - title rolls vertically in the direction of travel (later period → from below),
 *  - change badge pops with a spring,
 *  - kcal values count to their new numbers, weight tweens to its new value.
 */
@Composable
private fun PeriodSummaryCard(state: ProgressState) {
    val colors = KcalTheme.colors
    val locale = appLocale()
    val s = state.current ?: return
    val index = state.selected
    val period = state.period
    val title = if (period == ProgressPeriod.Weekly) {
        // A week that's only one day old shows that day, not "Nov 25 – Nov 25".
        val dates = if (s.start == s.end) localizedDate(s.start, "MMMd") else rangeText(localizedDate(s.start, "MMMd"), localizedDate(s.end, "MMMd"))
        stringResource(R.string.progress_week_title, index + 1, dates)
    } else localizedDate(s.start, "MMMMyyyy")

    KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = TitleKey(title, index),
                transitionSpec = {
                    val dir = if (targetState.index >= initialState.index) 1 else -1
                    (slideInVertically(KcalMotion.slideSpring) { it * dir } + fadeIn(KcalMotion.fadeTween(KcalMotion.DurationMedium))) togetherWith
                        (slideOutVertically(KcalMotion.slideSpring) { -it * dir } + fadeOut(KcalMotion.fadeTween()))
                },
                modifier = Modifier.weight(1f),
                label = "period-title",
            ) { key ->
                Text(key.title, style = KcalTheme.typography.title, color = colors.textBrand)
            }
            AnimatedContent(
                targetState = s.weightChange?.let { weightChangeText(it, state.weightInLb) },
                transitionSpec = {
                    (fadeIn() + scaleIn(KcalMotion.bouncySpring(), initialScale = 0.6f)) togetherWith
                        (fadeOut(KcalMotion.fadeTween(120)) + scaleOut(targetScale = 0.8f))
                },
                label = "period-badge",
            ) { badge -> if (badge != null) KcalBadge(badge) }
        }
        Spacer(Modifier.height(12.dp))
        StatLine(stringResource(R.string.progress_burned)) { AnimatedNumber(s.burnedKcal, StatValueStyle, suffix = " " + kcalUnit()) }
        StatLine(stringResource(R.string.progress_intake)) { AnimatedNumber(s.intakeKcal, StatValueStyle, suffix = " " + kcalUnit()) }
        StatLine(stringResource(R.string.progress_weight)) { AnimatedWeight(s.weightKg, state.weightInLb, StatValueStyle) }
    }
}

private data class TitleKey(val title: String, val index: Int)

private val StatValueStyle: TextStyle
    @Composable get() = KcalTheme.typography.title.copy(fontWeight = FontWeight.Bold)

/** Weight tweens between values (e.g. 68.4 → 69.0) in the user's unit instead of jumping. */
@Composable
private fun AnimatedWeight(kg: Double?, inLb: Boolean, style: TextStyle) {
    if (kg == null) {
        Text("–", style = style, color = KcalTheme.colors.textBrand); return
    }
    val target = WeightUnits.display(kg, inLb).toFloat()
    val animated by animateFloatAsState(target, tween(KcalMotion.DurationChart, easing = KcalMotion.EmphasizedDecelerate), label = "weight")
    Text(valueWithUnit(KcalFormat.decimal1(animated.toDouble(), appLocale()), weightUnitLabel(inLb)), style = style, color = KcalTheme.colors.textBrand)
}

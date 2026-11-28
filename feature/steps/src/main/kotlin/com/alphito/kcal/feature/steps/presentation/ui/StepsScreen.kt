package com.alphito.kcal.feature.steps.presentation.ui

import com.alphito.kcal.core.designsystem.util.localizedDate
import com.alphito.kcal.core.designsystem.util.appLocale
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.chart.BarEntry
import com.alphito.kcal.core.designsystem.chart.KcalBarChart
import com.alphito.kcal.core.designsystem.chart.KcalLinearProgress
import com.alphito.kcal.core.designsystem.component.AnimatedNumber
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalDetailScaffold
import com.alphito.kcal.core.designsystem.component.SegmentedControl
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.core.model.EnergyCalculator
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.SharedKeys
import com.alphito.kcal.feature.steps.R
import com.alphito.kcal.feature.steps.domain.HistoryRange
import com.alphito.kcal.feature.steps.domain.StepsHistory
import com.alphito.kcal.feature.steps.domain.TrackingStatus
import com.alphito.kcal.feature.steps.presentation.components.StartStopButton
import com.alphito.kcal.feature.steps.presentation.mvi.StepsEffect
import com.alphito.kcal.feature.steps.presentation.mvi.StepsIntent
import com.alphito.kcal.feature.steps.presentation.mvi.StepsState
import com.alphito.kcal.feature.steps.presentation.mvi.StepsViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun StepsRoute() {
    val viewModel: StepsViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissions = remember {
        buildList {
            // A runtime permission only from Android 10; on Android 9 step sensors need none.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        viewModel.onIntent(
            StepsIntent.PermissionsResult(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || result[Manifest.permission.ACTIVITY_RECOGNITION] == true),
        )
    }
    ObserveEffects(viewModel.effects) {
        when (it) {
            StepsEffect.RequestPermissions -> launcher.launch(permissions)
            StepsEffect.Close -> navigator.back()
        }
    }
    StepsScreen(state, viewModel::onIntent)
}

@Composable
internal fun StepsScreen(state: StepsState, onIntent: (StepsIntent) -> Unit) {
    val colors = KcalTheme.colors
    KcalDetailScaffold(
        title = stringResource(R.string.steps_title),
        onBack = { onIntent(StepsIntent.Back) },
        sharedBoundsKey = SharedKeys.actionCard("steps"),
    ) {
        KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.steps_today), style = KcalTheme.typography.titleSmall, color = colors.textBrand)
                    Spacer(Modifier.height(4.dp))
                    AnimatedNumber(state.todaySteps, KcalTheme.typography.hero)
                    Text(
                        stringResource(R.string.steps_kcal, KcalFormat.int(state.todayKcal)),
                        style = KcalTheme.typography.title.copy(fontWeight = FontWeight.SemiBold), color = colors.textTertiary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.steps_goal, KcalFormat.int(state.goal)), style = KcalTheme.typography.body, color = colors.textSecondary)
                }
                StartStopButton(
                    tracking = state.status == TrackingStatus.Tracking,
                    startLabel = stringResource(R.string.steps_start),
                    stopLabel = stringResource(R.string.steps_stop),
                    enabled = state.status != TrackingStatus.NoSensor,
                    onClick = { onIntent(StepsIntent.ToggleTracking) },
                )
            }
            Spacer(Modifier.height(18.dp))
            KcalLinearProgress(state.progress, Modifier.fillMaxWidth().height(10.dp))
            Spacer(Modifier.height(18.dp))
            val hint = when {
                state.status == TrackingStatus.NoSensor -> R.string.steps_hint_no_sensor
                state.status == TrackingStatus.Tracking -> R.string.steps_hint_active
                state.permissionDenied -> R.string.steps_hint_denied
                else -> R.string.steps_hint_idle
            }
            AnimatedContent(hint, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "hint") {
                Text(stringResource(it), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        Spacer(Modifier.height(20.dp))
        HistoryCard(state, onIntent)
    }
}

@Composable
private fun HistoryCard(state: StepsState, onIntent: (StepsIntent) -> Unit) {
    val colors = KcalTheme.colors
    val locale = appLocale()
    val weekLabel = stringResource(R.string.steps_week_label)
    val buckets = remember(state.range, state.history, state.today, locale) {
        StepsHistory.buckets(state.range, state.history, state.today, locale) { String.format(locale, weekLabel, it) }
    }
    val entries = remember(buckets) { buckets.map { BarEntry(it.label, it.steps.toFloat(), "${it.label}: ${it.steps}") }.toImmutableList() }
    val selected = if (state.selectedBar in buckets.indices) state.selectedBar else buckets.lastIndex
    val ranges = remember { HistoryRange.entries }
    val labels = persistentListOf(stringResource(R.string.steps_months), stringResource(R.string.steps_weeks), stringResource(R.string.steps_days))

    KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
        Text(stringResource(R.string.steps_history), style = KcalTheme.typography.title, color = colors.textBrand)
        Spacer(Modifier.height(16.dp))
        SegmentedControl(labels, ranges.indexOf(state.range), { onIntent(StepsIntent.SelectRange(ranges[it])) })
        Spacer(Modifier.height(18.dp))
        KcalBarChart(entries, selected, { onIntent(StepsIntent.SelectBar(it)) }, maxBarWidth = if (state.range == HistoryRange.Days) 16.dp else 26.dp)
        Spacer(Modifier.height(16.dp))
        val bucket = buckets.getOrNull(selected)
        if (bucket != null) {
            val summary = when (state.range) {
                HistoryRange.Months -> stringResource(R.string.steps_month_summary, bucket.start.month.getDisplayName(TextStyle.FULL, locale), KcalFormat.int(bucket.steps))
                HistoryRange.Weeks -> stringResource(R.string.steps_week_summary, selected + 1, KcalFormat.int(bucket.steps))
                HistoryRange.Days -> stringResource(
                    R.string.steps_day_summary,
                    localizedDate(bucket.start, "EEEdMMM"),
                    KcalFormat.int(bucket.steps), KcalFormat.int(EnergyCalculator.stepsToKcal(bucket.steps)),
                )
            }
            AnimatedContent(summary, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "summary") {
                Text(it, style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}

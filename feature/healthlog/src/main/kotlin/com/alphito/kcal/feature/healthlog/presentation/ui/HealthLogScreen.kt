package com.alphito.kcal.feature.healthlog.presentation.ui

import com.alphito.kcal.core.designsystem.util.localizedDate
import com.alphito.kcal.core.designsystem.util.KcalUnit
import com.alphito.kcal.core.designsystem.util.unitLabel
import com.alphito.kcal.core.designsystem.util.appLocale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alphito.kcal.core.designsystem.component.KcalKeyboard
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.chart.KcalLineChart
import com.alphito.kcal.core.designsystem.component.BigNumberField
import com.alphito.kcal.core.designsystem.component.ChipStyle
import com.alphito.kcal.core.designsystem.component.IconChip
import com.alphito.kcal.core.designsystem.component.KcalBadge
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalChip
import com.alphito.kcal.core.designsystem.component.KcalDetailScaffold
import com.alphito.kcal.core.designsystem.component.KcalDivider
import com.alphito.kcal.core.designsystem.component.SegmentedControl
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.model.DairyReaction
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.model.IbsSymptom
import com.alphito.kcal.core.model.Severity
import com.alphito.kcal.core.model.SugarContext
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.core.designsystem.util.foodName
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.SharedKeys
import com.alphito.kcal.feature.healthlog.R
import com.alphito.kcal.feature.healthlog.presentation.mvi.HealthLogEffect
import com.alphito.kcal.feature.healthlog.presentation.mvi.HealthLogIntent
import com.alphito.kcal.feature.healthlog.presentation.mvi.HealthLogState
import com.alphito.kcal.feature.healthlog.presentation.mvi.HealthLogViewModel
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun HealthLogRoute() {
    val viewModel: HealthLogViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { if (it == HealthLogEffect.Close) navigator.back() }
    HealthLogScreen(state, viewModel::onIntent)
}

@Composable
internal fun HealthLogScreen(state: HealthLogState, onIntent: (HealthLogIntent) -> Unit) {
    KcalDetailScaffold(
        title = stringResource(R.string.health_title),
        onBack = { onIntent(HealthLogIntent.Back) },
        sharedBoundsKey = SharedKeys.actionCard("health"),
    ) {
        if (state.tabs.size > 1) {
            val labels = state.tabs.map { stringResource(it.tabLabel()) }.toImmutableList()
            SegmentedControl(labels, state.tabs.indexOf(state.active), { onIntent(HealthLogIntent.SelectTab(state.tabs[it])) })
            Spacer(Modifier.height(14.dp))
        }
        LogDayPicker(state, onIntent)
        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = state.active,
            transitionSpec = { fadeIn(KcalMotion.fadeTween(KcalMotion.DurationMedium)) togetherWith fadeOut(KcalMotion.fadeTween()) },
            label = "health-tab",
        ) { condition ->
            Column {
                Text(stringResource(condition.subtitle()), style = KcalTheme.typography.body, color = KcalTheme.colors.textSecondary)
                Spacer(Modifier.height(14.dp))
                when (condition) {
                    HealthCondition.Diabetes -> SugarLog(state, onIntent)
                    HealthCondition.LactoseIntolerant -> DairyLogContent(state, onIntent)
                    HealthCondition.Ibs -> IbsLogContent(state, onIntent)
                }
                Spacer(Modifier.height(14.dp))
                PlanInsightCard(condition, state)
            }
        }
    }
}

private fun HealthCondition.tabLabel() = when (this) {
    HealthCondition.Diabetes -> R.string.health_tab_diabetes
    HealthCondition.LactoseIntolerant -> R.string.health_tab_lactose
    HealthCondition.Ibs -> R.string.health_tab_ibs
}

private fun HealthCondition.subtitle() = when (this) {
    HealthCondition.Diabetes -> R.string.health_sub_diabetes
    HealthCondition.LactoseIntolerant -> R.string.health_sub_lactose
    HealthCondition.Ibs -> R.string.health_sub_ibs
}

@Composable
private fun SaveButton(text: String, saved: Boolean, onClick: () -> Unit) {
    AnimatedContent(saved, transitionSpec = { (fadeIn() + scaleIn(KcalMotion.bouncySpring(), 0.9f)) togetherWith fadeOut() }, label = "save") {
        KcalButton(if (it) stringResource(R.string.health_saved) else text, onClick, Modifier.fillMaxWidth())
    }
}

// ---------------------------------------------------------------- Diabetes

@Composable
private fun SugarLog(state: HealthLogState, onIntent: (HealthLogIntent) -> Unit) {
    val colors = KcalTheme.colors
    val locale = appLocale()
    KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
        Text(stringResource(R.string.sugar_today), style = KcalTheme.typography.titleMedium, color = colors.textBrand)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SugarContext.entries.forEach { c ->
                KcalChip(stringResource(c.label()), state.sugar.context == c, { onIntent(HealthLogIntent.SugarContextSelected(c)) }, multiSelect = false)
            }
        }
        Spacer(Modifier.height(16.dp))
        BigNumberField(state.sugar.input, { onIntent(HealthLogIntent.SugarInput(it)) }, unit = unitLabel(KcalUnit.MgDl), keyboard = KcalKeyboard.Number, placeholder = "0")
        Spacer(Modifier.height(8.dp))
        val range = state.sugar.context.range
        Text(
            if (state.sugar.context == SugarContext.AfterMeal) stringResource(R.string.sugar_target_after, KcalFormat.int(range.last + 1, locale))
            else stringResource(R.string.sugar_target, KcalFormat.int(range.first, locale), KcalFormat.int(range.last, locale)),
            style = KcalTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(state.invalidInput) {
            Text(stringResource(R.string.sugar_invalid), style = KcalTheme.typography.bodySmall, color = colors.accent,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        Spacer(Modifier.height(16.dp))
        SaveButton(stringResource(R.string.sugar_save), state.justSaved) { onIntent(HealthLogIntent.Save) }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.sugar_hint), style = KcalTheme.typography.bodySmall, color = colors.textSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(16.dp))
    val values = remember(state.sugar.week) { fillGaps(state.sugar.week.map { it?.mgDl?.toFloat() }).toImmutableList() }
    val labels = remember(state.days, locale) { state.days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, locale) }.toImmutableList() }
    KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
        Text(stringResource(R.string.sugar_trend), style = KcalTheme.typography.titleMedium, color = colors.textBrand)
        Spacer(Modifier.height(16.dp))
        KcalLineChart(values, labels, state.sugar.selected, { onIntent(HealthLogIntent.SelectSugarDay(it)) }, labelStyleBoldOnly = true)
        Spacer(Modifier.height(12.dp))
        val day = state.days.getOrNull(state.sugar.selected)?.dayOfWeek?.getDisplayName(TextStyle.SHORT, locale).orEmpty()
        val reading = state.sugar.week.getOrNull(state.sugar.selected)
        val summary = when {
            reading == null -> stringResource(R.string.sugar_summary_none, day)
            reading.isLow -> stringResource(R.string.sugar_summary_low, day, reading.mgDl)
            !reading.inNormalRange -> stringResource(R.string.sugar_summary_high, day, reading.mgDl)
            else -> stringResource(R.string.sugar_summary_normal, day, reading.mgDl)
        }
        Text(summary, style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
        state.sugar.average?.let { avg ->
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.sugar_stats, KcalFormat.int(state.sugar.inRangeCount, locale), KcalFormat.int(state.sugar.loggedCount, locale), KcalFormat.int(avg, locale)),
                style = KcalTheme.typography.bodySmall, color = colors.textBrand,
            )
        }
    }
}

// ---------------------------------------------------------------- Lactose intolerance

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DairyLogContent(state: HealthLogState, onIntent: (HealthLogIntent) -> Unit) {
    val colors = KcalTheme.colors
    val dairy = state.dairy
    KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(KcalIcons.Check, size = 42.dp, iconSize = 18.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.dairy_days_since), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
                Text(dairy.daysSinceReaction?.let { stringResource(R.string.dairy_days, it) } ?: "–",
                    style = KcalTheme.typography.title.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = colors.textBrand)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
        Text(stringResource(R.string.dairy_question), style = KcalTheme.typography.titleMedium, color = colors.textBrand)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KcalChip(stringResource(R.string.yes), dairy.hadDairy, { onIntent(HealthLogIntent.HadDairy(true)) }, Modifier.weight(1f), ChipStyle.Solid, multiSelect = false, fill = true)
            KcalChip(stringResource(R.string.no), !dairy.hadDairy, { onIntent(HealthLogIntent.HadDairy(false)) }, Modifier.weight(1f), ChipStyle.Solid, multiSelect = false, fill = true)
        }
        AnimatedVisibility(dairy.hadDairy, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.dairy_any_reaction), style = KcalTheme.typography.body, color = colors.textSecondary)
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DairyReaction.entries.forEach { r ->
                        KcalChip(stringResource(r.label()), r in dairy.reactions, { onIntent(HealthLogIntent.ToggleReaction(r)) }, style = ChipStyle.Solid)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        SaveButton(stringResource(R.string.health_save_log), state.justSaved) { onIntent(HealthLogIntent.Save) }
    }
    Spacer(Modifier.height(14.dp))
    val dairyLabels = DairyReaction.entries.associateWith { stringResource(it.label()) }
    val noReaction = stringResource(R.string.dairy_no_reaction)
    KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
        Text(stringResource(R.string.dairy_history), style = KcalTheme.typography.titleMedium, color = colors.textBrand)
        Spacer(Modifier.height(8.dp))
        if (dairy.history.isEmpty()) Text(stringResource(R.string.dairy_none_yet), style = KcalTheme.typography.body, color = colors.textSecondary)
        dairy.history.forEachIndexed { i, log ->
            if (i > 0) KcalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(localizedDate(log.date, "EEEdMMM"), style = KcalTheme.typography.titleSmall, color = colors.textPrimary)
                    val detail = when {
                        !log.hadDairy -> stringResource(R.string.dairy_no_dairy)
                        log.reactions.isEmpty() -> noReaction + if (log.note.isNotBlank()) " — ${log.note}" else ""
                        else -> log.reactions.joinToString(" + ") { dairyLabels.getValue(it) } + if (log.note.isNotBlank()) " ${log.note}" else ""
                    }
                    Text(detail, style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
                }
                KcalBadge(stringResource(if (log.hadDairy) R.string.dairy_badge else R.string.dairy_badge_none))
            }
        }
    }
}

private fun DairyReaction.label() = when (this) {
    DairyReaction.Bloating -> R.string.reaction_bloating
    DairyReaction.Gas -> R.string.reaction_gas
    DairyReaction.Diarrhea -> R.string.reaction_diarrhea
    DairyReaction.Nausea -> R.string.reaction_nausea
}

// ---------------------------------------------------------------- IBS

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IbsLogContent(state: HealthLogState, onIntent: (HealthLogIntent) -> Unit) {
    val colors = KcalTheme.colors
    val ibs = state.ibs
    val locale = appLocale()
    KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
        Text(stringResource(R.string.ibs_today), style = KcalTheme.typography.titleMedium, color = colors.textBrand)
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IbsSymptom.entries.forEach { s ->
                KcalChip(stringResource(s.label()), s in ibs.symptoms, { onIntent(HealthLogIntent.ToggleSymptom(s)) }, style = ChipStyle.Solid)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.ibs_severity), style = KcalTheme.typography.body, color = colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Severity.entries.forEach { sev ->
                KcalChip(stringResource(sev.label()), ibs.severity == sev, { onIntent(HealthLogIntent.SelectSeverity(sev)) },
                    Modifier.weight(1f), ChipStyle.Solid, multiSelect = false, fill = true)
            }
        }
        Spacer(Modifier.height(18.dp))
        SaveButton(stringResource(R.string.health_save_log), state.justSaved) { onIntent(HealthLogIntent.Save) }
    }
    Spacer(Modifier.height(14.dp))
    val values = remember(ibs.week) { fillGaps(ibs.week.map { log -> log?.let { if (it.symptoms.isEmpty()) 0f else it.severity.score.toFloat() } }).toImmutableList() }
    val labels = remember(state.days, locale) { state.days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, locale) }.toImmutableList() }
    KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
        Text(stringResource(R.string.ibs_trend), style = KcalTheme.typography.titleMedium, color = colors.textBrand)
        Spacer(Modifier.height(16.dp))
        KcalLineChart(values, labels, ibs.selected, { onIntent(HealthLogIntent.SelectIbsDay(it)) }, labelStyleBoldOnly = true)
        Spacer(Modifier.height(12.dp))
        val day = state.days.getOrNull(ibs.selected)?.dayOfWeek?.getDisplayName(TextStyle.SHORT, locale).orEmpty()
        val log = ibs.week.getOrNull(ibs.selected)
        val symptomLabels = IbsSymptom.entries.associateWith { stringResource(it.label()) }
        val summary = if (log == null || log.symptoms.isEmpty()) stringResource(R.string.ibs_summary_none, day)
        else stringResource(R.string.ibs_summary, day, stringResource(log.severity.label()), log.symptoms.joinToString(", ") { symptomLabels.getValue(it).lowercase(locale) })
        Text(summary, style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
    }
}

private fun IbsSymptom.label() = when (this) {
    IbsSymptom.Bloating -> R.string.reaction_bloating
    IbsSymptom.AbdominalPain -> R.string.ibs_abdominal_pain
    IbsSymptom.Diarrhea -> R.string.reaction_diarrhea
    IbsSymptom.Constipation -> R.string.ibs_constipation
}

private fun Severity.label() = when (this) {
    Severity.Mild -> R.string.ibs_mild
    Severity.Moderate -> R.string.ibs_moderate
    Severity.Severe -> R.string.ibs_severe
}

private fun SugarContext.label() = when (this) {
    SugarContext.Fasting -> R.string.sugar_ctx_fasting
    SugarContext.BeforeMeal -> R.string.sugar_ctx_before
    SugarContext.AfterMeal -> R.string.sugar_ctx_after
    SugarContext.Bedtime -> R.string.sugar_ctx_bedtime
}

/** Seven day chips: log or correct any of the last 7 days (today selected by default). */
@Composable
private fun LogDayPicker(state: HealthLogState, onIntent: (HealthLogIntent) -> Unit) {
    val locale = appLocale()
    Text(
        stringResource(R.string.health_log_for, state.logDay?.let { localizedDate(it, "EEEEdMMM") }.orEmpty()),
        style = KcalTheme.typography.bodySmall, color = KcalTheme.colors.textSecondary,
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        state.days.forEachIndexed { i, d ->
            KcalChip(
                if (i == state.days.lastIndex) stringResource(R.string.health_today) else d.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                selected = i == state.logIndex, onClick = { onIntent(HealthLogIntent.SelectLogDay(i)) },
                // Every chip needs a weight: an unweighted fill chip would take the whole row.
                modifier = Modifier.weight(if (i == state.days.lastIndex) 1.9f else 1f),
                style = ChipStyle.Solid, multiSelect = false, fill = true,
            )
        }
    }
}

/** Connects the log to the meal plan: what the plan does for this condition + what the data shows. */
@Composable
private fun PlanInsightCard(condition: HealthCondition, state: HealthLogState) {
    val colors = KcalTheme.colors
    val locale = appLocale()
    KcalCard(Modifier.fillMaxWidth(), color = colors.accentContainer, elevated = false, contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(KcalIcons.Health, size = 32.dp, iconSize = 16.dp, container = colors.surface)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.health_plan_title), style = KcalTheme.typography.titleSmall, color = colors.textBrand)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(
                when (condition) {
                    HealthCondition.Diabetes -> R.string.health_plan_diabetes
                    HealthCondition.Ibs -> R.string.health_plan_ibs
                    HealthCondition.LactoseIntolerant -> R.string.health_plan_lactose
                },
            ),
            style = KcalTheme.typography.bodySmall, color = colors.textPrimary,
        )
        Spacer(Modifier.height(8.dp))
        val insight = when (condition) {
            HealthCondition.Diabetes -> null
            HealthCondition.LactoseIntolerant -> stringResource(R.string.dairy_reactions_30, KcalFormat.int(state.dairy.reactionsLast30Days, locale))
            HealthCondition.Ibs -> {
                val ibs = state.ibs
                val language = com.alphito.kcal.core.designsystem.util.appLanguage()
                buildString {
                    if (ibs.loggedDays > 0) append(stringResource(R.string.ibs_free_days, KcalFormat.int(ibs.symptomFreeDays, locale), KcalFormat.int(ibs.loggedDays, locale)))
                    append(if (isEmpty()) "" else "\n")
                    append(
                        if (ibs.suspectFoods.isEmpty()) stringResource(R.string.ibs_no_suspects)
                        else stringResource(R.string.ibs_suspects, ibs.suspectFoods.joinToString(", ") { it.localizedName(language) }),
                    )
                }
            }
        }
        insight?.let { Text(it, style = KcalTheme.typography.bodySmall, color = colors.textSecondary) }
    }
}

/** Missing days carry the previous value forward so the trend line stays continuous. */
private fun fillGaps(values: List<Float?>): List<Float> {
    var last = values.firstOrNull { it != null } ?: 0f
    return values.map { v -> (v ?: last).also { last = it } }
}

package com.alphito.kcal.feature.dietplan.presentation.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.chart.ProgressRing
import com.alphito.kcal.core.designsystem.component.ChipStyle
import com.alphito.kcal.core.designsystem.component.FoodThumb
import com.alphito.kcal.core.designsystem.component.KcalBottomSheet
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalCheckbox
import com.alphito.kcal.core.designsystem.component.KcalChip
import com.alphito.kcal.core.designsystem.component.KcalDivider
import com.alphito.kcal.core.designsystem.component.KcalTopBar
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.modifier.kcalClickable
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.motion.kcalSharedBounds
import com.alphito.kcal.core.designsystem.motion.kcalSharedElement
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.core.designsystem.util.appLocale
import com.alphito.kcal.core.designsystem.util.foodName
import com.alphito.kcal.core.designsystem.util.kcalCostLabel
import com.alphito.kcal.core.designsystem.util.kcalLabel
import com.alphito.kcal.core.designsystem.util.label
import com.alphito.kcal.core.designsystem.util.mealTitle
import com.alphito.kcal.core.designsystem.util.regionLabel
import com.alphito.kcal.core.model.BudgetTier
import com.alphito.kcal.core.model.DayPlan
import com.alphito.kcal.core.model.Goal
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.model.MealSlot
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.core.navigation.SharedKeys
import com.alphito.kcal.feature.dietplan.R
import com.alphito.kcal.feature.dietplan.presentation.components.InfoButton
import com.alphito.kcal.feature.dietplan.presentation.components.MealInfoContent
import com.alphito.kcal.feature.dietplan.presentation.components.MealSlotCard
import com.alphito.kcal.feature.dietplan.presentation.components.SwapInfoContent
import com.alphito.kcal.feature.dietplan.presentation.components.SwapOptionCard
import com.alphito.kcal.feature.dietplan.presentation.mvi.ChecklistEffect
import com.alphito.kcal.feature.dietplan.presentation.mvi.ChecklistIntent
import com.alphito.kcal.feature.dietplan.presentation.mvi.ChecklistState
import com.alphito.kcal.feature.dietplan.presentation.mvi.DietPlanEffect
import com.alphito.kcal.feature.dietplan.presentation.mvi.DietPlanIntent
import com.alphito.kcal.feature.dietplan.presentation.mvi.DietPlanState
import com.alphito.kcal.feature.dietplan.presentation.mvi.DietPlanViewModel
import com.alphito.kcal.feature.dietplan.presentation.mvi.MealChecklistViewModel
import com.alphito.kcal.feature.dietplan.presentation.mvi.MealInfoViewModel
import com.alphito.kcal.feature.dietplan.presentation.mvi.PlanBasis
import com.alphito.kcal.feature.dietplan.presentation.mvi.SwapEffect
import com.alphito.kcal.feature.dietplan.presentation.mvi.SwapIntent
import com.alphito.kcal.feature.dietplan.presentation.mvi.SwapState
import com.alphito.kcal.feature.dietplan.presentation.mvi.SwapViewModel
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.format.TextStyle

// ------------------------------------------------------------------ Diet plan tab

@Composable
internal fun DietPlanRoute() {
    val viewModel: DietPlanViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { if (it is DietPlanEffect.OpenSwap) navigator.navigate(Route.SwapIngredient(it.itemId)) }
    DietPlanScreen(state, viewModel::onIntent)
    state.infoItemId?.let { id ->
        PlannedItemInfoSheet(id, onSwap = { viewModel.onIntent(DietPlanIntent.Swap(id)) }, onDismiss = { viewModel.onIntent(DietPlanIntent.DismissInfo) })
    }
    if (state.showHowItWorks) {
        HowYourPlanWorksSheet(state.basis, state.selected?.targetKcal ?: 0) { viewModel.onIntent(DietPlanIntent.DismissHowItWorks) }
    }
}

@Composable
internal fun DietPlanScreen(state: DietPlanState, onIntent: (DietPlanIntent) -> Unit) {
    val colors = KcalTheme.colors
    val locale = appLocale()
    val chips = rememberLazyListState()
    LaunchedEffect(state.selectedIndex) { chips.animateScrollToItem((state.selectedIndex - 2).coerceAtLeast(0)) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.diet_overline), style = KcalTheme.typography.titleSmall, color = colors.textBrand)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.diet_this_week), style = KcalTheme.typography.headlineSmall, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
        }
        LazyRow(state = chips, contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.days, key = { _, d -> d.date.toEpochDay() }) { index, day ->
                KcalChip(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    selected = index == state.selectedIndex,
                    onClick = { onIntent(DietPlanIntent.SelectDay(index)) },
                    style = ChipStyle.Solid, multiSelect = false,
                )
            }
        }
        // Day content slides in the direction of travel (earlier day → from the left).
        AnimatedContent(
            targetState = state.selectedIndex,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(KcalMotion.slideSpring) { it / 4 * dir } + fadeIn(KcalMotion.fadeTween(KcalMotion.DurationMedium))) togetherWith
                    (slideOutHorizontally(KcalMotion.slideSpring) { -it / 4 * dir } + fadeOut(KcalMotion.fadeTween()))
            },
            modifier = Modifier.weight(1f),
            label = "diet-day",
        ) { index ->
            val day = state.days.getOrNull(index)
            val bottomInset = com.alphito.kcal.core.designsystem.component.LocalBottomBarInset.current
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp + bottomInset),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (day == null || day.slots.all { it.items.isEmpty() }) {
                    item { Text(stringResource(R.string.diet_empty), style = KcalTheme.typography.body, color = colors.textSecondary) }
                } else {
                    item(key = "summary") { DaySummaryCard(day, state.basis) { onIntent(DietPlanIntent.ShowHowItWorks) } }
                    items(day.slots.filter { it.items.isNotEmpty() }, key = { it.mealType.name }) { slot ->
                        MealSlotCard(
                            slot,
                            onSwap = { onIntent(DietPlanIntent.Swap(it)) },
                            onInfo = { onIntent(DietPlanIntent.ShowInfo(it)) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "tip") { TipCard(day.date, state.basis) }
                }
            }
        }
    }
}

/** Planned kcal vs target, protein & fibre, and the "tuned for you" chips that open the explainer. */
@Composable
private fun DaySummaryCard(day: DayPlan, basis: PlanBasis, onHowItWorks: () -> Unit) {
    val colors = KcalTheme.colors
    val locale = appLocale()
    KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val progress = if (day.targetKcal > 0) (day.plannedKcal.toFloat() / day.targetKcal).coerceIn(0f, 1f) else 0f
            ProgressRing(progress = progress, modifier = Modifier.size(58.dp), strokeWidth = 6.dp) {
                Text(KcalFormat.int((progress * 100).toInt(), locale) + "%", style = KcalTheme.typography.labelSmall, color = colors.textBrand)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.diet_summary_planned, KcalFormat.int(day.plannedKcal, locale), KcalFormat.int(day.targetKcal, locale)),
                    style = KcalTheme.typography.titleSmall, color = colors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.diet_summary_macros, KcalFormat.int(day.plannedProteinG, locale), KcalFormat.int(day.plannedFiberG, locale)),
                    style = KcalTheme.typography.bodySmall, color = colors.textSecondary,
                )
            }
            InfoButton(onHowItWorks)
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.diet_tuned_for), style = KcalTheme.typography.caption, color = colors.textSecondary)
        Spacer(Modifier.height(6.dp))
        FlowRow(
            Modifier.kcalClickable(shape = KcalShapes.sm, onClick = onHowItWorks),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BasisChip(regionLabel(basis.country))
            BasisChip(stringResource(basis.goal.label()))
            BasisChip(stringResource(basis.budget.label()))
            basis.conditions.sortedBy { it.ordinal }.forEach { BasisChip(stringResource(it.label())) }
        }
    }
}

@Composable
private fun BasisChip(text: String) {
    Text(
        text, style = KcalTheme.typography.caption, color = KcalTheme.colors.textBrand,
        modifier = Modifier.background(KcalTheme.colors.accentContainer, KcalShapes.pill).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** One practical tip per day, picked from tips that match the user's goal and conditions. */
@Composable
private fun TipCard(date: LocalDate, basis: PlanBasis) {
    val tips = remember(basis) { tipsFor(basis) }
    val tip = tips[Math.floorMod(date.toEpochDay(), tips.size.toLong()).toInt()]
    KcalCard(Modifier.fillMaxWidth(), color = KcalTheme.colors.accentContainer, elevated = false, contentPadding = PaddingValues(16.dp)) {
        Text(stringResource(R.string.diet_tip_title), style = KcalTheme.typography.labelSmall, color = KcalTheme.colors.textBrand)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(tip), style = KcalTheme.typography.bodySmall, color = KcalTheme.colors.textPrimary)
    }
}

private fun tipsFor(basis: PlanBasis): List<Int> = buildList {
    if (HealthCondition.Diabetes in basis.conditions) { add(R.string.tip_diabetes_1); add(R.string.tip_diabetes_2) }
    if (HealthCondition.Ibs in basis.conditions) { add(R.string.tip_ibs_1); add(R.string.tip_ibs_2) }
    if (HealthCondition.LactoseIntolerant in basis.conditions) add(R.string.tip_lactose_1)
    when (basis.goal) {
        Goal.LoseWeight -> { add(R.string.tip_lose_1); add(R.string.tip_lose_2) }
        Goal.GainWeight -> add(R.string.tip_gain_1)
        Goal.MaintainWeight -> add(R.string.tip_maintain_1)
    }
    if (basis.budget == BudgetTier.Low) add(R.string.tip_budget_1)
    addAll(listOf(R.string.tip_general_1, R.string.tip_general_2, R.string.tip_general_3, R.string.tip_general_4))
}

@StringRes private fun Goal.label() = when (this) {
    Goal.LoseWeight -> R.string.basis_lose
    Goal.GainWeight -> R.string.basis_gain
    Goal.MaintainWeight -> R.string.basis_maintain
}

@StringRes private fun BudgetTier.label() = when (this) {
    BudgetTier.Low -> R.string.basis_budget_low
    BudgetTier.Medium -> R.string.basis_budget_medium
    BudgetTier.High -> R.string.basis_budget_high
}

@StringRes private fun HealthCondition.label() = when (this) {
    HealthCondition.Diabetes -> R.string.basis_diabetes
    HealthCondition.Ibs -> R.string.basis_ibs
    HealthCondition.LactoseIntolerant -> R.string.basis_lactose
}

/** "How your plan is built" – the rules in plain language, only those that apply to the user. */
@Composable
private fun HowYourPlanWorksSheet(basis: PlanBasis, targetKcal: Int, onDismiss: () -> Unit) {
    val locale = appLocale()
    KcalBottomSheet(onDismiss = onDismiss) { dismiss ->
        Text(stringResource(R.string.how_title), style = KcalTheme.typography.title, color = KcalTheme.colors.textBrand)
        Spacer(Modifier.height(14.dp))
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HowRow(KcalIcons.Flame, stringResource(R.string.how_target, KcalFormat.int(targetKcal, locale)))
            HowRow(KcalIcons.NavHome, stringResource(R.string.how_region, regionLabel(basis.country)))
            HowRow(
                KcalIcons.Budget,
                stringResource(
                    when (basis.budget) {
                        BudgetTier.Low -> R.string.how_budget_low
                        BudgetTier.Medium -> R.string.how_budget_medium
                        BudgetTier.High -> R.string.how_budget_high
                    },
                ),
            )
            if (HealthCondition.Diabetes in basis.conditions) HowRow(KcalIcons.Health, stringResource(R.string.how_diabetes))
            if (HealthCondition.Ibs in basis.conditions) HowRow(KcalIcons.Health, stringResource(R.string.how_ibs))
            if (HealthCondition.LactoseIntolerant in basis.conditions) HowRow(KcalIcons.Shield, stringResource(R.string.how_lactose))
            HowRow(KcalIcons.Swap, stringResource(R.string.how_swap))
            HowRow(KcalIcons.Info, stringResource(R.string.how_info))
            Text(stringResource(R.string.info_disclaimer), style = KcalTheme.typography.caption, color = KcalTheme.colors.textTertiary)
        }
        Spacer(Modifier.height(16.dp))
        KcalButton(stringResource(R.string.info_close), dismiss, Modifier.fillMaxWidth())
    }
}

@Composable
private fun HowRow(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        com.alphito.kcal.core.designsystem.component.IconChip(icon, size = 30.dp, iconSize = 16.dp)
        Spacer(Modifier.width(12.dp))
        Text(text, style = KcalTheme.typography.bodySmall, color = KcalTheme.colors.textPrimary, modifier = Modifier.padding(top = 5.dp))
    }
}

/** Info sheet for a planned item (diet plan + checklist). */
@Composable
internal fun PlannedItemInfoSheet(itemId: Long, onSwap: () -> Unit, onDismiss: () -> Unit) {
    val viewModel: MealInfoViewModel = koinViewModel(key = "meal-info-$itemId") { parametersOf(itemId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val insight = state.insight ?: return
    KcalBottomSheet(onDismiss = onDismiss) { dismiss ->
        MealInfoContent(
            food = insight.item.food, servings = insight.item.servings, mealType = insight.item.mealType,
            reasons = insight.reasons, conditions = state.conditions,
            primaryAction = stringResource(R.string.info_swap), onPrimary = onSwap, onDismiss = dismiss,
        )
    }
}

// ------------------------------------------------------------------ Meal checklist

@Composable
internal fun MealChecklistRoute() {
    val viewModel: MealChecklistViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var celebrate by remember { mutableStateOf(false) }
    ObserveEffects(viewModel.effects) {
        when (it) {
            ChecklistEffect.Close -> navigator.back()
            is ChecklistEffect.OpenSwap -> navigator.navigate(Route.SwapIngredient(it.itemId))
            ChecklistEffect.DayCompleted -> celebrate = true
        }
    }
    LaunchedEffect(celebrate) { if (celebrate) { delay(3_500); celebrate = false } }
    MealChecklistScreen(state, celebrate, viewModel::onIntent)
    state.infoItemId?.let { id ->
        PlannedItemInfoSheet(id, onSwap = { viewModel.onIntent(ChecklistIntent.Swap(id)) }, onDismiss = { viewModel.onIntent(ChecklistIntent.DismissInfo) })
    }
}

@Composable
internal fun MealChecklistScreen(state: ChecklistState, celebrate: Boolean, onIntent: (ChecklistIntent) -> Unit) {
    val colors = KcalTheme.colors
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 12.dp)) {
        KcalTopBar(stringResource(R.string.checklist_title), onBack = { onIntent(ChecklistIntent.Back) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            state.day?.let { ChecklistProgress(it, state.streak) }
            AnimatedVisibility(
                visible = celebrate,
                enter = fadeIn(KcalMotion.fadeTween()) + expandVertically(KcalMotion.sizeSpring),
                exit = fadeOut(KcalMotion.fadeTween()) + shrinkVertically(KcalMotion.sizeSpring),
            ) {
                Text(
                    stringResource(R.string.checklist_celebrate),
                    style = KcalTheme.typography.titleSmall, color = colors.onPrimary, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth().background(colors.primary, KcalShapes.md).padding(14.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            KcalCard(
                Modifier.fillMaxWidth().kcalSharedBounds(SharedKeys.TODAY_MEAL_CARD),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                state.slots.forEachIndexed { i, slot ->
                    if (i > 0) KcalDivider()
                    ChecklistRow(slot, onIntent)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.checklist_hint), style = KcalTheme.typography.caption, color = colors.textTertiary)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChecklistProgress(day: DayPlan, streak: Int) {
    val colors = KcalTheme.colors
    val locale = appLocale()
    val fraction by animateFloatAsState(
        if (day.plannedMeals == 0) 0f else day.checkedMeals.toFloat() / day.plannedMeals, KcalMotion.fadeTween(), label = "checklist-progress",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        ProgressRing(progress = fraction, modifier = Modifier.size(64.dp), strokeWidth = 7.dp) {
            Text("${KcalFormat.int(day.checkedMeals, locale)}/${KcalFormat.int(day.plannedMeals, locale)}", style = KcalTheme.typography.labelSmall, color = colors.textBrand)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            val left = day.plannedMeals - day.checkedMeals
            Text(
                when {
                    day.checkedMeals == 0 -> stringResource(R.string.checklist_msg_start)
                    left > 0 -> stringResource(R.string.checklist_msg_progress, KcalFormat.int(left, locale))
                    else -> stringResource(R.string.checklist_msg_done)
                },
                style = KcalTheme.typography.titleSmall, color = colors.textPrimary,
            )
            Text(
                stringResource(R.string.checklist_kcal, KcalFormat.int(day.eatenKcal, locale), KcalFormat.int(day.targetKcal, locale)),
                style = KcalTheme.typography.bodySmall, color = colors.textSecondary,
            )
            // Hidden at 0: a new user has nothing to celebrate yet.
            if (streak > 0) Text(pluralStringResource(R.plurals.checklist_streak, streak, KcalFormat.int(streak, locale)), style = KcalTheme.typography.bodySmall, color = colors.textBrand)
        }
    }
}

@Composable
private fun ChecklistRow(slot: MealSlot, onIntent: (ChecklistIntent) -> Unit) {
    val colors = KcalTheme.colors
    val lead = slot.items.first()
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        FoodThumb(
            slot.leadImageKey, imageUrl = slot.leadImageUrl, emoji = slot.leadEmoji, size = 46.dp, shape = CircleShape,
            modifier = Modifier.kcalSharedElement(SharedKeys.mealImage(lead.id)),
        )
        Spacer(Modifier.width(4.dp))
        // kcalClickable clips to its rounded shape: the inner padding keeps every glyph (incl. tall
        // Urdu/Arabic/Hindi ones and large font scales) clear of the corners. Row/Spacer above shrink
        // by the same amount so the text sits where it did.
        Column(
            Modifier.weight(1f)
                .kcalClickable(shape = KcalShapes.sm) { onIntent(ChecklistIntent.ShowInfo(lead.id)) }
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(slot.mealType.label(), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
            Text(
                mealTitle(slot), style = KcalTheme.typography.titleSmall, color = colors.textPrimary,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(kcalLabel(slot.totalKcal), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
        }
        InfoButton({ onIntent(ChecklistIntent.ShowInfo(lead.id)) })
        KcalCheckbox(slot.checked, { onIntent(ChecklistIntent.Toggle(slot, it)) })
    }
}

// ------------------------------------------------------------------ Swap ingredient

@Composable
internal fun SwapRoute(itemId: Long) {
    val viewModel: SwapViewModel = koinViewModel(key = "swap-$itemId") { parametersOf(itemId) }
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { if (it == SwapEffect.Close) navigator.back() }
    SwapScreen(state, viewModel::onIntent)
    val info = state.infoCandidate
    val current = state.current
    if (info != null && current != null) {
        KcalBottomSheet(onDismiss = { viewModel.onIntent(SwapIntent.DismissInfo) }) { dismiss ->
            SwapInfoContent(
                food = info.food, servings = info.servings, mealType = current.mealType, reasons = info.reasons,
                onChoose = { viewModel.onIntent(SwapIntent.Select(info.food.id)); dismiss() }, onDismiss = dismiss,
            )
        }
    }
}

@Composable
internal fun SwapScreen(state: SwapState, onIntent: (SwapIntent) -> Unit) {
    val colors = KcalTheme.colors
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 12.dp)) {
        KcalTopBar(stringResource(R.string.swap_title), onBack = { onIntent(SwapIntent.Back) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            val current = state.current
            Text(
                stringResource(R.string.swap_subtitle, current?.mealType?.label().orEmpty()),
                style = KcalTheme.typography.body, color = colors.textSecondary,
            )
            Spacer(Modifier.height(16.dp))
            if (current != null) {
                KcalCard(
                    Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.outline),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FoodThumb(
                            current.food.imageKey, imageUrl = current.food.imageUrl, emoji = current.food.emoji, size = 42.dp,
                            modifier = Modifier.kcalSharedElement(SharedKeys.mealImage(current.id)),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(stringResource(R.string.swap_currently).uppercase(), style = KcalTheme.typography.overline, color = colors.textSecondary)
                            Text(
                                foodName(current.food), style = KcalTheme.typography.titleSmall, color = colors.textPrimary,
                                modifier = Modifier.kcalSharedElement(SharedKeys.mealTitle(current.id)),
                            )
                            Text(kcalCostLabel(current.kcal, current.food.cost), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.swap_suggested), style = KcalTheme.typography.titleSmall, color = colors.textBrand)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.swap_explain), style = KcalTheme.typography.caption, color = colors.textSecondary)
            Spacer(Modifier.height(12.dp))
            if (state.loaded && state.candidates.isEmpty()) {
                Text(stringResource(R.string.swap_none), style = KcalTheme.typography.body, color = colors.textSecondary, textAlign = TextAlign.Start)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.candidates.forEach { candidate ->
                    SwapOptionCard(
                        candidate = candidate,
                        selected = candidate.food.id == state.selectedFoodId,
                        onClick = { onIntent(SwapIntent.Select(candidate.food.id)) },
                        onInfo = { onIntent(SwapIntent.ShowInfo(candidate.food.id)) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        KcalButton(
            stringResource(R.string.swap_confirm), { onIntent(SwapIntent.Confirm) },
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
            enabled = state.selectedFoodId != null && !state.saving,
        )
    }
}

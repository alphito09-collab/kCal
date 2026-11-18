package com.alphito.kcal.feature.dietplan.presentation.ui

import com.alphito.kcal.core.designsystem.util.foodName
import com.alphito.kcal.core.designsystem.util.mealTitle
import com.alphito.kcal.core.designsystem.util.appLocale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alphito.kcal.core.common.mvi.ObserveEffects
import com.alphito.kcal.core.designsystem.component.ChipStyle
import com.alphito.kcal.core.designsystem.component.FoodThumb
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalCheckbox
import com.alphito.kcal.core.designsystem.component.KcalChip
import com.alphito.kcal.core.designsystem.component.KcalDivider
import com.alphito.kcal.core.designsystem.component.KcalTopBar
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.motion.kcalSharedBounds
import com.alphito.kcal.core.designsystem.motion.kcalSharedElement
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.core.designsystem.util.kcalLabel
import com.alphito.kcal.core.designsystem.util.kcalPriceLabel
import com.alphito.kcal.core.designsystem.util.label
import com.alphito.kcal.core.navigation.LocalNavigator
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.core.navigation.SharedKeys
import com.alphito.kcal.feature.dietplan.R
import com.alphito.kcal.feature.dietplan.presentation.components.MealSlotCard
import com.alphito.kcal.feature.dietplan.presentation.components.SwapOptionCard
import com.alphito.kcal.feature.dietplan.presentation.mvi.ChecklistEffect
import com.alphito.kcal.feature.dietplan.presentation.mvi.ChecklistIntent
import com.alphito.kcal.feature.dietplan.presentation.mvi.ChecklistState
import com.alphito.kcal.feature.dietplan.presentation.mvi.DietPlanEffect
import com.alphito.kcal.feature.dietplan.presentation.mvi.DietPlanIntent
import com.alphito.kcal.feature.dietplan.presentation.mvi.DietPlanState
import com.alphito.kcal.feature.dietplan.presentation.mvi.DietPlanViewModel
import com.alphito.kcal.feature.dietplan.presentation.mvi.MealChecklistViewModel
import com.alphito.kcal.feature.dietplan.presentation.mvi.SwapEffect
import com.alphito.kcal.feature.dietplan.presentation.mvi.SwapIntent
import com.alphito.kcal.feature.dietplan.presentation.mvi.SwapState
import com.alphito.kcal.feature.dietplan.presentation.mvi.SwapViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.format.TextStyle
import java.util.Locale

// ------------------------------------------------------------------ Diet plan tab

@Composable
internal fun DietPlanRoute() {
    val viewModel: DietPlanViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { if (it is DietPlanEffect.OpenSwap) navigator.navigate(Route.SwapIngredient(it.itemId)) }
    DietPlanScreen(state, viewModel::onIntent)
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
                    item(key = "target") {
                        Text(
                            stringResource(R.string.diet_target, day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale), KcalFormat.int(day.targetKcal)),
                            style = KcalTheme.typography.bodySmall, color = colors.textSecondary,
                        )
                    }
                    items(day.slots.filter { it.items.isNotEmpty() }, key = { it.mealType.name }) { slot ->
                        MealSlotCard(slot, onSwap = { onIntent(DietPlanIntent.Swap(it)) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Meal checklist

@Composable
internal fun MealChecklistRoute() {
    val viewModel: MealChecklistViewModel = koinViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEffects(viewModel.effects) { if (it == ChecklistEffect.Close) navigator.back() }
    MealChecklistScreen(state, viewModel::onIntent)
}

@Composable
internal fun MealChecklistScreen(state: ChecklistState, onIntent: (ChecklistIntent) -> Unit) {
    val colors = KcalTheme.colors
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 12.dp)) {
        KcalTopBar(stringResource(R.string.checklist_title), onBack = { onIntent(ChecklistIntent.Back) })
        Text(
            stringResource(R.string.checklist_subtitle), style = KcalTheme.typography.body, color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
        KcalCard(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp).kcalSharedBounds(SharedKeys.TODAY_MEAL_CARD),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        ) {
            state.slots.forEachIndexed { i, slot ->
                if (i > 0) KcalDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    FoodThumb(
                        slot.leadImageKey, size = 46.dp, shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.kcalSharedElement(SharedKeys.mealImage(slot.items.first().id)),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(slot.mealType.label(), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
                        Text(mealTitle(slot), style = KcalTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(kcalLabel(slot.totalKcal), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    KcalCheckbox(slot.checked, { onIntent(ChecklistIntent.Toggle(slot, it)) })
                }
            }
        }
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
                KcalCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.outline)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FoodThumb(current.food.imageKey, size = 42.dp, modifier = Modifier.kcalSharedElement(SharedKeys.mealImage(current.id)))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(stringResource(R.string.swap_currently).uppercase(), style = KcalTheme.typography.overline, color = colors.textSecondary)
                            Text(
                                foodName(current.food), style = KcalTheme.typography.titleSmall, color = colors.textPrimary,
                                modifier = Modifier.kcalSharedElement(SharedKeys.mealTitle(current.id)),
                            )
                            Text(kcalPriceLabel(current.food.kcal, current.food.pricePkr), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.swap_suggested), style = KcalTheme.typography.titleSmall, color = colors.textBrand)
            Spacer(Modifier.height(12.dp))
            if (state.loaded && state.candidates.isEmpty()) {
                Text(stringResource(R.string.swap_none), style = KcalTheme.typography.body, color = colors.textSecondary, textAlign = TextAlign.Start)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.candidates.forEach { candidate ->
                    SwapOptionCard(
                        food = candidate.food, bestMatch = candidate.bestMatch,
                        selected = candidate.food.id == state.selectedFoodId,
                        onClick = { onIntent(SwapIntent.Select(candidate.food.id)) },
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

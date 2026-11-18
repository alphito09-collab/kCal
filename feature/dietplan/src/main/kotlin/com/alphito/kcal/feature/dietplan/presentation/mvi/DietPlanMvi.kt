package com.alphito.kcal.feature.dietplan.presentation.mvi

import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.data.repository.MealPlanRepository
import com.alphito.kcal.core.model.DayPlan
import com.alphito.kcal.core.model.MealSlot
import com.alphito.kcal.core.model.SwapCandidate
import com.alphito.kcal.core.model.PlannedItem
import com.alphito.kcal.feature.dietplan.domain.weekOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate

// ------------------------------------------------------------------ Diet plan (week)

data class DietPlanState(
    val days: ImmutableList<DayPlan> = persistentListOf(),
    val selectedIndex: Int = 0,
    val today: LocalDate = LocalDate.MIN,
) : UiState {
    val selected: DayPlan? get() = days.getOrNull(selectedIndex)
}

sealed interface DietPlanIntent : UiIntent {
    data class SelectDay(val index: Int) : DietPlanIntent
    data class Swap(val itemId: Long) : DietPlanIntent
}

sealed interface DietPlanEffect : UiEffect { data class OpenSwap(val itemId: Long) : DietPlanEffect }

class DietPlanViewModel(
    private val mealPlans: MealPlanRepository,
    clock: AppClock,
) : MviViewModel<DietPlanState, DietPlanIntent, DietPlanEffect>(DietPlanState()) {

    init {
        val today = clock.today()
        val week = weekOf(today)
        reduce { copy(today = today, selectedIndex = week.indexOf(today).coerceAtLeast(0)) }
        launchSafe { mealPlans.ensureWeekPlanned(today) }
        mealPlans.observeDays(week.first(), week.last())
            .onEach { days -> reduce { copy(days = days.toImmutableList()) } }
            .catch { onError(it) }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: DietPlanIntent) {
        when (intent) {
            is DietPlanIntent.SelectDay -> reduce { copy(selectedIndex = intent.index) }
            is DietPlanIntent.Swap -> sendEffect(DietPlanEffect.OpenSwap(intent.itemId))
        }
    }
}

// ------------------------------------------------------------------ Meal checklist (today)

data class ChecklistState(val slots: ImmutableList<MealSlot> = persistentListOf()) : UiState

sealed interface ChecklistIntent : UiIntent {
    data class Toggle(val slot: MealSlot, val checked: Boolean) : ChecklistIntent
    data object Back : ChecklistIntent
}

sealed interface ChecklistEffect : UiEffect { data object Close : ChecklistEffect }

class MealChecklistViewModel(
    private val mealPlans: MealPlanRepository,
    clock: AppClock,
) : MviViewModel<ChecklistState, ChecklistIntent, ChecklistEffect>(ChecklistState()) {
    init {
        val today = clock.today()
        mealPlans.observeDays(today, today)
            .onEach { days -> reduce { copy(slots = days.firstOrNull()?.slots?.filter { it.items.isNotEmpty() }.orEmpty().toImmutableList()) } }
            .catch { onError(it) }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: ChecklistIntent) {
        when (intent) {
            is ChecklistIntent.Toggle -> launchSafe { mealPlans.setChecked(intent.slot.date, intent.slot.mealType, intent.checked) }
            ChecklistIntent.Back -> sendEffect(ChecklistEffect.Close)
        }
    }
}

// ------------------------------------------------------------------ Swap ingredient

data class SwapState(
    val current: PlannedItem? = null,
    val candidates: ImmutableList<SwapCandidate> = persistentListOf(),
    val selectedFoodId: String? = null,
    val loaded: Boolean = false,
    val saving: Boolean = false,
) : UiState

sealed interface SwapIntent : UiIntent {
    data class Select(val foodId: String) : SwapIntent
    data object Confirm : SwapIntent
    data object Back : SwapIntent
}

sealed interface SwapEffect : UiEffect { data object Close : SwapEffect }

class SwapViewModel(
    private val itemId: Long,
    private val mealPlans: MealPlanRepository,
) : MviViewModel<SwapState, SwapIntent, SwapEffect>(SwapState()) {

    init {
        mealPlans.observeItem(itemId).onEach { reduce { copy(current = it) } }.catch { onError(it) }.launchIn(viewModelScope)
        launchSafe {
            val candidates = mealPlans.swapCandidates(itemId)
            reduce {
                copy(
                    candidates = candidates.toImmutableList(),
                    selectedFoodId = candidates.firstOrNull { it.bestMatch }?.food?.id,
                    loaded = true,
                )
            }
        }
    }

    override fun onIntent(intent: SwapIntent) {
        when (intent) {
            is SwapIntent.Select -> reduce { copy(selectedFoodId = intent.foodId) }
            SwapIntent.Back -> sendEffect(SwapEffect.Close)
            SwapIntent.Confirm -> {
                val foodId = currentState.selectedFoodId ?: return
                if (currentState.saving) return
                reduce { copy(saving = true) }
                launchSafe {
                    mealPlans.swap(itemId, foodId)
                    sendEffect(SwapEffect.Close)
                }
            }
        }
    }
}

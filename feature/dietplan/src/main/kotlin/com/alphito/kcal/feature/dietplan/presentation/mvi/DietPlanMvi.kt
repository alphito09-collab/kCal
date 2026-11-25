package com.alphito.kcal.feature.dietplan.presentation.mvi

import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.data.repository.MealPlanRepository
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.BudgetTier
import com.alphito.kcal.core.model.DayPlan
import com.alphito.kcal.core.model.Goal
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.model.MealInsight
import com.alphito.kcal.core.model.PlannedItem
import com.alphito.kcal.core.model.Regions
import com.alphito.kcal.core.model.SwapCandidate
import com.alphito.kcal.feature.dietplan.domain.weekOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate

/** What the plan was tuned for – shown as chips and explained in "How your plan is built". */
data class PlanBasis(
    val country: String = Regions.DEFAULT_COUNTRY,
    val budget: BudgetTier = BudgetTier.Medium,
    val goal: Goal = Goal.LoseWeight,
    val conditions: ImmutableSet<HealthCondition> = persistentSetOf(),
)

// ------------------------------------------------------------------ Diet plan (week)

data class DietPlanState(
    val days: ImmutableList<DayPlan> = persistentListOf(),
    val selectedIndex: Int = 0,
    val today: LocalDate = LocalDate.MIN,
    val basis: PlanBasis = PlanBasis(),
    /** Planned item whose info sheet is open. */
    val infoItemId: Long? = null,
    val showHowItWorks: Boolean = false,
) : UiState {
    val selected: DayPlan? get() = days.getOrNull(selectedIndex)
}

sealed interface DietPlanIntent : UiIntent {
    data class SelectDay(val index: Int) : DietPlanIntent
    data class Swap(val itemId: Long) : DietPlanIntent
    data class ShowInfo(val itemId: Long) : DietPlanIntent
    data object DismissInfo : DietPlanIntent
    data object ShowHowItWorks : DietPlanIntent
    data object DismissHowItWorks : DietPlanIntent
}

sealed interface DietPlanEffect : UiEffect { data class OpenSwap(val itemId: Long) : DietPlanEffect }

class DietPlanViewModel(
    private val mealPlans: MealPlanRepository,
    users: UserRepository,
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
        users.profile.filterNotNull().onEach { p ->
            reduce { copy(basis = PlanBasis(p.region, p.budget, p.goal, p.conditions.toImmutableSet())) }
        }.catch { onError(it) }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: DietPlanIntent) {
        when (intent) {
            is DietPlanIntent.SelectDay -> reduce { copy(selectedIndex = intent.index) }
            is DietPlanIntent.Swap -> {
                reduce { copy(infoItemId = null) }
                sendEffect(DietPlanEffect.OpenSwap(intent.itemId))
            }
            is DietPlanIntent.ShowInfo -> reduce { copy(infoItemId = intent.itemId) }
            DietPlanIntent.DismissInfo -> reduce { copy(infoItemId = null) }
            DietPlanIntent.ShowHowItWorks -> reduce { copy(showHowItWorks = true) }
            DietPlanIntent.DismissHowItWorks -> reduce { copy(showHowItWorks = false) }
        }
    }
}

// ------------------------------------------------------------------ Meal info sheet

data class MealInfoState(val insight: MealInsight? = null, val conditions: ImmutableSet<HealthCondition> = persistentSetOf()) : UiState

/** Backs the (i) sheet of one planned item: the dish, its ingredients and why it is planned. */
class MealInfoViewModel(
    itemId: Long,
    mealPlans: MealPlanRepository,
    users: UserRepository,
) : MviViewModel<MealInfoState, Nothing, Nothing>(MealInfoState()) {
    init {
        mealPlans.observeInsight(itemId).onEach { reduce { copy(insight = it) } }.catch { onError(it) }.launchIn(viewModelScope)
        users.profile.filterNotNull().onEach { reduce { copy(conditions = it.conditions.toImmutableSet()) } }
            .catch { onError(it) }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: Nothing) = Unit
}

// ------------------------------------------------------------------ Meal checklist (today)

data class ChecklistState(
    val day: DayPlan? = null,
    val streak: Int = 0,
    val infoItemId: Long? = null,
) : UiState {
    val slots get() = day?.slots?.filter { it.items.isNotEmpty() }.orEmpty()
}

sealed interface ChecklistIntent : UiIntent {
    data class Toggle(val slot: com.alphito.kcal.core.model.MealSlot, val checked: Boolean) : ChecklistIntent
    data class ShowInfo(val itemId: Long) : ChecklistIntent
    data object DismissInfo : ChecklistIntent
    data class Swap(val itemId: Long) : ChecklistIntent
    data object Back : ChecklistIntent
}

sealed interface ChecklistEffect : UiEffect {
    data object Close : ChecklistEffect
    data class OpenSwap(val itemId: Long) : ChecklistEffect
    /** Fired once when the last meal of the day is ticked – the screen celebrates. */
    data object DayCompleted : ChecklistEffect
}

class MealChecklistViewModel(
    private val mealPlans: MealPlanRepository,
    clock: AppClock,
) : MviViewModel<ChecklistState, ChecklistIntent, ChecklistEffect>(ChecklistState()) {
    init {
        val today = clock.today()
        launchSafe { mealPlans.ensureWeekPlanned(today) }
        mealPlans.observeDays(today, today)
            .onEach { days -> reduce { copy(day = days.firstOrNull()) } }
            .catch { onError(it) }
            .launchIn(viewModelScope)
        mealPlans.observeStreak(today).onEach { reduce { copy(streak = it) } }.catch { onError(it) }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: ChecklistIntent) {
        when (intent) {
            is ChecklistIntent.Toggle -> launchSafe {
                val day = currentState.day
                val completesDay = intent.checked && day != null && day.checkedMeals == day.plannedMeals - 1
                mealPlans.setChecked(intent.slot.date, intent.slot.mealType, intent.checked)
                if (completesDay) sendEffect(ChecklistEffect.DayCompleted)
            }
            is ChecklistIntent.ShowInfo -> reduce { copy(infoItemId = intent.itemId) }
            ChecklistIntent.DismissInfo -> reduce { copy(infoItemId = null) }
            is ChecklistIntent.Swap -> {
                reduce { copy(infoItemId = null) }
                sendEffect(ChecklistEffect.OpenSwap(intent.itemId))
            }
            ChecklistIntent.Back -> sendEffect(ChecklistEffect.Close)
        }
    }
}

// ------------------------------------------------------------------ Swap

data class SwapState(
    val current: PlannedItem? = null,
    val candidates: ImmutableList<SwapCandidate> = persistentListOf(),
    val selectedFoodId: String? = null,
    val loaded: Boolean = false,
    val saving: Boolean = false,
    /** Candidate whose info sheet is open. */
    val infoFoodId: String? = null,
) : UiState {
    val infoCandidate: SwapCandidate? get() = candidates.firstOrNull { it.food.id == infoFoodId }
}

sealed interface SwapIntent : UiIntent {
    data class Select(val foodId: String) : SwapIntent
    data class ShowInfo(val foodId: String) : SwapIntent
    data object DismissInfo : SwapIntent
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
            is SwapIntent.ShowInfo -> reduce { copy(infoFoodId = intent.foodId) }
            SwapIntent.DismissInfo -> reduce { copy(infoFoodId = null) }
            SwapIntent.Back -> sendEffect(SwapEffect.Close)
            SwapIntent.Confirm -> {
                val candidate = currentState.candidates.firstOrNull { it.food.id == currentState.selectedFoodId } ?: return
                if (currentState.saving) return
                reduce { copy(saving = true) }
                launchSafe {
                    mealPlans.swap(itemId, candidate.food.id, candidate.servings)
                    sendEffect(SwapEffect.Close)
                }
            }
        }
    }
}

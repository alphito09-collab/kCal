package com.alphito.kcal.feature.weight.presentation.mvi

import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.data.repository.CaloriePlanRepository
import com.alphito.kcal.core.data.repository.MealPlanRepository
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.data.repository.WeightRepository
import com.alphito.kcal.core.model.EnergyCalculator
import com.alphito.kcal.core.model.GoalProgress
import com.alphito.kcal.core.model.WeightEntry
import com.alphito.kcal.feature.weight.domain.SummarizeWeights
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.model.WeightUnits
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class WeightState(
    val input: String = "",
    val current: Double? = null,
    val previous: Double? = null,
    val change: Double? = null,
    val recent: ImmutableList<WeightEntry> = persistentListOf(),
    val justSaved: Boolean = false,
    val weightInLb: Boolean = false,
    /** Progress towards the profile's target weight; null for maintenance or no target. */
    val goal: GoalProgress? = null,
) : UiState

sealed interface WeightIntent : UiIntent {
    data class Input(val value: String) : WeightIntent
    data class ChangeUnit(val lb: Boolean) : WeightIntent
    data object Save : WeightIntent
    data object Back : WeightIntent
}

sealed interface WeightEffect : UiEffect { data object Close : WeightEffect }

class WeightViewModel(
    private val weights: WeightRepository,
    private val preferences: PreferencesRepository,
    users: UserRepository,
    caloriePlans: CaloriePlanRepository,
    private val mealPlans: MealPlanRepository,
    private val clock: AppClock,
) : MviViewModel<WeightState, WeightIntent, WeightEffect>(WeightState()) {

    private var inputTouched = false

    init {
        combine(
            weights.entries,
            preferences.preferences.map { it.weightInLb }.distinctUntilChanged(),
            users.profile,
            caloriePlans.averageSteps,
        ) { entries, inLb, profile, avgSteps ->
            val summary = SummarizeWeights(entries)
            Triple(summary, inLb, profile?.let { EnergyCalculator.goalProgress(it, summary.start, avgSteps) })
        }.onEach { (s, inLb, goal) ->
            reduce {
                copy(
                    current = s.current, previous = s.previous, change = s.change, recent = s.recent.toImmutableList(),
                    weightInLb = inLb, goal = goal,
                    // Input is shown in the user's unit; values are always stored in kg.
                    input = if (inputTouched && inLb == weightInLb) input else s.current?.let { WeightUnits.display(it, inLb).toString() }.orEmpty(),
                )
            }
        }.catch { onError(it) }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: WeightIntent) {
        when (intent) {
            is WeightIntent.Input -> {
                inputTouched = true
                val cleaned = intent.value.replace(',', '.').filter { it.isDigit() || it == '.' }.take(5)
                reduce { copy(input = cleaned, justSaved = false) }
            }
            is WeightIntent.ChangeUnit -> {
                val typedKg = currentState.input.toDoubleOrNull()?.let { WeightUnits.toKg(it, currentState.weightInLb) }
                reduce { copy(weightInLb = intent.lb, input = typedKg?.let { WeightUnits.display(it, intent.lb).toString() }.orEmpty()) }
                launchSafe { preferences.setWeightInLb(intent.lb) }
            }
            WeightIntent.Save -> {
                val kg = currentState.input.toDoubleOrNull()
                    ?.let { WeightUnits.toKg(it, currentState.weightInLb) }
                    ?.takeIf { it in 30.0..300.0 } ?: return
                launchSafe {
                    weights.log(clock.today(), kg) // also updates the profile weight
                    // Resizes the rest of the week's portions if the new weight moved the target noticeably.
                    mealPlans.ensureWeekPlanned(clock.today())
                    inputTouched = false
                    reduce { copy(justSaved = true) }
                    delay(1_600)
                    reduce { copy(justSaved = false) }
                }
            }
            WeightIntent.Back -> sendEffect(WeightEffect.Close)
        }
    }
}

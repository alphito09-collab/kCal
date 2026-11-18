package com.alphito.kcal.feature.water.presentation.mvi

import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.model.WaterDay
import com.alphito.kcal.feature.water.domain.WaterRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class WaterState(
    val day: WaterDay = WaterDay(java.time.LocalDate.MIN, 0, WaterDay.DEFAULT_GOAL_ML),
    val selectedAmount: Int = 250,
    val goalInput: String = WaterDay.DEFAULT_GOAL_ML.toString(),
    val editingGoal: Boolean = false,
) : UiState

sealed interface WaterIntent : UiIntent {
    data class SelectAmount(val ml: Int) : WaterIntent
    data object Add : WaterIntent
    data object ToggleEditGoal : WaterIntent
    data class GoalInput(val value: String) : WaterIntent
    data object Back : WaterIntent
}

sealed interface WaterEffect : UiEffect { data object Close : WaterEffect }

class WaterViewModel(
    private val repository: WaterRepository,
    private val clock: AppClock,
) : MviViewModel<WaterState, WaterIntent, WaterEffect>(WaterState()) {

    init {
        repository.observeDay(clock.today())
            .onEach { day -> reduce { copy(day = day, goalInput = if (editingGoal) goalInput else day.goalMl.toString()) } }
            .catch { onError(it) }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: WaterIntent) {
        when (intent) {
            is WaterIntent.SelectAmount -> reduce { copy(selectedAmount = intent.ml) }
            WaterIntent.Add -> launchSafe { repository.add(clock.today(), currentState.selectedAmount) }
            is WaterIntent.GoalInput -> reduce { copy(goalInput = intent.value.filter(Char::isDigit).take(5)) }
            WaterIntent.ToggleEditGoal -> {
                val s = currentState
                if (s.editingGoal) {
                    val goal = s.goalInput.toIntOrNull()?.coerceIn(500, 6_000) ?: s.day.goalMl
                    reduce { copy(editingGoal = false, goalInput = goal.toString()) }
                    launchSafe { repository.setGoal(goal) }
                } else reduce { copy(editingGoal = true) }
            }
            WaterIntent.Back -> sendEffect(WaterEffect.Close)
        }
    }
}

package com.alphito.kcal.feature.onboarding.presentation.mvi

import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.model.Goal
import com.alphito.kcal.core.model.WeightUnits

data class GoalState(
    val goal: Goal = Goal.LoseWeight,
    val targetWeight: String = "",
    val weightInLb: Boolean = false,
) : UiState {
    val needsTarget: Boolean get() = goal != Goal.MaintainWeight
}

sealed interface GoalIntent : UiIntent {
    data class Select(val goal: Goal) : GoalIntent
    data class Target(val value: String) : GoalIntent
    data object Submit : GoalIntent
}

sealed interface GoalEffect : UiEffect { data object Next : GoalEffect }

class GoalViewModel(
    private val users: UserRepository,
    private val preferences: PreferencesRepository,
) : MviViewModel<GoalState, GoalIntent, GoalEffect>(GoalState()) {
    init {
        launchSafe {
            val inLb = preferences.snapshot().weightInLb
            val p = users.current()
            reduce {
                copy(
                    weightInLb = inLb,
                    goal = p?.goal ?: goal,
                    targetWeight = p?.targetWeightKg?.let { WeightUnits.display(it, inLb).toString() }.orEmpty(),
                )
            }
        }
    }

    override fun onIntent(intent: GoalIntent) {
        when (intent) {
            is GoalIntent.Select -> reduce { copy(goal = intent.goal) }
            is GoalIntent.Target -> reduce { copy(targetWeight = intent.value.decimalOnly()) }
            GoalIntent.Submit -> launchSafe {
                val s = currentState
                val targetKg = s.targetWeight.toDoubleOrNull()?.let { WeightUnits.toKg(it, s.weightInLb) }
                users.update { it.copy(goal = s.goal, targetWeightKg = if (s.needsTarget) targetKg else null) }
                sendEffect(GoalEffect.Next)
            }
        }
    }
}

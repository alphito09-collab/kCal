package com.alphito.kcal.feature.onboarding.presentation.mvi

import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.feature.onboarding.domain.RegeneratePlanUseCase
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentSet

data class HealthConditionsState(
    val selected: ImmutableSet<HealthCondition> = persistentSetOf(),
    val noneSelected: Boolean = false,
    val isEdit: Boolean = false,
) : UiState

sealed interface HealthConditionsIntent : UiIntent {
    data class Toggle(val condition: HealthCondition) : HealthConditionsIntent
    data object ToggleNone : HealthConditionsIntent
    data object Submit : HealthConditionsIntent
    data object Back : HealthConditionsIntent
}

sealed interface HealthConditionsEffect : UiEffect {
    data object Next : HealthConditionsEffect
    data object Close : HealthConditionsEffect
}

class HealthConditionsViewModel(
    private val isEdit: Boolean,
    private val users: UserRepository,
    private val regeneratePlan: RegeneratePlanUseCase,
) : MviViewModel<HealthConditionsState, HealthConditionsIntent, HealthConditionsEffect>(HealthConditionsState(isEdit = isEdit)) {

    init {
        launchSafe {
            val conditions = users.current()?.conditions.orEmpty()
            reduce { copy(selected = conditions.toImmutableSet(), noneSelected = isEdit && conditions.isEmpty()) }
        }
    }

    override fun onIntent(intent: HealthConditionsIntent) {
        when (intent) {
            is HealthConditionsIntent.Toggle -> reduce {
                val set = selected.toPersistentSet()
                copy(selected = if (intent.condition in set) set.remove(intent.condition) else set.add(intent.condition), noneSelected = false)
            }
            HealthConditionsIntent.ToggleNone -> reduce { copy(noneSelected = !noneSelected, selected = persistentSetOf()) }
            HealthConditionsIntent.Back -> sendEffect(HealthConditionsEffect.Close)
            HealthConditionsIntent.Submit -> launchSafe {
                users.update { it.copy(conditions = currentState.selected) }
                if (isEdit) {
                    regeneratePlan(); sendEffect(HealthConditionsEffect.Close)
                } else sendEffect(HealthConditionsEffect.Next)
            }
        }
    }
}

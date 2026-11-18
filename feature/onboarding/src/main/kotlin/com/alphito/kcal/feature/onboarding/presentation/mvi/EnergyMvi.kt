package com.alphito.kcal.feature.onboarding.presentation.mvi

import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.EnergyCalculator
import com.alphito.kcal.core.model.Goal
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class EnergyState(val bmr: Int = 0, val tdee: Int = 0, val goal: Goal = Goal.LoseWeight) : UiState

sealed interface EnergyIntent : UiIntent { data object Next : EnergyIntent }

sealed interface EnergyEffect : UiEffect {
    data object OpenDeficitSetup : EnergyEffect
    data object Finished : EnergyEffect
}

class EnergyViewModel(
    users: UserRepository,
    private val completeOnboarding: com.alphito.kcal.feature.onboarding.domain.CompleteOnboardingUseCase,
) : MviViewModel<EnergyState, EnergyIntent, EnergyEffect>(EnergyState()) {

    init {
        users.profile.filterNotNull().onEach { p ->
            val needs = EnergyCalculator.needs(p)
            reduce { copy(bmr = needs.bmr, tdee = needs.tdee, goal = p.goal) }
        }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: EnergyIntent) {
        when (intent) {
            EnergyIntent.Next -> if (currentState.goal == Goal.LoseWeight) {
                sendEffect(EnergyEffect.OpenDeficitSetup)
            } else launchSafe {
                completeOnboarding(); sendEffect(EnergyEffect.Finished)
            }
        }
    }
}

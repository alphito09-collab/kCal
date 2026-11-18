package com.alphito.kcal.feature.onboarding.presentation.mvi

import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.EnergyCalculator
import com.alphito.kcal.core.model.UserProfile
import com.alphito.kcal.feature.onboarding.domain.CompleteOnboardingUseCase

data class DeficitState(
    val addSteps: Boolean = true,
    val totalDeficit: Int = UserProfile.DEFAULT_DEFICIT_KCAL,
    val stepsKcal: Int = UserProfile.DEFAULT_STEPS_KCAL,
    val editingExact: Boolean = false,
    val finishing: Boolean = false,
) : UiState {
    val effectiveSteps: Int get() = if (addSteps) stepsKcal else 0
    val dietKcal: Int get() = (totalDeficit - effectiveSteps).coerceAtLeast(0)
    val stepsFraction: Float get() = if (totalDeficit == 0) 0f else effectiveSteps / totalDeficit.toFloat()
    val stepsEquivalent: Int get() = EnergyCalculator.kcalToSteps(effectiveSteps)
}

sealed interface DeficitIntent : UiIntent {
    data class AddSteps(val value: Boolean) : DeficitIntent
    data object Increase : DeficitIntent
    data object Decrease : DeficitIntent
    data object EditExact : DeficitIntent
    data object DismissExact : DeficitIntent
    data class ExactValue(val kcal: Int) : DeficitIntent
    data object Finish : DeficitIntent
}

sealed interface DeficitEffect : UiEffect { data object Finished : DeficitEffect }

class DeficitViewModel(
    private val users: UserRepository,
    private val completeOnboarding: CompleteOnboardingUseCase,
) : MviViewModel<DeficitState, DeficitIntent, DeficitEffect>(DeficitState()) {

    init {
        launchSafe {
            val p = users.current() ?: return@launchSafe
            reduce { copy(addSteps = p.addStepsToDeficit, totalDeficit = p.dailyDeficitKcal, stepsKcal = p.stepsDeficitKcal) }
        }
    }

    override fun onIntent(intent: DeficitIntent) {
        when (intent) {
            is DeficitIntent.AddSteps -> reduce { copy(addSteps = intent.value) }
            DeficitIntent.Increase -> reduce { copy(stepsKcal = (stepsKcal + STEP).coerceAtMost(totalDeficit)) }
            DeficitIntent.Decrease -> reduce { copy(stepsKcal = (stepsKcal - STEP).coerceAtLeast(0)) }
            DeficitIntent.EditExact -> reduce { copy(editingExact = true) }
            DeficitIntent.DismissExact -> reduce { copy(editingExact = false) }
            is DeficitIntent.ExactValue -> reduce { copy(stepsKcal = intent.kcal.coerceIn(0, totalDeficit), editingExact = false) }
            DeficitIntent.Finish -> if (!currentState.finishing) {
                reduce { copy(finishing = true) }
                launchSafe {
                    val s = currentState
                    users.update { it.copy(addStepsToDeficit = s.addSteps, dailyDeficitKcal = s.totalDeficit, stepsDeficitKcal = s.stepsKcal) }
                    completeOnboarding()
                    sendEffect(DeficitEffect.Finished)
                }
            }
        }
    }

    override fun onError(throwable: Throwable) {
        super.onError(throwable)
        reduce { copy(finishing = false) }
    }

    private companion object { const val STEP = 50 }
}

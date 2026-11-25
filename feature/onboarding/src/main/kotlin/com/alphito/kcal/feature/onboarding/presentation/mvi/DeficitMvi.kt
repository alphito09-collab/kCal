package com.alphito.kcal.feature.onboarding.presentation.mvi

import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.CaloriePlan
import com.alphito.kcal.core.model.EnergyCalculator
import com.alphito.kcal.core.model.Goal
import com.alphito.kcal.core.model.UserProfile
import com.alphito.kcal.feature.onboarding.domain.CompleteOnboardingUseCase

data class DeficitState(
    val profile: UserProfile? = null,
    val addSteps: Boolean = true,
    val totalDeficit: Int = UserProfile.DEFAULT_DEFICIT_KCAL,
    val stepsKcal: Int = UserProfile.DEFAULT_STEPS_KCAL,
    val editingExact: Boolean = false,
    val finishing: Boolean = false,
) : UiState {
    /** The split exactly as the calculator will apply it, so this preview matches every other screen. */
    private val plan: CaloriePlan?
        get() = profile?.let {
            EnergyCalculator.plan(
                it.copy(goal = Goal.LoseWeight, addStepsToDeficit = addSteps, dailyDeficitKcal = totalDeficit, stepsDeficitKcal = stepsKcal),
            )
        }
    val effectiveSteps: Int get() = plan?.stepsDeficitKcal ?: 0
    val dietKcal: Int get() = plan?.dietDeficitKcal ?: 0
    val stepsFraction: Float get() = if (totalDeficit == 0) 0f else effectiveSteps / totalDeficit.toFloat()
    /** Total daily steps: the baseline already in TDEE plus the extra steps that burn [effectiveSteps]. */
    val stepsEquivalent: Int get() = EnergyCalculator.stepsForExtraKcal(effectiveSteps)
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
            // The total is personalised from the profile; the user only chooses how much comes from steps.
            val total = EnergyCalculator.recommendedDeficit(p)
            reduce { copy(profile = p, addSteps = p.addStepsToDeficit, totalDeficit = total, stepsKcal = p.stepsDeficitKcal.coerceIn(0, total)) }
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

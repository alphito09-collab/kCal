package com.alphito.kcal.feature.dashboard.presentation.mvi

import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.FeatureFlags
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.model.MealSlot
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.dashboard.domain.ObserveDashboardUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class DashboardState(
    val name: String = "",
    val eatenKcal: Int = 0,
    val targetKcal: Int = 0,
    val stepsKcal: Int = 0,
    val streakDays: Int = 0,
    val nextMeal: MealSlot? = null,
    val loaded: Boolean = false,
    val showFreeTrial: Boolean = false,
) : UiState {
    val ringProgress: Float get() = if (targetKcal <= 0) 0f else eatenKcal / targetKcal.toFloat()
}

sealed interface DashboardIntent : UiIntent {
    data class Open(val route: Route) : DashboardIntent
    data class CheckMeal(val slot: MealSlot) : DashboardIntent
    data object DismissTrial : DashboardIntent
    data object GetPremium : DashboardIntent
}

sealed interface DashboardEffect : UiEffect {
    data class Navigate(val route: Route) : DashboardEffect
}

class DashboardViewModel(
    private val observeDashboard: ObserveDashboardUseCase,
    private val preferences: PreferencesRepository,
) : MviViewModel<DashboardState, DashboardIntent, DashboardEffect>(DashboardState()) {

    private var trialScheduled = false

    init {
        launchSafe { observeDashboard.ensurePlan() }
        observeDashboard()
            .onEach { s ->
                reduce {
                    copy(
                        name = s.firstName, eatenKcal = s.eatenKcal, targetKcal = s.targetKcal, stepsKcal = s.stepsKcal,
                        streakDays = s.streakDays, nextMeal = s.nextMeal, loaded = true,
                    )
                }
                if (FeatureFlags.PREMIUM_ENABLED && !s.isPremium && !s.trialPromptSeen && !trialScheduled) scheduleTrialPrompt()
            }
            .catch { onError(it) }
            .launchIn(viewModelScope)
    }

    private fun scheduleTrialPrompt() {
        trialScheduled = true
        launchSafe {
            delay(TRIAL_DELAY_MS) // let the dashboard settle (ring + counters) before interrupting
            reduce { copy(showFreeTrial = true) }
        }
    }

    override fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.Open -> sendEffect(DashboardEffect.Navigate(intent.route))
            is DashboardIntent.CheckMeal -> launchSafe { observeDashboard.check(intent.slot) }
            DashboardIntent.DismissTrial -> dismissTrial()
            DashboardIntent.GetPremium -> {
                dismissTrial(); sendEffect(DashboardEffect.Navigate(Route.PremiumIntro()))
            }
        }
    }

    private fun dismissTrial() {
        reduce { copy(showFreeTrial = false) }
        launchSafe { preferences.markFreeTrialPromptSeen() }
    }

    private companion object { const val TRIAL_DELAY_MS = 1_400L }
}

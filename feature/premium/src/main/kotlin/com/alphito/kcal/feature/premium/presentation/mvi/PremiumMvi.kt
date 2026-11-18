package com.alphito.kcal.feature.premium.presentation.mvi

import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.model.PremiumPlan
import com.alphito.kcal.feature.premium.domain.SubscriptionGateway
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class PremiumState(
    val selected: PremiumPlan = PremiumPlan.Monthly,
    val active: PremiumPlan? = null,
    val purchasing: Boolean = false,
) : UiState

sealed interface PremiumIntent : UiIntent {
    data class Select(val plan: PremiumPlan) : PremiumIntent
    data object Purchase : PremiumIntent
    data object Back : PremiumIntent
}

sealed interface PremiumEffect : UiEffect {
    data object Purchased : PremiumEffect
    data object Close : PremiumEffect
}

class PremiumViewModel(
    preferences: PreferencesRepository,
    private val gateway: SubscriptionGateway,
) : MviViewModel<PremiumState, PremiumIntent, PremiumEffect>(PremiumState()) {

    init {
        preferences.preferences.onEach { prefs ->
            reduce { copy(active = prefs.subscription.activePlan, selected = prefs.subscription.activePlan ?: selected) }
        }.catch { onError(it) }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: PremiumIntent) {
        when (intent) {
            is PremiumIntent.Select -> reduce { copy(selected = intent.plan) }
            PremiumIntent.Back -> sendEffect(PremiumEffect.Close)
            PremiumIntent.Purchase -> {
                if (currentState.purchasing || currentState.active == currentState.selected) return
                reduce { copy(purchasing = true) }
                launchSafe {
                    val ok = gateway.purchase(currentState.selected)
                    reduce { copy(purchasing = false) }
                    if (ok) sendEffect(PremiumEffect.Purchased)
                }
            }
        }
    }

    override fun onError(throwable: Throwable) {
        super.onError(throwable)
        reduce { copy(purchasing = false) }
    }
}

package com.alphito.kcal.feature.premium.presentation.mvi

import android.app.Activity
import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.billing.BillingRepository
import com.alphito.kcal.core.billing.PurchaseOutcome
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.model.PremiumPlan
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class PremiumState(
    val selected: PremiumPlan = PremiumPlan.Monthly,
    val active: PremiumPlan? = null,
    val purchasing: Boolean = false,
    /** Google Play's localized prices ("PKR 899.00"); empty until Play answers → bundled fallback. */
    val prices: ImmutableMap<PremiumPlan, String> = persistentMapOf(),
) : UiState

sealed interface PremiumIntent : UiIntent {
    data class Select(val plan: PremiumPlan) : PremiumIntent
    data object Purchase : PremiumIntent
    data object Back : PremiumIntent
}

sealed interface PremiumEffect : UiEffect {
    /** The screen launches the Play sheet with its Activity (never stored in the ViewModel). */
    data class StartPurchase(val plan: PremiumPlan) : PremiumEffect
    data object Purchased : PremiumEffect
    data object Pending : PremiumEffect
    data object Failed : PremiumEffect
    data object Close : PremiumEffect
}

class PremiumViewModel(
    preferences: PreferencesRepository,
    private val billing: BillingRepository,
) : MviViewModel<PremiumState, PremiumIntent, PremiumEffect>(PremiumState()) {

    init {
        preferences.preferences.onEach { prefs ->
            reduce { copy(active = prefs.subscription.activePlan, selected = prefs.subscription.activePlan ?: selected) }
        }.catch { onError(it) }.launchIn(viewModelScope)
        billing.offers.onEach { offers ->
            reduce { copy(prices = offers.mapValues { it.value.formattedPrice }.toImmutableMap()) }
        }.launchIn(viewModelScope)
        billing.outcomes.onEach { outcome ->
            reduce { copy(purchasing = false) }
            when (outcome) {
                is PurchaseOutcome.Active -> sendEffect(PremiumEffect.Purchased)
                PurchaseOutcome.Pending -> sendEffect(PremiumEffect.Pending)
                is PurchaseOutcome.Failed -> sendEffect(PremiumEffect.Failed)
                PurchaseOutcome.Cancelled -> Unit
            }
        }.launchIn(viewModelScope)
        launchSafe { billing.refresh() }
    }

    override fun onIntent(intent: PremiumIntent) {
        when (intent) {
            is PremiumIntent.Select -> reduce { copy(selected = intent.plan) }
            PremiumIntent.Back -> sendEffect(PremiumEffect.Close)
            PremiumIntent.Purchase -> {
                if (currentState.purchasing || currentState.active == currentState.selected) return
                reduce { copy(purchasing = true) }
                sendEffect(PremiumEffect.StartPurchase(currentState.selected))
            }
        }
    }

    fun launchPurchase(activity: Activity, plan: PremiumPlan) = launchSafe {
        if (!billing.launchPurchase(activity, plan)) {
            reduce { copy(purchasing = false) }
        }
    }

    override fun onError(throwable: Throwable) {
        super.onError(throwable)
        reduce { copy(purchasing = false) }
    }
}

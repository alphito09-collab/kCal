package com.alphito.kcal.feature.onboarding.presentation.mvi

import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.BudgetTier
import com.alphito.kcal.core.model.UserProfile
import com.alphito.kcal.feature.onboarding.domain.RegeneratePlanUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class RegionBudgetState(
    val region: String = UserProfile.DEFAULT_REGION,
    val budget: BudgetTier = BudgetTier.Medium,
    val showRegionPicker: Boolean = false,
    val isEdit: Boolean = false,
    val regions: ImmutableList<String> = SUPPORTED_REGIONS,
) : UiState {
    companion object {
        /** The bundled catalogue is Pakistani; cities only tune availability and pricing. */
        val SUPPORTED_REGIONS = persistentListOf(
            "Pakistan — Lahore", "Pakistan — Karachi", "Pakistan — Islamabad",
            "Pakistan — Rawalpindi", "Pakistan — Faisalabad", "Pakistan — Peshawar",
        )
    }
}

sealed interface RegionBudgetIntent : UiIntent {
    data object OpenRegionPicker : RegionBudgetIntent
    data object DismissRegionPicker : RegionBudgetIntent
    data class RegionSelected(val region: String) : RegionBudgetIntent
    data class BudgetSelected(val budget: BudgetTier) : RegionBudgetIntent
    data object Submit : RegionBudgetIntent
    data object Back : RegionBudgetIntent
}

sealed interface RegionBudgetEffect : UiEffect {
    data object Next : RegionBudgetEffect
    data object Close : RegionBudgetEffect
}

class RegionBudgetViewModel(
    private val isEdit: Boolean,
    private val users: UserRepository,
    private val regeneratePlan: RegeneratePlanUseCase,
) : MviViewModel<RegionBudgetState, RegionBudgetIntent, RegionBudgetEffect>(RegionBudgetState(isEdit = isEdit)) {

    init {
        launchSafe {
            val p = users.current() ?: return@launchSafe
            reduce { copy(region = p.region, budget = p.budget) }
        }
    }

    override fun onIntent(intent: RegionBudgetIntent) {
        when (intent) {
            RegionBudgetIntent.OpenRegionPicker -> reduce { copy(showRegionPicker = true) }
            RegionBudgetIntent.DismissRegionPicker -> reduce { copy(showRegionPicker = false) }
            is RegionBudgetIntent.RegionSelected -> reduce { copy(region = intent.region, showRegionPicker = false) }
            is RegionBudgetIntent.BudgetSelected -> reduce { copy(budget = intent.budget) }
            RegionBudgetIntent.Back -> sendEffect(RegionBudgetEffect.Close)
            RegionBudgetIntent.Submit -> launchSafe {
                val s = currentState
                users.update { it.copy(region = s.region, budget = s.budget) }
                if (isEdit) {
                    regeneratePlan(); sendEffect(RegionBudgetEffect.Close)
                } else sendEffect(RegionBudgetEffect.Next)
            }
        }
    }
}

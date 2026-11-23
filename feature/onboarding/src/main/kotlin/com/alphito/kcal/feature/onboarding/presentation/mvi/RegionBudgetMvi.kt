package com.alphito.kcal.feature.onboarding.presentation.mvi

import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.BudgetTier
import com.alphito.kcal.core.model.Regions
import kotlinx.collections.immutable.toImmutableList
import java.util.Locale
import com.alphito.kcal.feature.onboarding.domain.RegeneratePlanUseCase
import kotlinx.collections.immutable.ImmutableList

data class RegionBudgetState(
    /** ISO country code. */
    val region: String = Regions.DEFAULT_COUNTRY,
    val budget: BudgetTier = BudgetTier.Medium,
    val showRegionPicker: Boolean = false,
    val isEdit: Boolean = false,
    val query: String = "",
    val regions: ImmutableList<String> = Regions.supportedCountries.toImmutableList(),
) : UiState

sealed interface RegionBudgetIntent : UiIntent {
    data object OpenRegionPicker : RegionBudgetIntent
    data object DismissRegionPicker : RegionBudgetIntent
    data class RegionSelected(val region: String) : RegionBudgetIntent
    data class Search(val query: String) : RegionBudgetIntent
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
    deviceCountry: String = Locale.getDefault().country,
) : MviViewModel<RegionBudgetState, RegionBudgetIntent, RegionBudgetEffect>(
    // First run: preselect the phone's country when we have a cuisine for it.
    RegionBudgetState(isEdit = isEdit, region = Regions.normalize(deviceCountry)),
) {

    init {
        launchSafe {
            val p = users.current() ?: return@launchSafe
            // A brand-new profile still carries the default; keep the device guess in that case.
            if (isEdit || p.region != Regions.DEFAULT_COUNTRY) reduce { copy(region = p.region) }
            reduce { copy(budget = p.budget) }
        }
    }

    override fun onIntent(intent: RegionBudgetIntent) {
        when (intent) {
            RegionBudgetIntent.OpenRegionPicker -> reduce { copy(showRegionPicker = true, query = "") }
            RegionBudgetIntent.DismissRegionPicker -> reduce { copy(showRegionPicker = false) }
            is RegionBudgetIntent.RegionSelected -> reduce { copy(region = intent.region, showRegionPicker = false) }
            is RegionBudgetIntent.Search -> reduce { copy(query = intent.query.take(40)) }
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

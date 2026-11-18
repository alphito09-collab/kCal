package com.alphito.kcal.feature.progress.presentation.mvi

import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.data.repository.MealPlanRepository
import com.alphito.kcal.core.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.alphito.kcal.core.data.repository.StepsRepository
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.data.repository.WeightRepository
import com.alphito.kcal.feature.progress.domain.PeriodStats
import com.alphito.kcal.feature.progress.domain.ProgressCalculator
import com.alphito.kcal.feature.progress.domain.ProgressPeriod
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate

data class ProgressState(
    val period: ProgressPeriod = ProgressPeriod.Weekly,
    val stats: ImmutableList<PeriodStats> = persistentListOf(),
    val selected: Int = ProgressCalculator.BUCKETS - 1,
    val today: LocalDate = LocalDate.MIN,
    val weightInLb: Boolean = false,
) : UiState {
    val current: PeriodStats? get() = stats.getOrNull(selected)
    val hasWeight: Boolean get() = stats.any { it.weightKg != null }
}

sealed interface ProgressIntent : UiIntent {
    data class SelectPeriod(val period: ProgressPeriod) : ProgressIntent
    data class SelectBucket(val index: Int) : ProgressIntent
}

sealed interface ProgressEffect : UiEffect

class ProgressViewModel(
    users: UserRepository,
    weights: WeightRepository,
    steps: StepsRepository,
    mealPlans: MealPlanRepository,
    preferences: PreferencesRepository,
    clock: AppClock,
    dispatchers: DispatcherProvider,
) : MviViewModel<ProgressState, ProgressIntent, ProgressEffect>(ProgressState()) {

    private val period = MutableStateFlow(ProgressPeriod.Weekly)

    init {
        val today = clock.today()
        val from = today.minusMonths(6).withDayOfMonth(1)
        reduce { copy(today = today) }
        combine(
            period, users.profile, weights.entries, steps.observeRange(from, today), mealPlans.observeDays(from, today),
        ) { p, profile, w, s, d ->
            p to ProgressCalculator.stats(p, today, profile, w, s, d)
        }
            .flowOn(dispatchers.default) // bucketing six months of rows stays off the main thread
            .onEach { (p, stats) -> reduce { copy(period = p, stats = stats.toImmutableList()) } }
            .catch { onError(it) }
            .launchIn(viewModelScope)
        preferences.preferences.map { it.weightInLb }.distinctUntilChanged()
            .onEach { inLb -> reduce { copy(weightInLb = inLb) } }
            .catch { onError(it) }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: ProgressIntent) {
        when (intent) {
            is ProgressIntent.SelectPeriod -> {
                reduce { copy(selected = ProgressCalculator.BUCKETS - 1) }
                period.value = intent.period
            }
            is ProgressIntent.SelectBucket -> reduce { copy(selected = intent.index) }
        }
    }
}

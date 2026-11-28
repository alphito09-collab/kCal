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
import com.alphito.kcal.core.data.repository.StepsRepository
import com.alphito.kcal.core.data.repository.TrackingStartRepository
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate

data class ProgressState(
    val period: ProgressPeriod = ProgressPeriod.Weekly,
    /** Every started period, index 0 … current. */
    val stats: ImmutableList<PeriodStats> = persistentListOf(),
    /** Absolute index of the selected period. */
    val selected: Int = 0,
    /** Absolute index of the last visible chart slot. */
    val windowEnd: Int = ProgressCalculator.SLOTS - 1,
    val startDate: LocalDate = LocalDate.MIN,
    val today: LocalDate = LocalDate.MIN,
    val hasWeighIns: Boolean = false,
    val loaded: Boolean = false,
    val weightInLb: Boolean = false,
) : UiState {
    val currentIndex: Int get() = stats.lastIndex.coerceAtLeast(0)
    /** Period indices of the six visible slots; those past [currentIndex] haven't started (label only, no point). */
    val slots: ImmutableList<Int> get() = ProgressCalculator.window(windowEnd).toList().toImmutableList()
    val current: PeriodStats? get() = stats.getOrNull(selected)
    val canPageBack: Boolean get() = ProgressCalculator.window(windowEnd).first > 0
    val canPageForward: Boolean get() = windowEnd < ProgressCalculator.defaultWindowEnd(currentIndex)
}

sealed interface ProgressIntent : UiIntent {
    data class SelectPeriod(val period: ProgressPeriod) : ProgressIntent
    /** Absolute period index; periods that haven't started can't be selected. */
    data class SelectBucket(val index: Int) : ProgressIntent
    data object PageBack : ProgressIntent
    data object PageForward : ProgressIntent
}

sealed interface ProgressEffect : UiEffect

class ProgressViewModel(
    users: UserRepository,
    weights: WeightRepository,
    steps: StepsRepository,
    mealPlans: MealPlanRepository,
    preferences: PreferencesRepository,
    trackingStart: TrackingStartRepository,
    clock: AppClock,
    dispatchers: DispatcherProvider,
) : MviViewModel<ProgressState, ProgressIntent, ProgressEffect>(ProgressState()) {

    private val period = MutableStateFlow(ProgressPeriod.Weekly)

    init {
        val today = clock.today()
        launchSafe {
            // The start date never moves, so it's read once and bounds every query.
            val start = minOf(trackingStart.startDate.first(), today)
            reduce { copy(startDate = start, today = today) }
            combine(
                period, users.profile, weights.entries, steps.observeRange(start, today), mealPlans.observeDays(start, today),
            ) { p, profile, w, s, d ->
                Triple(p, ProgressCalculator.stats(p, start, today, profile, w, s, d), w.isNotEmpty())
            }
                .flowOn(dispatchers.default)
                .onEach { (p, stats, hasWeighIns) ->
                    reduce {
                        val current = stats.lastIndex.coerceAtLeast(0)
                        val reset = !loaded || p != period
                        copy(
                            period = p, stats = stats.toImmutableList(), hasWeighIns = hasWeighIns, loaded = true,
                            // A new period type opens on its current period; data updates keep the user's place.
                            selected = if (reset) current else selected.coerceIn(0, current),
                            windowEnd = if (reset) ProgressCalculator.defaultWindowEnd(current) else windowEnd,
                        )
                    }
                }
                .catch { onError(it) }
                .launchIn(viewModelScope)
        }
        preferences.preferences.map { it.weightInLb }.distinctUntilChanged()
            .onEach { inLb -> reduce { copy(weightInLb = inLb) } }
            .catch { onError(it) }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: ProgressIntent) {
        when (intent) {
            is ProgressIntent.SelectPeriod -> period.value = intent.period
            is ProgressIntent.SelectBucket -> if (intent.index in currentState.stats.indices) reduce { copy(selected = intent.index) }
            ProgressIntent.PageBack -> if (currentState.canPageBack) reduce {
                val end = (windowEnd - ProgressCalculator.SLOTS).coerceAtLeast(ProgressCalculator.SLOTS - 1)
                copy(windowEnd = end, selected = minOf(end, currentIndex))
            }
            ProgressIntent.PageForward -> if (currentState.canPageForward) reduce {
                val end = (windowEnd + ProgressCalculator.SLOTS).coerceAtMost(ProgressCalculator.defaultWindowEnd(currentIndex))
                copy(windowEnd = end, selected = minOf(end, currentIndex))
            }
        }
    }
}

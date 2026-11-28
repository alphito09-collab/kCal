package com.alphito.kcal.feature.steps.presentation.mvi

import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.data.repository.StepsRepository
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.EnergyCalculator
import com.alphito.kcal.core.model.StepsDay
import com.alphito.kcal.feature.steps.domain.HistoryRange
import com.alphito.kcal.feature.steps.domain.StepTrackingController
import com.alphito.kcal.feature.steps.domain.TrackingStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate

data class StepsState(
    val todaySteps: Int = 0,
    val goal: Int = EnergyCalculator.DEFAULT_STEP_GOAL,
    val status: TrackingStatus = TrackingStatus.Idle,
    val permissionDenied: Boolean = false,
    val range: HistoryRange = HistoryRange.Months,
    val history: ImmutableList<StepsDay> = persistentListOf(),
    val selectedBar: Int = -1,
    val today: LocalDate = LocalDate.MIN,
) : UiState {
    val todayKcal: Int get() = EnergyCalculator.stepsToKcal(todaySteps)
    val progress: Float get() = (todaySteps / goal.toFloat()).coerceIn(0f, 1f)
}

sealed interface StepsIntent : UiIntent {
    data object ToggleTracking : StepsIntent
    data class PermissionsResult(val activityGranted: Boolean) : StepsIntent
    data class SelectRange(val range: HistoryRange) : StepsIntent
    data class SelectBar(val index: Int) : StepsIntent
    data object Back : StepsIntent
}

sealed interface StepsEffect : UiEffect {
    data object RequestPermissions : StepsEffect
    data object Close : StepsEffect
}

class StepsViewModel(
    private val steps: StepsRepository,
    private val controller: StepTrackingController,
    users: UserRepository,
    clock: AppClock,
) : MviViewModel<StepsState, StepsIntent, StepsEffect>(StepsState()) {

    init {
        val today = clock.today()
        reduce { copy(today = today) }
        // A plan that covers part of the deficit with steps raises the goal to match it.
        users.profile.map { p -> p?.let { EnergyCalculator.plan(it).stepGoal } ?: EnergyCalculator.DEFAULT_STEP_GOAL }
            .onEach { reduce { copy(goal = it) } }
            .catch { onError(it) }.launchIn(viewModelScope)
        combine(steps.observeDay(today), controller.status, steps.observeRange(today.minusMonths(6), today)) { count, status, history ->
            Triple(count, status, history)
        }.onEach { (count, status, history) ->
            reduce { copy(todaySteps = count, status = status, history = history.toImmutableList()) }
        }.catch { onError(it) }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: StepsIntent) {
        when (intent) {
            StepsIntent.ToggleTracking -> when (currentState.status) {
                TrackingStatus.Tracking -> controller.stop()
                TrackingStatus.Idle -> sendEffect(StepsEffect.RequestPermissions)
                TrackingStatus.NoSensor -> Unit
            }
            is StepsIntent.PermissionsResult -> {
                reduce { copy(permissionDenied = !intent.activityGranted) }
                if (intent.activityGranted) controller.start() // also remembers "tracking on"
            }
            is StepsIntent.SelectRange -> reduce { copy(range = intent.range, selectedBar = -1) }
            is StepsIntent.SelectBar -> reduce { copy(selectedBar = intent.index) }
            StepsIntent.Back -> sendEffect(StepsEffect.Close)
        }
    }
}

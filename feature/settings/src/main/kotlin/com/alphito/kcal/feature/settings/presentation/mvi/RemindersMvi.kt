package com.alphito.kcal.feature.settings.presentation.mvi

import androidx.lifecycle.viewModelScope
import com.alphito.kcal.core.common.mvi.MviViewModel
import com.alphito.kcal.core.common.mvi.UiEffect
import com.alphito.kcal.core.common.mvi.UiIntent
import com.alphito.kcal.core.common.mvi.UiState
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.model.ReminderType
import com.alphito.kcal.core.notifications.ReminderScheduler
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class RemindersState(val enabled: ImmutableSet<ReminderType> = persistentSetOf()) : UiState

sealed interface RemindersIntent : UiIntent {
    data class Toggle(val type: ReminderType, val enabled: Boolean) : RemindersIntent
    data class PermissionResult(val type: ReminderType, val granted: Boolean) : RemindersIntent
    data object Back : RemindersIntent
}

sealed interface RemindersEffect : UiEffect {
    /** Ask for POST_NOTIFICATIONS (Android 13+) before turning a reminder on. */
    data class RequestPermission(val type: ReminderType) : RemindersEffect
    data object PermissionDenied : RemindersEffect
    data object Close : RemindersEffect
}

class RemindersViewModel(
    private val preferences: PreferencesRepository,
    private val scheduler: ReminderScheduler,
) : MviViewModel<RemindersState, RemindersIntent, RemindersEffect>(RemindersState()) {

    init {
        preferences.preferences.onEach { reduce { copy(enabled = it.reminders.toImmutableSet()) } }
            .catch { onError(it) }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: RemindersIntent) {
        when (intent) {
            is RemindersIntent.Toggle ->
                if (intent.enabled) sendEffect(RemindersEffect.RequestPermission(intent.type))
                else launchSafe { preferences.setReminder(intent.type, false); scheduler.cancel(intent.type) }
            is RemindersIntent.PermissionResult ->
                if (intent.granted) launchSafe { preferences.setReminder(intent.type, true); scheduler.schedule(intent.type) }
                else sendEffect(RemindersEffect.PermissionDenied)
            RemindersIntent.Back -> sendEffect(RemindersEffect.Close)
        }
    }
}

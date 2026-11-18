package com.alphito.kcal.core.common.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Unidirectional MVI store.
 *
 *  Intent ──▶ [onIntent] ──▶ reduce ──▶ [state] (StateFlow, conflated, survives config change)
 *                         └─▶ [sendEffect] ──▶ [effects] (Channel – delivered exactly once)
 *
 * - State is a single immutable snapshot, so Compose recomposes only what changed.
 * - Effects use a buffered [Channel] so one-shot events are never replayed after rotation
 *   and are not lost while the UI is in the background (collected with `repeatOnLifecycle`).
 * - Every coroutine launched through [launchSafe] is guarded by a [CoroutineExceptionHandler]
 *   so a failing repository call can never crash the process.
 */
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(capacity = Channel.BUFFERED, onBufferOverflow = BufferOverflow.SUSPEND)
    val effects: Flow<E> = _effects.receiveAsFlow()

    protected val currentState: S get() = _state.value

    abstract fun onIntent(intent: I)

    protected fun reduce(reducer: S.() -> S) = _state.update(reducer)

    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effects.send(effect) }
    }

    protected open fun onError(throwable: Throwable) {
        Log.e(this::class.simpleName, "Unhandled error", throwable)
    }

    private val errorHandler = CoroutineExceptionHandler { _, throwable -> onError(throwable) }

    protected fun launchSafe(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch(errorHandler, block = block)
}

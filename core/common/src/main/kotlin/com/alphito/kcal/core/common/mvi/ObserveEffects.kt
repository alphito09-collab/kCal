package com.alphito.kcal.core.common.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.skydoves.compose.stability.runtime.IgnoreStabilityReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Collects one-shot effects only while the screen is at least STARTED, on
 * `Dispatchers.Main.immediate` so an effect emitted during a lifecycle transition is never dropped.
 */
@IgnoreStabilityReport // a Flow is an interface by design; this helper never restarts or skips
@Composable
@NonRestartableComposable // pure side-effect, like LaunchedEffect: no restart group needed
fun <E> ObserveEffects(effects: Flow<E>, onEffect: suspend (E) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect = rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                effects.collect { currentOnEffect.value(it) }
            }
        }
    }
}

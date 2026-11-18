package com.alphito.kcal.core.common.mvi

import androidx.compose.runtime.Immutable

/** Marker for a screen's single immutable render state. */
@Immutable
interface UiState

/** Marker for user/system intentions flowing into a ViewModel. */
interface UiIntent

/** Marker for one-shot side effects (navigation, toasts, intents) flowing out of a ViewModel. */
interface UiEffect

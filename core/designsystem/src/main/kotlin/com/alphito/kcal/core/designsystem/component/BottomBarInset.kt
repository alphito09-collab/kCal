package com.alphito.kcal.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Height of the floating bottom bar (including the system nav bar). Tab screens add it to the
 * end of their scroll content so nothing hides behind the bar – without the whole NavDisplay
 * relayouting when the bar slides in/out during navigation.
 */
val LocalBottomBarInset = staticCompositionLocalOf { 0.dp }

@Composable
fun BottomBarSpacer(extra: Dp = 0.dp) {
    Spacer(Modifier.height(LocalBottomBarInset.current + extra))
}

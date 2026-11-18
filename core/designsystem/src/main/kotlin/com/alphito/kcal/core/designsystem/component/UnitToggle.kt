package com.alphito.kcal.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf

/** Compact two-option unit switch ("cm | ft / in", "kg | lb") with the sliding spring thumb. */
@Composable
fun UnitToggle(
    first: String,
    second: String,
    secondSelected: Boolean,
    onSelect: (secondSelected: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.width(132.dp)) {
        SegmentedControl(persistentListOf(first, second), if (secondSelected) 1 else 0, { onSelect(it == 1) }, height = 36.dp)
    }
}

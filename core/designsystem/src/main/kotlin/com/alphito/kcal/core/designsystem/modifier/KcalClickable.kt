package com.alphito.kcal.core.designsystem.modifier

import androidx.compose.foundation.clickable
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import com.alphito.kcal.core.designsystem.theme.KcalShapes

/**
 * The click treatment for everything except the main navigation tabs:
 * a bounded brand ripple, clipped to the component's [shape] so it follows rounded corners
 * and circles and fills the whole item. (Only the bottom tabs / nav rail use a spring squish –
 * never both on the same element.)
 */
@Composable
fun Modifier.kcalClickable(
    shape: Shape = KcalShapes.md,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = this
    .clip(shape)
    .clickable(
        interactionSource = null,
        indication = ripple(),
        enabled = enabled,
        role = role,
        onClickLabel = onClickLabel,
        onClick = onClick,
    )

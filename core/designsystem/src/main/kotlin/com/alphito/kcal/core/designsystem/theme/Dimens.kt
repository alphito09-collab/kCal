package com.alphito.kcal.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** 4-pt spacing scale + the corner radii used in Figma (8 → 24, 999 = pill). */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    /** Horizontal screen gutter used by every 360-wide Figma frame. */
    val screen = 20.dp
}

object KcalShapes {
    val xs = RoundedCornerShape(8.dp)
    val sm = RoundedCornerShape(12.dp)
    val md = RoundedCornerShape(16.dp)
    val lg = RoundedCornerShape(20.dp)
    val card = RoundedCornerShape(22.dp)
    val xl = RoundedCornerShape(24.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val pill = CircleShape
}

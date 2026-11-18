package com.alphito.kcal.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.modifier.pressScale
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class BottomBarItem(val label: String, @DrawableRes val icon: Int)

/**
 * Bottom navigation from Figma: a blush pill highlights the selected tab. Press feedback is a
 * spring squish only (no ripple), so it reads as one gesture together with the sliding pill.
 * The pill is a single element that *slides* between tabs on a spring (offset read in layout),
 * instead of four independent backgrounds fading.
 */
@Composable
fun KcalBottomBar(
    items: ImmutableList<BottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KcalTheme.colors
    Column(modifier.fillMaxWidth().background(colors.background)) {
        KcalDivider(Modifier.padding(horizontal = 20.dp))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .height(52.dp)
                .selectableGroup(),
        ) {
            val slot = maxWidth / items.size
            val pillWidth = slot - 12.dp
            val pillOffset by animateDpAsState(slot * selectedIndex.coerceAtLeast(0) + 6.dp, KcalMotion.dpSpring, label = "nav-pill")
            if (selectedIndex >= 0) {
                Box(
                    Modifier
                        .offset { IntOffset(pillOffset.roundToPx(), 0) }
                        .width(pillWidth)
                        .fillMaxHeight()
                        .background(colors.accentContainer, KcalShapes.md),
                )
            }
            Row(Modifier.fillMaxWidth().fillMaxHeight(), horizontalArrangement = Arrangement.SpaceEvenly) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val tint by animateColorAsState(
                        if (selected) colors.textBrand else colors.textTertiary, KcalMotion.fadeTween(), label = "nav-tint",
                    )
                    val source = remember { MutableInteractionSource() }
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pressScale(source, 0.86f)
                            .selectable(selected, interactionSource = source, indication = null, role = Role.Tab) { onSelect(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(painterResource(item.icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.label,
                            style = if (selected) KcalTheme.typography.labelSmall else KcalTheme.typography.bodySmall,
                            color = tint,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

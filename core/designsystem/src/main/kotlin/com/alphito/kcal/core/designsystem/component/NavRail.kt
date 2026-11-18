package com.alphito.kcal.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.modifier.pressScale
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import kotlinx.collections.immutable.ImmutableList

private val ItemHeight = 72.dp
private val ItemGap = 8.dp

/**
 * Navigation rail for tablets / landscape (≥ 600 dp wide) – the same tabs as [KcalBottomBar],
 * with the blush pill sliding vertically on a spring and spring-only press feedback.
 */
@Composable
fun KcalNavRail(
    items: ImmutableList<BottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KcalTheme.colors
    val pillOffset by animateDpAsState((ItemHeight + ItemGap) * selectedIndex.coerceAtLeast(0), KcalMotion.dpSpring, label = "rail-pill")
    Box(
        modifier
            .fillMaxHeight()
            .width(96.dp)
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.selectableGroup()) {
            if (selectedIndex >= 0) {
                Box(
                    Modifier
                        .offset { IntOffset(0, pillOffset.roundToPx()) }
                        .padding(horizontal = 12.dp)
                        .width(72.dp)
                        .height(ItemHeight)
                        .background(colors.accentContainer, KcalShapes.md),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(ItemGap)) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val tint by animateColorAsState(if (selected) colors.textBrand else colors.textTertiary, KcalMotion.fadeTween(), label = "rail-tint")
                    val source = remember { MutableInteractionSource() }
                    Column(
                        Modifier
                            .width(96.dp)
                            .height(ItemHeight)
                            .pressScale(source, 0.86f)
                            .selectable(selected, interactionSource = source, indication = null, role = Role.Tab) { onSelect(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(painterResource(item.icon), null, tint = tint, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.label, style = if (selected) KcalTheme.typography.labelSmall else KcalTheme.typography.bodySmall,
                            color = tint, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

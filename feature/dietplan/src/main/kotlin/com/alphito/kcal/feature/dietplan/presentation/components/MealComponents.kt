package com.alphito.kcal.feature.dietplan.presentation.components

import com.alphito.kcal.core.designsystem.util.foodPortion
import com.alphito.kcal.core.designsystem.util.foodName
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.component.FoodThumb
import com.alphito.kcal.core.designsystem.component.KcalBadge
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalDivider
import com.alphito.kcal.core.designsystem.component.RadioDot
import com.alphito.kcal.core.designsystem.modifier.kcalClickable
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.motion.kcalSharedElement
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.kcalPriceLabel
import com.alphito.kcal.core.designsystem.util.label
import com.alphito.kcal.core.model.Food
import com.alphito.kcal.core.model.MealSlot
import com.alphito.kcal.core.model.PlannedItem
import com.alphito.kcal.core.navigation.SharedKeys
import com.alphito.kcal.feature.dietplan.R

/** One meal of the day (Breakfast / Lunch …) listing each item with a Swap action. */
@Composable
internal fun MealSlotCard(slot: MealSlot, onSwap: (Long) -> Unit, modifier: Modifier = Modifier) {
    KcalCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        Text(slot.mealType.label(), style = KcalTheme.typography.labelSmall, color = KcalTheme.colors.textBrand)
        slot.items.forEachIndexed { index, item ->
            if (index > 0) KcalDivider(Modifier.padding(vertical = 2.dp))
            PlannedItemRow(item, onSwap)
        }
    }
}

@Composable
private fun PlannedItemRow(item: PlannedItem, onSwap: (Long) -> Unit) {
    val colors = KcalTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        FoodThumb(item.food.imageKey, size = 30.dp, modifier = Modifier.kcalSharedElement(SharedKeys.mealImage(item.id)))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                foodName(item.food), style = KcalTheme.typography.label, color = colors.textPrimary,
                modifier = Modifier.kcalSharedElement(SharedKeys.mealTitle(item.id)),
            )
            Text(foodPortion(item.food), style = KcalTheme.typography.caption, color = colors.textSecondary)
            Text(kcalPriceLabel(item.food.kcal, item.food.pricePkr), style = KcalTheme.typography.caption, color = colors.textSecondary)
        }
        Box(
            Modifier
                .kcalClickable(shape = KcalShapes.pill) { onSwap(item.id) }
                .border(1.dp, colors.textPrimary.copy(alpha = 0.7f), KcalShapes.pill)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(stringResource(R.string.diet_swap), style = KcalTheme.typography.labelSmall, color = colors.textPrimary)
        }
    }
}

/** Substitute option for the swap screen; selected = blush fill + plum border + filled radio. */
@Composable
internal fun SwapOptionCard(food: Food, bestMatch: Boolean, selected: Boolean, onClick: () -> Unit) {
    val colors = KcalTheme.colors
    val bg by animateColorAsState(if (selected) colors.accentContainer else colors.surface, KcalMotion.fadeTween(), label = "swap-bg")
    val border by animateColorAsState(if (selected) colors.primary else colors.outline, KcalMotion.fadeTween(), label = "swap-border")
    Row(
        Modifier
            .fillMaxWidth()
            .kcalClickable(shape = KcalShapes.lg, onClick = onClick)
            .background(bg, KcalShapes.lg)
            .border(BorderStroke(if (selected) 2.dp else 1.5.dp, border), KcalShapes.lg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FoodThumb(food.imageKey, size = 42.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(foodName(food), style = KcalTheme.typography.titleSmall, color = colors.textPrimary)
            if (bestMatch) {
                Spacer(Modifier.height(4.dp))
                KcalBadge(stringResource(R.string.swap_best_match), container = colors.primary, content = colors.onPrimary, uppercase = true)
            }
            Spacer(Modifier.height(4.dp))
            Text(kcalPriceLabel(food.kcal, food.pricePkr), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
        }
        RadioDot(selected)
    }
}

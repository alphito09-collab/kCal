package com.alphito.kcal.feature.dashboard.presentation.components

import com.alphito.kcal.core.designsystem.util.mealTitle
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.chart.ProgressRing
import com.alphito.kcal.core.designsystem.component.AnimatedNumber
import com.alphito.kcal.core.designsystem.component.FoodThumb
import com.alphito.kcal.core.designsystem.component.IconChip
import com.alphito.kcal.core.designsystem.component.KcalCard
import com.alphito.kcal.core.designsystem.component.KcalCheckbox
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.motion.KcalMotion
import com.alphito.kcal.core.designsystem.motion.kcalSharedBounds
import com.alphito.kcal.core.designsystem.motion.kcalSharedElement
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.core.designsystem.util.kcalLabel
import com.alphito.kcal.core.designsystem.util.label
import com.alphito.kcal.core.model.MealSlot
import com.alphito.kcal.core.navigation.SharedKeys
import com.alphito.kcal.feature.dashboard.R

/** "Daily Calorie Tracking" card: animated ring + three stat rows. */
@Composable
internal fun CalorieTrackingCard(
    eatenKcal: Int,
    targetKcal: Int,
    progress: Float,
    stepsKcal: Int,
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    val colors = KcalTheme.colors
    KcalCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 24.dp, end = 20.dp, top = 24.dp, bottom = 24.dp)) {
        Text(stringResource(R.string.dash_daily_tracking), style = KcalTheme.typography.titleMedium, color = colors.textBrand)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(progress = progress, modifier = Modifier.size(110.dp), strokeWidth = 11.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedNumber(eatenKcal, KcalTheme.typography.display.copy(fontSize = KcalTheme.typography.headline.fontSize))
                    Text(stringResource(R.string.dash_kcal_today), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
            Spacer(Modifier.width(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                StatRow(KcalIcons.Steps, stringResource(R.string.dash_steps), stringResource(R.string.dash_kcal_value, KcalFormat.int(stepsKcal)))
                StatRow(KcalIcons.Utensils, stringResource(R.string.dash_eating), stringResource(R.string.dash_kcal_value, KcalFormat.int(eatenKcal)))
                StatRow(KcalIcons.Flame, stringResource(R.string.dash_streak), "🔥 " + stringResource(R.string.dash_streak_value, streakDays))
            }
        }
        if (targetKcal > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.dash_of_target, KcalFormat.int(targetKcal)),
                style = KcalTheme.typography.caption, color = colors.textTertiary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun StatRow(@DrawableRes icon: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconChip(icon, size = 34.dp, iconSize = 18.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = KcalTheme.typography.bodySmall, color = KcalTheme.colors.textSecondary)
            Text(value, style = KcalTheme.typography.titleSmall, color = KcalTheme.colors.textBrand)
        }
    }
}

/** Next unchecked meal. Photo is a shared element into the Meal Checklist; the row cross-fades as meals are ticked. */
@Composable
internal fun NextMealCard(slot: MealSlot?, onCheck: (MealSlot) -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KcalTheme.colors
    KcalCard(
        modifier.fillMaxWidth().kcalSharedBounds(SharedKeys.TODAY_MEAL_CARD),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        onClick = onOpen,
    ) {
        AnimatedContent(
            targetState = slot,
            contentKey = { it?.mealType },
            transitionSpec = {
                (fadeIn(KcalMotion.fadeTween(KcalMotion.DurationMedium)) + slideInVertically { it / 3 }) togetherWith
                    (fadeOut(KcalMotion.fadeTween()) + slideOutVertically { -it / 3 })
            },
            label = "next-meal",
        ) { meal ->
            if (meal == null) {
                Column {
                    Text(stringResource(R.string.dash_all_done), style = KcalTheme.typography.titleSmall, color = colors.textPrimary)
                    Text(stringResource(R.string.dash_all_done_sub), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FoodThumb(
                        meal.leadImageKey, imageUrl = meal.leadImageUrl, emoji = meal.leadEmoji, size = 36.dp,
                        modifier = Modifier.kcalSharedElement(SharedKeys.mealImage(meal.items.first().id)),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.dash_up_next, meal.mealType.label()), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
                        Text(mealTitle(meal), style = KcalTheme.typography.titleSmall.copy(fontSize = KcalTheme.typography.titleMedium.fontSize), color = colors.textPrimary, maxLines = 1)
                        Text(kcalLabel(meal.totalKcal), style = KcalTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    KcalCheckbox(checked = meal.checked, onCheckedChange = { onCheck(meal) })
                }
            }
        }
    }
}

/** Action grid card – the whole card is a container-transform source for its destination screen. */
@Composable
internal fun ActionCard(
    @DrawableRes icon: Int,
    title: String,
    sharedKey: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KcalCard(
        modifier.kcalSharedBounds(SharedKeys.actionCard(sharedKey)),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 24.dp),
        onClick = onClick,
    ) {
        IconChip(icon, size = 42.dp, iconSize = 22.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            title, style = KcalTheme.typography.titleSmall, color = KcalTheme.colors.textPrimary,
            modifier = Modifier.kcalSharedElement(SharedKeys.actionTitle(sharedKey)),
        )
    }
}

@Composable
internal fun ViewAllLink(onClick: () -> Unit) {
    Row(
        Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.alphito.kcal.core.designsystem.component.KcalTextButton(
            stringResource(R.string.dash_view_all), onClick, color = KcalTheme.colors.textBrand,
        )
        Icon(painterResource(KcalIcons.ViewAll), null, tint = KcalTheme.colors.textBrand, modifier = Modifier.size(14.dp))
    }
}

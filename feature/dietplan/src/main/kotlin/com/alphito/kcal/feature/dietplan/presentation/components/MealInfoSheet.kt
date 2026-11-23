package com.alphito.kcal.feature.dietplan.presentation.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alphito.kcal.core.designsystem.component.FoodThumb
import com.alphito.kcal.core.designsystem.component.IconChip
import com.alphito.kcal.core.designsystem.component.KcalBadge
import com.alphito.kcal.core.designsystem.component.KcalButton
import com.alphito.kcal.core.designsystem.component.KcalDivider
import com.alphito.kcal.core.designsystem.component.KcalTextButton
import com.alphito.kcal.core.designsystem.icon.KcalIcons
import com.alphito.kcal.core.designsystem.modifier.kcalClickable
import com.alphito.kcal.core.designsystem.theme.KcalShapes
import com.alphito.kcal.core.designsystem.theme.KcalTheme
import com.alphito.kcal.core.designsystem.util.KcalFormat
import com.alphito.kcal.core.designsystem.util.appLanguage
import com.alphito.kcal.core.designsystem.util.appLocale
import com.alphito.kcal.core.designsystem.util.costLabel
import com.alphito.kcal.core.designsystem.util.foodName
import com.alphito.kcal.core.designsystem.util.gramsText
import com.alphito.kcal.core.designsystem.util.kcalLabel
import com.alphito.kcal.core.designsystem.util.label
import com.alphito.kcal.core.model.Food
import com.alphito.kcal.core.model.Glycemic
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.model.IngredientCategory
import com.alphito.kcal.core.model.MealReason
import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.SwapReason
import com.alphito.kcal.feature.dietplan.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlin.math.roundToInt

/**
 * Body of the meal-info bottom sheet: what the dish is, what's in it and why it's in the plan.
 * Without this guidance people don't trust (or stick with) a plan – so every line is concrete.
 */
@Composable
internal fun ColumnScope.MealInfoContent(
    food: Food,
    servings: Double,
    mealType: MealType,
    reasons: ImmutableList<MealReason>,
    conditions: ImmutableSet<HealthCondition>,
    primaryAction: String,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
        FoodHeader(food, servings, mealType)
        Spacer(Modifier.height(16.dp))
        NutritionRow(food, servings, showGlycemic = HealthCondition.Diabetes in conditions)
        if (reasons.isNotEmpty()) {
            SectionTitle(R.string.info_why_title)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { reasons.forEach { ReasonRow(it) } }
        }
        IngredientsSection(food, servings)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.info_disclaimer), style = KcalTheme.typography.caption, color = KcalTheme.colors.textTertiary)
        Spacer(Modifier.height(16.dp))
    }
    KcalButton(primaryAction, onPrimary, Modifier.fillMaxWidth())
    Spacer(Modifier.height(4.dp))
    KcalTextButton(stringResource(R.string.info_close), onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally))
}

/** Swap-candidate variant: the same dish details plus why it is a good alternative. */
@Composable
internal fun ColumnScope.SwapInfoContent(
    food: Food,
    servings: Double,
    mealType: MealType,
    reasons: ImmutableList<SwapReason>,
    onChoose: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
        FoodHeader(food, servings, mealType)
        Spacer(Modifier.height(16.dp))
        NutritionRow(food, servings, showGlycemic = true)
        if (reasons.isNotEmpty()) {
            SectionTitle(R.string.info_why_alternative)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reasons.forEach { SwapReasonChip(it) }
            }
        }
        IngredientsSection(food, servings)
        Spacer(Modifier.height(16.dp))
    }
    KcalButton(stringResource(R.string.info_choose), onChoose, Modifier.fillMaxWidth())
    Spacer(Modifier.height(4.dp))
    KcalTextButton(stringResource(R.string.info_close), onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally))
}

@Composable
private fun FoodHeader(food: Food, servings: Double, mealType: MealType) {
    val colors = KcalTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        FoodThumb(food.imageKey, imageUrl = food.imageUrl, emoji = food.emoji, size = 64.dp, shape = KcalShapes.md)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(mealType.label(), style = KcalTheme.typography.labelSmall, color = colors.textBrand)
            Text(foodName(food), style = KcalTheme.typography.title, color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                kcalLabel((food.kcal * servings).roundToInt()) + " · " + costLabel(food.cost),
                style = KcalTheme.typography.bodySmall, color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun NutritionRow(food: Food, servings: Double, showGlycemic: Boolean) {
    val locale = appLocale()
    fun g(v: Double) = KcalFormat.int((v * servings).roundToInt(), locale)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NutrientPill(stringResource(R.string.info_protein), g(food.proteinG))
        NutrientPill(stringResource(R.string.info_carbs), g(food.carbsG))
        NutrientPill(stringResource(R.string.info_fat), g(food.fatG))
        NutrientPill(stringResource(R.string.info_fiber), g(food.fiberG))
        if (showGlycemic) {
            KcalBadge(
                stringResource(
                    when (food.glycemic) {
                        Glycemic.Low -> R.string.info_gi_low
                        Glycemic.Medium -> R.string.info_gi_medium
                        Glycemic.High -> R.string.info_gi_high
                    },
                ),
                container = KcalTheme.colors.accentContainer, content = KcalTheme.colors.textBrand, uppercase = false,
            )
        }
    }
}

@Composable
private fun NutrientPill(name: String, grams: String) {
    val colors = KcalTheme.colors
    Column(
        Modifier.background(colors.accentContainer, KcalShapes.sm).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.info_grams, grams), style = KcalTheme.typography.titleSmall, color = colors.textPrimary)
        Text(name, style = KcalTheme.typography.caption, color = colors.textSecondary)
    }
}

@Composable
private fun SectionTitle(@StringRes text: Int) {
    Spacer(Modifier.height(20.dp))
    Text(stringResource(text), style = KcalTheme.typography.titleSmall, color = KcalTheme.colors.textBrand)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ReasonRow(reason: MealReason) {
    Row(verticalAlignment = Alignment.Top) {
        IconChip(reason.icon(), size = 30.dp, iconSize = 16.dp)
        Spacer(Modifier.width(12.dp))
        Text(reason.text(), style = KcalTheme.typography.bodySmall, color = KcalTheme.colors.textPrimary, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun IngredientsSection(food: Food, servings: Double) {
    if (food.ingredients.isEmpty()) return
    val colors = KcalTheme.colors
    val language = appLanguage()
    SectionTitle(R.string.info_whats_in)
    food.ingredients.sortedByDescending { it.grams }.forEachIndexed { i, ingredient ->
        if (i > 0) KcalDivider(Modifier.padding(vertical = 2.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ingredient.localizedName(language), style = KcalTheme.typography.label, color = colors.textPrimary)
                Text(stringResource(ingredient.category.purpose()), style = KcalTheme.typography.caption, color = colors.textSecondary)
            }
            Text(gramsText((ingredient.grams * servings).roundToInt()), style = KcalTheme.typography.labelSmall, color = colors.textSecondary)
        }
    }
}

/** Small reason chip on swap options ("Same 410 kcal", "Cheaper"…). */
@Composable
internal fun SwapReasonChip(reason: SwapReason) {
    Text(
        reason.text(),
        style = KcalTheme.typography.caption,
        color = KcalTheme.colors.textBrand,
        modifier = Modifier.background(KcalTheme.colors.accentContainer, KcalShapes.pill).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Round (i) button – opens the info sheet; 40dp touch target with ripple only. */
@Composable
internal fun InfoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.info_open)
    Row(
        modifier.size(40.dp).kcalClickable(shape = KcalShapes.pill, onClickLabel = description, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(KcalIcons.Info), description, tint = KcalTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
    }
}

// ---------------------------------------------------------------- reason → text / icon

@Composable
@ReadOnlyComposable
private fun MealReason.text(): String {
    val locale = appLocale()
    return when (this) {
        is MealReason.FitsTarget -> stringResource(R.string.reason_fits_target, mealType.label(), KcalFormat.int(mealKcal, locale), KcalFormat.int(targetKcal, locale))
        is MealReason.HighProtein -> stringResource(R.string.reason_high_protein, KcalFormat.int(grams, locale))
        is MealReason.HighFiber -> stringResource(R.string.reason_high_fiber, KcalFormat.int(grams, locale))
        MealReason.GentleOnBloodSugar -> stringResource(R.string.reason_blood_sugar)
        MealReason.GentleOnGut -> stringResource(R.string.reason_gut)
        MealReason.LactoseFriendly -> stringResource(R.string.reason_lactose)
        MealReason.LocalDish -> stringResource(R.string.reason_local)
        MealReason.BudgetFriendly -> stringResource(R.string.reason_budget)
        MealReason.PlantProtein -> stringResource(R.string.reason_plant_protein)
        MealReason.FillingForFewerCalories -> stringResource(R.string.reason_filling)
        MealReason.EnergyForGain -> stringResource(R.string.reason_energy)
        MealReason.Omega3 -> stringResource(R.string.reason_omega3)
    }
}

@DrawableRes
private fun MealReason.icon(): Int = when (this) {
    is MealReason.FitsTarget, MealReason.EnergyForGain -> KcalIcons.Flame
    is MealReason.HighProtein, MealReason.FillingForFewerCalories -> KcalIcons.Utensils
    is MealReason.HighFiber, MealReason.PlantProtein, MealReason.Omega3 -> KcalIcons.Heart
    MealReason.GentleOnBloodSugar, MealReason.GentleOnGut -> KcalIcons.Health
    MealReason.LactoseFriendly -> KcalIcons.Shield
    MealReason.LocalDish -> KcalIcons.NavHome
    MealReason.BudgetFriendly -> KcalIcons.Budget
}

@Composable
@ReadOnlyComposable
internal fun SwapReason.text(): String {
    val locale = appLocale()
    return when (this) {
        is SwapReason.SameCalories -> stringResource(R.string.swap_reason_similar_kcal)
        SwapReason.Cheaper -> stringResource(R.string.swap_reason_cheaper)
        is SwapReason.MoreProtein -> stringResource(R.string.swap_reason_more_protein, KcalFormat.int(grams, locale))
        is SwapReason.MoreFiber -> stringResource(R.string.swap_reason_more_fiber, KcalFormat.int(grams, locale))
        SwapReason.LowerGlycemic -> stringResource(R.string.swap_reason_lower_gi)
        SwapReason.LocalDish -> stringResource(R.string.swap_reason_local)
        SwapReason.Vegetarian -> stringResource(R.string.swap_reason_vegetarian)
    }
}

@StringRes
private fun IngredientCategory.purpose(): Int = when (this) {
    IngredientCategory.Grain -> R.string.ing_grain
    IngredientCategory.Tuber -> R.string.ing_tuber
    IngredientCategory.Legume -> R.string.ing_legume
    IngredientCategory.PlantProtein -> R.string.ing_plant_protein
    IngredientCategory.Meat -> R.string.ing_meat
    IngredientCategory.Fish -> R.string.ing_fish
    IngredientCategory.Egg -> R.string.ing_egg
    IngredientCategory.Dairy -> R.string.ing_dairy
    IngredientCategory.Fat -> R.string.ing_fat
    IngredientCategory.Vegetable -> R.string.ing_vegetable
    IngredientCategory.Fruit -> R.string.ing_fruit
    IngredientCategory.Nuts -> R.string.ing_nuts
    IngredientCategory.Sweet -> R.string.ing_sweet
    IngredientCategory.Drink -> R.string.ing_drink
    IngredientCategory.Condiment -> R.string.ing_condiment
}

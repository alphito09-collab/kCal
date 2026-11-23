package com.alphito.kcal.core.model

/**
 * Why a dish is in the user's plan. Computed (never hand-written per dish) from the dish's
 * ingredients and the user's profile, so every one of them is true and fully localizable.
 */
sealed interface MealReason {
    /** Portion sized (in quarter servings) to this meal's share of [targetKcal], the daily target. */
    data class FitsTarget(val mealKcal: Int, val targetKcal: Int, val mealType: MealType) : MealReason
    data class HighProtein(val grams: Int) : MealReason
    data class HighFiber(val grams: Int) : MealReason
    /** Diabetes: low/medium glycaemic, no added sugar. */
    data object GentleOnBloodSugar : MealReason
    /** IBS: no onion/garlic-heavy, legume-heavy or spicy ingredients. */
    data object GentleOnGut : MealReason
    /** Lactose intolerance: no high-lactose dairy (yogurt/cheese in small amounts are fine). */
    data object LactoseFriendly : MealReason
    data object LocalDish : MealReason
    data object BudgetFriendly : MealReason
    data object PlantProtein : MealReason
    /** Weight loss: lots of protein + fibre per calorie keeps you full. */
    data object FillingForFewerCalories : MealReason
    /** Weight gain: energy-dense but wholesome. */
    data object EnergyForGain : MealReason
    data object Omega3 : MealReason
}

/** Why an alternative is suggested on the Swap screen. */
sealed interface SwapReason {
    data class SameCalories(val kcal: Int) : SwapReason
    data object Cheaper : SwapReason
    data class MoreProtein(val grams: Int) : SwapReason
    data class MoreFiber(val grams: Int) : SwapReason
    data object LowerGlycemic : SwapReason
    data object LocalDish : SwapReason
    data object Vegetarian : SwapReason
}

/** Everything the meal-info sheet shows for one planned item. */
data class MealInsight(
    val item: PlannedItem,
    val reasons: kotlinx.collections.immutable.ImmutableList<MealReason>,
)

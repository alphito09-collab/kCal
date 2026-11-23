package com.alphito.kcal.core.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import java.time.LocalDate
import kotlin.math.roundToInt

enum class MealType { Breakfast, Lunch, Snack, Dinner }

/** What role a food plays in a meal. `Main` is a complete dish; `Protein` is served with a side. */
enum class FoodRole { BreakfastMain, Drink, Main, Protein, Side, Snack }

/**
 * Derived from the ingredients by the catalogue build (backend/firebase/seed/build_catalog.py):
 * `Dairy` = high-lactose, `HighFodmap` = Monash-style thresholds, `Vegetarian` = no meat or fish.
 */
enum class FoodTag { Dairy, HighSugar, AddedSugar, Spicy, HighFodmap, Meat, Fish, Egg, Nuts, Gluten, Vegetarian }

/** Glycaemic level of the dish, weighted by where its carbohydrate comes from. */
enum class Glycemic { Low, Medium, High }

enum class IngredientCategory {
    Grain, Tuber, Legume, PlantProtein, Meat, Fish, Egg, Dairy, Fat, Vegetable, Fruit, Nuts, Sweet, Drink, Condiment;

    companion object {
        /** Catalogue keys are snake_case ("plant_protein"). */
        fun fromKey(key: String): IngredientCategory =
            entries.firstOrNull { it.name.equals(key.replace("_", ""), ignoreCase = true) } ?: Condiment
    }
}

data class FoodIngredient(
    val id: String,
    val grams: Int,
    val category: IngredientCategory,
    val names: ImmutableMap<String, String>,
) {
    fun localizedName(language: String): String = names[language] ?: names["en"] ?: id
}

data class Food(
    val id: String,
    /** English name – used for logs, search and as the last-resort label. */
    val name: String,
    val kcal: Int,
    val role: FoodRole,
    val mealTypes: ImmutableSet<MealType>,
    val tags: ImmutableSet<FoodTag>,
    /** Bundled photo key ("" when the dish only has an emoji / remote photo). */
    val imageKey: String = "",
    /** Localized names by language tag ("en", "ar" …). */
    val names: ImmutableMap<String, String> = persistentMapOf(),
    /** 1 = budget staple · 2 = moderate · 3 = premium – relative to local market prices. */
    val cost: Int = 2,
    val emoji: String = "",
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val glycemic: Glycemic = Glycemic.Medium,
    val ingredients: ImmutableList<FoodIngredient> = persistentListOf(),
    /** Remote photo for foods without a bundled image. */
    val imageUrl: String? = null,
    /** Cluster ids or ISO country codes where the dish is common; empty = everywhere. */
    val regions: ImmutableSet<String> = persistentSetOf(),
    /** Retired foods stay in Room (old plans reference them) but are never planned again. */
    val active: Boolean = true,
) {
    fun localizedName(language: String): String = names[language] ?: names["en"] ?: name

    val isGlobal: Boolean get() = regions.isEmpty()
}

data class PlannedItem(
    val id: Long,
    val date: LocalDate,
    val mealType: MealType,
    val position: Int,
    val food: Food,
    /** Portion multiplier chosen so the day lands on the user's calorie target (0.5 – 2.0). */
    val servings: Double = 1.0,
) {
    val kcal: Int get() = (food.kcal * servings).roundToInt()
    val proteinG: Double get() = food.proteinG * servings
    val carbsG: Double get() = food.carbsG * servings
    val fiberG: Double get() = food.fiberG * servings
    fun grams(ingredient: FoodIngredient): Int = (ingredient.grams * servings).roundToInt()
}

data class MealSlot(
    val date: LocalDate,
    val mealType: MealType,
    val items: ImmutableList<PlannedItem>,
    val checked: Boolean,
) {
    val totalKcal: Int get() = items.sumOf { it.kcal }
    val totalProteinG: Double get() = items.sumOf { it.proteinG }
    val title: String get() = items.joinToString(" + ") { it.food.name }
    val leadImageKey: String? get() = items.firstOrNull()?.food?.imageKey
    val leadImageUrl: String? get() = items.firstOrNull()?.food?.imageUrl
    val leadEmoji: String? get() = items.firstOrNull()?.food?.emoji
}

data class DayPlan(
    val date: LocalDate,
    val targetKcal: Int,
    val slots: ImmutableList<MealSlot>,
) {
    val eatenKcal: Int get() = slots.filter { it.checked }.sumOf { it.totalKcal }
    val plannedKcal: Int get() = slots.sumOf { it.totalKcal }
    val plannedProteinG: Int get() = slots.sumOf { it.totalProteinG }.roundToInt()
    val plannedFiberG: Int get() = slots.sumOf { s -> s.items.sumOf { it.fiberG } }.roundToInt()
    val plannedMeals: Int get() = slots.count { it.items.isNotEmpty() }
    val checkedMeals: Int get() = slots.count { it.checked && it.items.isNotEmpty() }
    val nextUnchecked: MealSlot? get() = slots.firstOrNull { !it.checked && it.items.isNotEmpty() }
}

/** A swap option with the portion that keeps the meal's calories, and why it is suggested. */
data class SwapCandidate(
    val food: Food,
    val bestMatch: Boolean,
    val servings: Double = 1.0,
    val reasons: ImmutableList<SwapReason> = persistentListOf(),
) {
    val kcal: Int get() = (food.kcal * servings).roundToInt()
}

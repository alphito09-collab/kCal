package com.alphito.kcal.core.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import java.time.LocalDate

enum class MealType { Breakfast, Lunch, Snack, Dinner }

/** What role a food plays in a meal. `Main` is a complete dish; `Protein` is served with a side. */
enum class FoodRole { BreakfastMain, Drink, Main, Protein, Side, Snack }

enum class FoodTag { Dairy, HighSugar, Spicy, HighFodmap, Meat, Vegetarian }

data class Food(
    val id: String,
    val name: String,
    val portion: String,
    val kcal: Int,
    val pricePkr: Int,
    val role: FoodRole,
    val mealTypes: ImmutableSet<MealType>,
    val imageKey: String,
    val tags: ImmutableSet<FoodTag>,
)

data class PlannedItem(
    val id: Long,
    val date: LocalDate,
    val mealType: MealType,
    val position: Int,
    val food: Food,
)

data class MealSlot(
    val date: LocalDate,
    val mealType: MealType,
    val items: ImmutableList<PlannedItem>,
    val checked: Boolean,
) {
    val totalKcal: Int get() = items.sumOf { it.food.kcal }
    val title: String get() = items.joinToString(" + ") { it.food.name }
    val leadImageKey: String? get() = items.firstOrNull()?.food?.imageKey
}

data class DayPlan(
    val date: LocalDate,
    val targetKcal: Int,
    val slots: ImmutableList<MealSlot>,
) {
    val eatenKcal: Int get() = slots.filter { it.checked }.sumOf { it.totalKcal }
    val nextUnchecked: MealSlot? get() = slots.firstOrNull { !it.checked && it.items.isNotEmpty() }
}

data class SwapCandidate(val food: Food, val bestMatch: Boolean)

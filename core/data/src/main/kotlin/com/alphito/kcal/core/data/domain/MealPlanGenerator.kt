package com.alphito.kcal.core.data.domain

import com.alphito.kcal.core.model.BudgetTier
import com.alphito.kcal.core.model.Food
import com.alphito.kcal.core.model.FoodRole
import com.alphito.kcal.core.model.FoodTag
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.SwapCandidate
import com.alphito.kcal.core.model.UserProfile
import java.time.LocalDate
import kotlin.math.abs

/** A generated (not yet persisted) plan item. */
data class GeneratedItem(val date: LocalDate, val mealType: MealType, val position: Int, val foodId: String)

/**
 * Builds a week of meals from the catalogue, filtered by health conditions and budget.
 * Pure Kotlin – no Android, no I/O – so the rules are unit-tested directly.
 */
class MealPlanGenerator {

    fun isAllowed(food: Food, profile: UserProfile): Boolean {
        val blocked = buildSet {
            if (HealthCondition.Diabetes in profile.conditions) add(FoodTag.HighSugar)
            if (HealthCondition.LactoseIntolerant in profile.conditions) add(FoodTag.Dairy)
            if (HealthCondition.Ibs in profile.conditions) { add(FoodTag.HighFodmap); add(FoodTag.Spicy) }
        }
        if (food.tags.any { it in blocked }) return false
        if (profile.budget == BudgetTier.Low && food.pricePkr > LOW_BUDGET_MAX_ITEM_PKR) return false
        return true
    }

    fun generate(profile: UserProfile, catalog: List<Food>, start: LocalDate, days: Int = 7): List<GeneratedItem> {
        val allowed = catalog.filter { isAllowed(it, profile) }
        fun pool(role: FoodRole, meal: MealType) = allowed.filter { it.role == role && meal in it.mealTypes }

        val breakfastMains = pool(FoodRole.BreakfastMain, MealType.Breakfast)
        val drinks = pool(FoodRole.Drink, MealType.Breakfast)
        val lunchMains = pool(FoodRole.Main, MealType.Lunch)
        val lunchSides = pool(FoodRole.Side, MealType.Lunch)
        val snacks = pool(FoodRole.Snack, MealType.Snack).ifEmpty { pool(FoodRole.Side, MealType.Snack) }
        val dinnerProteins = pool(FoodRole.Protein, MealType.Dinner)
        val dinnerMains = pool(FoodRole.Main, MealType.Dinner)
        val dinnerSides = pool(FoodRole.Side, MealType.Dinner)

        return (0 until days).flatMap { offset ->
            val date = start.plusDays(offset.toLong())
            // Rotation keyed on the day of week keeps a plan stable when it is regenerated.
            val d = date.dayOfWeek.value
            buildList {
                fun add(meal: MealType, vararg foods: Food?) =
                    foods.filterNotNull().forEachIndexed { i, f -> add(GeneratedItem(date, meal, i, f.id)) }

                add(MealType.Breakfast, breakfastMains.rotate(d), drinks.rotate(d))
                add(MealType.Lunch, lunchMains.rotate(d - 2), lunchSides.rotate(d))
                add(MealType.Snack, snacks.rotate(d))
                // Alternate protein + side with complete dishes for dinner.
                if (d % 2 == 0 && dinnerProteins.isNotEmpty()) {
                    add(MealType.Dinner, dinnerProteins.rotate(d), dinnerSides.rotate(d))
                } else {
                    val main = dinnerMains.filter { it.id != lunchMains.rotate(d - 2)?.id }.rotate(d) ?: dinnerMains.rotate(d)
                    add(MealType.Dinner, main ?: dinnerProteins.rotate(d), if (main == null) dinnerSides.rotate(d) else null)
                }
            }
        }
    }

    /** Same-role alternatives, closest in calories (then price) first; the closest is "Best match". */
    fun swapCandidates(current: Food, mealType: MealType, profile: UserProfile, catalog: List<Food>): List<SwapCandidate> {
        val equivalentRoles = when (current.role) {
            FoodRole.Main, FoodRole.Protein -> setOf(FoodRole.Main, FoodRole.Protein)
            else -> setOf(current.role)
        }
        return catalog.asSequence()
            .filter { it.id != current.id && it.role in equivalentRoles && mealType in it.mealTypes && isAllowed(it, profile) }
            .sortedWith(compareBy({ abs(it.kcal - current.kcal) }, { abs(it.pricePkr - current.pricePkr) }))
            .mapIndexed { index, food -> SwapCandidate(food, bestMatch = index == 0) }
            .toList()
    }

    private fun <T> List<T>.rotate(seed: Int): T? = if (isEmpty()) null else this[Math.floorMod(seed, size)]

    companion object {
        const val LOW_BUDGET_MAX_ITEM_PKR = 120
    }
}

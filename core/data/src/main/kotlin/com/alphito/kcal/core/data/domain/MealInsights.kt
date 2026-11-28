package com.alphito.kcal.core.data.domain

import com.alphito.kcal.core.model.Food
import com.alphito.kcal.core.model.FoodRole
import com.alphito.kcal.core.model.FoodTag
import com.alphito.kcal.core.model.Glycemic
import com.alphito.kcal.core.model.Goal
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.model.IngredientCategory
import com.alphito.kcal.core.model.MealReason
import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.PlannedItem
import com.alphito.kcal.core.model.Regions
import com.alphito.kcal.core.model.SwapReason
import com.alphito.kcal.core.model.UserProfile
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Turns nutrition data into the short, honest "why" lines shown in the meal-info sheet and on swap
 * options. Every reason is derived from the dish's ingredients + the user's profile – nothing is
 * hand-written per dish, so it stays true when the catalogue changes and is fully localizable.
 */
object MealInsights {

    /** [targetKcal] is the daily target from [com.alphito.kcal.core.data.repository.CaloriePlanRepository]. */
    fun reasons(item: PlannedItem, profile: UserProfile, targetKcal: Int, slotKcal: Int = item.kcal): List<MealReason> {
        val food = item.food
        val light = item.mealType == MealType.Snack || food.role == FoodRole.Drink || food.role == FoodRole.Side
        val protein = item.proteinG
        val fiber = item.fiberG
        val conditions = profile.conditions
        return buildList {
            // What the user cares about most comes first: their health condition.
            if (HealthCondition.Diabetes in conditions && food.glycemic != Glycemic.High && FoodTag.HighSugar !in food.tags && FoodTag.AddedSugar !in food.tags) {
                add(MealReason.GentleOnBloodSugar)
            }
            if (HealthCondition.Ibs in conditions && FoodTag.HighFodmap !in food.tags && FoodTag.Spicy !in food.tags) {
                add(MealReason.GentleOnGut)
            }
            if (HealthCondition.LactoseIntolerant in conditions && FoodTag.Dairy !in food.tags) add(MealReason.LactoseFriendly)

            if (targetKcal > 0 && food.role != FoodRole.Drink) {
                add(MealReason.FitsTarget(slotKcal, targetKcal, item.mealType))
            }
            if (protein >= if (light) 8 else 15) add(MealReason.HighProtein(protein.roundToInt()))
            if (fiber >= if (light) 3 else 6) add(MealReason.HighFiber(fiber.roundToInt()))
            if (FoodTag.Vegetarian in food.tags && protein >= 12 &&
                food.ingredients.any { it.category == IngredientCategory.Legume || it.category == IngredientCategory.PlantProtein }
            ) add(MealReason.PlantProtein)
            if (food.ingredients.any { it.id in OILY_FISH }) add(MealReason.Omega3)
            when (profile.goal) {
                Goal.LoseWeight -> if (item.kcal > 0 && (protein + fiber) * 100 / item.kcal >= MealPlanGenerator.FILLING_DENSITY) {
                    add(MealReason.FillingForFewerCalories)
                }
                Goal.GainWeight -> if (item.kcal >= ENERGY_DENSE_KCAL) add(MealReason.EnergyForGain)
                Goal.MaintainWeight -> Unit
            }
            if (Regions.isLocal(food, profile.region)) add(MealReason.LocalDish)
            if (food.cost == 1) add(MealReason.BudgetFriendly)
        }
    }

    fun swapReasons(current: PlannedItem, candidate: Food, servings: Double, profile: UserProfile): List<SwapReason> {
        val kcal = (candidate.kcal * servings).roundToInt()
        val protein = candidate.proteinG * servings - current.proteinG
        val fiber = candidate.fiberG * servings - current.fiberG
        return buildList {
            if (abs(kcal - current.kcal) <= SAME_KCAL_TOLERANCE) add(SwapReason.SameCalories(kcal))
            if (candidate.cost < current.food.cost) add(SwapReason.Cheaper)
            if (protein >= 5) add(SwapReason.MoreProtein(protein.roundToInt()))
            if (fiber >= 3) add(SwapReason.MoreFiber(fiber.roundToInt()))
            if (candidate.glycemic < current.food.glycemic) add(SwapReason.LowerGlycemic)
            if (Regions.isLocal(candidate, profile.region) && !Regions.isLocal(current.food, profile.region)) add(SwapReason.LocalDish)
            if (FoodTag.Vegetarian in candidate.tags && FoodTag.Vegetarian !in current.food.tags) add(SwapReason.Vegetarian)
        }
    }

    private val OILY_FISH = setOf("salmon", "sardines", "tuna")
    private const val ENERGY_DENSE_KCAL = 450
    private const val SAME_KCAL_TOLERANCE = 40
}

package com.alphito.kcal.core.data.domain

import com.alphito.kcal.core.model.Food
import com.alphito.kcal.core.model.FoodRole
import com.alphito.kcal.core.model.FoodTag
import com.alphito.kcal.core.model.Glycemic
import com.alphito.kcal.core.model.Goal
import com.alphito.kcal.core.model.HealthCondition
import com.alphito.kcal.core.model.IngredientCategory
import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.PlannedItem
import com.alphito.kcal.core.model.Regions
import com.alphito.kcal.core.model.SwapCandidate
import com.alphito.kcal.core.model.UserProfile
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

/** A generated (not yet persisted) plan item. */
data class GeneratedItem(
    val date: LocalDate,
    val mealType: MealType,
    val position: Int,
    val foodId: String,
    val servings: Double = 1.0,
)

/**
 * Builds a week of meals from the catalogue. Pure Kotlin – no Android, no I/O – so every rule is
 * unit-tested directly.
 *
 * Rules, in priority order:
 *  1. **Health conditions are absolute** (never relaxed): diabetes → no added-sugar (≥5 g) or high-GI
 *     dishes; IBS → no high-FODMAP or spicy dishes; lactose intolerance → no high-lactose dairy.
 *  2. **Local first**: the user's country/cuisine dishes + global staples. Other regions are only
 *     used when a meal would otherwise be empty.
 *  3. **Budget**: dishes up to the budget's cost tier; relaxed before falling back to other regions.
 *  4. **Calorie target**: each meal gets its share of the daily target ([MEAL_SHARE]); portions
 *     are scaled in quarter steps (0.5×–2×). Diabetes additionally caps carbs per meal.
 *  5. **Variety with repetition** (batch-cook friendly): a main is not repeated within 2 days,
 *     lunch and dinner differ, and the choice is seeded by the date so regenerating is stable.
 */
class MealPlanGenerator {

    /** Absolute health rules – the only filter that is never relaxed. */
    fun isSafe(food: Food, profile: UserProfile): Boolean {
        val c = profile.conditions
        if (HealthCondition.Diabetes in c &&
            (FoodTag.HighSugar in food.tags || FoodTag.AddedSugar in food.tags || food.glycemic == Glycemic.High)
        ) return false
        if (HealthCondition.LactoseIntolerant in c && FoodTag.Dairy in food.tags) return false
        if (HealthCondition.Ibs in c && (FoodTag.HighFodmap in food.tags || FoodTag.Spicy in food.tags)) return false
        return true
    }

    fun isAllowed(food: Food, profile: UserProfile): Boolean =
        food.active && isSafe(food, profile) && Regions.isAvailable(food, profile.region) && food.cost <= profile.budget.maxDishCost

    fun generate(
        profile: UserProfile,
        catalog: List<Food>,
        start: LocalDate,
        days: Int = 7,
        dailyTargetKcal: Int = 0,
    ): List<GeneratedItem> {
        if (days <= 0) return emptyList()
        val usable = catalog.filter { it.active && isSafe(it, profile) }
        if (usable.isEmpty()) return emptyList()
        val pools = Pools(usable, profile)
        val lastUsed = HashMap<String, Long>()

        return (0 until days).flatMap { offset ->
            val date = start.plusDays(offset.toLong())
            val day = date.toEpochDay()
            val random = Random(seed(profile, day))
            fun pick(role: FoodRole, meal: MealType, exclude: Set<String> = emptySet(), predicate: (Food) -> Boolean = { true }): Food? {
                val pool = pools.get(role, meal).filter { it.id !in exclude && predicate(it) }
                val fresh = pool.filter { (lastUsed[it.id] ?: Long.MIN_VALUE) < day - REPEAT_GAP_DAYS }
                return weightedPick(fresh.ifEmpty { pool }, profile, random)?.also {
                    if (role != FoodRole.Drink && role != FoodRole.Side) lastUsed[it.id] = day
                }
            }

            val meals = buildMap<MealType, List<Food>> {
                // Breakfast: a main + a drink (tea/coffee/milk drink are light; the main carries the calories).
                put(MealType.Breakfast, listOfNotNull(pick(FoodRole.BreakfastMain, MealType.Breakfast), pick(FoodRole.Drink, MealType.Breakfast)))

                // Lunch: a complete dish, topped up with a vegetable side when it is light.
                val lunchMain = pick(FoodRole.Main, MealType.Lunch) ?: pick(FoodRole.Protein, MealType.Lunch)
                val lunch = buildList {
                    lunchMain?.let(::add)
                    when {
                        lunchMain?.role == FoodRole.Protein -> pick(FoodRole.Side, MealType.Lunch, predicate = ::isStarchySide)?.let(::add)
                        lunchMain != null && lunchMain.kcal < share(dailyTargetKcal, MealType.Lunch) * LIGHT_MAIN_RATIO ->
                            pick(FoodRole.Side, MealType.Lunch, predicate = { !isStarchySide(it) })?.let(::add)
                    }
                }
                put(MealType.Lunch, lunch)

                put(MealType.Snack, listOfNotNull(pick(FoodRole.Snack, MealType.Snack) ?: pick(FoodRole.Side, MealType.Snack)))

                // Dinner alternates a protein + starchy side with a complete dish (never lunch's dish).
                // Only when an in-budget protein exists – otherwise the fallback would overspend.
                val proteinNight = day % 2 == 0L &&
                    pools.get(FoodRole.Protein, MealType.Dinner).any { it.cost <= profile.budget.maxDishCost }
                val dinner = if (proteinNight) {
                    listOfNotNull(pick(FoodRole.Protein, MealType.Dinner), pick(FoodRole.Side, MealType.Dinner, predicate = ::isStarchySide))
                } else {
                    val main = pick(FoodRole.Main, MealType.Dinner, exclude = setOfNotNull(lunchMain?.id))
                    if (main != null) listOf(main)
                    else listOfNotNull(pick(FoodRole.Protein, MealType.Dinner), pick(FoodRole.Side, MealType.Dinner, predicate = ::isStarchySide))
                }
                put(MealType.Dinner, dinner)
            }

            meals.flatMap { (meal, foods) ->
                val servings = servingsFor(foods, meal, dailyTargetKcal, profile)
                foods.mapIndexed { i, f -> GeneratedItem(date, meal, i, f.id, if (f.role == FoodRole.Drink) 1.0 else servings) }
            }
        }
    }

    /**
     * Same-role alternatives for one planned item. Each candidate gets the portion that keeps the
     * item's calories; ranked by calorie match, then local, cheaper and more protein.
     */
    fun swapCandidates(current: PlannedItem, profile: UserProfile, catalog: List<Food>): List<SwapCandidate> {
        val roles = when (current.food.role) {
            FoodRole.Main, FoodRole.Protein -> setOf(FoodRole.Main, FoodRole.Protein)
            FoodRole.Side, FoodRole.Snack -> setOf(FoodRole.Side, FoodRole.Snack)
            else -> setOf(current.food.role)
        }
        fun eligible(f: Food) = f.id != current.food.id && f.active && f.role in roles && current.mealType in f.mealTypes && isSafe(f, profile)
        val local = catalog.filter { eligible(it) && Regions.isAvailable(it, profile.region) }
        // Thin local pool → also offer other cuisines (still condition-safe).
        val pool = if (local.size >= MIN_SWAP_OPTIONS) local else local + catalog.filter { eligible(it) && it !in local }
        val targetKcal = current.kcal
        return pool.map { food ->
            val servings = if (food.role == FoodRole.Drink || food.kcal == 0) 1.0 else quarter(targetKcal.toDouble() / food.kcal)
            food to servings
        }.sortedWith(
            compareBy<Pair<Food, Double>>(
                { abs((it.first.kcal * it.second).roundToInt() - targetKcal) / KCAL_BUCKET },
                { if (Regions.isLocal(it.first, profile.region)) 0 else 1 },
                { if (it.first.cost <= profile.budget.maxDishCost) 0 else 1 },
                { it.first.cost },
                { -it.first.proteinG * it.second },
            ),
        ).take(MAX_SWAP_OPTIONS).mapIndexed { index, (food, servings) ->
            SwapCandidate(food, bestMatch = index == 0, servings = servings, reasons = MealInsights.swapReasons(current, food, servings, profile).toImmutableList())
        }
    }

    // ---------------------------------------------------------------- internals

    private class Pools(private val usable: List<Food>, private val profile: UserProfile) {
        private val cache = HashMap<Pair<FoodRole, MealType>, List<Food>>()

        /** Fallback ladder: local+budget → local → any region+budget → any region (always safe). */
        fun get(role: FoodRole, meal: MealType): List<Food> = cache.getOrPut(role to meal) {
            val base = usable.filter { it.role == role && meal in it.mealTypes }
            val local = base.filter { Regions.isAvailable(it, profile.region) }
            local.filter { it.cost <= profile.budget.maxDishCost }.ifEmpty { local }
                .ifEmpty { base.filter { it.cost <= profile.budget.maxDishCost } }
                .ifEmpty { base }
        }
    }

    private fun weightedPick(pool: List<Food>, profile: UserProfile, random: Random): Food? {
        if (pool.isEmpty()) return null
        val weights = pool.map { food ->
            var w = if (Regions.isLocal(food, profile.region)) 3.0 else 1.0
            if (HealthCondition.Diabetes in profile.conditions && food.glycemic == Glycemic.Low) w *= 2
            if (profile.goal == Goal.LoseWeight && food.kcal > 0 && (food.proteinG + food.fiberG) * 100 / food.kcal >= FILLING_DENSITY) w *= 1.5
            if (food.cost < profile.budget.maxDishCost) w *= 1.2
            w
        }
        var r = random.nextDouble() * weights.sum()
        pool.forEachIndexed { i, f -> r -= weights[i]; if (r <= 0) return f }
        return pool.last()
    }

    private fun servingsFor(foods: List<Food>, meal: MealType, dailyTarget: Int, profile: UserProfile): Double {
        if (dailyTarget <= 0) return 1.0
        val fixed = foods.filter { it.role == FoodRole.Drink }.sumOf { it.kcal }
        val scalable = foods.filter { it.role != FoodRole.Drink }
        val base = scalable.sumOf { it.kcal }
        if (base <= 0) return 1.0
        var factor = (share(dailyTarget, meal) - fixed).coerceAtLeast(0).toDouble() / base
        if (HealthCondition.Diabetes in profile.conditions) {
            val carbs = scalable.sumOf { it.carbsG }
            // Drinks are not scaled, so their carbs come off the meal's budget first.
            val drinkCarbs = foods.filter { it.role == FoodRole.Drink }.sumOf { it.carbsG }
            val cap = (if (meal == MealType.Snack) SNACK_CARB_CAP_G else MEAL_CARB_CAP_G) - drinkCarbs
            // Round *down* when capping so the meal never exceeds the carb budget.
            if (carbs > 0 && carbs * factor > cap) return (floor(cap / carbs * 4) / 4).coerceIn(MIN_SERVINGS, MAX_SERVINGS)
        }
        return quarter(factor)
    }

    private fun isStarchySide(food: Food): Boolean = food.ingredients.any {
        it.category == IngredientCategory.Grain || it.category == IngredientCategory.Tuber || it.category == IngredientCategory.Legume
    } || food.ingredients.isEmpty()

    private fun seed(profile: UserProfile, day: Long): Long =
        day * 1_000_003 + profile.region.hashCode() * 31L + profile.conditions.sumOf { it.ordinal + 1 } * 7L + profile.budget.ordinal

    companion object {
        /** Share of the daily target per meal (dietitian-style 25/35/10/30 split). */
        val MEAL_SHARE: Map<MealType, Double> = mapOf(
            MealType.Breakfast to 0.25, MealType.Lunch to 0.35, MealType.Snack to 0.10, MealType.Dinner to 0.30,
        )

        fun share(dailyTarget: Int, meal: MealType): Int = (dailyTarget * (MEAL_SHARE[meal] ?: 0.0)).roundToInt()

        /** Rounds a portion factor to quarter servings and keeps it realistic (½× – 2×). */
        fun quarter(factor: Double): Double = (floor(factor * 4 + 0.5) / 4).coerceIn(MIN_SERVINGS, MAX_SERVINGS)

        const val MIN_SERVINGS = 0.5
        const val MAX_SERVINGS = 2.0
        /** ADA-style consistency: ~45–60 g carbohydrate per main meal for most adults with type 2 diabetes. */
        const val MEAL_CARB_CAP_G = 60.0
        const val SNACK_CARB_CAP_G = 25.0
        const val REPEAT_GAP_DAYS = 2
        const val FILLING_DENSITY = 6.0
        private const val LIGHT_MAIN_RATIO = 0.8
        private const val KCAL_BUCKET = 40
        private const val MIN_SWAP_OPTIONS = 3
        private const val MAX_SWAP_OPTIONS = 8
    }
}

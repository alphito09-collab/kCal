package com.alphito.kcal.core.model

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

enum class Gender { Female, Male, Other }

enum class Goal { LoseWeight, GainWeight, MaintainWeight }

/**
 * Weekly food budget, relative to the user's local market (dishes carry a 1–3 cost tier):
 * Low plans budget staples only, Medium adds moderate dishes, High allows everything.
 */
enum class BudgetTier(val maxDishCost: Int) {
    Low(1),
    Medium(2),
    High(3),
}

enum class HealthCondition { Diabetes, Ibs, LactoseIntolerant }

/** Everything the onboarding flow collects. Immutable – updates go through `copy`. */
data class UserProfile(
    val name: String,
    val age: Int,
    val gender: Gender,
    val heightCm: Int,
    val weightKg: Double,
    val goal: Goal = Goal.LoseWeight,
    val targetWeightKg: Double? = null,
    /** ISO 3166 country code – see [Regions]. */
    val region: String = Regions.DEFAULT_COUNTRY,
    val budget: BudgetTier = BudgetTier.Medium,
    val conditions: ImmutableSet<HealthCondition> = persistentSetOf(),
    /** Weight-loss split: the user covers [stepsDeficitKcal] of [dailyDeficitKcal] with extra steps. See [EnergyCalculator.plan]. */
    val addStepsToDeficit: Boolean = true,
    val dailyDeficitKcal: Int = DEFAULT_DEFICIT_KCAL,
    val stepsDeficitKcal: Int = DEFAULT_STEPS_KCAL,
) {
    val initials: String
        get() = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "K" }

    companion object {
        /** Fallback only. Onboarding stores [EnergyCalculator.recommendedDeficit] for the user. */
        const val DEFAULT_DEFICIT_KCAL = 500
        /** ≈ 3,000 steps above the everyday baseline. */
        const val DEFAULT_STEPS_KCAL = 150
    }
}

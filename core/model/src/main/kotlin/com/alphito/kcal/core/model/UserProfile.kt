package com.alphito.kcal.core.model

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

enum class Gender { Female, Male, Other }

enum class Goal { LoseWeight, GainWeight, MaintainWeight }

enum class BudgetTier(val minPkrPerWeek: Int, val maxPkrPerWeek: Int?) {
    Low(2_000, 3_500),
    Medium(3_500, 6_000),
    High(6_000, null),
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
    val region: String = DEFAULT_REGION,
    val budget: BudgetTier = BudgetTier.Medium,
    val conditions: ImmutableSet<HealthCondition> = persistentSetOf(),
    val addStepsToDeficit: Boolean = true,
    val dailyDeficitKcal: Int = DEFAULT_DEFICIT_KCAL,
    val stepsDeficitKcal: Int = DEFAULT_STEPS_KCAL,
) {
    val dietDeficitKcal: Int
        get() = if (addStepsToDeficit) (dailyDeficitKcal - stepsDeficitKcal).coerceAtLeast(0) else dailyDeficitKcal

    val initials: String
        get() = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "K" }

    companion object {
        const val DEFAULT_REGION = "Pakistan — Lahore"
        const val DEFAULT_DEFICIT_KCAL = 1_000
        const val DEFAULT_STEPS_KCAL = 300
        const val DEFAULT_SURPLUS_KCAL = 400
    }
}

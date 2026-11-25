package com.alphito.kcal.core.model

import kotlin.math.roundToInt

data class EnergyNeeds(val bmr: Int, val tdee: Int)

/** Pure energy maths (Mifflin–St Jeor). Kept free of Android so it is trivially unit-testable. */
object EnergyCalculator {
    /** Light activity multiplier – kCal assumes everyday movement and adds tracked steps on top. */
    const val ACTIVITY_FACTOR = 1.44
    /** Average kcal burned per step for an adult walking at a normal pace. */
    const val KCAL_PER_STEP = 0.05
    const val DEFAULT_STEP_GOAL = 8_000

    fun bmr(profile: UserProfile): Int {
        val base = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age
        val adjusted = when (profile.gender) {
            Gender.Male -> base + 5
            Gender.Female -> base - 161
            Gender.Other -> base - 78 // midpoint of the two published constants
        }
        return roundTo10(adjusted)
    }

    fun needs(profile: UserProfile): EnergyNeeds {
        val bmr = bmr(profile)
        return EnergyNeeds(bmr = bmr, tdee = roundTo10(bmr * ACTIVITY_FACTOR))
    }

    /** Daily food intake target after applying the user's goal. */
    fun dailyIntakeTarget(profile: UserProfile): Int {
        val tdee = needs(profile).tdee
        return when (profile.goal) {
            Goal.LoseWeight -> (tdee - profile.dietDeficitKcal).coerceAtLeast(minSafeIntake(profile.gender))
            Goal.GainWeight -> tdee + UserProfile.DEFAULT_SURPLUS_KCAL
            Goal.MaintainWeight -> tdee
        }
    }

    fun stepsToKcal(steps: Int): Int = (steps * KCAL_PER_STEP).roundToInt()

    fun kcalToSteps(kcal: Int): Int = (kcal / KCAL_PER_STEP).roundToInt()

    private fun roundTo10(value: Double): Int = (value / 10.0).roundToInt() * 10

    /**
     * Widely used calorie floors for unsupervised weight loss (women 1,200 · men 1,500 kcal/day);
     * below them nutrient needs are hard to meet. "Other" uses the midpoint.
     */
    fun minSafeIntake(gender: Gender): Int = when (gender) {
        Gender.Female -> 1_200
        Gender.Male -> 1_500
        Gender.Other -> 1_350
    }

    const val MIN_SAFE_INTAKE = 1_200
}

package com.alphito.kcal.core.model

import kotlinx.collections.immutable.ImmutableSet
import java.time.LocalDate

data class WaterDay(val date: LocalDate, val consumedMl: Int, val goalMl: Int) {
    val glasses: Int get() = consumedMl / GLASS_ML
    val goalGlasses: Int get() = (goalMl / GLASS_ML).coerceAtLeast(1)
    val progress: Float get() = if (goalMl == 0) 0f else (consumedMl.toFloat() / goalMl).coerceIn(0f, 1f)

    companion object {
        const val GLASS_ML = 250
        const val DEFAULT_GOAL_ML = 2_000
    }
}

data class WeightEntry(val date: LocalDate, val weightKg: Double)

data class StepsDay(val date: LocalDate, val steps: Int) {
    val kcal: Int get() = EnergyCalculator.stepsToKcal(steps)
}

/**
 * When a glucose reading was taken – targets differ (ADA, non-pregnant adults):
 * fasting / before a meal 80–130 mg/dL, 1–2 h after eating < 180, bedtime 90–150.
 */
enum class SugarContext(val range: IntRange) {
    Fasting(80..130),
    BeforeMeal(80..130),
    AfterMeal(70..179),
    Bedtime(90..150),
}

data class SugarReading(val date: LocalDate, val mgDl: Int, val context: SugarContext = SugarContext.Fasting) {
    val inNormalRange: Boolean get() = mgDl in context.range
    val isLow: Boolean get() = mgDl < context.range.first
}

enum class DairyReaction { Bloating, Gas, Diarrhea, Nausea }

data class DairyLog(
    val date: LocalDate,
    val hadDairy: Boolean,
    val reactions: ImmutableSet<DairyReaction>,
    val note: String,
)

enum class IbsSymptom { Bloating, AbdominalPain, Diarrhea, Constipation }

enum class Severity(val score: Int) { Mild(1), Moderate(2), Severe(3) }

data class IbsLog(
    val date: LocalDate,
    val symptoms: ImmutableSet<IbsSymptom>,
    val severity: Severity,
)

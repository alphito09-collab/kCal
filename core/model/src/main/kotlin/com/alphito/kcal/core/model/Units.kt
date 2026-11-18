package com.alphito.kcal.core.model

import kotlin.math.roundToInt

data class FeetInches(val feet: Int, val inches: Int)

/** Height is always stored in centimetres; imperial is an input/display choice only. */
object HeightUnits {
    private const val CM_PER_INCH = 2.54

    fun toFeetInches(cm: Int): FeetInches {
        val totalInches = (cm / CM_PER_INCH).roundToInt()
        return FeetInches(totalInches / 12, totalInches % 12)
    }

    fun toCm(feet: Int, inches: Int): Int = ((feet * 12 + inches) * CM_PER_INCH).roundToInt()
}

/** Weight is always stored in kilograms; pounds are an input/display choice only. */
object WeightUnits {
    const val KG_PER_LB = 0.45359237

    fun toLb(kg: Double): Double = kg / KG_PER_LB
    fun toKg(lb: Double): Double = lb * KG_PER_LB

    /** Value in the user's unit, rounded to one decimal (what the UI shows / edits). */
    fun display(kg: Double, inLb: Boolean): Double = round1(if (inLb) toLb(kg) else kg)

    /** Converts a value typed in the user's unit back to kg. */
    fun toKg(value: Double, inLb: Boolean): Double = if (inLb) toKg(value) else value

    private fun round1(v: Double) = Math.round(v * 10.0) / 10.0
}

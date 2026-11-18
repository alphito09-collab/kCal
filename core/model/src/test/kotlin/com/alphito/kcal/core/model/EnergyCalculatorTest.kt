package com.alphito.kcal.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EnergyCalculatorTest {
    private val ayesha = UserProfile(name = "Ayesha", age = 24, gender = Gender.Female, heightCm = 165, weightKg = 68.0)

    @Test fun `bmr uses mifflin st jeor for women`() {
        // 10*68 + 6.25*165 - 5*24 - 161 = 1430.25
        assertEquals(1430, EnergyCalculator.bmr(ayesha))
    }

    @Test fun `tdee applies light activity factor`() {
        assertEquals(2060, EnergyCalculator.needs(ayesha).tdee)
    }

    @Test fun `diet deficit excludes the part covered by steps`() {
        assertEquals(700, ayesha.dietDeficitKcal)
        assertEquals(2060 - 700, EnergyCalculator.dailyIntakeTarget(ayesha))
    }

    @Test fun `intake target never drops below the safe minimum`() {
        val aggressive = ayesha.copy(dailyDeficitKcal = 3_000, addStepsToDeficit = false)
        assertEquals(EnergyCalculator.MIN_SAFE_INTAKE, EnergyCalculator.dailyIntakeTarget(aggressive))
    }

    @Test fun `steps convert to kcal`() {
        assertEquals(214, EnergyCalculator.stepsToKcal(4_280))
    }

    @Test fun `initials take first two words`() {
        assertEquals("AZ", ayesha.copy(name = "Aman Zahra").initials)
    }
}

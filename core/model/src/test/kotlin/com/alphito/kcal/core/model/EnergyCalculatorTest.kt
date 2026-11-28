package com.alphito.kcal.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyCalculatorTest {
    // BMR 1430 · TDEE 2060 · BMI 25.0
    private val ayesha = UserProfile(name = "Ayesha", age = 24, gender = Gender.Female, heightCm = 165, weightKg = 68.0)

    // ---------------------------------------------------------------- BMR / TDEE

    @Test fun `activity factor stays fixed at 1_44`() {
        assertEquals(1.44, EnergyCalculator.ACTIVITY_FACTOR, 0.0)
    }

    @Test fun `bmr uses mifflin st jeor for women`() {
        // 10*68 + 6.25*165 - 5*24 - 161 = 1430.25
        assertEquals(1430, EnergyCalculator.bmr(ayesha))
    }

    @Test fun `bmr uses mifflin st jeor for men`() {
        // 10*68 + 6.25*165 - 5*24 + 5 = 1596.25
        assertEquals(1600, EnergyCalculator.bmr(ayesha.copy(gender = Gender.Male)))
    }

    @Test fun `bmr for other uses the midpoint constant`() {
        // 10*68 + 6.25*165 - 5*24 - 78 = 1513.25
        assertEquals(1510, EnergyCalculator.bmr(ayesha.copy(gender = Gender.Other)))
    }

    @Test fun `tdee is bmr times the fixed factor rounded to 10`() {
        assertEquals(2060, EnergyCalculator.needs(ayesha).tdee) // 1430 * 1.44 = 2059.2
        assertEquals(2300, EnergyCalculator.needs(ayesha.copy(gender = Gender.Male)).tdee) // 1600 * 1.44 = 2304
        assertEquals(2170, EnergyCalculator.needs(ayesha.copy(gender = Gender.Other)).tdee) // 1510 * 1.44 = 2174.4
    }

    @Test fun `bmr uses current weight not the target`() {
        assertEquals(EnergyCalculator.bmr(ayesha), EnergyCalculator.bmr(ayesha.copy(targetWeightKg = 55.0)))
    }

    // ---------------------------------------------------------------- Weight loss

    @Test fun `recommended deficit is 20 percent of tdee`() {
        // 2060 * 0.20 = 412 → 400 (within the 25% / 1%-per-week caps of 500 / 700)
        assertEquals(400, EnergyCalculator.recommendedDeficit(ayesha))
    }

    @Test fun `max deficit is the lower of 25 percent tdee and 1 percent body weight a week`() {
        assertEquals(500, EnergyCalculator.maxDeficit(ayesha)) // min(515→500, 748→700)
    }

    @Test fun `loss plan splits the deficit between diet and steps`() {
        val plan = EnergyCalculator.plan(ayesha.copy(dailyDeficitKcal = 400, stepsDeficitKcal = 150))
        assertEquals(400, plan.deficitKcal)
        assertEquals(150, plan.stepsDeficitKcal)
        assertEquals(250, plan.dietDeficitKcal)
        assertEquals(2060 - 250, plan.intakeKcal)
        assertEquals(PlanLimit.None, plan.limit)
        assertEquals(1810, EnergyCalculator.dailyIntakeTarget(ayesha.copy(dailyDeficitKcal = 400, stepsDeficitKcal = 150)))
    }

    @Test fun `without steps the whole deficit comes from food`() {
        val plan = EnergyCalculator.plan(ayesha.copy(dailyDeficitKcal = 400, addStepsToDeficit = false))
        assertEquals(0, plan.stepsDeficitKcal)
        assertEquals(1660, plan.intakeKcal)
        assertEquals(EnergyCalculator.DEFAULT_STEP_GOAL, plan.stepGoal)
    }

    @Test fun `legacy flat 1000 kcal deficit is capped to the safe maximum`() {
        val plan = EnergyCalculator.plan(ayesha.copy(dailyDeficitKcal = 1_000, stepsDeficitKcal = 300))
        assertEquals(500, plan.deficitKcal)
        assertEquals(1860, plan.intakeKcal) // 2060 - (500 - 300)
    }

    @Test fun `steps share can never exceed the deficit`() {
        val plan = EnergyCalculator.plan(ayesha.copy(dailyDeficitKcal = 400, stepsDeficitKcal = 900))
        assertEquals(400, plan.stepsDeficitKcal)
        assertEquals(0, plan.dietDeficitKcal)
        assertEquals(2060, plan.intakeKcal)
    }

    @Test fun `expected weekly loss follows the total deficit`() {
        val plan = EnergyCalculator.plan(ayesha) // default 500 deficit, 150 from steps
        assertEquals(-500, plan.dailyBalanceKcal)
        assertEquals(-500 * 7 / 7_700.0, plan.expectedWeeklyChangeKg, 1e-9)
    }

    // ---------------------------------------------------------------- Safety floors

    @Test fun `intake target never drops below the safe minimum`() {
        // 70 y, 150 cm, 50 kg woman: TDEE 1340, deficit capped at 300 → 1040 → raised to 1200.
        val small = UserProfile("B", 70, Gender.Female, 150, 50.0, dailyDeficitKcal = 3_000, addStepsToDeficit = false)
        val plan = EnergyCalculator.plan(small)
        assertEquals(1_200, plan.intakeKcal)
        assertEquals(PlanLimit.CalorieFloor, plan.limit)
        assertEquals(140, plan.dietDeficitKcal)
    }

    @Test fun `men have a higher calorie floor`() {
        // 75 y, 155 cm, 55 kg man: TDEE 1660, max deficit 400 → 1260 → raised to 1500.
        val man = UserProfile("C", 75, Gender.Male, 155, 55.0, dailyDeficitKcal = 3_000, addStepsToDeficit = false)
        assertEquals(1_500, EnergyCalculator.dailyIntakeTarget(man))
    }

    @Test fun `floor never pushes a loss target above tdee`() {
        // 80 y, 145 cm, 45 kg woman: TDEE 1150 is already under the 1200 floor.
        val tiny = UserProfile("D", 80, Gender.Female, 145, 45.0, addStepsToDeficit = false)
        val plan = EnergyCalculator.plan(tiny)
        assertEquals(1150, plan.tdee)
        assertEquals(1150, plan.intakeKcal)
        assertEquals(0, plan.dietDeficitKcal)
    }

    @Test fun `underweight users get no deficit`() {
        val thin = ayesha.copy(weightKg = 48.0, targetWeightKg = 45.0) // BMI 17.6
        val plan = EnergyCalculator.plan(thin)
        assertEquals(PlanLimit.LowBodyWeight, plan.limit)
        assertEquals(plan.tdee, plan.intakeKcal)
        assertEquals(0, EnergyCalculator.recommendedDeficit(thin))
    }

    // ---------------------------------------------------------------- Gain / maintain

    @Test fun `gain uses a moderate surplus`() {
        // 2060 * 0.10 = 206 → 200 → raised to the 250 minimum (≤ 0.5%/week cap of 350)
        val plan = EnergyCalculator.plan(ayesha.copy(goal = Goal.GainWeight, targetWeightKg = 72.0))
        assertEquals(250, plan.surplusKcal)
        assertEquals(2310, plan.intakeKcal)
        assertEquals(0, plan.deficitKcal)
    }

    @Test fun `gain surplus scales with tdee up to the cap`() {
        // 30 y, 185 cm, 100 kg man: TDEE 2890 → 289 → 300
        val big = UserProfile("E", 30, Gender.Male, 185, 100.0, goal = Goal.GainWeight)
        assertEquals(3190, EnergyCalculator.dailyIntakeTarget(big))
    }

    @Test fun `gain surplus is limited for very light users`() {
        // 40 kg: 0.5% a week = 220 kcal/day → 200
        val light = UserProfile("F", 20, Gender.Female, 160, 40.0, goal = Goal.GainWeight)
        assertEquals(200, EnergyCalculator.plan(light).surplusKcal)
    }

    @Test fun `maintenance eats tdee`() {
        val plan = EnergyCalculator.plan(ayesha.copy(goal = Goal.MaintainWeight))
        assertEquals(2060, plan.intakeKcal)
        assertEquals(0, plan.dailyBalanceKcal)
    }

    @Test fun `reached loss goal switches to maintenance`() {
        val plan = EnergyCalculator.plan(ayesha.copy(targetWeightKg = 68.0))
        assertEquals(PlanLimit.GoalReached, plan.limit)
        assertEquals(2060, plan.intakeKcal)
    }

    @Test fun `target on the wrong side counts as reached`() {
        assertEquals(PlanLimit.GoalReached, EnergyCalculator.plan(ayesha.copy(targetWeightKg = 75.0)).limit)
        assertEquals(PlanLimit.GoalReached, EnergyCalculator.plan(ayesha.copy(goal = Goal.GainWeight, targetWeightKg = 60.0)).limit)
    }

    // ---------------------------------------------------------------- Steps

    @Test fun `steps convert to kcal`() {
        assertEquals(214, EnergyCalculator.stepsToKcal(4_280))
        assertEquals(0, EnergyCalculator.stepsToKcal(-50))
    }

    @Test fun `only steps above the baseline add to expenditure`() {
        assertEquals(0, EnergyCalculator.extraStepsKcal(4_000))
        assertEquals(200, EnergyCalculator.extraStepsKcal(9_000))
        assertEquals(2060 + 350, EnergyCalculator.dailyExpenditure(2060, 12_000))
        assertEquals(2060, EnergyCalculator.dailyExpenditure(2060, 0))
    }

    @Test fun `steps share sets the daily step goal`() {
        assertEquals(8_000, EnergyCalculator.stepsForExtraKcal(150)) // 5,000 baseline + 3,000
        val plan = EnergyCalculator.plan(ayesha.copy(dailyDeficitKcal = 500, stepsDeficitKcal = 300))
        assertEquals(11_000, plan.stepGoal)
    }

    @Test fun `steps are never added back to the food target`() {
        val withSteps = EnergyCalculator.plan(ayesha.copy(dailyDeficitKcal = 400, stepsDeficitKcal = 200))
        // Steps lower the food cut, they don't raise intake above TDEE.
        assertTrue(withSteps.intakeKcal <= withSteps.tdee)
        assertEquals(withSteps.deficitKcal, withSteps.dietDeficitKcal + withSteps.stepsDeficitKcal)
    }

    // ---------------------------------------------------------------- Goal progress

    @Test fun `loss progress uses start current and target`() {
        val p = EnergyCalculator.goalProgress(ayesha.copy(targetWeightKg = 60.0), startWeightKg = 70.0)!!
        assertEquals(8.0, p.remainingKg, 1e-9)
        assertEquals(0.2f, p.fraction, 1e-6f)
        assertFalse(p.reached)
        assertEquals(18, p.weeksToGoal) // 8 kg / 0.4545 kg per week = 17.6
        assertFalse(p.targetOutsideHealthyBmi)
    }

    @Test fun `gain progress estimates weeks from the surplus`() {
        val p = EnergyCalculator.goalProgress(ayesha.copy(goal = Goal.GainWeight, targetWeightKg = 72.0))!!
        assertEquals(4.0, p.remainingKg, 1e-9)
        assertEquals(0f, p.fraction, 0f)
        assertEquals(18, p.weeksToGoal) // 4 kg / (250 * 7 / 7700) = 17.6
    }

    @Test fun `reached goal reports full progress and no eta`() {
        val p = EnergyCalculator.goalProgress(ayesha.copy(targetWeightKg = 68.05), startWeightKg = 72.0)!!
        assertTrue(p.reached)
        assertEquals(1f, p.fraction, 0f)
        assertEquals(0.0, p.remainingKg, 0.0)
        assertNull(p.weeksToGoal)
    }

    @Test fun `unhealthy targets are flagged`() {
        assertTrue(EnergyCalculator.goalProgress(ayesha.copy(targetWeightKg = 48.0))!!.targetOutsideHealthyBmi)
        assertTrue(EnergyCalculator.goalProgress(ayesha.copy(goal = Goal.GainWeight, targetWeightKg = 80.0))!!.targetOutsideHealthyBmi)
    }

    @Test fun `no eta when the plan cannot move weight`() {
        val p = EnergyCalculator.goalProgress(ayesha.copy(weightKg = 48.0, targetWeightKg = 45.0))!!
        assertNull(p.weeksToGoal)
    }

    @Test fun `no goal progress for maintenance or missing target`() {
        assertNull(EnergyCalculator.goalProgress(ayesha))
        assertNull(EnergyCalculator.goalProgress(ayesha.copy(goal = Goal.MaintainWeight, targetWeightKg = 60.0)))
        assertNull(EnergyCalculator.goalProgress(ayesha.copy(targetWeightKg = -5.0)))
    }

    // ---------------------------------------------------------------- Invalid input

    @Test fun `invalid profiles produce an empty plan instead of negative numbers`() {
        val empty = UserProfile("G", age = 0, gender = Gender.Female, heightCm = 0, weightKg = 0.0)
        assertFalse(EnergyCalculator.isValid(empty))
        assertEquals(0, EnergyCalculator.bmr(empty))
        assertEquals(0, EnergyCalculator.dailyIntakeTarget(empty))
        assertEquals(0, EnergyCalculator.recommendedDeficit(empty))
        assertNull(EnergyCalculator.goalProgress(empty.copy(targetWeightKg = 60.0)))
        assertEquals(0, EnergyCalculator.dailyIntakeTarget(ayesha.copy(weightKg = Double.NaN)))
    }

    @Test fun `negative stored deficits are ignored`() {
        assertEquals(2060, EnergyCalculator.dailyIntakeTarget(ayesha.copy(dailyDeficitKcal = -200)))
    }

    // ---------------------------------------------------------------- End-to-end scenarios

    @Test fun `scenario weight loss - female 22y 165cm 70kg to 60kg at 8000 steps`() {
        // Onboarding stores the recommended deficit and the default 150 kcal steps share.
        val base = UserProfile("S1", 22, Gender.Female, 165, 70.0, goal = Goal.LoseWeight, targetWeightKg = 60.0)
        val p = base.copy(dailyDeficitKcal = EnergyCalculator.recommendedDeficit(base))
        val plan = EnergyCalculator.plan(p)
        assertEquals(1460, plan.bmr)             // 700 + 1031.25 - 110 - 161 = 1460.25
        assertEquals(2100, plan.tdee)            // 1460 * 1.44 = 2102.4
        assertEquals(400, plan.deficitKcal)      // 2100 * 20% = 420 → 400
        assertEquals(150, plan.stepsDeficitKcal)
        assertEquals(250, plan.dietDeficitKcal)
        assertEquals(1850, plan.intakeKcal)
        assertEquals(PlanLimit.None, plan.limit) // 1850 ≥ 1200 floor
        assertEquals(8_000, plan.stepGoal)
        assertEquals(400, EnergyCalculator.stepsToKcal(8_000))      // gross walking kcal
        assertEquals(150, EnergyCalculator.extraStepsKcal(8_000))   // above the 5,000 already in TDEE
        // The 150 kcal from steps is counted once, as burn. Intake doesn't rise with steps.
        assertEquals(-400, plan.intakeKcal - EnergyCalculator.dailyExpenditure(plan.tdee, 8_000))
        assertEquals(28, EnergyCalculator.goalProgress(p)!!.weeksToGoal) // 10 kg / 0.364 kg a week
    }

    @Test fun `scenario weight gain - male 25y 175cm 60kg to 70kg at 10000 steps`() {
        val p = UserProfile("S2", 25, Gender.Male, 175, 60.0, goal = Goal.GainWeight, targetWeightKg = 70.0)
        val plan = EnergyCalculator.plan(p)
        assertEquals(1570, plan.bmr)             // 600 + 1093.75 - 125 + 5 = 1573.75
        assertEquals(2260, plan.tdee)            // 1570 * 1.44 = 2260.8
        assertEquals(250, plan.surplusKcal)      // 226 → 250 minimum, under the 300 cap
        assertEquals(2510, plan.intakeKcal)
        assertEquals(2510, plan.intakeKcal)      // no step history yet
        assertEquals(500, EnergyCalculator.stepsToKcal(10_000))
        assertEquals(250, EnergyCalculator.extraStepsKcal(10_000))
        // With a week averaging 10,000 steps the 250 kcal walked above the baseline is eaten back once.
        val active = EnergyCalculator.plan(p, averageSteps = 10_000)
        assertEquals(250, active.stepsAllowanceKcal)
        assertEquals(2760, active.intakeKcal)    // 2260 + 250 + 250
        assertEquals(250, active.intakeKcal - EnergyCalculator.dailyExpenditure(active.tdee, 10_000))
        assertEquals(250, active.dailyBalanceKcal)
    }

    // ---------------------------------------------------------------- Weight-gain step allowance

    private val gainer = UserProfile("G2", 25, Gender.Male, 175, 60.0, goal = Goal.GainWeight, targetWeightKg = 70.0)

    @Test fun `gain allowance is zero at or below the baseline`() {
        for (steps in listOf(0, 3_000, 4_999, 5_000)) {
            val plan = EnergyCalculator.plan(gainer, averageSteps = steps)
            assertEquals("steps=$steps", 0, plan.stepsAllowanceKcal)
            assertEquals("steps=$steps", 2510, plan.intakeKcal)
        }
    }

    @Test fun `gain allowance covers only steps above the baseline`() {
        assertEquals(50, EnergyCalculator.plan(gainer, averageSteps = 6_000).stepsAllowanceKcal)
        assertEquals(250, EnergyCalculator.plan(gainer, averageSteps = 10_000).stepsAllowanceKcal)
        assertEquals(500, EnergyCalculator.plan(gainer, averageSteps = 15_000).stepsAllowanceKcal)
        assertEquals(2260 + 500 + 250, EnergyCalculator.dailyIntakeTarget(gainer, averageSteps = 15_000))
    }

    @Test fun `gain surplus stays the same whatever the activity level`() {
        for (steps in listOf(0, 2_000, 5_000, 7_500, 10_000, 15_000, 25_000)) {
            val plan = EnergyCalculator.plan(gainer, averageSteps = steps)
            // Balance on a day at that activity = intake − (TDEE + extra steps): always the planned surplus.
            assertEquals("steps=$steps", 250, plan.intakeKcal - EnergyCalculator.dailyExpenditure(plan.tdee, steps))
            assertEquals("steps=$steps", 250, plan.dailyBalanceKcal)
            assertEquals("steps=$steps", 250 * 7 / 7_700.0, plan.expectedWeeklyChangeKg, 1e-9)
        }
    }

    @Test fun `gain eta does not change with activity`() {
        val idle = EnergyCalculator.goalProgress(gainer, averageSteps = 0)!!
        val active = EnergyCalculator.goalProgress(gainer, averageSteps = 12_000)!!
        assertEquals(idle.weeksToGoal, active.weeksToGoal)
        assertEquals(44, active.weeksToGoal) // 10 kg / (250 * 7 / 7700)
    }

    @Test fun `step average never changes loss or maintenance targets`() {
        val loss = ayesha.copy(dailyDeficitKcal = 400, stepsDeficitKcal = 150)
        val maintain = ayesha.copy(goal = Goal.MaintainWeight)
        for (steps in listOf(0, 5_000, 12_000)) {
            assertEquals(1810, EnergyCalculator.dailyIntakeTarget(loss, steps))
            assertEquals(0, EnergyCalculator.plan(loss, steps).stepsAllowanceKcal)
            assertEquals(2060, EnergyCalculator.dailyIntakeTarget(maintain, steps))
        }
    }

    @Test fun `reached gain goal ignores the step allowance`() {
        val reached = EnergyCalculator.plan(gainer.copy(weightKg = 70.0), averageSteps = 12_000)
        assertEquals(PlanLimit.GoalReached, reached.limit)
        assertEquals(0, reached.stepsAllowanceKcal)
        assertEquals(reached.tdee, reached.intakeKcal)
    }

    // ---------------------------------------------------------------- 7-day step average

    private val today = java.time.LocalDate.of(2026, 11, 25)
    private fun day(back: Long, steps: Int) = StepsDay(today.minusDays(back), steps)

    @Test fun `average uses the previous seven days and skips today`() {
        val history = (1L..7L).map { day(it, 10_000) } + day(0, 30_000)
        assertEquals(10_000, EnergyCalculator.averageSteps(history, today))
    }

    @Test fun `average ignores days older than a week`() {
        val history = listOf(day(1, 8_000), day(2, 6_000), day(8, 40_000), day(30, 40_000))
        assertEquals(7_000, EnergyCalculator.averageSteps(history, today))
    }

    @Test fun `average counts only tracked days`() {
        // Two tracked days in the week: one day of data isn't diluted by five untracked ones.
        assertEquals(9_000, EnergyCalculator.averageSteps(listOf(day(1, 12_000), day(3, 6_000), day(4, 0)), today))
    }

    @Test fun `average is zero without history`() {
        assertEquals(0, EnergyCalculator.averageSteps(emptyList(), today))
        assertEquals(0, EnergyCalculator.averageSteps(listOf(day(0, 9_000)), today))
    }

    @Test fun `scenario maintenance - female 28y 160cm 55kg at 5000 steps`() {
        val p = UserProfile("S3", 28, Gender.Female, 160, 55.0, goal = Goal.MaintainWeight)
        val plan = EnergyCalculator.plan(p)
        assertEquals(1250, plan.bmr)             // 550 + 1000 - 140 - 161 = 1249
        assertEquals(1800, plan.tdee)            // 1250 * 1.44
        assertEquals(1800, plan.intakeKcal)
        assertEquals(250, EnergyCalculator.stepsToKcal(5_000))
        assertEquals(0, EnergyCalculator.extraStepsKcal(5_000))     // all inside TDEE
        assertEquals(1800, EnergyCalculator.dailyExpenditure(plan.tdee, 5_000))
        // Choosing "lose" with target = current also falls back to maintenance.
        val sameTarget = EnergyCalculator.plan(p.copy(goal = Goal.LoseWeight, targetWeightKg = 55.0))
        assertEquals(PlanLimit.GoalReached, sameTarget.limit)
        assertEquals(1800, sameTarget.intakeKcal)
    }

    // ---------------------------------------------------------------- Unrelated model helpers

    @Test fun `legacy city regions migrate to the default country`() {
        assertEquals("PK", Regions.normalize("Pakistan — Lahore"))
        assertEquals("DE", Regions.normalize("de"))
        assertEquals(CuisineRegion.LatinAmerica, Regions.regionOf("BR"))
    }

    @Test fun `initials take first two words`() {
        assertEquals("AZ", ayesha.copy(name = "Aman Zahra").initials)
    }
}

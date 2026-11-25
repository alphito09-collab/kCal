package com.alphito.kcal.core.model

import kotlin.math.abs
import kotlin.math.ceil
import java.time.LocalDate
import kotlin.math.roundToInt

data class EnergyNeeds(val bmr: Int, val tdee: Int)

/** Why a plan differs from the plain goal-based recommendation. */
enum class PlanLimit {
    None,
    /** Target weight already reached (or set on the wrong side of the current weight): the plan holds maintenance. */
    GoalReached,
    /** BMI is already below 18.5, so no weight-loss deficit is planned. */
    LowBodyWeight,
    /** TDEE − diet deficit fell below the calorie floor, so intake was raised to the floor. */
    CalorieFloor,
}

/**
 * One day's calorie budget. Every screen (onboarding, dashboard, diet plan, meal insights, steps,
 * progress, weight) reads it from [EnergyCalculator.plan], so none of them recompute the pieces.
 */
data class CaloriePlan(
    val bmr: Int,
    val tdee: Int,
    /** Total daily deficit (diet + extra steps). 0 unless the plan is losing weight. */
    val deficitKcal: Int,
    /** Part of [deficitKcal] burned by steps above [EnergyCalculator.BASELINE_STEPS]. */
    val stepsDeficitKcal: Int,
    /** Part of the deficit taken off food, so TDEE − intake while losing weight. */
    val dietDeficitKcal: Int,
    /** Daily surplus while gaining weight, otherwise 0. */
    val surplusKcal: Int,
    /**
     * Weight gain only: calories burned by the 7-day average steps above [EnergyCalculator.BASELINE_STEPS],
     * added to intake so walking doesn't eat the surplus. Counted once: it is the same burn the expenditure side adds.
     */
    val stepsAllowanceKcal: Int = 0,
    /** Daily food target used by meal plans, dashboard, tracking and insights. */
    val intakeKcal: Int,
    val stepGoal: Int,
    val limit: PlanLimit,
) {
    /** Planned daily energy balance including the planned steps (negative means a deficit). */
    val dailyBalanceKcal: Int get() = intakeKcal - tdee - stepsDeficitKcal - stepsAllowanceKcal

    /** Expected weekly change in kg if the plan is followed (signed). */
    val expectedWeeklyChangeKg: Double get() = dailyBalanceKcal * 7 / EnergyCalculator.KCAL_PER_KG
}

/** How far the user is on the way to their target weight. */
data class GoalProgress(
    val startWeightKg: Double,
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    /** Kilograms still to lose or gain. 0 once the target is reached. */
    val remainingKg: Double,
    /** Share of the start→target distance already covered, in 0..1. */
    val fraction: Float,
    val reached: Boolean,
    /** Expected change per week at the current plan (kg, signed). */
    val weeklyChangeKg: Double,
    /** Weeks to the target at that pace. null when already reached or when the plan doesn't move weight. */
    val weeksToGoal: Int?,
    /** The target's BMI falls outside 18.5–25 in the direction of travel. */
    val targetOutsideHealthyBmi: Boolean,
)

/**
 * Pure energy maths (Mifflin–St Jeor). Kept free of Android so it is trivially unit-testable.
 *
 * BMR and TDEE always use the *current* weight. The target weight only decides when a goal is
 * reached and how long it takes. Steps count once: the activity factor already covers everyday
 * movement up to [BASELINE_STEPS]. Only steps above that add to expenditure. When losing weight,
 * those calories are the "through steps" share of the deficit and are never added back to food.
 * When gaining, the 7-day average of them is added to intake so the surplus survives the walking.
 */
object EnergyCalculator {
    /** Light activity multiplier: kCal assumes everyday movement and adds tracked steps on top. */
    const val ACTIVITY_FACTOR = 1.44
    /** Average kcal burned per step for an adult walking at a normal pace. */
    const val KCAL_PER_STEP = 0.05
    const val DEFAULT_STEP_GOAL = 8_000
    /** Everyday steps already covered by [ACTIVITY_FACTOR] ("low active", ~5k a day). */
    const val BASELINE_STEPS = 5_000
    /** Days of step history averaged for the weight-gain step allowance. */
    const val RECENT_STEPS_DAYS = 7
    /** Energy in one kilogram of body weight change (commonly used ~7,700 kcal). */
    const val KCAL_PER_KG = 7_700.0

    /** Recommended deficit: 20% of TDEE, at least 300 kcal… */
    const val DEFICIT_SHARE = 0.20
    const val MIN_DEFICIT_KCAL = 300
    /** …but never more than 25% of TDEE or more than 1% of body weight lost per week. */
    const val MAX_DEFICIT_SHARE = 0.25
    const val MAX_WEEKLY_LOSS_FRACTION = 0.01

    /** Lean-gain surplus: 10% of TDEE within 250–500 kcal, at most 0.5% of body weight gained per week. */
    const val SURPLUS_SHARE = 0.10
    const val MIN_SURPLUS_KCAL = 250
    const val MAX_SURPLUS_KCAL = 500
    const val MAX_WEEKLY_GAIN_FRACTION = 0.005

    const val UNDERWEIGHT_BMI = 18.5
    const val OVERWEIGHT_BMI = 25.0
    /** Weigh-ins within this distance of the target count as reaching it. */
    const val GOAL_TOLERANCE_KG = 0.1

    /** Age, height and weight are present and positive. Anything else gets an empty (all-zero) plan. */
    fun isValid(profile: UserProfile): Boolean =
        profile.age > 0 && profile.heightCm > 0 && profile.weightKg > 0.0 && profile.weightKg.isFinite()

    fun bmr(profile: UserProfile): Int {
        if (!isValid(profile)) return 0
        val base = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age
        val adjusted = when (profile.gender) {
            Gender.Male -> base + 5
            Gender.Female -> base - 161
            Gender.Other -> base - 78 // midpoint of the two published constants
        }
        return roundTo10(adjusted).coerceAtLeast(0)
    }

    fun needs(profile: UserProfile): EnergyNeeds {
        val bmr = bmr(profile)
        return EnergyNeeds(bmr = bmr, tdee = roundTo10(bmr * ACTIVITY_FACTOR))
    }

    fun bmi(weightKg: Double, heightCm: Int): Double =
        if (heightCm <= 0 || weightKg <= 0.0) 0.0 else weightKg / ((heightCm / 100.0) * (heightCm / 100.0))

    /**
     * The daily calorie budget for this profile. [averageSteps] is the recent daily average (see
     * [averageSteps]); only weight gain uses it, so loss and maintenance targets never depend on steps.
     */
    fun plan(profile: UserProfile, averageSteps: Int = 0): CaloriePlan {
        val needs = needs(profile)
        val maintenance = CaloriePlan(
            bmr = needs.bmr, tdee = needs.tdee, deficitKcal = 0, stepsDeficitKcal = 0, dietDeficitKcal = 0,
            surplusKcal = 0, intakeKcal = needs.tdee, stepGoal = DEFAULT_STEP_GOAL, limit = PlanLimit.None,
        )
        if (!isValid(profile)) return maintenance // all zeros: bmr() returns 0 for invalid input
        val tdee = needs.tdee
        return when (profile.goal) {
            Goal.MaintainWeight -> maintenance
            Goal.GainWeight ->
                if (goalReached(profile)) maintenance.copy(limit = PlanLimit.GoalReached)
                else {
                    val surplus = surplus(profile, tdee)
                    val allowance = extraStepsKcal(averageSteps)
                    maintenance.copy(surplusKcal = surplus, stepsAllowanceKcal = allowance, intakeKcal = tdee + allowance + surplus)
                }
            Goal.LoseWeight -> when {
                isUnderweight(profile) -> maintenance.copy(limit = PlanLimit.LowBodyWeight)
                goalReached(profile) -> maintenance.copy(limit = PlanLimit.GoalReached)
                else -> deficitPlan(profile, maintenance)
            }
        }
    }

    /** Daily food intake target after applying the user's goal. */
    fun dailyIntakeTarget(profile: UserProfile, averageSteps: Int = 0): Int = plan(profile, averageSteps).intakeKcal

    /**
     * Deficit suggested at onboarding, before the user splits it between steps and diet.
     * 0 when weight loss isn't appropriate (goal reached, underweight, invalid profile).
     */
    fun recommendedDeficit(profile: UserProfile): Int {
        if (!isValid(profile) || isUnderweight(profile) || goalReached(profile)) return 0
        val recommended = roundTo50(needs(profile).tdee * DEFICIT_SHARE).coerceAtLeast(MIN_DEFICIT_KCAL)
        return recommended.coerceAtMost(maxDeficit(profile))
    }

    /** Upper safety bound for any deficit: 25% of TDEE and 1% of body weight per week. */
    fun maxDeficit(profile: UserProfile): Int {
        if (!isValid(profile)) return 0
        val byTdee = floorTo50(needs(profile).tdee * MAX_DEFICIT_SHARE)
        val byWeight = floorTo50(profile.weightKg * MAX_WEEKLY_LOSS_FRACTION * KCAL_PER_KG / 7)
        return minOf(byTdee, byWeight).coerceAtLeast(0)
    }

    fun stepsToKcal(steps: Int): Int = (steps.coerceAtLeast(0) * KCAL_PER_STEP).roundToInt()

    fun kcalToSteps(kcal: Int): Int = (kcal.coerceAtLeast(0) / KCAL_PER_STEP).roundToInt()

    /** Calories from steps above [BASELINE_STEPS]. Only these are on top of TDEE. */
    fun extraStepsKcal(steps: Int): Int = stepsToKcal(steps - BASELINE_STEPS)

    /** Total daily steps needed to burn [extraKcal] on top of the baseline already in TDEE. */
    fun stepsForExtraKcal(extraKcal: Int): Int = BASELINE_STEPS + kcalToSteps(extraKcal)

    /**
     * Average daily steps over the [RECENT_STEPS_DAYS] days before [today]. Today is still in progress,
     * so it's excluded. Only days with tracked steps count, so a few missing days don't drag the
     * average down. Returns 0 when nothing was tracked.
     */
    fun averageSteps(history: List<StepsDay>, today: LocalDate): Int {
        val from = today.minusDays(RECENT_STEPS_DAYS.toLong())
        val days = history.filter { !it.date.isBefore(from) && it.date.isBefore(today) && it.steps > 0 }
        return if (days.isEmpty()) 0 else (days.sumOf { it.steps.toLong() } / days.size).toInt()
    }

    /** Energy burned on a day with [steps] tracked steps: TDEE plus only the steps above the baseline. */
    fun dailyExpenditure(tdee: Int, steps: Int): Int = tdee + extraStepsKcal(steps)

    /**
     * Progress towards [UserProfile.targetWeightKg]. [startWeightKg] is the first weigh-in
     * (falls back to the current weight). null for maintenance or when no target is set.
     */
    fun goalProgress(profile: UserProfile, startWeightKg: Double? = null, averageSteps: Int = 0): GoalProgress? {
        val target = profile.targetWeightKg?.takeIf { it > 0.0 && it.isFinite() } ?: return null
        if (profile.goal == Goal.MaintainWeight || !isValid(profile)) return null
        val losing = profile.goal == Goal.LoseWeight
        val current = profile.weightKg
        val start = startWeightKg?.takeIf { it > 0.0 && it.isFinite() } ?: current
        val remaining = (if (losing) current - target else target - current).coerceAtLeast(0.0)
        val reached = remaining <= GOAL_TOLERANCE_KG
        val distance = if (losing) start - target else target - start
        val fraction = when {
            reached -> 1f
            distance <= GOAL_TOLERANCE_KG -> 0f
            else -> ((distance - remaining) / distance).toFloat().coerceIn(0f, 1f)
        }
        val weekly = plan(profile, averageSteps).expectedWeeklyChangeKg
        val pace = abs(weekly)
        val targetBmi = bmi(target, profile.heightCm)
        return GoalProgress(
            startWeightKg = start,
            currentWeightKg = current,
            targetWeightKg = target,
            remainingKg = if (reached) 0.0 else remaining,
            fraction = fraction,
            reached = reached,
            weeklyChangeKg = weekly,
            weeksToGoal = if (reached || pace < MIN_PACE_KG) null else ceil(remaining / pace).toInt(),
            targetOutsideHealthyBmi = if (losing) targetBmi < UNDERWEIGHT_BMI else targetBmi > OVERWEIGHT_BMI,
        )
    }

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

    private fun deficitPlan(profile: UserProfile, maintenance: CaloriePlan): CaloriePlan {
        val tdee = maintenance.tdee
        // The stored deficit is honoured, but never beyond the safety bound (older profiles stored a flat 1,000).
        val deficit = profile.dailyDeficitKcal.coerceAtLeast(0).coerceAtMost(maxDeficit(profile))
        val steps = if (profile.addStepsToDeficit) profile.stepsDeficitKcal.coerceIn(0, deficit) else 0
        // A floor above TDEE would mean eating a surplus while losing: cap it at TDEE.
        val floor = minOf(minSafeIntake(profile.gender), tdee)
        val wanted = tdee - (deficit - steps)
        val intake = maxOf(wanted, floor)
        return maintenance.copy(
            deficitKcal = (tdee - intake) + steps,
            stepsDeficitKcal = steps,
            dietDeficitKcal = tdee - intake,
            intakeKcal = intake,
            stepGoal = if (steps > 0) maxOf(DEFAULT_STEP_GOAL, stepsForExtraKcal(steps)) else DEFAULT_STEP_GOAL,
            limit = if (intake > wanted) PlanLimit.CalorieFloor else PlanLimit.None,
        )
    }

    private fun surplus(profile: UserProfile, tdee: Int): Int {
        val byWeight = floorTo50(profile.weightKg * MAX_WEEKLY_GAIN_FRACTION * KCAL_PER_KG / 7)
        return roundTo50(tdee * SURPLUS_SHARE).coerceIn(MIN_SURPLUS_KCAL, MAX_SURPLUS_KCAL).coerceAtMost(byWeight)
    }

    private fun isUnderweight(profile: UserProfile): Boolean = bmi(profile.weightKg, profile.heightCm) < UNDERWEIGHT_BMI

    /** Target reached, or set on the wrong side of the current weight for the chosen goal. */
    private fun goalReached(profile: UserProfile): Boolean {
        val target = profile.targetWeightKg?.takeIf { it > 0.0 && it.isFinite() } ?: return false
        return when (profile.goal) {
            Goal.LoseWeight -> profile.weightKg - target <= GOAL_TOLERANCE_KG
            Goal.GainWeight -> target - profile.weightKg <= GOAL_TOLERANCE_KG
            Goal.MaintainWeight -> false
        }
    }

    private const val MIN_PACE_KG = 0.01

    private fun roundTo10(value: Double): Int = (value / 10.0).roundToInt() * 10
    private fun roundTo50(value: Double): Int = (value / 50.0).roundToInt() * 50
    private fun floorTo50(value: Double): Int = (value / 50.0).toInt() * 50
}

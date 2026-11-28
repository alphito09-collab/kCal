package com.alphito.kcal.feature.dashboard.domain

import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.data.repository.MealPlanRepository
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.data.repository.StepsRepository
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.model.EnergyCalculator
import com.alphito.kcal.core.model.MealSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class DashboardSnapshot(
    val firstName: String,
    val eatenKcal: Int,
    val targetKcal: Int,
    val stepsKcal: Int,
    val streakDays: Int,
    val nextMeal: MealSlot?,
    val isPremium: Boolean,
    val trialPromptSeen: Boolean,
)

/** Merges profile, today's plan, steps, streak and premium state into one stream. */
class ObserveDashboardUseCase(
    private val users: UserRepository,
    private val mealPlans: MealPlanRepository,
    private val steps: StepsRepository,
    private val preferences: PreferencesRepository,
    private val clock: AppClock,
) {
    operator fun invoke(): Flow<DashboardSnapshot> {
        val today = clock.today()
        return combine(
            users.profile,
            mealPlans.observeDays(today, today),
            steps.observeDay(today),
            mealPlans.observeStreak(today),
            preferences.preferences,
        ) { profile, days, todaySteps, streak, prefs ->
            val day = days.firstOrNull()
            DashboardSnapshot(
                firstName = profile?.name?.substringBefore(' ').orEmpty(),
                eatenKcal = day?.eatenKcal ?: 0,
                // Same number the diet plan shows: the day carries the target from the calorie plan.
                targetKcal = day?.targetKcal ?: 0,
                stepsKcal = EnergyCalculator.stepsToKcal(todaySteps),
                streakDays = streak,
                nextMeal = day?.nextUnchecked,
                isPremium = prefs.subscription.isPremium,
                trialPromptSeen = prefs.freeTrialPromptSeen,
            )
        }
    }

    suspend fun ensurePlan() = mealPlans.ensureWeekPlanned(clock.today())

    suspend fun check(slot: MealSlot) = mealPlans.setChecked(slot.date, slot.mealType, true)
}

package com.alphito.kcal.feature.onboarding.domain

import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.data.repository.MealPlanRepository
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.data.repository.SampleDataSeeder
import com.alphito.kcal.core.data.repository.TrackingStartRepository
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.data.repository.WeightRepository

/** Input rules for the "Tell us about you" form. */
object ProfileRules {
    val AGE = 13..100
    val HEIGHT_CM = 120..230
    val WEIGHT_KG = 30.0..300.0

    fun isValid(name: String, age: Int?, heightCm: Int?, weightKg: Double?): Boolean =
        name.isNotBlank() && age in AGE && heightCm in HEIGHT_CM && weightKg != null && weightKg in WEIGHT_KG
}

/** Final onboarding step: build the week plan, record the first weigh-in and unlock the app. */
class CompleteOnboardingUseCase(
    private val users: UserRepository,
    private val preferences: PreferencesRepository,
    private val mealPlans: MealPlanRepository,
    private val weights: WeightRepository,
    private val sampleData: SampleDataSeeder,
    private val trackingStart: TrackingStartRepository,
    private val clock: AppClock,
    private val sampleDataEnabled: Boolean,
) {
    suspend operator fun invoke() {
        val profile = users.current() ?: return
        val today = clock.today()
        // Debug sample history starts the tracking period at its first fake day so Progress stays consistent.
        val seededFrom = if (sampleDataEnabled) sampleData.seed(today, profile.weightKg) else null
        weights.log(today, profile.weightKg)
        mealPlans.regenerate(today)
        mealPlans.ensureWeekPlanned(today)
        preferences.setOnboardingDay(today)
        trackingStart.init(seededFrom ?: today) // no-op if already set: the start date never moves
        preferences.setOnboardingComplete(true)
    }
}

/** Editing region/budget/conditions from Settings must rebuild the rest of this week. */
class RegeneratePlanUseCase(private val mealPlans: MealPlanRepository, private val clock: AppClock) {
    suspend operator fun invoke() = mealPlans.regenerate(clock.today())
}

package com.alphito.kcal.core.data.repository

import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.model.CaloriePlan
import com.alphito.kcal.core.model.EnergyCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/**
 * The user's calorie plan with recent activity applied. Meal plans, dashboard, insights and weight
 * tracking all read targets from here, so they never disagree about the number.
 */
interface CaloriePlanRepository {
    /** null until onboarding has created a profile. */
    val plan: Flow<CaloriePlan?>
    /** Average daily steps over the previous week (the input to the weight-gain step allowance). */
    val averageSteps: Flow<Int>
    suspend fun current(): CaloriePlan?
}

internal class CaloriePlanRepositoryImpl(
    private val users: UserRepository,
    private val steps: StepsRepository,
    private val clock: AppClock,
) : CaloriePlanRepository {
    override val averageSteps: Flow<Int> get() = steps.observeRecentAverage(clock.today())

    override val plan: Flow<CaloriePlan?>
        get() = combine(users.profile, averageSteps) { profile, avg -> profile?.let { EnergyCalculator.plan(it, avg) } }
            .distinctUntilChanged()

    override suspend fun current(): CaloriePlan? = plan.first()
}

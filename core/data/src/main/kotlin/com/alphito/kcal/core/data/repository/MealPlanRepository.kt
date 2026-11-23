package com.alphito.kcal.core.data.repository

import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.data.domain.MealInsights
import com.alphito.kcal.core.data.domain.MealPlanGenerator
import com.alphito.kcal.core.data.mapper.toModel
import com.alphito.kcal.core.database.dao.FoodDao
import com.alphito.kcal.core.database.dao.MealPlanDao
import com.alphito.kcal.core.datastore.PreferencesDataSource
import com.alphito.kcal.core.database.entity.MealCheckEntity
import com.alphito.kcal.core.database.entity.PlannedItemEntity
import com.alphito.kcal.core.model.DayPlan
import com.alphito.kcal.core.model.EnergyCalculator
import com.alphito.kcal.core.model.MealInsight
import com.alphito.kcal.core.model.MealSlot
import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.PlannedItem
import com.alphito.kcal.core.model.SwapCandidate
import com.alphito.kcal.core.model.UserProfile
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

interface MealPlanRepository {
    fun observeDays(from: LocalDate, to: LocalDate): Flow<List<DayPlan>>
    fun observeItem(itemId: Long): Flow<PlannedItem?>
    /** Consecutive days (ending today, or yesterday if today is not started) with a checked meal. */
    fun observeStreak(today: LocalDate): Flow<Int>
    suspend fun ensureWeekPlanned(today: LocalDate)
    suspend fun regenerate(today: LocalDate)
    suspend fun setChecked(date: LocalDate, mealType: MealType, checked: Boolean)
    suspend fun swapCandidates(itemId: Long): List<SwapCandidate>
    suspend fun swap(itemId: Long, foodId: String, servings: Double)
    /** The item plus the reasons it is in the plan, for the meal-info sheet. */
    fun observeInsight(itemId: Long): Flow<MealInsight?>
    /** Foods from meals the user ticked on these days (health-log pattern insights). */
    suspend fun foodsEatenOn(days: List<LocalDate>): List<com.alphito.kcal.core.model.Food>
}

internal class MealPlanRepositoryImpl(
    private val mealPlanDao: MealPlanDao,
    private val foodDao: FoodDao,
    private val catalogRepository: CatalogRepository,
    private val preferences: PreferencesDataSource,
    private val userRepository: UserRepository,
    private val generator: MealPlanGenerator,
    private val dispatchers: DispatcherProvider,
) : MealPlanRepository {

    private val planMutex = Mutex()

    override fun observeDays(from: LocalDate, to: LocalDate): Flow<List<DayPlan>> = combine(
        mealPlanDao.observeRange(from.toEpochDay(), to.toEpochDay()),
        mealPlanDao.observeChecks(from.toEpochDay(), to.toEpochDay()),
        userRepository.profile,
    ) { items, checks, profile ->
        val target = profile?.let(EnergyCalculator::dailyIntakeTarget) ?: 0
        val checkedSet = checks.filter { it.checked }.map { it.epochDay to it.mealType }.toSet()
        val byDay = items.map { it.toModel() }.groupBy { it.date }
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.map { date ->
            val dayItems = byDay[date].orEmpty()
            DayPlan(
                date = date,
                targetKcal = target,
                slots = MealType.entries.map { type ->
                    MealSlot(
                        date = date,
                        mealType = type,
                        items = dayItems.filter { it.mealType == type }.sortedBy { it.position }.toImmutableList(),
                        checked = (date.toEpochDay() to type.name) in checkedSet,
                    )
                }.toImmutableList(),
            )
        }.toList()
    }.distinctUntilChanged().flowOn(dispatchers.default)

    override fun observeItem(itemId: Long): Flow<PlannedItem?> =
        mealPlanDao.observeItem(itemId).map { it?.toModel() }.distinctUntilChanged().flowOn(dispatchers.io)

    override fun observeStreak(today: LocalDate): Flow<Int> = mealPlanDao.observeCheckedDays().map { days ->
        val set = days.toHashSet()
        var cursor = if (today.toEpochDay() in set) today.toEpochDay() else today.toEpochDay() - 1
        var streak = 0
        while (cursor in set) { streak++; cursor-- }
        streak
    }.distinctUntilChanged().flowOn(dispatchers.default)

    override suspend fun ensureWeekPlanned(today: LocalDate) = withContext(dispatchers.io) {
        planMutex.withLock {
            seedCatalog()
            val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            // The catalogue (or its health rules) changed since this plan was made: re-plan from
            // today so no dish that is now unsafe or retired stays on the menu. History is kept.
            val catalogVersion = preferences.catalogVersion()
            if (mealPlanDao.lastPlannedDay() != null && preferences.planCatalogVersion() != catalogVersion) {
                writePlan(firstReplannableDay(today), weekEnd, replace = true)
                preferences.setPlanCatalogVersion(catalogVersion)
                return@withLock
            }
            val last = mealPlanDao.lastPlannedDay()
            if (last == null || last < weekEnd.toEpochDay()) {
                val start = if (last == null) weekStart(today) else LocalDate.ofEpochDay(last + 1)
                writePlan(start, weekEnd, replace = false)
            }
        }
    }

    override suspend fun regenerate(today: LocalDate) = withContext(dispatchers.io) {
        planMutex.withLock {
            seedCatalog()
            // Past days keep their history; today onwards follows the new profile.
            writePlan(firstReplannableDay(today), today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)), replace = true)
        }
    }

    override suspend fun setChecked(date: LocalDate, mealType: MealType, checked: Boolean) = withContext(dispatchers.io) {
        mealPlanDao.upsertCheck(MealCheckEntity(date.toEpochDay(), mealType.name, checked))
    }

    override suspend fun swapCandidates(itemId: Long): List<SwapCandidate> = withContext(dispatchers.io) {
        val item = mealPlanDao.getItem(itemId)?.toModel() ?: return@withContext emptyList()
        val profile = userRepository.current() ?: return@withContext emptyList()
        generator.swapCandidates(item, profile, foodDao.active().map { it.toModel() })
    }

    override suspend fun swap(itemId: Long, foodId: String, servings: Double) = withContext(dispatchers.io) {
        mealPlanDao.updateFood(itemId, foodId, MealPlanGenerator.quarter(servings))
    }

    override suspend fun foodsEatenOn(days: List<LocalDate>) = withContext(dispatchers.io) {
        if (days.isEmpty()) emptyList() else foodDao.eatenOn(days.map { it.toEpochDay() }).map { it.toModel() }
    }

    override fun observeInsight(itemId: Long): Flow<MealInsight?> =
        combine(observeItem(itemId), userRepository.profile) { item, profile ->
            if (item == null || profile == null) null
            else MealInsight(item, MealInsights.reasons(item, profile).toImmutableList())
        }.distinctUntilChanged().flowOn(dispatchers.default)

    private suspend fun writePlan(start: LocalDate, end: LocalDate, replace: Boolean) {
        val profile: UserProfile = userRepository.current() ?: return
        val days = (end.toEpochDay() - start.toEpochDay() + 1).toInt().coerceAtLeast(0)
        if (days == 0) return
        val items = generator.generate(
            profile, foodDao.active().map { it.toModel() }, start, days, EnergyCalculator.dailyIntakeTarget(profile),
        ).map {
            PlannedItemEntity(
                epochDay = it.date.toEpochDay(), mealType = it.mealType.name, position = it.position,
                foodId = it.foodId, servings = it.servings,
            )
        }
        if (replace) mealPlanDao.replaceFrom(start.toEpochDay(), items) else mealPlanDao.insertItems(items)
        preferences.setPlanCatalogVersion(preferences.catalogVersion())
    }

    private suspend fun seedCatalog() = catalogRepository.ensureBundled()

    /**
     * Today stays as recorded once any meal is ticked – re-planning it would silently change what
     * the user already ate (and their calorie history). Changes then apply from tomorrow.
     */
    private suspend fun firstReplannableDay(today: LocalDate): LocalDate =
        if (mealPlanDao.checkedCount(today.toEpochDay()) > 0) today.plusDays(1) else today

    private fun weekStart(date: LocalDate) = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

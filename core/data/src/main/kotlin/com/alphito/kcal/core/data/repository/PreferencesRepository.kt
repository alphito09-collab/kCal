package com.alphito.kcal.core.data.repository

import com.alphito.kcal.core.datastore.PreferencesDataSource
import com.alphito.kcal.core.model.AppLanguage
import com.alphito.kcal.core.model.AppPreferences
import com.alphito.kcal.core.model.PremiumPlan
import com.alphito.kcal.core.model.ReminderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface PreferencesRepository {
    val preferences: Flow<AppPreferences>
    suspend fun snapshot(): AppPreferences
    suspend fun setOnboardingComplete(value: Boolean)
    /** Records the day onboarding finished; the meal streak only counts from then. */
    suspend fun setOnboardingDay(date: java.time.LocalDate)
    suspend fun setIntroSeen(value: Boolean)
    suspend fun setHeightInFeet(value: Boolean)
    suspend fun setWeightInLb(value: Boolean)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setActivityTracking(enabled: Boolean)
    suspend fun setWaterGoal(ml: Int)
    suspend fun markFreeTrialPromptSeen()
    suspend fun setRating(stars: Int)
    suspend fun setReminder(type: ReminderType, enabled: Boolean)
    /** Activates a plan locally. Real purchases must be confirmed by Play Billing first. */
    suspend fun setPlan(plan: PremiumPlan?)
}

internal class PreferencesRepositoryImpl(private val source: PreferencesDataSource) : PreferencesRepository {
    override val preferences: Flow<AppPreferences> = source.preferences
    override suspend fun snapshot(): AppPreferences = source.preferences.first()
    override suspend fun setOnboardingComplete(value: Boolean) { source.setOnboardingComplete(value) }
    override suspend fun setOnboardingDay(date: java.time.LocalDate) { source.setOnboardingDay(date.toEpochDay()) }
    override suspend fun setIntroSeen(value: Boolean) { source.setIntroSeen(value) }
    override suspend fun setHeightInFeet(value: Boolean) { source.setHeightInFeet(value) }
    override suspend fun setWeightInLb(value: Boolean) { source.setWeightInLb(value) }
    override suspend fun setLanguage(language: AppLanguage) { source.setLanguage(language) }
    override suspend fun setActivityTracking(enabled: Boolean) { source.setActivityTracking(enabled) }
    override suspend fun setWaterGoal(ml: Int) { source.setWaterGoal(ml) }
    override suspend fun markFreeTrialPromptSeen() { source.setFreeTrialPromptSeen() }
    override suspend fun setRating(stars: Int) { source.setRating(stars) }
    override suspend fun setReminder(type: ReminderType, enabled: Boolean) { source.setReminder(type, enabled) }
    override suspend fun setPlan(plan: PremiumPlan?) { source.setPlan(plan) }
}

package com.alphito.kcal.core.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.alphito.kcal.core.model.AppLanguage
import com.alphito.kcal.core.model.AppPreferences
import com.alphito.kcal.core.model.PremiumPlan
import com.alphito.kcal.core.model.ReminderType
import com.alphito.kcal.core.model.SubscriptionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Typed facade over Preferences DataStore. I/O never touches the main thread. */
class PreferencesDataSource(context: Context, scope: CoroutineScope) {

    private val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.preferencesDataStoreFile("kcal_prefs") },
    )

    val preferences: Flow<AppPreferences> = store.data
        .catch { error ->
            if (error is IOException) {
                Log.w("PreferencesDataSource", "Corrupt prefs, using defaults", error)
                emit(emptyPreferences())
            } else throw error
        }
        .map { it.toModel() }
        .distinctUntilChanged()

    suspend fun setOnboardingComplete(value: Boolean) = store.edit { it[ONBOARDED] = value }
    suspend fun catalogVersion(): Long = store.data.map { it[CATALOG_VERSION] ?: 0L }.first()
    suspend fun setCatalogVersion(value: Long) = store.edit { it[CATALOG_VERSION] = value }
    /** Catalogue version the current plan was generated from. */
    suspend fun planCatalogVersion(): Long = store.data.map { it[PLAN_CATALOG_VERSION] ?: 0L }.first()
    suspend fun setPlanCatalogVersion(value: Long) = store.edit { it[PLAN_CATALOG_VERSION] = value }
    suspend fun setIntroSeen(value: Boolean) = store.edit { it[INTRO_SEEN] = value }
    suspend fun setHeightInFeet(value: Boolean) = store.edit { it[HEIGHT_FEET] = value }
    suspend fun setWeightInLb(value: Boolean) = store.edit { it[WEIGHT_LB] = value }
    suspend fun setLanguage(language: AppLanguage) = store.edit { it[LANGUAGE] = language.tag }
    suspend fun setActivityTracking(enabled: Boolean) = store.edit { it[ACTIVITY] = enabled }
    suspend fun setWaterGoal(ml: Int) = store.edit { it[WATER_GOAL] = ml }
    suspend fun setFreeTrialPromptSeen() = store.edit { it[TRIAL_SEEN] = true }
    suspend fun setRating(stars: Int) = store.edit { it[RATING] = stars }
    suspend fun setReminder(type: ReminderType, enabled: Boolean) = store.edit {
        val current = it[REMINDERS].orEmpty()
        it[REMINDERS] = if (enabled) current + type.name else current - type.name
    }
    suspend fun setPlan(plan: PremiumPlan?) = store.edit {
        if (plan == null) it.remove(PLAN) else it[PLAN] = plan.name
    }

    suspend fun clear() = store.edit { prefs ->
        val language = prefs[LANGUAGE]
        prefs.clear()
        if (language != null) prefs[LANGUAGE] = language // keep the UI language after sign-out
    }

    private fun Preferences.toModel() = AppPreferences(
        onboardingComplete = this[ONBOARDED] ?: false,
        introSeen = this[INTRO_SEEN] ?: false,
        heightInFeet = this[HEIGHT_FEET] ?: false,
        weightInLb = this[WEIGHT_LB] ?: false,
        language = AppLanguage.fromTag(this[LANGUAGE]),
        activityTrackingEnabled = this[ACTIVITY] ?: false,
        waterGoalMl = this[WATER_GOAL] ?: AppPreferences().waterGoalMl,
        freeTrialPromptSeen = this[TRIAL_SEEN] ?: false,
        subscription = SubscriptionState(activePlan = this[PLAN]?.let { runCatching { PremiumPlan.valueOf(it) }.getOrNull() }),
        lastRating = this[RATING] ?: 0,
        reminders = this[REMINDERS].orEmpty().mapNotNull { n -> ReminderType.entries.firstOrNull { it.name == n } }.toSet(),
    )

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarding_complete")
        val CATALOG_VERSION = androidx.datastore.preferences.core.longPreferencesKey("catalog_version")
        val PLAN_CATALOG_VERSION = androidx.datastore.preferences.core.longPreferencesKey("plan_catalog_version")
        val INTRO_SEEN = booleanPreferencesKey("intro_seen")
        val HEIGHT_FEET = booleanPreferencesKey("height_in_feet")
        val WEIGHT_LB = booleanPreferencesKey("weight_in_lb")
        val LANGUAGE = stringPreferencesKey("language")
        val ACTIVITY = booleanPreferencesKey("activity_tracking")
        val WATER_GOAL = intPreferencesKey("water_goal_ml")
        val TRIAL_SEEN = booleanPreferencesKey("free_trial_seen")
        val PLAN = stringPreferencesKey("premium_plan")
        val RATING = intPreferencesKey("rating")
        val REMINDERS = androidx.datastore.preferences.core.stringSetPreferencesKey("reminders")
    }
}

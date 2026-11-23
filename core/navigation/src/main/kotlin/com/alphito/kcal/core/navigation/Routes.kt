package com.alphito.kcal.core.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every destination in the app. Keys live in core so features can navigate to each other
 * without depending on each other's modules. Keys are @Serializable so the back stack
 * survives configuration changes *and* process death.
 */
/** All keys are immutable data objects/classes, so routes are safe Compose parameters. */
@Immutable
@Serializable
sealed interface Route : NavKey {

    // ---- Launch / onboarding -------------------------------------------------------------
    /** Animated splash. First session shows 'Get Started'; returning users auto-continue after 4 s. */
    @Serializable data object Splash : Route
    @Serializable data object Intro : Route

    /** Profile wizard. `edit = true` re-uses the screen from Settings and pops on save. */
    @Serializable data class AboutYou(val edit: Boolean = false) : Route
    @Serializable data class HealthConditions(val edit: Boolean = false) : Route
    @Serializable data object GoalSelection : Route
    @Serializable data class RegionBudget(val edit: Boolean = false) : Route
    @Serializable data object EnergyNeeds : Route
    @Serializable data object DeficitSetup : Route

    // ---- Top level (bottom navigation) -------------------------------------------------------
    @Serializable
    sealed interface TopLevel : Route
    @Serializable data object Home : TopLevel
    @Serializable data object DietPlan : TopLevel
    @Serializable data object Progress : TopLevel
    @Serializable data object Settings : TopLevel

    // ---- Details --------------------------------------------------------------------------
    @Serializable data object MealChecklist : Route
    @Serializable data class SwapIngredient(val plannedItemId: Long) : Route
    @Serializable data object Water : Route
    @Serializable data object Steps : Route
    @Serializable data object WeightCheckIn : Route
    @Serializable data object HealthLog : Route
    /** `onboarding = true` during the first session (Continue -> intro) instead of Settings. */
    @Serializable data class Language(val onboarding: Boolean = false) : Route
    @Serializable data object PrivacySecurity : Route
    @Serializable data object Reminders : Route
    @Serializable data class PremiumIntro(val fromOnboarding: Boolean = false) : Route
    @Serializable data class Premium(val fromOnboarding: Boolean = false) : Route
}

/** Shared-element keys, defined once so both sides of a transition always agree. */
object SharedKeys {
    const val LOGO = "kcal-logo"
    fun actionCard(name: String) = "action-card-$name"
    fun actionTitle(name: String) = "action-title-$name"
    fun mealImage(id: Long) = "meal-image-$id"
    fun mealTitle(id: Long) = "meal-title-$id"
    const val TODAY_MEAL_CARD = "today-meal-card"
    const val PREMIUM_CROWN = "premium-crown"
}

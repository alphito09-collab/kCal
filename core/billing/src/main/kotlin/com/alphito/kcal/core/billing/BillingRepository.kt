package com.alphito.kcal.core.billing

import android.app.Activity
import com.alphito.kcal.core.model.PremiumPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** A plan as sold by Google Play (price already localized for the user's store country). */
data class PlanOffer(
    val plan: PremiumPlan,
    val formattedPrice: String,
    val billingPeriod: String,
    internal val offerToken: String,
)

sealed interface PurchaseOutcome {
    data class Active(val plan: PremiumPlan) : PurchaseOutcome
    data object Pending : PurchaseOutcome
    data object Cancelled : PurchaseOutcome
    data class Failed(val reason: String) : PurchaseOutcome
}

interface BillingRepository {
    /** Plans available for purchase (empty until Play responds / when billing is unavailable). */
    val offers: StateFlow<Map<PremiumPlan, PlanOffer>>
    val outcomes: Flow<PurchaseOutcome>

    /** Connects, loads products and restores/re-verifies existing subscriptions. */
    suspend fun refresh()

    /** Opens the Google Play purchase sheet. The [activity] is used only for this call. */
    suspend fun launchPurchase(activity: Activity, plan: PremiumPlan): Boolean
}

object BillingProducts {
    /** One subscription product with two base plans – configure the same ids in Play Console. */
    const val SUBSCRIPTION_ID = "kcal_premium"
    const val BASE_PLAN_WEEKLY = "weekly"
    const val BASE_PLAN_MONTHLY = "monthly"

    fun basePlan(plan: PremiumPlan) = when (plan) {
        PremiumPlan.Weekly -> BASE_PLAN_WEEKLY
        PremiumPlan.Monthly -> BASE_PLAN_MONTHLY
    }

    fun planFor(basePlanId: String?) = when (basePlanId) {
        BASE_PLAN_WEEKLY -> PremiumPlan.Weekly
        BASE_PLAN_MONTHLY -> PremiumPlan.Monthly
        else -> null
    }
}

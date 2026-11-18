package com.alphito.kcal.feature.premium.domain

import com.alphito.kcal.core.model.PremiumPlan

/**
 * Purchase boundary. Swap [com.alphito.kcal.feature.premium.data.LocalSubscriptionGateway]
 * for a Google Play Billing implementation before shipping paid plans.
 */
interface SubscriptionGateway {
    suspend fun purchase(plan: PremiumPlan): Boolean
}

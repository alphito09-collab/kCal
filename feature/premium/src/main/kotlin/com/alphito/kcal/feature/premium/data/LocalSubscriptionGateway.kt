package com.alphito.kcal.feature.premium.data

import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.model.PremiumPlan
import com.alphito.kcal.feature.premium.domain.SubscriptionGateway

/** Development gateway: records the chosen plan locally (no payment is taken). */
internal class LocalSubscriptionGateway(private val preferences: PreferencesRepository) : SubscriptionGateway {
    override suspend fun purchase(plan: PremiumPlan): Boolean {
        preferences.setPlan(plan)
        return true
    }
}

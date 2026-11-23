package com.alphito.kcal.core.common.flags

import kotlinx.coroutines.flow.StateFlow

/**
 * Remotely controlled switches (Firebase Remote Config when configured, safe defaults otherwise),
 * so features can be enabled for users without shipping a new build.
 */
data class AppFlags(
    /** Premium screens, Settings rows and the dashboard trial dialog. */
    val premiumEnabled: Boolean = false,
    /** Interstitial on the returning-user splash ("This action may contain ads"). */
    val adsEnabled: Boolean = true,
    /** Minimum minutes between two interstitials. */
    val interstitialIntervalMinutes: Int = 10,
    /**
     * Verify Play purchases with the `verifySubscription` Cloud Function. Off while the project is
     * on the free Spark plan (no Functions): purchases are then acknowledged on the device.
     */
    val serverVerificationEnabled: Boolean = false,
)

interface FeatureFlags {
    val flags: StateFlow<AppFlags>
    /** Fetches fresh values in the background; never throws. */
    suspend fun refresh()
}

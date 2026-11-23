package com.alphito.kcal.core.ads

import android.app.Activity

/** Ad placements used by kCal. Premium users never see ads. */
interface AdsManager {
    /** Gathers UMP consent (shows the form once if required), initializes the SDK and preloads the splash ad. */
    suspend fun prepareSplashAd(activity: Activity)

    /** Shows the preloaded splash interstitial and suspends until it is dismissed. False if nothing was shown. */
    suspend fun showSplashAd(activity: Activity): Boolean

    /** True when a privacy-options entry point must be offered (GDPR / US state regulations). */
    val privacyOptionsRequired: Boolean

    fun showPrivacyOptions(activity: Activity)
}

/** Ad configuration from the app module (test IDs in debug, real IDs from gradle properties in release). */
data class AdsConfig(val appId: String, val splashInterstitialUnitId: String)

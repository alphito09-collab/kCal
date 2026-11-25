package com.alphito.kcal.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.common.flags.FeatureFlags
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * GMA Next-Gen SDK + UMP consent.
 *
 *  - consent is requested before any ad request; ads are only requested when `canRequestAds()`;
 *  - the SDK is initialized once, on a background thread;
 *  - the splash interstitial is skipped for premium users, when disabled by Remote Config and
 *    within the frequency cap (`interstitial_interval_minutes`);
 *  - SDK callbacks arrive on a background thread, so all UI work is moved to the main dispatcher.
 */
internal class KcalAdsManager(
    private val context: Context,
    private val config: AdsConfig,
    private val preferences: PreferencesRepository,
    private val featureFlags: FeatureFlags,
    private val dispatchers: DispatcherProvider,
) : AdsManager {

    // Both read SharedPreferences from disk: created lazily and first touched on the IO dispatcher.
    private val consent: ConsentInformation by lazy { UserMessagingPlatform.getConsentInformation(context) }
    private val storage by lazy { context.getSharedPreferences("kcal_ads", Context.MODE_PRIVATE) }
    private val initialized = AtomicBoolean(false)

    /** Cached after consent gathering so the UI can read it without disk access. */
    @Volatile private var privacyRequired = false

    @Volatile private var splashAd: InterstitialAd? = null
    /** Completes when the current splash preload finishes (with or without an ad). */
    @Volatile private var loading: CompletableDeferred<Unit>? = null

    override val privacyOptionsRequired: Boolean get() = privacyRequired

    override suspend fun prepareSplashAd(activity: Activity) {
        if (!isEligible()) return
        withContext(dispatchers.io) { consent } // warm up the disk-backed consent store off the main thread
        val canRequest = gatherConsent(activity)
        privacyRequired = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (!canRequest) return
        if (splashAd != null) return
        val done = CompletableDeferred<Unit>().also { loading = it }
        try {
            initializeSdk()
            val result = withContext(dispatchers.io) {
                runCatching { InterstitialAd.load(AdRequest.Builder(config.splashInterstitialUnitId).build()) }.getOrNull()
            }
            splashAd = (result as? AdLoadResult.Success)?.ad
            if (splashAd == null) Log.d(TAG, "Splash interstitial not available: $result")
        } finally {
            done.complete(Unit)
        }
    }

    override suspend fun showSplashAd(activity: Activity): Boolean {
        // A slow network may still be loading when the 4 s splash ends: give it a short grace period.
        if (splashAd == null) withTimeoutOrNull(LOAD_GRACE_MS) { loading?.await() }
        val ad = splashAd ?: return false
        if (!isEligible()) return false
        splashAd = null
        val finished = CompletableDeferred<Boolean>()
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                storage.edit { putLong(KEY_LAST_SHOWN, System.currentTimeMillis()) }
            }
            override fun onAdDismissedFullScreenContent() { finished.complete(true) }
            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.d(TAG, "Interstitial failed to show: ${fullScreenContentError.message}")
                finished.complete(false)
            }
        }
        withContext(dispatchers.main) { ad.show(activity) }
        // Never trap the user on the splash if the SDK misbehaves.
        return withTimeoutOrNull(MAX_AD_DURATION_MS) { finished.await() } ?: false
    }

    override fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "Privacy options form: ${error.message}")
        }
    }

    private suspend fun isEligible(): Boolean = withContext(dispatchers.io) {
        val flags = featureFlags.flags.value
        if (!flags.adsEnabled) return@withContext false
        if (preferences.snapshot().subscription.isPremium) return@withContext false
        val lastShown = storage.getLong(KEY_LAST_SHOWN, 0L)
        System.currentTimeMillis() - lastShown >= flags.interstitialIntervalMinutes * 60_000L
    }

    private suspend fun gatherConsent(activity: Activity): Boolean = withContext(dispatchers.main) {
        suspendCancellableCoroutine { cont ->
            consent.requestConsentInfoUpdate(
                activity,
                ConsentRequestParameters.Builder().build(),
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) Log.w(TAG, "Consent form: ${formError.message}")
                        if (cont.isActive) cont.resume(consent.canRequestAds())
                    }
                },
                { requestError ->
                    Log.w(TAG, "Consent info update failed: ${requestError.message}")
                    if (cont.isActive) cont.resume(consent.canRequestAds())
                },
            )
        }
    }

    private suspend fun initializeSdk() {
        if (!initialized.compareAndSet(false, true)) return
        withContext(dispatchers.io) {
            runCatching { MobileAds.initialize(context, InitializationConfig.Builder(config.appId).build()) }
                .onFailure { initialized.set(false); Log.w(TAG, "MobileAds init failed", it) }
        }
    }

    private companion object {
        const val TAG = "KcalAds"
        const val KEY_LAST_SHOWN = "splash_last_shown"
        const val MAX_AD_DURATION_MS = 120_000L
        const val LOAD_GRACE_MS = 3_000L
    }
}

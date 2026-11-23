package com.alphito.kcal.core.firebase

import android.util.Log
import com.alphito.kcal.core.common.flags.AppFlags
import com.alphito.kcal.core.common.flags.FeatureFlags
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/** Feature flags backed by Firebase Remote Config; falls back to [AppFlags] defaults. */
internal class RemoteConfigFeatureFlags(
    private val firebase: KcalFirebase,
    private val minimumFetchIntervalSeconds: Long,
) : FeatureFlags {

    private val defaults = AppFlags()
    private val _flags = MutableStateFlow(defaults)
    override val flags: StateFlow<AppFlags> = _flags.asStateFlow()

    override suspend fun refresh() {
        if (!firebase.isAvailable) return
        runCatching {
            val config = FirebaseRemoteConfig.getInstance()
            config.setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = minimumFetchIntervalSeconds }).await()
            config.setDefaultsAsync(
                mapOf(
                    KEY_PREMIUM to defaults.premiumEnabled,
                    KEY_ADS to defaults.adsEnabled,
                    KEY_INTERVAL to defaults.interstitialIntervalMinutes.toLong(),
                    KEY_SERVER_VERIFICATION to defaults.serverVerificationEnabled,
                ),
            ).await()
            // Offline: keep going with the values activated on a previous launch.
            runCatching { config.fetchAndActivate().await() }
                .onFailure { Log.w(TAG, "Remote Config fetch failed – using last activated values", it) }
            _flags.value = AppFlags(
                premiumEnabled = config.getBoolean(KEY_PREMIUM),
                adsEnabled = config.getBoolean(KEY_ADS),
                interstitialIntervalMinutes = config.getLong(KEY_INTERVAL).toInt().coerceAtLeast(0),
                serverVerificationEnabled = config.getBoolean(KEY_SERVER_VERIFICATION),
            )
        }.onFailure { Log.w(TAG, "Remote Config unavailable – keeping current flags", it) }
    }

    private companion object {
        const val KEY_PREMIUM = "premium_enabled"
        const val KEY_ADS = "ads_enabled"
        const val KEY_INTERVAL = "interstitial_interval_minutes"
        const val KEY_SERVER_VERIFICATION = "server_verification_enabled"
        const val TAG = "FeatureFlags"
    }
}

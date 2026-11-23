package com.alphito.kcal.core.ads

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Koin properties set by the app module. */
const val ADMOB_APP_ID_PROPERTY = "kcal.admobAppId"
const val ADMOB_SPLASH_UNIT_PROPERTY = "kcal.admobSplashUnit"

/** Google's public test IDs – used whenever no real IDs are configured. */
const val TEST_ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
const val TEST_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

val adsModule = module {
    single {
        AdsConfig(
            appId = getProperty(ADMOB_APP_ID_PROPERTY, TEST_ADMOB_APP_ID),
            splashInterstitialUnitId = getProperty(ADMOB_SPLASH_UNIT_PROPERTY, TEST_INTERSTITIAL_UNIT_ID),
        )
    }
    single<AdsManager> { KcalAdsManager(androidContext(), get(), get(), get(), get()) }
}

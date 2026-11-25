package com.alphito.kcal.core.firebase

import com.alphito.kcal.core.common.flags.FeatureFlags
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Property set by the app: Remote Config minimum fetch interval (short in debug, 1 h in release). */
const val REMOTE_CONFIG_INTERVAL_PROPERTY = "kcal.remoteConfigInterval"

val firebaseModule = module {
    single { KcalFirebase(androidContext()) }
    single<FeatureFlags> { RemoteConfigFeatureFlags(get(), getProperty(REMOTE_CONFIG_INTERVAL_PROPERTY, 3_600L)) }
    single { PushTokenRegistrar(get()) }
}

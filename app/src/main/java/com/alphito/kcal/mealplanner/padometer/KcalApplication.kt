package com.alphito.kcal.mealplanner.padometer

import android.app.Application
import android.os.StrictMode
import com.alphito.kcal.feature.onboarding.di.SAMPLE_DATA_PROPERTY
import com.alphito.kcal.mealplanner.padometer.di.kcalModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Cold-start budget: only Koin *definitions* are registered here (no instance is created –
 * Room, DataStore and repositories are lazy singletons resolved off the main thread on first use).
 */
class KcalApplication : Application() {

    override fun onCreate() {
        if (BuildConfig.DEBUG) enableStrictMode()
        super.onCreate()
        startKoin {
            if (BuildConfig.DEBUG) androidLogger(Level.INFO)
            androidContext(this@KcalApplication)
            properties(mapOf(SAMPLE_DATA_PROPERTY to BuildConfig.DEBUG))
            modules(kcalModules)
        }
    }

    /** Surfaces accidental disk/network work on the main thread (the #1 ANR cause) in debug. */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().detectLeakedRegistrationObjects()
                .detectActivityLeaks().penaltyLog().build(),
        )
    }
}

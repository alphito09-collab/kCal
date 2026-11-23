package com.alphito.kcal.mealplanner.padometer

import android.app.Application
import android.os.StrictMode
import com.alphito.kcal.core.ads.ADMOB_APP_ID_PROPERTY
import com.alphito.kcal.core.ads.ADMOB_SPLASH_UNIT_PROPERTY
import com.alphito.kcal.core.common.di.ApplicationScope
import com.alphito.kcal.core.common.flags.FeatureFlags
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.data.sync.CatalogSyncWorker
import com.alphito.kcal.core.firebase.PushTokenRegistrar
import com.alphito.kcal.core.firebase.REMOTE_CONFIG_INTERVAL_PROPERTY
import com.alphito.kcal.core.notifications.KcalNotifications
import com.alphito.kcal.core.notifications.ReminderScheduler
import com.alphito.kcal.feature.onboarding.di.SAMPLE_DATA_PROPERTY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.GlobalContext
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
            properties(
                mapOf(
                    SAMPLE_DATA_PROPERTY to BuildConfig.DEBUG,
                    // Debug fetches Remote Config every minute so console changes show up quickly.
                    REMOTE_CONFIG_INTERVAL_PROPERTY to if (BuildConfig.DEBUG) 60L else 3600L,
                    ADMOB_APP_ID_PROPERTY to BuildConfig.ADMOB_APP_ID,
                    ADMOB_SPLASH_UNIT_PROPERTY to BuildConfig.ADMOB_SPLASH_UNIT,
                ),
            )
            workManagerFactory()
            modules(kcalModules)
        }
        startBackgroundServices()
    }

    /**
     * Everything that touches disk or network runs on the application scope (Dispatchers.Default/IO),
     * never on the main thread, so it does not count against cold start.
     */
    private fun startBackgroundServices() {
        val koin = GlobalContext.get()
        koin.get<CoroutineScope>(ApplicationScope).launch {
            koin.get<KcalNotifications>().createChannels()
            CatalogSyncWorker.schedule(this@KcalApplication)
            runCatching { koin.get<FeatureFlags>().refresh() }
            koin.get<ReminderScheduler>().sync(koin.get<PreferencesRepository>().snapshot().reminders)
            runCatching { koin.get<PushTokenRegistrar>().registerCurrentToken() }
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

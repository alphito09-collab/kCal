package com.alphito.kcal.core.common.di

import com.alphito.kcal.core.common.dispatcher.DefaultDispatcherProvider
import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.common.network.ConnectivityNetworkMonitor
import com.alphito.kcal.core.common.network.NetworkMonitor
import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.common.time.SystemAppClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Process-wide scope for work that must outlive a screen (e.g. saving after navigation). */
val ApplicationScope = named("ApplicationScope")

val commonModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider }
    single<AppClock> { SystemAppClock }
    single<CoroutineScope>(ApplicationScope) {
        CoroutineScope(SupervisorJob() + get<DispatcherProvider>().default)
    }
    single<NetworkMonitor> { ConnectivityNetworkMonitor(androidContext(), get(ApplicationScope)) }
}

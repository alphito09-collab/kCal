package com.alphito.kcal.core.datastore

import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val datastoreModule = module {
    // DataStore must be a process-wide singleton (two instances on one file throw) and does
    // its file I/O on the IO dispatcher.
    single { PreferencesDataSource(androidContext(), CoroutineScope(get<DispatcherProvider>().io + SupervisorJob())) }
}

package com.alphito.kcal.core.notifications

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val notificationsModule = module {
    single { KcalNotifications(androidContext()) }
    single { ReminderScheduler(androidContext()) }
    workerOf(::ReminderWorker)
}

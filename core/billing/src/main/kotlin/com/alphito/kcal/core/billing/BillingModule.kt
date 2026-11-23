package com.alphito.kcal.core.billing

import com.alphito.kcal.core.common.di.ApplicationScope
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val billingModule = module {
    single<BillingRepository> { PlayBillingRepository(androidContext(), get(), get(), get(), get(ApplicationScope), get()) }
}

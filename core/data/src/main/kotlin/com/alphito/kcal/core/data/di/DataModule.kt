package com.alphito.kcal.core.data.di

import com.alphito.kcal.core.data.domain.MealPlanGenerator
import com.alphito.kcal.core.data.repository.AccountRepository
import com.alphito.kcal.core.data.repository.CaloriePlanRepository
import com.alphito.kcal.core.data.repository.CaloriePlanRepositoryImpl
import com.alphito.kcal.core.data.repository.CatalogRepository
import com.alphito.kcal.core.data.repository.CatalogRepositoryImpl
import com.alphito.kcal.core.data.sync.CatalogSyncWorker
import org.koin.androidx.workmanager.dsl.workerOf
import com.alphito.kcal.core.data.repository.AccountRepositoryImpl
import com.alphito.kcal.core.data.repository.MealPlanRepository
import com.alphito.kcal.core.data.repository.MealPlanRepositoryImpl
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.data.repository.PreferencesRepositoryImpl
import com.alphito.kcal.core.data.repository.SampleDataSeeder
import com.alphito.kcal.core.data.repository.StepsRepository
import com.alphito.kcal.core.data.repository.StepsRepositoryImpl
import com.alphito.kcal.core.data.repository.TrackingStartRepository
import com.alphito.kcal.core.data.repository.TrackingStartRepositoryImpl
import com.alphito.kcal.core.data.repository.UserRepository
import com.alphito.kcal.core.data.repository.UserRepositoryImpl
import com.alphito.kcal.core.data.repository.WeightRepository
import com.alphito.kcal.core.data.repository.WeightRepositoryImpl
import com.alphito.kcal.core.database.di.databaseModule
import com.alphito.kcal.core.firebase.firebaseModule
import com.alphito.kcal.core.datastore.datastoreModule
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    includes(databaseModule, datastoreModule, firebaseModule)
    singleOf(::MealPlanGenerator)
    singleOf(::UserRepositoryImpl) { bind<UserRepository>() }
    singleOf(::PreferencesRepositoryImpl) { bind<PreferencesRepository>() }
    singleOf(::MealPlanRepositoryImpl) { bind<MealPlanRepository>() }
    singleOf(::StepsRepositoryImpl) { bind<StepsRepository>() }
    singleOf(::CaloriePlanRepositoryImpl) { bind<CaloriePlanRepository>() }
    singleOf(::TrackingStartRepositoryImpl) { bind<TrackingStartRepository>() }
    singleOf(::WeightRepositoryImpl) { bind<WeightRepository>() }
    singleOf(::AccountRepositoryImpl) { bind<AccountRepository>() }
    singleOf(::SampleDataSeeder)
    singleOf(::CatalogRepositoryImpl) { bind<CatalogRepository>() }
    workerOf(::CatalogSyncWorker)
}

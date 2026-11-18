package com.alphito.kcal.feature.healthlog.di

import com.alphito.kcal.core.navigation.NavMotion
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.healthlog.data.HealthLogRepositoryImpl
import com.alphito.kcal.feature.healthlog.domain.HealthLogRepository
import com.alphito.kcal.feature.healthlog.presentation.mvi.HealthLogViewModel
import com.alphito.kcal.feature.healthlog.presentation.ui.HealthLogRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val healthLogModule = module {
    singleOf(::HealthLogRepositoryImpl) { bind<HealthLogRepository>() }
    viewModelOf(::HealthLogViewModel)
    navigation<Route.HealthLog>(metadata = NavMotion.Container) { HealthLogRoute() }
}

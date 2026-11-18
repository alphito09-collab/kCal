package com.alphito.kcal.feature.dashboard.di

import com.alphito.kcal.core.navigation.NavMotion
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.dashboard.domain.ObserveDashboardUseCase
import com.alphito.kcal.feature.dashboard.presentation.mvi.DashboardViewModel
import com.alphito.kcal.feature.dashboard.presentation.ui.DashboardRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val dashboardModule = module {
    factoryOf(::ObserveDashboardUseCase)
    viewModelOf(::DashboardViewModel)
    navigation<Route.Home>(metadata = NavMotion.TopLevel) { DashboardRoute() }
}

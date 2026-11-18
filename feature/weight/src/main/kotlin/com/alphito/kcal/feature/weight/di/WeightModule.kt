package com.alphito.kcal.feature.weight.di

import com.alphito.kcal.core.navigation.NavMotion
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.weight.presentation.mvi.WeightViewModel
import com.alphito.kcal.feature.weight.presentation.ui.WeightRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val weightModule = module {
    viewModelOf(::WeightViewModel)
    navigation<Route.WeightCheckIn>(metadata = NavMotion.Container) { WeightRoute() }
}

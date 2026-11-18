package com.alphito.kcal.feature.water.di

import com.alphito.kcal.core.navigation.NavMotion
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.water.data.WaterRepositoryImpl
import com.alphito.kcal.feature.water.domain.WaterRepository
import com.alphito.kcal.feature.water.presentation.mvi.WaterViewModel
import com.alphito.kcal.feature.water.presentation.ui.WaterRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val waterModule = module {
    singleOf(::WaterRepositoryImpl) { bind<WaterRepository>() }
    viewModelOf(::WaterViewModel)
    navigation<Route.Water>(metadata = NavMotion.Container) { WaterRoute() }
}

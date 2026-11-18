package com.alphito.kcal.feature.progress.di

import com.alphito.kcal.core.navigation.NavMotion
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.progress.presentation.mvi.ProgressViewModel
import com.alphito.kcal.feature.progress.presentation.ui.ProgressRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val progressModule = module {
    viewModelOf(::ProgressViewModel)
    navigation<Route.Progress>(metadata = NavMotion.TopLevel) { ProgressRoute() }
}

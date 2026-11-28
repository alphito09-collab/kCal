package com.alphito.kcal.feature.steps.di

import com.alphito.kcal.core.common.di.ApplicationScope
import com.alphito.kcal.core.navigation.NavMotion
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.steps.data.service.StepTrackingControllerImpl
import com.alphito.kcal.feature.steps.domain.StepTrackingController
import com.alphito.kcal.feature.steps.presentation.mvi.StepsViewModel
import com.alphito.kcal.feature.steps.presentation.ui.StepsRoute
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val stepsModule = module {
    single<StepTrackingController> { StepTrackingControllerImpl(androidContext(), get(), get(ApplicationScope)) }
    viewModelOf(::StepsViewModel)
    navigation<Route.Steps>(metadata = NavMotion.Container) { StepsRoute() }
}

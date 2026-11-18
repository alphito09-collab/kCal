package com.alphito.kcal.feature.onboarding.di

import com.alphito.kcal.core.navigation.NavMotion
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.onboarding.domain.CompleteOnboardingUseCase
import com.alphito.kcal.feature.onboarding.domain.RegeneratePlanUseCase
import com.alphito.kcal.feature.onboarding.presentation.mvi.AboutYouViewModel
import com.alphito.kcal.feature.onboarding.presentation.mvi.DeficitViewModel
import com.alphito.kcal.feature.onboarding.presentation.mvi.EnergyViewModel
import com.alphito.kcal.feature.onboarding.presentation.mvi.GoalViewModel
import com.alphito.kcal.feature.onboarding.presentation.mvi.HealthConditionsViewModel
import com.alphito.kcal.feature.onboarding.presentation.mvi.RegionBudgetViewModel
import com.alphito.kcal.feature.onboarding.presentation.mvi.SplashViewModel
import com.alphito.kcal.feature.onboarding.presentation.ui.AboutYouRoute
import com.alphito.kcal.feature.onboarding.presentation.ui.DeficitRoute
import com.alphito.kcal.feature.onboarding.presentation.ui.EnergyRoute
import com.alphito.kcal.feature.onboarding.presentation.ui.GoalRoute
import com.alphito.kcal.feature.onboarding.presentation.ui.HealthConditionsRoute
import com.alphito.kcal.feature.onboarding.presentation.ui.IntroScreen
import com.alphito.kcal.feature.onboarding.presentation.ui.RegionBudgetRoute
import com.alphito.kcal.feature.onboarding.presentation.ui.SplashRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

/** Property set by the app (true only in debug builds) to seed sample history. */
const val SAMPLE_DATA_PROPERTY = "kcal.sampleData"

@OptIn(KoinExperimentalAPI::class)
val onboardingModule = module {
    factory {
        CompleteOnboardingUseCase(get(), get(), get(), get(), get(), get(), getProperty(SAMPLE_DATA_PROPERTY, false))
    }
    factoryOf(::RegeneratePlanUseCase)

    viewModel { SplashViewModel(get(), get()) }
    viewModel { (edit: Boolean) -> AboutYouViewModel(edit, get(), get()) }
    viewModel { (edit: Boolean) -> HealthConditionsViewModel(edit, get(), get()) }
    viewModelOf(::GoalViewModel)
    viewModel { (edit: Boolean) -> RegionBudgetViewModel(edit, get(), get()) }
    viewModelOf(::EnergyViewModel)
    viewModelOf(::DeficitViewModel)

    navigation<Route.Splash>(metadata = NavMotion.Fade) { SplashRoute() }
    navigation<Route.Intro> { IntroScreen() }
    navigation<Route.AboutYou> { route -> AboutYouRoute(route.edit) }
    navigation<Route.HealthConditions> { route -> HealthConditionsRoute(route.edit) }
    navigation<Route.GoalSelection> { GoalRoute() }
    navigation<Route.RegionBudget> { route -> RegionBudgetRoute(route.edit) }
    navigation<Route.EnergyNeeds> { EnergyRoute() }
    navigation<Route.DeficitSetup> { DeficitRoute() }
}

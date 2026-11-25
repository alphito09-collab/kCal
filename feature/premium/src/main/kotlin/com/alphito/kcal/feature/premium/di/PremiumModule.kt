package com.alphito.kcal.feature.premium.di

import com.alphito.kcal.core.navigation.NavMotion
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.premium.presentation.mvi.PremiumViewModel
import com.alphito.kcal.feature.premium.presentation.ui.PremiumIntroScreen
import com.alphito.kcal.feature.premium.presentation.ui.PremiumRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val premiumModule = module {
    viewModelOf(::PremiumViewModel)
    navigation<Route.PremiumIntro>(metadata = NavMotion.Fade) { route -> PremiumIntroScreen(route.fromOnboarding) }
    navigation<Route.Premium> { route -> PremiumRoute(route.fromOnboarding) }
}

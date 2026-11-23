package com.alphito.kcal.feature.dietplan.di

import com.alphito.kcal.core.navigation.NavMotion
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.dietplan.presentation.mvi.DietPlanViewModel
import com.alphito.kcal.feature.dietplan.presentation.mvi.MealChecklistViewModel
import com.alphito.kcal.feature.dietplan.presentation.mvi.MealInfoViewModel
import com.alphito.kcal.feature.dietplan.presentation.mvi.SwapViewModel
import com.alphito.kcal.feature.dietplan.presentation.ui.DietPlanRoute
import com.alphito.kcal.feature.dietplan.presentation.ui.MealChecklistRoute
import com.alphito.kcal.feature.dietplan.presentation.ui.SwapRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val dietPlanModule = module {
    viewModelOf(::DietPlanViewModel)
    viewModelOf(::MealChecklistViewModel)
    viewModel { (itemId: Long) -> SwapViewModel(itemId, get()) }
    viewModel { (itemId: Long) -> MealInfoViewModel(itemId, get(), get()) }

    navigation<Route.DietPlan>(metadata = NavMotion.TopLevel) { DietPlanRoute() }
    navigation<Route.MealChecklist>(metadata = NavMotion.Container) { MealChecklistRoute() }
    navigation<Route.SwapIngredient> { route -> SwapRoute(route.plannedItemId) }
}

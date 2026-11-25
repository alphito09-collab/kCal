package com.alphito.kcal.feature.settings.di

import com.alphito.kcal.feature.settings.presentation.mvi.RemindersViewModel
import com.alphito.kcal.feature.settings.presentation.ui.RemindersRoute

import com.alphito.kcal.core.navigation.NavMotion
import com.alphito.kcal.core.navigation.Route
import com.alphito.kcal.feature.settings.data.JsonDataExporter
import com.alphito.kcal.feature.settings.data.LocalOnlyPasswordService
import com.alphito.kcal.feature.settings.domain.DataExporter
import com.alphito.kcal.feature.settings.domain.PasswordService
import com.alphito.kcal.feature.settings.presentation.mvi.LanguageViewModel
import com.alphito.kcal.feature.settings.presentation.mvi.PrivacyViewModel
import com.alphito.kcal.feature.settings.presentation.mvi.SettingsViewModel
import com.alphito.kcal.feature.settings.presentation.ui.LanguageRoute
import com.alphito.kcal.feature.settings.presentation.ui.PrivacyRoute
import com.alphito.kcal.feature.settings.presentation.ui.SettingsRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val settingsModule = module {
    factoryOf(::LocalOnlyPasswordService) { bind<PasswordService>() }
    factoryOf(::JsonDataExporter) { bind<DataExporter>() }
    viewModelOf(::SettingsViewModel)
    viewModel { (onboarding: Boolean) -> LanguageViewModel(onboarding, get()) }
    viewModelOf(::PrivacyViewModel)
    viewModelOf(::RemindersViewModel)

    navigation<Route.Settings>(metadata = NavMotion.TopLevel) { SettingsRoute() }
    navigation<Route.Language> { route -> LanguageRoute(route.onboarding) }
    navigation<Route.PrivacySecurity> { PrivacyRoute() }
    navigation<Route.Reminders> { RemindersRoute() }
}

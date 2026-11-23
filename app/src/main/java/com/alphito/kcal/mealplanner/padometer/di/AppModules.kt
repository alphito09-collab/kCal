package com.alphito.kcal.mealplanner.padometer.di

import com.alphito.kcal.core.ads.adsModule
import com.alphito.kcal.core.billing.billingModule
import com.alphito.kcal.core.common.di.commonModule
import com.alphito.kcal.core.notifications.notificationsModule
import com.alphito.kcal.core.data.di.dataModule
import com.alphito.kcal.feature.dashboard.di.dashboardModule
import com.alphito.kcal.feature.dietplan.di.dietPlanModule
import com.alphito.kcal.feature.healthlog.di.healthLogModule
import com.alphito.kcal.feature.onboarding.di.onboardingModule
import com.alphito.kcal.feature.premium.di.premiumModule
import com.alphito.kcal.feature.progress.di.progressModule
import com.alphito.kcal.feature.settings.di.settingsModule
import com.alphito.kcal.feature.steps.di.stepsModule
import com.alphito.kcal.feature.water.di.waterModule
import com.alphito.kcal.feature.weight.di.weightModule

/** The whole graph: core first, then every feature (each also registers its nav entries). */
val kcalModules = listOf(
    commonModule,
    dataModule,
    billingModule,
    adsModule,
    notificationsModule,
    onboardingModule,
    dashboardModule,
    dietPlanModule,
    waterModule,
    stepsModule,
    weightModule,
    healthLogModule,
    progressModule,
    settingsModule,
    premiumModule,
)

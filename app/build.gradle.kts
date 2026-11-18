plugins {
    alias(libs.plugins.kcal.android.application)
}

android {
    namespace = "com.alphito.kcal.mealplanner.padometer"

    defaultConfig {
        applicationId = "com.alphito.kcal.mealplanner.padometer"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            // Full R8: shrink, optimize and obfuscate *all* code (app + libraries), shrink resources.
            optimization {
                enable = true
                keepRules {
                    files.add(file("proguard-rules.pro"))
                }
            }
        }
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/versions/9/previous-compilation-data.bin", "DebugProbesKt.bin")
    }
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:data"))

    // Features
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:dietplan"))
    implementation(project(":feature:water"))
    implementation(project(":feature:steps"))
    implementation(project(":feature:weight"))
    implementation(project(":feature:healthlog"))
    implementation(project(":feature:progress"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:premium"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.collections.immutable)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.navigation3)

    testImplementation(libs.junit)
    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

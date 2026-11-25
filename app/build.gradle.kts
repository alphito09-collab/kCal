plugins {
    alias(libs.plugins.kcal.android.application)
}

// Firebase is optional at build time: drop app/google-services.json in to enable it
// (see backend/firebase/README.md). Without it the app runs fully offline.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

/** Real AdMob IDs come from ~/.gradle/gradle.properties or CI; Google's test IDs otherwise. */
fun adProperty(name: String, fallback: String) = (findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: fallback

android {
    namespace = "com.alphito.kcal.mealplanner.padometer"

    defaultConfig {
        applicationId = "com.alphito.kcal.mealplanner.padometer"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val admobAppId = adProperty("kcal.admob.appId", "ca-app-pub-3940256099942544~3347511713")
        buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
        // UMP (consent) reads the app id from the manifest.
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField(
            "String", "ADMOB_SPLASH_UNIT",
            "\"${adProperty("kcal.admob.splashUnit", "ca-app-pub-3940256099942544/1033173712")}\"",
        )
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

// GMA Next-Gen SDK must not be mixed with the legacy play-services-ads artifacts.
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}

dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:data"))
    implementation(project(":core:firebase"))
    implementation(project(":core:billing"))
    implementation(project(":core:ads"))
    implementation(project(":core:notifications"))

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
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

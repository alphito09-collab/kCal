plugins {
    alias(libs.plugins.kcal.android.library)
    alias(libs.plugins.kcal.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "com.alphito.kcal.core.navigation" }

dependencies {
    api(libs.androidx.navigation3.runtime)
    api(libs.kotlinx.serialization.core)
    implementation(libs.androidx.compose.ui)
}

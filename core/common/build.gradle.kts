plugins {
    alias(libs.plugins.kcal.android.library)
    alias(libs.plugins.kcal.android.compose)
}

android { namespace = "com.alphito.kcal.core.common" }

dependencies {
    api(libs.kotlinx.coroutines.android)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}

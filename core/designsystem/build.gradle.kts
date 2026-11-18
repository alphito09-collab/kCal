plugins {
    alias(libs.plugins.kcal.android.library)
    alias(libs.plugins.kcal.android.compose)
}

android {
    namespace = "com.alphito.kcal.core.designsystem"
    // Design-system resources are shared by every feature module.
    androidResources.enable = true
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.kotlinx.collections.immutable)
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
}

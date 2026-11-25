plugins {
    alias(libs.plugins.kcal.android.feature)
}

android { namespace = "com.alphito.kcal.feature.onboarding" }

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(project(":core:ads"))
}

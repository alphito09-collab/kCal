plugins {
    alias(libs.plugins.kcal.android.feature)
}

android { namespace = "com.alphito.kcal.feature.steps" }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
}

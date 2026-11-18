plugins {
    alias(libs.plugins.kcal.android.feature)
}

android { namespace = "com.alphito.kcal.feature.settings" }

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}

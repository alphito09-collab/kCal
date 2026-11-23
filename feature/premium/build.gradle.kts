plugins {
    alias(libs.plugins.kcal.android.feature)
}

android { namespace = "com.alphito.kcal.feature.premium" }

dependencies {
    implementation(project(":core:billing"))
    implementation(libs.androidx.activity.compose)
}

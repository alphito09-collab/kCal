plugins {
    alias(libs.plugins.kcal.android.feature)
}

android { namespace = "com.alphito.kcal.feature.water" }

dependencies {
    implementation(project(":core:database"))
}

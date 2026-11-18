plugins {
    alias(libs.plugins.kcal.android.feature)
}

android { namespace = "com.alphito.kcal.feature.healthlog" }

dependencies {
    implementation(project(":core:database"))
}

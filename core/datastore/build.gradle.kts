plugins {
    alias(libs.plugins.kcal.android.library)
}

android { namespace = "com.alphito.kcal.core.datastore" }

dependencies {
    api(project(":core:model"))
    api(libs.androidx.datastore.preferences)
    implementation(project(":core:common"))
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}

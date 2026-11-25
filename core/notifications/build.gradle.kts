plugins {
    alias(libs.plugins.kcal.android.library)
}

android { namespace = "com.alphito.kcal.core.notifications" }

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:firebase"))
    implementation(libs.androidx.core.ktx)
    api(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.workmanager)
}

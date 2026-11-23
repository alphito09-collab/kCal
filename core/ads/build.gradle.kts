plugins {
    alias(libs.plugins.kcal.android.library)
}

android { namespace = "com.alphito.kcal.core.ads" }

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(libs.ads.mobile.sdk)
    implementation(libs.ump)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}

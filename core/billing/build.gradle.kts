plugins {
    alias(libs.plugins.kcal.android.library)
}

android { namespace = "com.alphito.kcal.core.billing" }

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:firebase"))
    api(libs.play.billing.ktx)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}

plugins {
    alias(libs.plugins.kcal.android.library)
    alias(libs.plugins.kcal.android.room)
}

android { namespace = "com.alphito.kcal.core.database" }

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.androidx.room.runtime)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}

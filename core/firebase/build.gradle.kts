plugins {
    alias(libs.plugins.kcal.android.library)
}

android { namespace = "com.alphito.kcal.core.firebase" }

dependencies {
    api(project(":core:common"))
    api(platform(libs.firebase.bom))
    api(libs.firebase.auth)
    api(libs.firebase.firestore)
    api(libs.firebase.functions)
    api(libs.firebase.messaging)
    implementation(libs.firebase.config)
    api(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}

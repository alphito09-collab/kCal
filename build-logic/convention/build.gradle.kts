plugins {
    `kotlin-dsl`
}

group = "com.alphito.kcal.buildlogic"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.stability.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "kcal.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "kcal.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "kcal.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "kcal.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidRoom") {
            id = "kcal.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("jvmLibrary") {
            id = "kcal.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}

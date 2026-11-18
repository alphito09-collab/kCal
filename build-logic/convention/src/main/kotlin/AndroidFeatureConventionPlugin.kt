import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Feature module = Android library + Compose + the standard MVI/navigation/DI stack.
 * Every feature is organised as `domain/`, `data/` and `presentation/{ui,components,mvi}`.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("kcal.android.library")
        pluginManager.apply("kcal.android.compose")

        dependencies {
            add("implementation", project(":core:common"))
            add("implementation", project(":core:model"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:navigation"))
            add("implementation", project(":core:data"))

            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-navigation3-runtime"))
            add("implementation", libs.lib("kotlinx-collections-immutable"))

            add("implementation", platform(libs.lib("koin-bom")))
            add("implementation", libs.lib("koin-android"))
            add("implementation", libs.lib("koin-compose"))
            add("implementation", libs.lib("koin-compose-viewmodel"))
            add("implementation", libs.lib("koin-compose-navigation3"))

            add("testImplementation", platform(libs.lib("koin-bom")))
            add("testImplementation", libs.lib("koin-test"))
        }
    }
}

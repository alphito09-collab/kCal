import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import com.skydoves.compose.stability.gradle.StabilityAnalyzerExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** Compose setup: BOM, tooling, compiler stability config, reports and the stability analyzer. */
internal fun Project.configureAndroidCompose(extension: CommonExtension) {
    extension.buildFeatures.compose = true

    // AGP 9.5's Compose Preview / Layoutlib tasks (used by the stability analyzer's dump/check)
    // need host unit tests with Android resources for *every* variant, release included.
    (extensions.getByName("androidComponents") as AndroidComponentsExtension<*, *, *>).beforeVariants { variant ->
        (variant as? HasHostTestsBuilder)?.hostTests?.get(HostTestBuilder.UNIT_TEST_TYPE)?.apply {
            enable = true
            includeAndroidResources = true
        }
    }

    dependencies {
        val bom = libs.lib("androidx-compose-bom")
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))
        add("implementation", libs.lib("androidx-compose-ui-tooling-preview"))
        add("debugImplementation", libs.lib("androidx-compose-ui-tooling"))
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // Shared stability configuration: domain models + java.time + immutable collections.
        stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("compose_compiler_config.conf"),
        )
        if (providers.gradleProperty("kcal.composeReports").orNull == "true") {
            reportsDestination.set(layout.buildDirectory.dir("compose_compiler"))
            metricsDestination.set(layout.buildDirectory.dir("compose_compiler"))
        }
    }

    // Compose Stability Analyzer: same stability rules as the compiler, baselines in <module>/stability.
    val stabilityConfig = rootProject.layout.projectDirectory.file("compose_compiler_config.conf")
    extensions.configure<StabilityAnalyzerExtension> {
        stabilityConfigurationFiles.add(stabilityConfig)
        stabilityValidation {
            enabled.set(true)
            outputDir.set(layout.projectDirectory.dir("stability"))
            includeTests.set(false)
            stabilityConfigurationFiles.add(stabilityConfig)
        }
    }
}

// Top-level build file. Plugins are declared here (apply false) so that the convention plugins
// in build-logic and every module share one classloader / one set of plugin versions.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.stability.analyzer) apply false
    alias(libs.plugins.google.services) apply false
}

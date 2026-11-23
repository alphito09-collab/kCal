pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "kCal"

include(":app")

// Core layer
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:navigation")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:firebase")
include(":core:billing")
include(":core:ads")
include(":core:notifications")

// Feature layer
include(":feature:onboarding")
include(":feature:dashboard")
include(":feature:dietplan")
include(":feature:water")
include(":feature:steps")
include(":feature:weight")
include(":feature:healthlog")
include(":feature:progress")
include(":feature:settings")
include(":feature:premium")

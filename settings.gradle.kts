rootProject.name = "Zerno"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")

include(":entities")
include(":arch")
include(":designsystem")
include(":data")
include(":navigation")

include(":feature:onboarding", ":feature:onboarding:api")
include(":feature:today", ":feature:today:api")
include(":feature:reader", ":feature:reader:api")
include(":feature:plans", ":feature:plans:api")
include(":feature:progress", ":feature:progress:api")
include(":feature:gamification", ":feature:gamification:api")
include(":feature:reflection", ":feature:reflection:api")
include(":feature:account", ":feature:account:api")
include(":feature:settings", ":feature:settings:api")

plugins {
    alias(libs.plugins.zerno.feature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.datetime)
        // Sessions, streak and seals from gamification; the current plan from plans.
        implementation(projects.feature.gamification.api)
        implementation(projects.feature.plans.api)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }
}

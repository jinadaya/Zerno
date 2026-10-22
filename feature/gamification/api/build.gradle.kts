plugins {
    alias(libs.plugins.zerno.feature.api)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // GamificationState exposes flows and local dates.
        api(libs.kotlinx.coroutines.core)
        api(libs.kotlinx.datetime)
    }
}

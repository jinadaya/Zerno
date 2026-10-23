plugins {
    alias(libs.plugins.zerno.feature.api)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // Plans exposes today's reading as a flow keyed by local date.
        api(libs.kotlinx.coroutines.core)
        api(libs.kotlinx.datetime)
    }
}

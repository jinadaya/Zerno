plugins {
    alias(libs.plugins.zerno.feature.api)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // Reflections exposes a flow.
        api(libs.kotlinx.coroutines.core)
    }
}

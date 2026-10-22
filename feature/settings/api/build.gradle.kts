plugins {
    alias(libs.plugins.zerno.feature.api)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // AppLanguage exposes a flow.
        api(libs.kotlinx.coroutines.core)
    }
}

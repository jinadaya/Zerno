plugins {
    alias(libs.plugins.zerno.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.bundles.navigation)
        }
    }
}
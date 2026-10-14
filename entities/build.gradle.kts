plugins {
    alias(libs.plugins.zerno.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.serialization.core)
    }
}

plugins {
    alias(libs.plugins.zerno.feature)
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(libs.multiplatform.settings.test)
        implementation(libs.kotlinx.coroutines.test)
    }
}

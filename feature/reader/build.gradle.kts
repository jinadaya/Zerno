plugins {
    alias(libs.plugins.zerno.feature)
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }
}

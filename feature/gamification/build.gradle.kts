plugins {
    alias(libs.plugins.zerno.feature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.datetime)
    }
}

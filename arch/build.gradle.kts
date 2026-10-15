plugins {
    alias(libs.plugins.zerno.kmp.compose)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.androidx.lifecycle.viewmodel)
        implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    }
}
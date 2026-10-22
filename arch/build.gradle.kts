plugins {
    alias(libs.plugins.zerno.kmp.compose)
}

// MviViewModel extends ViewModel and takes a SavedStateHandle, so features need both on their classpath.
kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.androidx.lifecycle.viewmodel)
        api(libs.androidx.lifecycle.viewmodel.savedstate)
    }
}

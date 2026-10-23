plugins {
    alias(libs.plugins.zerno.feature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // The day's reading, so a reminder skips a day already read.
        implementation(projects.feature.gamification.api)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.activity.compose)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.multiplatform.settings.test)
        implementation(libs.kotlinx.coroutines.test)
    }
}

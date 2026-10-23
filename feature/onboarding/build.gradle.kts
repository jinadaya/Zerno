plugins {
    alias(libs.plugins.zerno.feature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // Starting the recommended plan and opening its first day in the reader.
        implementation(projects.feature.plans.api)
        implementation(projects.feature.reader.api)
        implementation(libs.kotlinx.datetime)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.multiplatform.settings.test)
    }
}

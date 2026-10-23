plugins {
    alias(libs.plugins.zerno.feature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // Today's plan day, the reader to open it in, and the streak week.
        implementation(projects.feature.plans.api)
        implementation(projects.feature.reader.api)
        implementation(projects.feature.gamification.api)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }
}

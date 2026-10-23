plugins {
    alias(libs.plugins.zerno.feature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // A new reflection is reported to gamification, which marks the day Done.
        implementation(projects.feature.gamification.api)
        // A saved verse opens in the reader.
        implementation(projects.feature.reader.api)
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.json)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }
}

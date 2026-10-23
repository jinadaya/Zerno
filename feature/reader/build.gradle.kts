plugins {
    alias(libs.plugins.zerno.feature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // A daily session ends by completing its plan day and reporting the session to gamification.
        implementation(projects.feature.plans.api)
        implementation(projects.feature.gamification.api)
        implementation(projects.feature.today.api)
        // The session complete screen hands over to writing a reflection.
        implementation(projects.feature.reflection.api)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }
}

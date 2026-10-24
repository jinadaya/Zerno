plugins {
    alias(libs.plugins.zerno.feature)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.json)
        // Starting a plan goes to Today, where its day is.
        implementation(projects.feature.today.api)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.ktor.client.mock)
    }
}

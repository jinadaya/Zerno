plugins {
    alias(libs.plugins.zerno.feature)
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }
}

compose.resources {
    // Strings are the reader's own, reached through its screens only.
    publicResClass = false
    packageOfResClass = "app.zerno.feature.reader.resources"
}

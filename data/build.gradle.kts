plugins {
    alias(libs.plugins.zerno.room)
    // Compose resources carry the bundled Bible files (KJV, RST) on both platforms.
    alias(libs.plugins.zerno.kmp.compose)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.entities)
        // TranslationStore and bibleDirectory() take and return kotlinx.io Path, so callers need it too.
        api(libs.kotlinx.io.core)
        // ZernoHttpClient hands out a Ktor HttpClient; feature data layers call it directly.
        api(libs.bundles.ktor)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.ktor.client.okhttp)
    }
    sourceSets.iosMain.dependencies {
        implementation(libs.ktor.client.darwin)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.ktor.client.mock)
        implementation(libs.kotlinx.coroutines.test)
    }
}

compose.resources {
    // Bible files are reached through TranslationStore, not through Res directly.
    publicResClass = false
    packageOfResClass = "app.zerno.data.resources"
}

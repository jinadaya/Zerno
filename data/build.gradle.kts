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
    }
}

compose.resources {
    // Bible files are reached through TranslationStore, not through Res directly.
    publicResClass = false
    packageOfResClass = "app.zerno.data.resources"
}

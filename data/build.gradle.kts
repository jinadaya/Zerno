plugins {
    alias(libs.plugins.zerno.room)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.entities)
    }
}

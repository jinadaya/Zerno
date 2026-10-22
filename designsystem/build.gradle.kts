plugins {
    alias(libs.plugins.zerno.kmp.compose)
    alias(libs.plugins.zerno.sj.tokens)
}

kotlin {
    android {
        // Screenshot tests render on Robolectric; it needs the merged manifest and the font assets.
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.roborazzi)
            implementation(libs.roborazzi.compose)
            // Roborazzi leaves these to the app: its activity hosts the content through setContent.
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.ui.test.junit4)
        }
    }
}

compose.resources {
    // Fonts and mascots are reached through the design system API, not through Res directly.
    publicResClass = false
    packageOfResClass = "app.zerno.designsystem.resources"
}

// Screenshots are compared with the committed PNGs by default and a pixel change fails the test.
// `-Proborazzi.record=true` rewrites them instead (README, Screenshot tests).
val recordScreenshots = providers.gradleProperty("roborazzi.record").map { it.toBoolean() }.orElse(false)

tasks.withType<Test>().configureEach {
    val record = recordScreenshots.get()
    systemProperty("roborazzi.test.record", record)
    systemProperty("roborazzi.test.verify", !record)
    inputs.files(fileTree("src/androidHostTest/screenshots")).withPropertyName("screenshots")
}

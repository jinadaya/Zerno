import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "app.zerno.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "zerno.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "zerno.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("room") {
            id = "zerno.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("feature") {
            id = "zerno.feature"
            implementationClass = "FeatureConventionPlugin"
        }
        register("featureApi") {
            id = "zerno.feature.api"
            implementationClass = "FeatureApiConventionPlugin"
        }
    }
}

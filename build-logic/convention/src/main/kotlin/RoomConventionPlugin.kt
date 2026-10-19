import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Room (KMP) with the bundled SQLite driver; the compiler runs through KSP for each target. */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("zerno.kmp.library")
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("androidx.room")

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                // Database classes and DAOs built on RoomDatabase are used from feature modules.
                api(libs.lib("room-runtime"))
                api(libs.lib("sqlite-bundled"))
            }
        }

        // KSP creates its per-target configurations lazily, so wire them up as they appear.
        val kspConfigurations = setOf("kspAndroid", "kspAndroidMain", "kspIosArm64", "kspIosSimulatorArm64")
        configurations.configureEach {
            if (name in kspConfigurations) {
                dependencies.add(project.dependencies.create(libs.lib("room-compiler").get()))
            }
        }
    }
}

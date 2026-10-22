import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `:feature:x`: its own `:api`, the shared base modules (`:navigation` for `screen<T>`), Compose and Koin.
 * Other features are reached only through their `:api` modules.
 */
class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("zerno.kmp.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        registerModuleRules()

        // A feature's strings are its own, reached through its screens: `Res` stays internal, in `<namespace>.resources`.
        extensions.configure<ComposeExtension> {
            extensions.configure<ResourcesExtension> {
                publicResClass = false
                packageOfResClass = "$zernoNamespace.resources"
            }
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                api(project("$path:api"))
                implementation(project(":arch"))
                implementation(project(":designsystem"))
                implementation(project(":entities"))
                implementation(project(":data"))
                implementation(project(":navigation"))
                implementation(libs.lib("koin-core"))
                implementation(libs.lib("koin-compose"))
                implementation(libs.lib("koin-compose-viewmodel"))
                implementation(libs.lib("kotlinx-serialization-core"))
            }
        }
    }
}

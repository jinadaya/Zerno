import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `:feature:x:api`: routes and contracts other features may use. No Compose UI, only `:entities` and the
 * Navigation 3 runtime for `NavKey`.
 */
class FeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("zerno.kmp.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        registerModuleRules()

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                api(project(":entities"))
                api(libs.lib("androidx-navigation3-runtime"))
                implementation(libs.lib("kotlinx-serialization-core"))
            }
        }
    }
}

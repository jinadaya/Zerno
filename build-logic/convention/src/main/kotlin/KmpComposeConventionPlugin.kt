import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** A shared module with Compose Multiplatform UI and resources. No Material on purpose. */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("zerno.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            // Compose resources (fonts, vectors) ship as Android assets; the KMP library plugin leaves them off by default.
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                androidResources.enable = true
            }

            sourceSets.commonMain.dependencies {
                implementation(libs.lib("compose-runtime"))
                implementation(libs.lib("compose-foundation"))
                implementation(libs.lib("compose-ui"))
                implementation(libs.lib("compose-components-resources"))
                implementation(libs.lib("compose-ui-tooling-preview"))
            }
        }

        dependencies {
            add("androidRuntimeClasspath", libs.lib("compose-ui-tooling"))
        }
    }
}

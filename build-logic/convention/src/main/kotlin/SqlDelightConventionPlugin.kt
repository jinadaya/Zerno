import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** SQLDelight with the Android and native drivers; each module declares its own databases in `sqldelight { }`. */
class SqlDelightConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("zerno.kmp.library")
        pluginManager.apply("app.cash.sqldelight")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                // Generated database classes expose runtime types (Query, Transacter) to callers.
                api(libs.lib("sqldelight-runtime"))
            }
            sourceSets.androidMain.dependencies {
                implementation(libs.lib("sqldelight-android-driver"))
            }
            sourceSets.iosMain.dependencies {
                implementation(libs.lib("sqldelight-native-driver"))
            }
        }
    }
}

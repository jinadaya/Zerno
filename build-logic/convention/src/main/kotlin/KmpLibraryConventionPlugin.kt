import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Base for every shared module: Kotlin Multiplatform with Android and iOS targets,
 * plus an Android library so the module plugs into the Android app.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.library")

        extensions.configure<KotlinMultiplatformExtension> {
            androidTarget {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
            iosArm64()
            iosSimulatorArm64()

            sourceSets.commonMain.dependencies {
                implementation(libs.lib("kotlinx-coroutines-core"))
            }
            sourceSets.commonTest.dependencies {
                implementation(libs.lib("kotlin-test"))
            }
        }

        extensions.configure<LibraryExtension> {
            namespace = zernoNamespace
            compileSdk = libs.int("android-compileSdk")
            defaultConfig {
                minSdk = libs.int("android-minSdk")
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}

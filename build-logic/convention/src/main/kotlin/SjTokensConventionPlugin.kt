import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import tokens.GenerateSjTokensTask

/**
 * Registers `generateSjTokens`, which turns `<module>/tokens/tokens.json` into Kotlin in `commonMain`.
 * The json stays the one source of truth: edit it and the next build picks up the change.
 */
class SjTokensConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val modulePath = path.removePrefix(":").replace(':', '/')
        val generate = tasks.register<GenerateSjTokensTask>("generateSjTokens") {
            group = "design system"
            description = "Generates SjColors, SjSpacing, SjRadius and SjSize from tokens/tokens.json."
            tokensFile.set(layout.projectDirectory.file("tokens/tokens.json"))
            packageName.set("app.zerno.designsystem.tokens")
            sourceName.set("$modulePath/tokens/tokens.json")
            outputDir.set(layout.buildDirectory.dir("generated/sjTokens/commonMain/kotlin"))
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.configure {
                    kotlin.srcDir(generate.flatMap { it.outputDir })
                }
            }
        }

        // Generate on IDE sync too, so the classes resolve before the first build.
        tasks.matching { it.name == "prepareKotlinIdeaImport" }.configureEach { dependsOn(generate) }
    }
}

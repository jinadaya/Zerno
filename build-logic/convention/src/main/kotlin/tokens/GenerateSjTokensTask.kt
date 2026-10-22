package tokens

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Writes `SjColors`, `SjSpacing`, `SjRadius`, `SjSize` and `SjTypography` from `tokens.json`. */
@CacheableTask
abstract class GenerateSjTokensTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokensFile: RegularFileProperty

    @get:Input
    abstract val packageName: Property<String>

    /** Shown in the generated header so readers know where to edit. */
    @get:Input
    abstract val sourceName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val files = SjTokensGenerator.generate(
            json = tokensFile.get().asFile.readText(),
            packageName = packageName.get(),
            sourceName = sourceName.get(),
        )
        val packageDir = outputDir.get().asFile.resolve(packageName.get().replace('.', '/'))
        outputDir.get().asFile.deleteRecursively()
        packageDir.mkdirs()
        files.forEach { (name, content) -> packageDir.resolve(name).writeText(content) }
    }
}

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Locales every module with strings must cover: `values` (English) and `values-ru` (Russian). */
internal val requiredStringFolders = listOf("values", "values-ru")

private val stringKey = Regex("""<(?:string|plurals|string-array)\s+name\s*=\s*"([^"]+)"""")

/** Resource names declared in one strings xml file. */
internal fun stringKeys(xml: String): Set<String> = stringKey.findAll(xml).map { it.groupValues[1] }.toSet()

/**
 * Keys missing from a required locale, as one line each. [keysByFolder] maps a resource folder (`values`,
 * `values-ru`) to the keys declared in it. A module without any strings needs none.
 */
internal fun missingStringKeys(keysByFolder: Map<String, Set<String>>): List<String> {
    val all = requiredStringFolders.flatMap { keysByFolder[it].orEmpty() }.toSortedSet()
    return requiredStringFolders.flatMap { folder ->
        val present = keysByFolder[folder].orEmpty()
        all.filterNot { it in present }.map { "$folder: missing \"$it\"" }
    }
}

abstract class CheckStringsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stringFiles: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val keysByFolder = stringFiles.files
            .groupBy { it.parentFile.name }
            .mapValues { (_, files) -> files.flatMap { stringKeys(it.readText()) }.toSet() }
        val missing = missingStringKeys(keysByFolder)
        if (missing.isNotEmpty()) {
            throw GradleException("Strings missing a translation:\n" + missing.joinToString("\n") { "  $it" })
        }
    }
}

/** Registers `checkStrings` over `composeResources/values*` and runs it as part of `check`. */
internal fun Project.registerStringsCheck() {
    val strings = tasks.register("checkStrings", CheckStringsTask::class.java) {
        group = "verification"
        description = "Fails when a string exists in one of ${requiredStringFolders.joinToString()} but not the other."
        stringFiles.from(
            layout.projectDirectory.dir("src/commonMain/composeResources").asFileTree.matching {
                requiredStringFolders.forEach { include("$it/*.xml") }
            },
        )
    }
    tasks.matching { it.name == "check" }.configureEach { dependsOn(strings) }
}

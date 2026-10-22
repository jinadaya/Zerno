import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * The module rules from the README, checked on declared project dependencies:
 * - `:feature:x` reaches another feature only through `:feature:y:api`;
 * - `:feature:x:api` depends on `:entities` and other `:api` modules, never an implementation module.
 * Returns one line per broken dependency.
 */
internal fun moduleRuleViolations(path: String, dependencies: Collection<String>): List<String> {
    val api = path.isFeatureApi()
    return dependencies.distinct().sorted().mapNotNull { dependency ->
        when {
            dependency == path -> null
            api && dependency != ":entities" && !dependency.isFeatureApi() ->
                "$path -> $dependency: an :api module may depend only on :entities and other :api modules"
            !api && dependency.isFeature() && dependency != "$path:api" && !dependency.isFeatureApi() ->
                "$path -> $dependency: depend on $dependency:api instead"
            else -> null
        }
    }
}

private fun String.isFeature() = startsWith(":feature:")

private fun String.isFeatureApi() = isFeature() && endsWith(":api")

abstract class CheckModuleRulesTask : DefaultTask() {
    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val projectDependencies: ListProperty<String>

    @TaskAction
    fun check() {
        val violations = moduleRuleViolations(modulePath.get(), projectDependencies.get())
        if (violations.isNotEmpty()) {
            throw GradleException("Module rules broken:\n" + violations.joinToString("\n") { "  $it" })
        }
    }
}

/** Registers `checkModuleRules` and runs it as part of `check`. */
internal fun Project.registerModuleRules() {
    val rules = tasks.register("checkModuleRules", CheckModuleRulesTask::class.java) {
        group = "verification"
        description = "Fails when this module depends on a module the rules forbid."
        modulePath.set(path)
        projectDependencies.set(
            provider {
                configurations.flatMap { configuration ->
                    configuration.dependencies.withType(ProjectDependency::class.java).map { it.path }
                }
            },
        )
    }
    tasks.matching { it.name == "check" }.configureEach { dependsOn(rules) }
}

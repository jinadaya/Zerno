import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()

internal fun VersionCatalog.int(alias: String): Int =
    findVersion(alias).get().requiredVersion.toInt()

/** `:feature:today:api` -> `app.zerno.feature.today.api`, used as the Android namespace. */
internal val Project.zernoNamespace: String
    get() = "app.zerno" + path.replace(':', '.').replace('-', '_')

package app.zerno.feature.plans.data

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.feature.plans.domain.PlanEntry
import app.zerno.feature.plans.domain.parsePlanEntries
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Where the plans come from: the last catalog the backend served, kept in the `plans.catalog` row of `app_meta`, or
 * the bundled plans before the first one arrives (or if it cannot be read back). [fetch] asks the backend for a newer
 * one; nothing here throws for a network problem, so the app works offline.
 */
class PlanCatalog(private val bundled: BundledPlans, private val remote: RemotePlans?, private val meta: AppMetaDao) {
    /** What a refresh found. */
    sealed interface Update {
        /** A new catalog: [entries] in list order, at least one. */
        data class Fresh(val version: String, val entries: List<PlanEntry>) : Update

        /** The backend has nothing newer. */
        data object Unchanged : Update

        /** The backend could not be reached, or served nothing this app can read; try again later. */
        data object Failed : Update
    }

    suspend fun load(): List<PlanEntry> =
        stored()?.let { parsePlanEntries(it.plans) }?.takeIf { it.isNotEmpty() } ?: bundled.entries()

    @Suppress("TooGenericExceptionCaught")
    suspend fun fetch(): Update {
        val remote = remote ?: return Update.Failed
        val served = try {
            remote.fetch(stored()?.version)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return Update.Failed
        }
        val files = served.files ?: return Update.Unchanged
        val entries = parsePlanEntries(files)
        return if (entries.isEmpty()) Update.Failed else Update.Fresh(served.version, entries)
    }

    /** Keeps [entries] as the catalog for the next start, under the backend's [version]. */
    suspend fun keep(version: String, entries: List<PlanEntry>) {
        meta.put(AppMeta(KEY, json.encodeToString(Stored(version, entries.map { it.json }))))
    }

    private suspend fun stored(): Stored? =
        meta.get(KEY)?.let { runCatching { json.decodeFromString<Stored>(it) }.getOrNull() }

    @Serializable
    private data class Stored(val version: String, val plans: List<String>)

    private companion object {
        const val KEY = "plans.catalog"
        val json = Json { ignoreUnknownKeys = true }
    }
}

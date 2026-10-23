package app.zerno.feature.reflection.data

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.format
import app.zerno.entities.bible.parse
import app.zerno.entities.saved.Reflection
import app.zerno.entities.saved.ReflectionId
import app.zerno.entities.session.SessionId
import app.zerno.feature.reflection.api.SavedReflection
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * Reflections as JSON in the `reflections` row of the `app_meta` key-value table, so the prototype needs no schema
 * change. A value the app cannot read back counts as none; an entry whose passage no longer parses is dropped.
 */
class ReflectionStore(private val meta: AppMetaDao) {
    suspend fun load(): List<SavedReflection> = meta.get(KEY)?.let { value ->
        runCatching { json.decodeFromString<List<StoredReflection>>(value).mapNotNull { it.toSaved() } }.getOrNull()
    }.orEmpty()

    suspend fun save(reflections: List<SavedReflection>) {
        meta.put(AppMeta(KEY, json.encodeToString(reflections.map(StoredReflection::from))))
    }

    @Serializable
    private data class StoredReflection(
        val id: String,
        /** English reference text, which `PassageRef.parse` reads back. */
        val passage: String,
        val text: String,
        val createdAt: Long,
        val updatedAt: Long,
        val sessionId: String? = null,
    ) {
        fun toSaved(): SavedReflection? {
            val ref = PassageRef.parse(passage) ?: return null
            val reflection = Reflection(
                id = ReflectionId(id),
                passage = ref,
                text = text,
                createdAt = Instant.fromEpochMilliseconds(createdAt),
                updatedAt = Instant.fromEpochMilliseconds(updatedAt),
            )
            return SavedReflection(reflection, sessionId?.let(::SessionId))
        }

        companion object {
            fun from(saved: SavedReflection) = with(saved.reflection) {
                StoredReflection(
                    id = id.value,
                    passage = passage.format(),
                    text = text,
                    createdAt = createdAt.toEpochMilliseconds(),
                    updatedAt = updatedAt.toEpochMilliseconds(),
                    sessionId = saved.sessionId?.value,
                )
            }
        }
    }

    private companion object {
        const val KEY = "reflections"
        val json = Json { ignoreUnknownKeys = true }
    }
}

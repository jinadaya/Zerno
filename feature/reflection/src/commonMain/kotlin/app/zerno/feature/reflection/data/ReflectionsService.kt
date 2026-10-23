package app.zerno.feature.reflection.data

import app.zerno.entities.bible.PassageRef
import app.zerno.entities.saved.Reflection
import app.zerno.entities.saved.ReflectionId
import app.zerno.entities.session.SessionId
import app.zerno.feature.gamification.api.GamificationEvents
import app.zerno.feature.reflection.api.Reflections
import app.zerno.feature.reflection.api.SavedReflection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** [Reflections] on the [ReflectionStore], held in memory after the first read. Keep one per app. */
class ReflectionsService(
    private val store: ReflectionStore,
    private val gamification: GamificationEvents,
    private val clock: Clock,
) : Reflections {
    private val lock = Mutex()

    /** Newest first; null until first read from the store. */
    private val reflections = MutableStateFlow<List<SavedReflection>?>(null)

    override val all: Flow<List<SavedReflection>> = flow {
        lock.withLock { loaded() }
        emitAll(reflections.filterNotNull())
    }

    override suspend fun forSession(sessionId: SessionId): SavedReflection? =
        lock.withLock { loaded().firstOrNull { it.sessionId == sessionId } }

    override suspend fun save(
        passage: PassageRef,
        text: String,
        sessionId: SessionId?,
        afterPlanStep: Boolean,
    ): SavedReflection {
        require(text.isNotBlank()) { "A reflection needs some text" }
        val now = clock.now()
        val (saved, isNew) = lock.withLock {
            val current = loaded()
            val existing = sessionId?.let { id -> current.firstOrNull { it.sessionId == id } }
            val saved = if (existing == null) {
                val id = ReflectionId("reflection-${now.toEpochMilliseconds()}-${current.size}")
                SavedReflection(Reflection(id, passage, text.trim(), createdAt = now), sessionId)
            } else {
                existing.copy(reflection = existing.reflection.copy(text = text.trim(), updatedAt = now))
            }
            val updated = (current.filterNot { it.reflection.id == saved.reflection.id } + saved).newestFirst()
            store.save(updated)
            reflections.value = updated
            saved to (existing == null)
        }
        if (isNew) gamification.onReflectionSaved(afterPlanStep)
        return saved
    }

    private suspend fun loaded(): List<SavedReflection> = reflections.value
        ?: store.load().newestFirst().also { reflections.value = it }
}

private fun List<SavedReflection>.newestFirst() = sortedByDescending { it.reflection.createdAt }

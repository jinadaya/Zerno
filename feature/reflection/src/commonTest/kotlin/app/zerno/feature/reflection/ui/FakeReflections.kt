package app.zerno.feature.reflection.ui

import app.zerno.entities.bible.PassageRef
import app.zerno.entities.saved.Reflection
import app.zerno.entities.saved.ReflectionId
import app.zerno.entities.session.SessionId
import app.zerno.feature.reflection.api.Reflections
import app.zerno.feature.reflection.api.SavedReflection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

data class Saved(val passage: PassageRef, val text: String, val session: SessionId?, val afterPlan: Boolean)

fun saved(
    id: String,
    passage: PassageRef,
    text: String,
    session: SessionId? = null,
    at: String = "2026-10-22T07:00:00Z",
) = SavedReflection(Reflection(ReflectionId(id), passage, text, Instant.parse(at)), session)

class FakeReflections : Reflections {
    private val state = MutableStateFlow<List<SavedReflection>>(emptyList())
    val saves = mutableListOf<Saved>()
    var fail = false

    fun add(reflection: SavedReflection) {
        state.value += reflection
    }

    override val all: Flow<List<SavedReflection>> = state

    override suspend fun forSession(sessionId: SessionId) = state.value.firstOrNull { it.sessionId == sessionId }

    override suspend fun save(
        passage: PassageRef,
        text: String,
        sessionId: SessionId?,
        afterPlanStep: Boolean,
    ): SavedReflection {
        check(!fail) { "Disk full" }
        saves += Saved(passage, text, sessionId, afterPlanStep)
        return SavedReflection(Reflection(ReflectionId("new"), passage, text, Instant.DISTANT_PAST), sessionId)
    }

    override suspend fun get(id: ReflectionId) = state.value.firstOrNull { it.reflection.id == id }

    override suspend fun edit(id: ReflectionId, text: String): SavedReflection {
        check(!fail) { "Disk full" }
        val edited = requireNotNull(get(id)).let { it.copy(reflection = it.reflection.copy(text = text)) }
        state.value = state.value.map { if (it.reflection.id == id) edited else it }
        return edited
    }

    override suspend fun delete(id: ReflectionId) {
        state.value = state.value.filterNot { it.reflection.id == id }
    }
}

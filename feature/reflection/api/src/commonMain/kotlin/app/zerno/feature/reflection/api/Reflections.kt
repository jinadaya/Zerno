package app.zerno.feature.reflection.api

import app.zerno.entities.bible.PassageRef
import app.zerno.entities.saved.Reflection
import app.zerno.entities.session.SessionId
import kotlinx.coroutines.flow.Flow

/** The user's private reflections, kept on the device. */
interface Reflections {
    /** Every reflection, newest first. */
    val all: Flow<List<SavedReflection>>

    /** The reflection written after [sessionId], if any. */
    suspend fun forSession(sessionId: SessionId): SavedReflection?

    /**
     * Saves [text] on [passage]. With a [sessionId], replaces the session's reflection if it has one. Only a new
     * reflection counts for the day and the achievements; [afterPlanStep] says it followed a plan day.
     */
    suspend fun save(
        passage: PassageRef,
        text: String,
        sessionId: SessionId? = null,
        afterPlanStep: Boolean = false,
    ): SavedReflection
}

/** A reflection with the session it was written after, if any. */
data class SavedReflection(val reflection: Reflection, val sessionId: SessionId? = null)

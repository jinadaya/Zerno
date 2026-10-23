package app.zerno.feature.reflection.api

import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.saved.Bookmark
import app.zerno.entities.saved.BookmarkId
import app.zerno.entities.saved.Reflection
import app.zerno.entities.saved.ReflectionId
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

    suspend fun get(id: ReflectionId): SavedReflection?

    /** Replaces the text of reflection [id], keeping its passage, session and creation date. */
    suspend fun edit(id: ReflectionId, text: String): SavedReflection

    suspend fun delete(id: ReflectionId)
}

/**
 * Verses the user saved, kept on the device. Passages are in KJV numbering, like plans; a translation with another
 * numbering maps its verse before saving.
 */
interface Bookmarks {
    /** Every bookmark, newest first. */
    val all: Flow<List<Bookmark>>

    /** Saves [passage]; one already saved is returned as it is and does not count again for achievements. */
    suspend fun save(passage: PassageRef, translation: TranslationId): Bookmark

    suspend fun delete(id: BookmarkId)
}

/** A reflection with the session it was written after, if any. */
data class SavedReflection(val reflection: Reflection, val sessionId: SessionId? = null)

package app.zerno.feature.reflection.api

import androidx.navigation3.runtime.NavKey
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.session.SessionId
import kotlinx.serialization.Serializable

/**
 * Writing a reflection on [passage] (KJV numbering). After a reading session, [sessionId] ties it to the session,
 * which keeps one reflection; [afterPlanStep] says the session was a plan day.
 */
@Serializable
data class ReflectionRoute(
    val passage: PassageRef,
    val sessionId: SessionId? = null,
    val afterPlanStep: Boolean = false,
) : NavKey

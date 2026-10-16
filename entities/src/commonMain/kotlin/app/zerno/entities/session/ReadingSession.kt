package app.zerno.entities.session

import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.plan.PlanId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@Serializable
@JvmInline
value class SessionId(val value: String) {
    override fun toString(): String = value
}

/**
 * One daily reading session (A4). A session counts toward the streak for the local date of
 * [completedAt] in [timeZoneId], so travelling never moves a day that was already read (G1).
 *
 * @property planId plan the session belongs to, or null for free reading.
 * @property planDay day of that plan, set together with [planId].
 * @property completedAt null while the session is unfinished and can be resumed.
 * @property timeZoneId IANA zone the device was in when the session ended, e.g. "Europe/Moscow".
 */
@Serializable
data class ReadingSession(
    val id: SessionId,
    val passage: PassageRef,
    val translation: TranslationId,
    val planId: PlanId? = null,
    val planDay: Int? = null,
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val timeZoneId: String,
) {
    init {
        require((planId == null) == (planDay == null)) { "planId and planDay must be set together" }
        require(completedAt == null || completedAt >= startedAt) { "Session $id completed before it started" }
    }

    val isCompleted: Boolean
        get() = completedAt != null
}

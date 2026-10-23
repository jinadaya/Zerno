package app.zerno.feature.reader.domain

import app.zerno.data.time.TimeZoneProvider
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Versification
import app.zerno.entities.bible.mapVersification
import app.zerno.entities.plan.PlanId
import app.zerno.entities.session.ReadingSession
import app.zerno.entities.session.SessionId
import app.zerno.feature.gamification.api.GamificationEvents
import app.zerno.feature.plans.api.Plans
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.Instant

/** A plan day being read: [passage] in KJV numbering, as the plan writes it. */
data class DailySession(val planId: PlanId, val day: Int, val passage: PassageRef, val startedAt: Instant)

/**
 * The plan day the reader is on, if any. The reader offers "Finish reading" at the end of its passage; finishing
 * reports the session to gamification and completes the plan day. Keep one per app.
 */
class DailyReading(
    private val bible: BibleSource,
    private val plans: Plans,
    private val gamification: GamificationEvents,
    private val clock: Clock,
    private val zones: TimeZoneProvider,
) {
    private val _current = MutableStateFlow<DailySession?>(null)
    val current: StateFlow<DailySession?> = _current.asStateFlow()

    /** Starts reading [day] of [planId]; coming back to the same day keeps its start time. */
    fun begin(planId: PlanId, day: Int, passage: PassageRef) {
        val running = _current.value
        if (running?.planId == planId && running.day == day) return
        _current.value = DailySession(planId, day, passage, clock.now())
    }

    /** True when [location] is the last chapter of the current session's passage, in its translation's numbering. */
    suspend fun endsAt(location: ReaderLocation): Boolean {
        val session = _current.value ?: return false
        val numbering = bible.details(location.translation).versification
        val mapped = session.passage.mapVersification(Versification.English, numbering)
        return location.chapter.book == mapped.book && location.chapter.chapter == mapped.endChapter
    }

    /**
     * Completes the current session read in [translation]: gamification records it for today's streak, then the
     * plan day is ticked. Returns the session, or null when no plan day is being read.
     */
    suspend fun finish(translation: TranslationId): ReadingSession? {
        val running = _current.value ?: return null
        val session = ReadingSession(
            id = SessionId("${running.planId}-${running.day}-${running.startedAt.toEpochMilliseconds()}"),
            passage = running.passage,
            translation = translation,
            planId = running.planId,
            planDay = running.day,
            startedAt = running.startedAt,
            completedAt = clock.now(),
            timeZoneId = zones.current().id,
        )
        gamification.onSessionCompleted(session)
        plans.completeDay(running.planId, running.day)
        _current.value = null
        return session
    }
}

package app.zerno.debug

import app.zerno.data.prefs.AppPreferences
import app.zerno.data.time.OffsetClock
import app.zerno.data.time.TimeZoneProvider
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.session.ReadingSession
import app.zerno.entities.session.SessionId
import app.zerno.feature.gamification.api.GamificationEvents
import app.zerno.feature.gamification.api.GamificationState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/** What the debug menu does to reproduce streak, plan and onboarding cases. */
class DebugActions(
    val clock: OffsetClock,
    private val zones: TimeZoneProvider,
    private val prefs: AppPreferences,
    private val events: GamificationEvents,
    private val state: GamificationState,
) {
    /** Moves the app's clock; the streak and Today follow at once. */
    suspend fun moveClock(by: Duration) {
        clock.offset += by
        state.refresh()
    }

    suspend fun resetClock() {
        clock.offset = Duration.ZERO
        state.refresh()
    }

    /** The next launch, and the next visit to the start route, shows onboarding again. */
    fun resetOnboarding() = prefs.setOnboardingDone(false)

    /** One finished session a day for [days] days, ending yesterday, so today's reading extends the streak. */
    suspend fun seedStreak(days: Int) {
        (days downTo 1).forEach { seedSession(daysAgo = it) }
    }

    /** One session [daysAgo] days ago, to test returning after a break. */
    suspend fun seedSession(daysAgo: Int) {
        val completed = clock.now() - daysAgo.days
        val chapter = (daysAgo - 1) % JOHN_CHAPTERS + 1
        events.onSessionCompleted(
            ReadingSession(
                id = SessionId("debug-${completed.toEpochMilliseconds()}"),
                passage = PassageRef.chapter(JOHN, chapter),
                translation = TranslationId("kjv"),
                startedAt = completed - 10.minutes,
                completedAt = completed,
                timeZoneId = zones.current().id,
            ),
        )
    }

    private companion object {
        val JOHN = BookId(500)
        const val JOHN_CHAPTERS = 21
    }
}

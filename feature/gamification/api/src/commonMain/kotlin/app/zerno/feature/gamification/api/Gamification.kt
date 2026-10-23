package app.zerno.feature.gamification.api

import app.zerno.entities.achievement.Achievement
import app.zerno.entities.session.ReadingSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

/**
 * What other features report to gamification. Each call stores the event, then recomputes the streak and the
 * achievements; anything newly unlocked comes out of [GamificationState.newlyUnlocked].
 */
interface GamificationEvents {
    /** A finished session; one with `completedAt == null` is ignored. */
    suspend fun onSessionCompleted(session: ReadingSession)

    /** @param afterPlanStep the reflection answered the prompt after a plan day. */
    suspend fun onReflectionSaved(afterPlanStep: Boolean = false)

    suspend fun onVerseSaved()

    suspend fun onQuestionAnswered()

    /** @param wholeBible the plan was a whole-Bible plan. */
    suspend fun onPlanCompleted(wholeBible: Boolean = false)
}

interface GamificationState {
    val streak: StateFlow<StreakState>

    /** Every achievement in catalog order, with progress and unlock dates. Empty until the first computation. */
    val achievements: StateFlow<List<Achievement>>

    /** Achievements unlocked by an event, once each, for the end-of-session screen and toasts. */
    val newlyUnlocked: Flow<List<Achievement>>

    /** Recomputes for the current date and zone, e.g. when the app returns to the foreground after midnight. */
    suspend fun refresh()
}

enum class StreakDayStatus { Empty, Read, Done }

data class StreakDay(val date: LocalDate, val status: StreakDayStatus)

data class StreakState(
    val today: LocalDate,
    /** Consecutive read days ending today or yesterday. */
    val current: Int,
    val longest: Int,
    /** The seven days of the current week, in order. */
    val week: List<StreakDay>,
    val daysReadThisWeek: Int,
    /** The last date with a reading, or null before the first. */
    val lastRead: LocalDate? = null,
)

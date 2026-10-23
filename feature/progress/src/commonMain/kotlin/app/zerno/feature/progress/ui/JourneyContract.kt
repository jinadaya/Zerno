package app.zerno.feature.progress.ui

import app.zerno.entities.achievement.Achievement
import app.zerno.feature.gamification.api.ReadingHistory
import app.zerno.feature.gamification.api.StreakDayStatus
import app.zerno.feature.gamification.api.StreakState
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.progress.domain.HistoryEntry
import app.zerno.feature.progress.domain.JourneyNumbers
import app.zerno.feature.progress.domain.MonthCalendar
import app.zerno.feature.progress.domain.current
import app.zerno.feature.progress.domain.entries
import app.zerno.feature.progress.domain.firstOfMonth
import app.zerno.feature.progress.domain.monthCalendar
import app.zerno.feature.progress.domain.nextMonth
import app.zerno.feature.progress.domain.numbers
import app.zerno.feature.progress.domain.previousMonth
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.UserEvent
import kotlinx.datetime.LocalDate

/**
 * Everything the Journey tab shows. [calendar] is the month in [shownMonth], or the current month when that is null;
 * it is null until the streak is computed.
 */
data class JourneyState(
    val streak: StreakState? = null,
    val numbers: JourneyNumbers = JourneyNumbers(),
    val currentPlan: PlanOverview? = null,
    val history: List<HistoryEntry> = emptyList(),
    val sealsUnlocked: Int = 0,
    val sealsTotal: Int = 0,
    val days: Map<LocalDate, StreakDayStatus> = emptyMap(),
    val shownMonth: LocalDate? = null,
    val calendar: MonthCalendar? = null,
) : ScreenState

sealed interface JourneyEvent : UserEvent {
    /** The tab came into view; recomputes in case the date changed. */
    data object Shown : JourneyEvent

    data object PreviousMonthClicked : JourneyEvent

    data object NextMonthClicked : JourneyEvent

    data object CurrentPlanClicked : JourneyEvent

    data object AchievementsClicked : JourneyEvent
}

sealed interface JourneyInnerEvent : InnerEvent {
    data class StreakLoaded(val streak: StreakState) : JourneyInnerEvent

    data class HistoryLoaded(val history: ReadingHistory, val plans: List<PlanOverview>) : JourneyInnerEvent

    data class AchievementsLoaded(val achievements: List<Achievement>) : JourneyInnerEvent

    data class MonthShown(val month: LocalDate) : JourneyInnerEvent
}

sealed interface JourneyEffect : SideEffect {
    data object OpenPlans : JourneyEffect

    data object OpenAchievements : JourneyEffect
}

object JourneyReducer : MviReducer<JourneyState, JourneyInnerEvent> {
    override fun reduce(oldState: JourneyState, event: JourneyInnerEvent): JourneyState = when (event) {
        is JourneyInnerEvent.StreakLoaded -> oldState.copy(streak = event.streak).withCalendar()

        is JourneyInnerEvent.HistoryLoaded -> oldState.copy(
            numbers = event.history.numbers(),
            currentPlan = event.plans.current,
            history = event.history.entries(event.plans.associate { it.plan.id to it.plan }),
            days = event.history.days,
        ).withCalendar()

        is JourneyInnerEvent.AchievementsLoaded -> oldState.copy(
            sealsUnlocked = event.achievements.count { it.isUnlocked },
            sealsTotal = event.achievements.size,
        )

        is JourneyInnerEvent.MonthShown -> oldState.copy(shownMonth = event.month).withCalendar()
    }

    private fun JourneyState.withCalendar(): JourneyState {
        val today = streak?.today ?: return this
        val month = shownMonth ?: today.firstOfMonth()
        return copy(calendar = monthCalendar(month, today, days))
    }
}

/** The month before or after the one shown, or null when there is nothing to show there. */
internal fun JourneyState.monthAfterStep(forward: Boolean): LocalDate? {
    val calendar = calendar ?: return null
    return when {
        forward && calendar.hasNext -> calendar.month.nextMonth()
        !forward && calendar.hasPrevious -> calendar.month.previousMonth()
        else -> null
    }
}

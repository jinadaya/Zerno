package app.zerno.feature.reader.ui

import app.zerno.designsystem.component.SjAchievementSet
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.session.SessionId
import app.zerno.feature.gamification.api.StreakDayStatus
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.UserEvent

/**
 * The finished session, or null when it is gone (the app was restarted), and the plan step it ticked. [today] is
 * today's mark, so a reflection turns Read into Done while the screen is open.
 */
data class SessionCompleteState(
    val summary: SessionSummary? = null,
    val streak: Int = 0,
    val today: StreakDayStatus = StreakDayStatus.Empty,
) : ScreenState

data class SessionSummary(
    val sessionId: SessionId,
    val passage: PassageRef,
    val translation: String,
    val planTitle: String?,
    val day: Int?,
    val planLength: Int?,
    val seals: List<SealText>,
)

/** A newly unlocked achievement with its texts in the UI language. */
data class SealText(val title: String, val description: String, val set: SjAchievementSet)

sealed interface SessionCompleteEvent : UserEvent {
    data object ReflectClicked : SessionCompleteEvent

    data object DoneClicked : SessionCompleteEvent
}

sealed interface SessionCompleteInnerEvent : InnerEvent {
    data class Loaded(val summary: SessionSummary?) : SessionCompleteInnerEvent

    data class StreakChanged(val streak: Int, val today: StreakDayStatus) : SessionCompleteInnerEvent
}

sealed interface SessionCompleteEffect : SideEffect {
    data class OpenReflection(val passage: PassageRef, val sessionId: SessionId, val afterPlanStep: Boolean) :
        SessionCompleteEffect

    data object BackToToday : SessionCompleteEffect
}

object SessionCompleteReducer : MviReducer<SessionCompleteState, SessionCompleteInnerEvent> {
    override fun reduce(oldState: SessionCompleteState, event: SessionCompleteInnerEvent): SessionCompleteState =
        when (event) {
            is SessionCompleteInnerEvent.Loaded -> oldState.copy(summary = event.summary)
            is SessionCompleteInnerEvent.StreakChanged -> oldState.copy(streak = event.streak, today = event.today)
        }
}

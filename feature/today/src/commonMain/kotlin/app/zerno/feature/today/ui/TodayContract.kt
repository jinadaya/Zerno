package app.zerno.feature.today.ui

import app.zerno.feature.gamification.api.StreakState
import app.zerno.feature.today.domain.TodayCard
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.UserEvent

/**
 * [planLoaded] is false until the plans answer; then a null [card] means no plan is active. [streak] is null until
 * the streak is first computed.
 */
data class TodayState(
    val planLoaded: Boolean = false,
    val card: TodayCard? = null,
    val streak: StreakState? = null,
    val openFailed: Boolean = false,
) : ScreenState

sealed interface TodayEvent : UserEvent {
    /** The screen came into view, first or on return from the reader. */
    data object Shown : TodayEvent

    data object ActionClicked : TodayEvent

    data object ChoosePlanClicked : TodayEvent
}

sealed interface TodayInnerEvent : InnerEvent {
    data class CardChanged(val card: TodayCard?) : TodayInnerEvent

    data class StreakChanged(val streak: StreakState) : TodayInnerEvent

    data object OpenFailed : TodayInnerEvent
}

sealed interface TodayEffect : SideEffect {
    data object OpenReader : TodayEffect

    data object OpenPlans : TodayEffect
}

object TodayReducer : MviReducer<TodayState, TodayInnerEvent> {
    override fun reduce(oldState: TodayState, event: TodayInnerEvent): TodayState = when (event) {
        is TodayInnerEvent.CardChanged -> oldState.copy(planLoaded = true, card = event.card, openFailed = false)
        is TodayInnerEvent.StreakChanged -> oldState.copy(streak = event.streak)
        TodayInnerEvent.OpenFailed -> oldState.copy(openFailed = true)
    }
}

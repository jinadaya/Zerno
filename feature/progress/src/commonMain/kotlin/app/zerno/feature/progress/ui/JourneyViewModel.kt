package app.zerno.feature.progress.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.feature.gamification.api.GamificationState
import app.zerno.feature.plans.api.Plans
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel
import kotlinx.coroutines.flow.combine

/** The Journey tab: totals, coverage by book, the current plan, the streak, a month of reading and the history. */
class JourneyViewModel(savedStateHandle: SavedStateHandle, private val gamification: GamificationState, plans: Plans) :
    MviViewModel<JourneyState, JourneyEvent, JourneyInnerEvent, JourneyEffect, ScreenSnapshot.NoSnapshot>(
        savedStateHandle = savedStateHandle,
        initialState = JourneyState(),
        reducer = JourneyReducer,
        snapshotHandler = SnapshotHandler.noSnapshot(JourneyState()),
    ) {
    init {
        subscription(STREAK) { gamification.streak.collect { emit(JourneyInnerEvent.StreakLoaded(it)) } }
        subscription(HISTORY) {
            combine(gamification.history, plans.overview) { history, overview ->
                JourneyInnerEvent.HistoryLoaded(history, overview)
            }.collect { emit(it) }
        }
        subscription(SEALS) {
            gamification.achievements.collect { emit(JourneyInnerEvent.AchievementsLoaded(it)) }
        }
    }

    override fun onEvent(event: JourneyEvent) {
        when (event) {
            JourneyEvent.Shown -> intent(REFRESH) { gamification.refresh() }
            JourneyEvent.PreviousMonthClicked -> step(forward = false)
            JourneyEvent.NextMonthClicked -> step(forward = true)
            JourneyEvent.CurrentPlanClicked -> intent(OPEN) { postEffect(JourneyEffect.OpenPlans) }
            JourneyEvent.AchievementsClicked -> intent(OPEN) { postEffect(JourneyEffect.OpenAchievements) }
        }
    }

    private fun step(forward: Boolean) {
        val month = state.value.monthAfterStep(forward) ?: return
        intent(MONTH) { emit(JourneyInnerEvent.MonthShown(month)) }
    }

    private companion object {
        const val STREAK = "streak"
        const val HISTORY = "history"
        const val SEALS = "seals"
        const val REFRESH = "refresh"
        const val OPEN = "open"
        const val MONTH = "month"
    }
}

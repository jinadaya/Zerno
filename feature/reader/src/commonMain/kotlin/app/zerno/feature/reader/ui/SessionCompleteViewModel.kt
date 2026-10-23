package app.zerno.feature.reader.ui

import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.SavedStateHandle
import app.zerno.designsystem.component.SjAchievementSet
import app.zerno.entities.achievement.AchievementSet
import app.zerno.feature.gamification.api.AchievementTexts
import app.zerno.feature.gamification.api.GamificationState
import app.zerno.feature.gamification.api.StreakDayStatus
import app.zerno.feature.plans.api.Plans
import app.zerno.feature.reader.domain.DailyReading
import app.zerno.feature.reader.domain.FinishedSession
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel

/**
 * The session [DailyReading] finished last, with its plan step, new seals and the streak as it updates. Writing a
 * reflection from here turns today from Read into Done.
 */
class SessionCompleteViewModel(
    savedStateHandle: SavedStateHandle,
    private val daily: DailyReading,
    private val plans: Plans,
    private val gamification: GamificationState,
    private val texts: AchievementTexts,
) : MviViewModel<
    SessionCompleteState,
    SessionCompleteEvent,
    SessionCompleteInnerEvent,
    SessionCompleteEffect,
    ScreenSnapshot.NoSnapshot,
    >(
    savedStateHandle = savedStateHandle,
    initialState = SessionCompleteState(),
    reducer = SessionCompleteReducer,
    snapshotHandler = SnapshotHandler.noSnapshot(SessionCompleteState()),
) {
    init {
        intent(LOAD) { emit(SessionCompleteInnerEvent.Loaded(daily.lastFinished.value?.let { summary(it) })) }
        subscription(STREAK) {
            gamification.streak.collect { streak ->
                val today = streak.week.firstOrNull { it.date == streak.today }?.status
                emit(SessionCompleteInnerEvent.StreakChanged(streak.current, today ?: StreakDayStatus.Empty))
            }
        }
    }

    override fun onEvent(event: SessionCompleteEvent) {
        when (event) {
            SessionCompleteEvent.ReflectClicked -> state.value.summary?.let { summary ->
                intent(NAVIGATE) {
                    postEffect(
                        SessionCompleteEffect.OpenReflection(
                            summary.passage,
                            summary.sessionId,
                            afterPlanStep = summary.day != null,
                        ),
                    )
                }
            }

            SessionCompleteEvent.DoneClicked -> intent(NAVIGATE) { postEffect(SessionCompleteEffect.BackToToday) }
        }
    }

    private suspend fun summary(finished: FinishedSession): SessionSummary {
        val session = finished.session
        val plan = session.planId?.let { id -> plans.catalog().firstOrNull { it.id == id } }
        return SessionSummary(
            sessionId = session.id,
            passage = session.passage,
            translation = session.translation.value.uppercase(),
            planTitle = plan?.title?.get(Locale.current.language),
            day = session.planDay,
            planLength = plan?.length,
            seals = finished.newSeals.map { SealText(texts.title(it), texts.description(it), it.set.seal) },
        )
    }

    private val AchievementSet.seal: SjAchievementSet
        get() = when (this) {
            AchievementSet.Bird -> SjAchievementSet.Bird
            AchievementSet.Sheep -> SjAchievementSet.Sheep
            AchievementSet.Lamp -> SjAchievementSet.Lamp
        }

    private companion object {
        const val LOAD = "load"
        const val STREAK = "streak"
        const val NAVIGATE = "navigate"
    }
}

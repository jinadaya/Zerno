package app.zerno.feature.today.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.feature.gamification.api.GamificationState
import app.zerno.feature.plans.api.PlanToday
import app.zerno.feature.plans.api.Plans
import app.zerno.feature.reader.api.PassageProgress
import app.zerno.feature.reader.api.ReaderLauncher
import app.zerno.feature.today.domain.TodayAction
import app.zerno.feature.today.domain.TodayCard
import app.zerno.feature.today.domain.todayCard
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Today's plan day with its button state, and the streak week. The date comes from the streak, so the card moves on
 * when [GamificationState.refresh] sees a new day; reading progress is read again each time the screen is shown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    savedStateHandle: SavedStateHandle,
    private val plans: Plans,
    private val reader: ReaderLauncher,
    private val gamification: GamificationState,
) : MviViewModel<TodayState, TodayEvent, TodayInnerEvent, TodayEffect, ScreenSnapshot.NoSnapshot>(
    savedStateHandle = savedStateHandle,
    initialState = TodayState(),
    reducer = TodayReducer,
    snapshotHandler = SnapshotHandler.noSnapshot(TodayState()),
) {
    /** The plan day last loaded; null before loading and when no plan is active. */
    private var today: PlanToday? = null

    init {
        subscription(STREAK) {
            gamification.streak.collect { emit(TodayInnerEvent.StreakChanged(it)) }
        }
        subscription(PLAN) {
            gamification.streak.map { it.today }.distinctUntilChanged().flatMapLatest(plans::today).collectLatest {
                today = it
                emit(TodayInnerEvent.CardChanged(it?.let { day -> todayCard(day, progress(day)) }))
            }
        }
    }

    override fun onEvent(event: TodayEvent) {
        when (event) {
            TodayEvent.Shown -> intent(REFRESH) {
                gamification.refresh()
                today?.let { emit(TodayInnerEvent.CardChanged(todayCard(it, progress(it)))) }
            }

            TodayEvent.ActionClicked -> state.value.card?.let(::open)

            TodayEvent.ChoosePlanClicked -> intent(OPEN) { postEffect(TodayEffect.OpenPlans) }
        }
    }

    // A reader that cannot open (no Bible installed) leaves the card in place with a message.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun open(card: TodayCard) = intent(OPEN) {
        try {
            reader.openPlanDay(card.plan.id, card.day.day, card.passage, resume = card.action == TodayAction.Continue)
            postEffect(TodayEffect.OpenReader)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(TodayInnerEvent.OpenFailed)
        }
    }

    // Progress only decides between Start and Continue, so a reader that cannot answer counts as not started.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun progress(day: PlanToday): PassageProgress? {
        if (day.done) return null
        return try {
            reader.progressIn(day.day.passages.first())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val STREAK = "streak"
        const val PLAN = "plan"
        const val REFRESH = "refresh"
        const val OPEN = "open"
    }
}

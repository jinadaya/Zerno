package app.zerno.feature.plans.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.feature.plans.api.PlanRunStatus
import app.zerno.feature.plans.api.Plans
import app.zerno.feature.plans.domain.inListOrder
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel
import kotlinx.coroutines.CancellationException

/** Every plan with its progress: the active one first, then paused, not started and finished. */
class PlansViewModel(savedStateHandle: SavedStateHandle, plans: Plans) :
    MviViewModel<PlansState, PlansEvent, PlansInnerEvent, PlansEffect, ScreenSnapshot.NoSnapshot>(
        savedStateHandle = savedStateHandle,
        initialState = PlansState(),
        reducer = PlansReducer,
        snapshotHandler = SnapshotHandler.noSnapshot(PlansState()),
    ) {
    init {
        subscription(LOAD) { plans.overview.collect { emit(PlansInnerEvent.Loaded(it.inListOrder())) } }
    }

    override fun onEvent(event: PlansEvent) {
        when (event) {
            is PlansEvent.PlanClicked -> intent(OPEN) { postEffect(PlansEffect.OpenPlan(event.planId)) }
        }
    }

    private companion object {
        const val LOAD = "load"
        const val OPEN = "open"
    }
}

/**
 * One plan. Starting makes it the active plan (pausing any other, whose days are kept) and goes to Today; pausing
 * keeps its days for later.
 */
class PlanDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val route: PlanDetailRoute,
    private val plans: Plans,
) : MviViewModel<PlanDetailState, PlanDetailEvent, PlanDetailInnerEvent, PlanDetailEffect, ScreenSnapshot.NoSnapshot>(
    savedStateHandle = savedStateHandle,
    initialState = PlanDetailState(),
    reducer = PlanDetailReducer,
    snapshotHandler = SnapshotHandler.noSnapshot(PlanDetailState()),
) {
    init {
        subscription(LOAD) {
            plans.overview.collect { all ->
                val plan = all.firstOrNull { it.plan.id == route.planId }
                val otherActive = all.any { it.status == PlanRunStatus.Active && it.plan.id != route.planId }
                emit(PlanDetailInnerEvent.Loaded(plan, otherActive))
            }
        }
    }

    override fun onEvent(event: PlanDetailEvent) {
        val plan = state.value.plan
        when (event) {
            PlanDetailEvent.StartClicked -> if (plan != null && plan.status != PlanRunStatus.Active) {
                change(PlanDetailEffect.OpenToday) { plans.start(route.planId) }
            }

            PlanDetailEvent.PauseClicked -> if (plan?.status == PlanRunStatus.Active) {
                change(effect = null) { plans.pause() }
            }

            PlanDetailEvent.BackClicked -> intent(CLOSE) { postEffect(PlanDetailEffect.Close) }
        }
    }

    // Storage can fail; the plan stays as it was with a message.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun change(effect: PlanDetailEffect?, action: suspend () -> Unit) {
        if (state.value.busy) return
        intent(CHANGE) {
            emit(PlanDetailInnerEvent.Busy)
            try {
                action()
                emit(PlanDetailInnerEvent.Done)
                effect?.let { postEffect(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(PlanDetailInnerEvent.Failed)
            }
        }
    }

    private companion object {
        const val LOAD = "load"
        const val CHANGE = "change"
        const val CLOSE = "close"
    }
}

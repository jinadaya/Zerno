package app.zerno.feature.plans.ui

import app.zerno.entities.plan.PlanId
import app.zerno.feature.plans.api.PlanOverview
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.UserEvent

/** The plan list in list order; null until loaded. */
data class PlansState(val plans: List<PlanOverview>? = null) : ScreenState

sealed interface PlansEvent : UserEvent {
    data class PlanClicked(val planId: PlanId) : PlansEvent
}

sealed interface PlansInnerEvent : InnerEvent {
    data class Loaded(val plans: List<PlanOverview>) : PlansInnerEvent
}

sealed interface PlansEffect : SideEffect {
    data class OpenPlan(val planId: PlanId) : PlansEffect
}

object PlansReducer : MviReducer<PlansState, PlansInnerEvent> {
    override fun reduce(oldState: PlansState, event: PlansInnerEvent): PlansState = when (event) {
        is PlansInnerEvent.Loaded -> oldState.copy(plans = event.plans)
    }
}

/** [plan] is null until loaded. [otherActive] when another plan is current, so starting this one switches. */
data class PlanDetailState(
    val plan: PlanOverview? = null,
    val otherActive: Boolean = false,
    val busy: Boolean = false,
    val failed: Boolean = false,
) : ScreenState

sealed interface PlanDetailEvent : UserEvent {
    /** Start, resume, switch to or start again, depending on the plan's state. */
    data object StartClicked : PlanDetailEvent

    data object PauseClicked : PlanDetailEvent

    data object BackClicked : PlanDetailEvent
}

sealed interface PlanDetailInnerEvent : InnerEvent {
    data class Loaded(val plan: PlanOverview?, val otherActive: Boolean) : PlanDetailInnerEvent

    data object Busy : PlanDetailInnerEvent

    data object Done : PlanDetailInnerEvent

    data object Failed : PlanDetailInnerEvent
}

sealed interface PlanDetailEffect : SideEffect {
    /** A plan was started: today's day is on Today. */
    data object OpenToday : PlanDetailEffect

    data object Close : PlanDetailEffect
}

object PlanDetailReducer : MviReducer<PlanDetailState, PlanDetailInnerEvent> {
    override fun reduce(oldState: PlanDetailState, event: PlanDetailInnerEvent): PlanDetailState = when (event) {
        is PlanDetailInnerEvent.Loaded -> oldState.copy(plan = event.plan, otherActive = event.otherActive)
        PlanDetailInnerEvent.Busy -> oldState.copy(busy = true, failed = false)
        PlanDetailInnerEvent.Done -> oldState.copy(busy = false)
        PlanDetailInnerEvent.Failed -> oldState.copy(busy = false, failed = true)
    }
}

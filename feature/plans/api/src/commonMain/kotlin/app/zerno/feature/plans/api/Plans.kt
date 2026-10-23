package app.zerno.feature.plans.api

import app.zerno.entities.plan.PlanDay
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Reading plans as other features see them. */
interface Plans {
    /** Every plan the user can start, in the order the plan list shows them. */
    suspend fun catalog(): List<ReadingPlan>

    /** Makes [planId] the active plan from today, pausing any other; a paused plan resumes where it stopped. */
    suspend fun start(planId: PlanId)

    /** Pauses the active plan, keeping its days; Today then offers no plan day until one is started. */
    suspend fun pause()

    /** Every catalog plan with the user's progress in it, in catalog order, updated as plans change. */
    val overview: Flow<List<PlanOverview>>

    /**
     * Marks [day] of [planId] read today. Days can be completed in any order; completing one again keeps its first
     * date, and the plan completes with its last day. A plan never started is left alone.
     */
    suspend fun completeDay(planId: PlanId, day: Int)

    /**
     * The plan day to show on [date], updated as plans start and days complete: a day completed that date (so it
     * reads "Read again"), else the active plan's next day. Null when no plan is active and none finished that date.
     */
    fun today(date: LocalDate): Flow<PlanToday?>
}

/** One day of a plan as the Today card shows it. [done] is true once it was completed on the date asked for. */
data class PlanToday(val plan: ReadingPlan, val day: PlanDay, val done: Boolean)

enum class PlanRunStatus { NotStarted, Active, Paused, Completed }

/**
 * A plan and how far the user is in it. [completedDays] are the days done in the current run, kept when the plan is
 * paused or another is started; [nextDay] is the lowest day not done, null once finished.
 */
data class PlanOverview(
    val plan: ReadingPlan,
    val status: PlanRunStatus,
    val completedDays: Set<Int> = emptySet(),
    val nextDay: Int? = 1,
    val completedOn: LocalDate? = null,
    val timesCompleted: Int = 0,
)

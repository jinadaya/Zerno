package app.zerno.feature.plans.domain

import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.plans.api.PlanRunStatus
import app.zerno.feature.plans.api.PlanToday
import kotlinx.datetime.LocalDate

/**
 * The plan day to show on [date]: from the active plan, or from a plan finished that date so its last day still
 * reads "Read again". Null when neither exists or the plan is no longer in [catalog].
 */
fun PlanJourney.todayIn(catalog: List<ReadingPlan>, date: LocalDate): PlanToday? {
    val progress = active ?: plans.values.firstOrNull { it.completedOn == date } ?: return null
    val plan = catalog.firstOrNull { it.id == progress.planId } ?: return null
    val step = progress.dayFor(date) ?: return null
    val day = plan.days.firstOrNull { it.day == step.day } ?: return null
    return PlanToday(plan, day, step.done)
}

/** Every plan in [catalog] with this journey's progress in it, in catalog order. */
fun PlanJourney.overview(catalog: List<ReadingPlan>): List<PlanOverview> = catalog.map { plan ->
    val progress = plans[plan.id] ?: return@map PlanOverview(plan, PlanRunStatus.NotStarted)
    PlanOverview(
        plan = plan,
        status = when (progress.status) {
            PlanStatus.Active -> PlanRunStatus.Active
            PlanStatus.Paused -> PlanRunStatus.Paused
            PlanStatus.Completed -> PlanRunStatus.Completed
        },
        completedDays = progress.completed.keys,
        nextDay = progress.nextDay,
        completedOn = progress.completedOn,
        timesCompleted = progress.timesCompleted,
    )
}

/** Average reading time of a day of the plan, in whole minutes rounded up. */
val ReadingPlan.minutesPerDay: Int
    get() = (days.sumOf { it.minutes } + length - 1) / length

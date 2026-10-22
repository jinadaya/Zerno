package app.zerno.feature.plans.domain

import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan
import kotlinx.datetime.LocalDate

enum class PlanStatus { Active, Paused, Completed }

/**
 * How far the user is in one plan. Days are never tied to calendar dates: the next day to read is the lowest one not
 * done yet, so missed days roll forward instead of piling up, and any day can be opened and completed early.
 *
 * @property completed day number to the local date it was first completed in the current run.
 * @property timesCompleted finished runs before the current one.
 */
data class PlanProgress(
    val planId: PlanId,
    val length: Int,
    val status: PlanStatus,
    val startedOn: LocalDate,
    val completed: Map<Int, LocalDate> = emptyMap(),
    val completedOn: LocalDate? = null,
    val timesCompleted: Int = 0,
) {
    init {
        require(length > 0) { "Plan $planId has no days" }
        require(completed.keys.all { it in 1..length }) { "Plan $planId has completed days outside 1..$length" }
    }

    /** The lowest day not done yet, or null once every day is done. */
    val nextDay: Int?
        get() = (1..length).firstOrNull { it !in completed }

    val completedCount: Int
        get() = completed.size

    /**
     * The day to show on [date]: a day completed that date (so it reads "Read again"), otherwise [nextDay].
     * Null when the plan is finished and nothing was read from it that date.
     */
    fun dayFor(date: LocalDate): PlanStep? {
        val doneThatDate = completed.filterValues { it == date }.keys.maxOrNull()
        return doneThatDate?.let { PlanStep(it, done = true) } ?: nextDay?.let { PlanStep(it, done = false) }
    }
}

/** A plan day as the Today card sees it. */
data class PlanStep(val day: Int, val done: Boolean)

/**
 * Every plan the user has touched, with at most one [PlanStatus.Active]. Switching plans pauses the old one and keeps
 * its completed days, so switching back resumes where the user left off.
 */
data class PlanJourney(
    val plans: Map<PlanId, PlanProgress> = emptyMap(),
) {
    init {
        require(plans.values.count { it.status == PlanStatus.Active } <= 1) { "More than one active plan" }
        require(plans.all { (id, progress) -> id == progress.planId }) { "Plan ids do not match their progress" }
    }

    val active: PlanProgress?
        get() = plans.values.firstOrNull { it.status == PlanStatus.Active }

    /**
     * Makes [plan] the active one and pauses any other. A paused plan resumes with its days; a completed plan starts
     * a new run. A plan whose length changed in a new content version keeps only the days that still exist.
     */
    fun start(plan: ReadingPlan, today: LocalDate): PlanJourney {
        val existing = plans[plan.id]
        val started = when {
            existing == null -> PlanProgress(plan.id, plan.length, PlanStatus.Active, startedOn = today)
            existing.status == PlanStatus.Completed ->
                PlanProgress(plan.id, plan.length, PlanStatus.Active, startedOn = today, timesCompleted = existing.timesCompleted)
            else -> existing.copy(
                length = plan.length,
                status = PlanStatus.Active,
                completed = existing.completed.filterKeys { it <= plan.length },
            )
        }
        return copy(plans = pause().plans + (plan.id to started))
    }

    /** Pauses the active plan, if any. Its days are kept. */
    fun pause(): PlanJourney = copy(
        plans = plans.mapValues { (_, p) -> if (p.status == PlanStatus.Active) p.copy(status = PlanStatus.Paused) else p },
    )

    /**
     * Marks [day] of [planId] done on [today]. Any day can be completed, in any order; completing it again keeps the
     * first date. The plan completes when every day is done. Plans not started are ignored.
     */
    fun completeDay(planId: PlanId, day: Int, today: LocalDate): PlanJourney {
        val progress = plans[planId] ?: return this
        if (progress.status == PlanStatus.Completed) return this
        require(day in 1..progress.length) { "Day $day is outside plan $planId (1..${progress.length})" }
        val completed = if (day in progress.completed) progress.completed else progress.completed + (day to today)
        val finished = completed.size == progress.length
        val updated = progress.copy(
            completed = completed,
            status = if (finished) PlanStatus.Completed else progress.status,
            completedOn = if (finished) today else null,
            timesCompleted = if (finished) progress.timesCompleted + 1 else progress.timesCompleted,
        )
        return copy(plans = plans + (planId to updated))
    }

    /** Plans finished at least once, for the plan achievements. */
    val plansCompleted: Int
        get() = plans.values.sumOf { it.timesCompleted }
}

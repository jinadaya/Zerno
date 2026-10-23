package app.zerno.feature.plans.api

import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan

/** Reading plans as other features see them. */
interface Plans {
    /** Every plan the user can start, in the order the plan list shows them. */
    suspend fun catalog(): List<ReadingPlan>

    /** Makes [planId] the active plan from today, pausing any other; a paused plan resumes where it stopped. */
    suspend fun start(planId: PlanId)
}

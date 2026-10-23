package app.zerno.feature.plans.ui

import androidx.navigation3.runtime.NavKey
import app.zerno.entities.plan.PlanId
import kotlinx.serialization.Serializable

// PlansRoute, the tab other features open, is in `:feature:plans:api`.

/** One plan: what it is, its days, and starting, pausing or switching to it. */
@Serializable
data class PlanDetailRoute(val planId: PlanId) : NavKey

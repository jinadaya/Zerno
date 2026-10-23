package app.zerno.feature.plans.data

import app.zerno.data.time.TimeZoneProvider
import app.zerno.data.time.today
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.plans.api.Plans
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** [Plans] on the bundled catalog and the stored [PlanJourneyStore]. Keep one per app. */
class PlansService(
    private val bundled: BundledPlans,
    private val store: PlanJourneyStore,
    private val clock: Clock,
    private val zones: TimeZoneProvider,
) : Plans {
    private val lock = Mutex()
    private var catalog: List<ReadingPlan>? = null

    override suspend fun catalog(): List<ReadingPlan> = lock.withLock { loadedCatalog() }

    override suspend fun start(planId: PlanId) = lock.withLock {
        val plan = requireNotNull(loadedCatalog().firstOrNull { it.id == planId }) { "No plan $planId" }
        store.save(store.load().start(plan, clock.today(zones)))
    }

    private suspend fun loadedCatalog(): List<ReadingPlan> = catalog ?: bundled.load().also { catalog = it }
}

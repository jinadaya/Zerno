package app.zerno.feature.plans.data

import app.zerno.data.time.TimeZoneProvider
import app.zerno.data.time.today
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.plans.api.PlanToday
import app.zerno.feature.plans.api.Plans
import app.zerno.feature.plans.domain.PlanJourney
import app.zerno.feature.plans.domain.overview
import app.zerno.feature.plans.domain.todayIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

/**
 * [Plans] on the bundled catalog and the stored [PlanJourneyStore]. Keep one per app: it holds the journey in memory
 * after the first read, so every change goes through it.
 */
class PlansService(
    private val bundled: BundledPlans,
    private val store: PlanJourneyStore,
    private val clock: Clock,
    private val zones: TimeZoneProvider,
) : Plans {
    private val lock = Mutex()
    private var catalog: List<ReadingPlan>? = null

    /** Null until first read from the store. */
    private val journey = MutableStateFlow<PlanJourney?>(null)

    override suspend fun catalog(): List<ReadingPlan> = lock.withLock { loadedCatalog() }

    override suspend fun start(planId: PlanId) = lock.withLock {
        val plan = requireNotNull(loadedCatalog().firstOrNull { it.id == planId }) { "No plan $planId" }
        save(loadedJourney().start(plan, clock.today(zones)))
    }

    override suspend fun pause() = lock.withLock { save(loadedJourney().pause()) }

    override val overview: Flow<List<PlanOverview>> = flow {
        val plans = lock.withLock {
            loadedJourney()
            loadedCatalog()
        }
        emitAll(journey.filterNotNull().map { it.overview(plans) })
    }.distinctUntilChanged()

    override suspend fun completeDay(planId: PlanId, day: Int) = lock.withLock {
        save(loadedJourney().completeDay(planId, day, clock.today(zones)))
    }

    override fun today(date: LocalDate): Flow<PlanToday?> = flow {
        val plans = lock.withLock {
            loadedJourney()
            loadedCatalog()
        }
        emitAll(journey.filterNotNull().map { it.todayIn(plans, date) })
    }.distinctUntilChanged()

    private suspend fun save(updated: PlanJourney) {
        store.save(updated)
        journey.value = updated
    }

    private suspend fun loadedCatalog(): List<ReadingPlan> = catalog ?: bundled.load().also { catalog = it }

    private suspend fun loadedJourney(): PlanJourney = journey.value ?: store.load().also { journey.value = it }
}

package app.zerno.feature.plans.data

import app.zerno.data.time.TimeZoneProvider
import app.zerno.data.time.today
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.plans.api.PlanToday
import app.zerno.feature.plans.api.Plans
import app.zerno.feature.plans.domain.PlanEntry
import app.zerno.feature.plans.domain.PlanJourney
import app.zerno.feature.plans.domain.mergeCatalog
import app.zerno.feature.plans.domain.overview
import app.zerno.feature.plans.domain.todayIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * [Plans] on the [PlanCatalog] (served by the backend, bundled until then) and the stored [PlanJourneyStore]. Keep
 * one per app: it holds the catalog and the journey in memory after the first read, so every change goes through it.
 */
class PlansService(
    private val catalogSource: PlanCatalog,
    private val store: PlanJourneyStore,
    private val clock: Clock,
    private val zones: TimeZoneProvider,
) : Plans {
    private val lock = Mutex()

    /** Null until first read; replaced when a refresh brings a newer catalog. */
    private val entries = MutableStateFlow<List<PlanEntry>?>(null)
    private var lastRefresh: Instant? = null

    /** Null until first read from the store. */
    private val journey = MutableStateFlow<PlanJourney?>(null)

    override suspend fun catalog(): List<ReadingPlan> = lock.withLock { loadedEntries().map { it.plan } }

    override suspend fun start(planId: PlanId) = lock.withLock {
        val plan = requireNotNull(loadedEntries().firstOrNull { it.plan.id == planId }?.plan) { "No plan $planId" }
        save(loadedJourney().start(plan, clock.today(zones)))
    }

    override suspend fun pause() = lock.withLock { save(loadedJourney().pause()) }

    override val overview: Flow<List<PlanOverview>> = withCatalog { journey, plans -> journey.overview(plans) }

    override suspend fun completeDay(planId: PlanId, day: Int) = lock.withLock {
        save(loadedJourney().completeDay(planId, day, clock.today(zones)))
    }

    override fun today(date: LocalDate): Flow<PlanToday?> = withCatalog { journey, plans ->
        journey.todayIn(plans, date)
    }

    /**
     * Asks the backend for a newer catalog, at most once per [RefreshInterval] after an answer. The request runs
     * outside the lock, so starting a plan never waits on the network; plans the user has started stay in the
     * catalog even if the backend drops them.
     */
    override suspend fun refreshCatalog() {
        val now = clock.now()
        if (lastRefresh?.let { now - it < RefreshInterval } == true) return
        when (val update = catalogSource.fetch()) {
            PlanCatalog.Update.Failed -> return

            PlanCatalog.Update.Unchanged -> Unit

            is PlanCatalog.Update.Fresh -> lock.withLock {
                val merged = mergeCatalog(update.entries, loadedEntries(), loadedJourney().plans.keys)
                catalogSource.keep(update.version, merged)
                entries.value = merged
            }
        }
        lastRefresh = now
    }

    private fun <T> withCatalog(read: (PlanJourney, List<ReadingPlan>) -> T): Flow<T> = flow {
        lock.withLock {
            loadedJourney()
            loadedEntries()
        }
        emitAll(
            combine(journey.filterNotNull(), entries.filterNotNull()) { journey, entries ->
                read(journey, entries.map { it.plan })
            },
        )
    }.distinctUntilChanged()

    private suspend fun save(updated: PlanJourney) {
        store.save(updated)
        journey.value = updated
    }

    private suspend fun loadedEntries(): List<PlanEntry> =
        entries.value ?: catalogSource.load().also { entries.value = it }

    private suspend fun loadedJourney(): PlanJourney = journey.value ?: store.load().also { journey.value = it }

    private companion object {
        val RefreshInterval = 1.hours
    }
}

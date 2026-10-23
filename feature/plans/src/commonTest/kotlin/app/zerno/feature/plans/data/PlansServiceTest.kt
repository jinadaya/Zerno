package app.zerno.feature.plans.data

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.data.time.FakeClock
import app.zerno.entities.plan.PlanId
import app.zerno.feature.plans.domain.PlanJourney
import app.zerno.feature.plans.domain.PlanStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PlansServiceTest {
    private val rows = mutableMapOf<String, String>()
    private val dao = object : AppMetaDao {
        override suspend fun get(key: String) = rows[key]
        override suspend fun put(meta: AppMeta) {
            rows[meta.key] = meta.value
        }
    }
    private val today = LocalDate(2026, 10, 22)
    private val store = PlanJourneyStore(dao)
    private val plans = PlansService(
        bundled = BundledPlans(),
        store = store,
        clock = FakeClock(today.atStartOfDayIn(TimeZone.UTC)),
        zones = { TimeZone.UTC },
    )

    @Test
    fun freshInstallHasNoPlans() = runTest {
        assertEquals(PlanJourney(), store.load())
    }

    @Test
    fun startingAPlanStoresItAsActiveFromToday() = runTest {
        plans.start(PlanId("gospel-of-john"))

        val active = store.load().active
        assertEquals(PlanId("gospel-of-john"), active?.planId)
        assertEquals(21, active?.length)
        assertEquals(today, active?.startedOn)
        assertEquals(1, active?.nextDay)
    }

    @Test
    fun startingAnotherPlanPausesTheFirstAndKeepsItsDays() = runTest {
        plans.start(PlanId("first-steps"))
        store.save(store.load().completeDay(PlanId("first-steps"), 1, today))

        plans.start(PlanId("prayer"))

        val journey = store.load()
        assertEquals(PlanId("prayer"), journey.active?.planId)
        assertEquals(PlanStatus.Paused, journey.plans.getValue(PlanId("first-steps")).status)
        assertEquals(mapOf(1 to today), journey.plans.getValue(PlanId("first-steps")).completed)
    }

    @Test
    fun unknownPlanFails() = runTest {
        assertFailsWith<IllegalArgumentException> { plans.start(PlanId("nope")) }
        assertNull(store.load().active)
    }

    @Test
    fun unreadableStoredValueCountsAsNoPlans() = runTest {
        rows["plans.journey"] = "{not json"
        assertEquals(PlanJourney(), store.load())
    }
}

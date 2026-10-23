package app.zerno.feature.plans.data

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.data.time.FakeClock
import app.zerno.entities.plan.PlanId
import app.zerno.feature.plans.api.PlanToday
import app.zerno.feature.plans.domain.PlanJourney
import app.zerno.feature.plans.domain.PlanStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        plans.completeDay(PlanId("first-steps"), 1)

        plans.start(PlanId("prayer"))

        val journey = store.load()
        assertEquals(PlanId("prayer"), journey.active?.planId)
        assertEquals(PlanStatus.Paused, journey.plans.getValue(PlanId("first-steps")).status)
        assertEquals(mapOf(1 to today), journey.plans.getValue(PlanId("first-steps")).completed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun todayFollowsTheActivePlan() = runTest {
        val seen = mutableListOf<PlanToday?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { plans.today(today).toList(seen) }
        assertEquals(listOf<PlanToday?>(null), seen)

        plans.start(PlanId("gospel-of-john"))
        runCurrent()
        val first = assertNotNull(seen.last())
        assertEquals(PlanId("gospel-of-john"), first.plan.id)
        assertEquals(1, first.day.day)
        assertFalse(first.done)

        plans.completeDay(PlanId("gospel-of-john"), 1)
        runCurrent()
        assertEquals(PlanToday(first.plan, first.day, done = true), seen.last())
        assertEquals(3, seen.size)
    }

    @Test
    fun theNextDateShowsTheNextDay() = runTest {
        plans.start(PlanId("gospel-of-john"))
        plans.completeDay(PlanId("gospel-of-john"), 1)

        val tomorrow = assertNotNull(plans.today(today.plus(1, DateTimeUnit.DAY)).first())
        assertEquals(2, tomorrow.day.day)
        assertFalse(tomorrow.done)
    }

    @Test
    fun aPlanFinishedTodayStillShowsItsLastDay() = runTest {
        plans.start(PlanId("prayer"))
        (1..14).forEach { plans.completeDay(PlanId("prayer"), it) }

        assertNull(store.load().active)
        val last = assertNotNull(plans.today(today).first())
        assertEquals(14, last.day.day)
        assertTrue(last.done)
        assertNull(plans.today(today.plus(1, DateTimeUnit.DAY)).first())
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

package app.zerno.feature.plans.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.data.time.FakeClock
import app.zerno.entities.plan.PlanId
import app.zerno.feature.plans.api.PlanRunStatus
import app.zerno.feature.plans.data.BundledPlans
import app.zerno.feature.plans.data.PlanCatalog
import app.zerno.feature.plans.data.PlanJourneyStore
import app.zerno.feature.plans.data.PlansService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PlanDetailViewModelTest {
    private val rows = mutableMapOf<String, String>()
    private val dao = object : AppMetaDao {
        override suspend fun get(key: String) = rows[key]
        override suspend fun put(meta: AppMeta) {
            rows[meta.key] = meta.value
        }
    }
    private val plans = PlansService(
        catalogSource = PlanCatalog(BundledPlans(), remote = null, meta = dao),
        store = PlanJourneyStore(dao),
        clock = FakeClock(Instant.parse("2026-10-22T07:00:00Z")),
        zones = { TimeZone.UTC },
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(id: String) = PlanDetailViewModel(SavedStateHandle(), PlanDetailRoute(PlanId(id)), plans)

    // The bundled catalog is read from files; loading it up front keeps real IO out of the view model's virtual time.
    private suspend fun loadCatalog() = plans.catalog()

    @Test
    fun startingAPlanMakesItCurrentAndGoesToToday() = runTest {
        loadCatalog()
        val viewModel = viewModel("gospel-of-john")
        advanceUntilIdle()
        assertEquals(PlanRunStatus.NotStarted, viewModel.state.value.plan?.status)

        viewModel.sendEvent(PlanDetailEvent.StartClicked)
        advanceUntilIdle()

        assertEquals(PlanDetailEffect.OpenToday, viewModel.effects.first())
        assertEquals(PlanRunStatus.Active, viewModel.state.value.plan?.status)
    }

    @Test
    fun switchingFromAnotherPlanKeepsItsDays() = runTest {
        loadCatalog()
        plans.start(PlanId("first-steps"))
        plans.completeDay(PlanId("first-steps"), 1)
        val prayer = viewModel("prayer")
        advanceUntilIdle()
        assertTrue(prayer.state.value.otherActive)

        prayer.sendEvent(PlanDetailEvent.StartClicked)
        advanceUntilIdle()

        val firstSteps = viewModel("first-steps")
        advanceUntilIdle()
        assertEquals(PlanRunStatus.Paused, firstSteps.state.value.plan?.status)
        assertEquals(setOf(1), firstSteps.state.value.plan?.completedDays)
        assertFalse(prayer.state.value.otherActive)
    }

    @Test
    fun pausingTheCurrentPlan() = runTest {
        loadCatalog()
        plans.start(PlanId("prayer"))
        val viewModel = viewModel("prayer")
        advanceUntilIdle()

        viewModel.sendEvent(PlanDetailEvent.PauseClicked)
        advanceUntilIdle()

        assertEquals(PlanRunStatus.Paused, viewModel.state.value.plan?.status)
    }
}

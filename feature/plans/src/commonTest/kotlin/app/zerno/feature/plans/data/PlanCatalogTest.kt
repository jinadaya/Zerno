package app.zerno.feature.plans.data

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.data.net.ApiConfig
import app.zerno.data.net.zernoHttpClient
import app.zerno.data.time.FakeClock
import app.zerno.entities.plan.PlanId
import app.zerno.feature.plans.domain.planFile
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PlanCatalogTest {
    private val rows = mutableMapOf<String, String>()
    private val dao = object : AppMetaDao {
        override suspend fun get(key: String) = rows[key]
        override suspend fun put(meta: AppMeta) {
            rows[meta.key] = meta.value
        }
    }
    private val bundledIds = listOf("first-steps", "gospel-of-john", "prayer")

    /** What the backend answers next; every request is kept in [engine]'s history. */
    private var answer: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = {
        respondError(HttpStatusCode.ServiceUnavailable)
    }
    private val engine = MockEngine { request -> answer(request) }
    private val client = zernoHttpClient(ApiConfig("http://backend.test/", maxRetries = 0), engine = engine)
    private val catalog = PlanCatalog(BundledPlans(), RemotePlans(client), dao)

    private fun serve(version: String, vararg ids: String) {
        answer = { respond("""{"version": "$version", "plans": [${ids.joinToString { planFile(it) }}]}""") }
    }

    @Test
    fun theBundledPlansUntilTheBackendAnswers() = runTest {
        assertEquals(bundledIds, catalog.load().map { it.plan.id.value })
        assertIs<PlanCatalog.Update.Failed>(catalog.fetch())
        assertEquals(bundledIds, catalog.load().map { it.plan.id.value })
    }

    @Test
    fun aKeptCatalogIsLoadedNextTime() = runTest {
        serve("v1", "new", "first-steps")
        val update = assertIs<PlanCatalog.Update.Fresh>(catalog.fetch())
        assertEquals("http://backend.test/v1/plans", engine.requestHistory.single().url.toString())
        catalog.keep(update.version, update.entries)

        assertEquals(listOf("new", "first-steps"), catalog.load().map { it.plan.id.value })
    }

    @Test
    fun theKeptVersionIsSentAndAnUnchangedAnswerKeepsTheCatalog() = runTest {
        serve("v1", "new")
        val update = assertIs<PlanCatalog.Update.Fresh>(catalog.fetch())
        catalog.keep(update.version, update.entries)

        answer = { respond("", HttpStatusCode.NotModified) }
        assertIs<PlanCatalog.Update.Unchanged>(catalog.fetch())
        assertEquals("\"v1\"", engine.requestHistory.last().headers[HttpHeaders.IfNoneMatch])
        assertEquals(listOf("new"), catalog.load().map { it.plan.id.value })
    }

    @Test
    fun aCatalogWithNothingReadableIsIgnored() = runTest {
        answer = { respond("""{"version": "v2", "plans": [${planFile("odd", passage = "Hezekiah 1")}]}""") }
        assertIs<PlanCatalog.Update.Failed>(catalog.fetch())
    }

    @Test
    fun anUnreadableKeptCatalogFallsBackToTheBundledPlans() = runTest {
        rows["plans.catalog"] = "{not json"
        assertEquals(bundledIds, catalog.load().map { it.plan.id.value })
    }
}

class PlansRefreshTest {
    private val rows = mutableMapOf<String, String>()
    private val dao = object : AppMetaDao {
        override suspend fun get(key: String) = rows[key]
        override suspend fun put(meta: AppMeta) {
            rows[meta.key] = meta.value
        }
    }
    private var served = listOf("first-steps", "gospel-of-john", "prayer", "new")
    private val engine = MockEngine {
        respond("""{"version": "${served.joinToString("+")}", "plans": [${served.joinToString { planFile(it) }}]}""")
    }
    private val clock = FakeClock(Instant.parse("2026-10-22T07:00:00Z"))
    private val plans = PlansService(
        catalogSource = PlanCatalog(
            BundledPlans(),
            RemotePlans(zernoHttpClient(ApiConfig("http://backend.test/", maxRetries = 0), engine = engine)),
            dao,
        ),
        store = PlanJourneyStore(dao),
        clock = clock,
        zones = { TimeZone.UTC },
    )

    private suspend fun shown() = plans.overview.first().map { it.plan.id.value }

    @Test
    fun aNewPlanAppearsAfterARefresh() = runTest {
        assertEquals(listOf("first-steps", "gospel-of-john", "prayer"), shown())

        plans.refreshCatalog()

        assertEquals(listOf("first-steps", "gospel-of-john", "prayer", "new"), shown())
    }

    @Test
    fun aStartedPlanStaysWhenTheBackendDropsIt() = runTest {
        plans.start(PlanId("prayer"))
        served = listOf("new")

        plans.refreshCatalog()

        assertEquals(listOf("new", "prayer"), shown())
        assertEquals("prayer", plans.today(LocalDate(2026, 10, 22)).first()?.plan?.id?.value)
    }

    @Test
    fun theBackendIsAskedAtMostOnceAnHour() = runTest {
        plans.refreshCatalog()
        clock.advanceBy(30.minutes)
        plans.refreshCatalog()
        assertEquals(1, engine.requestHistory.size)

        clock.advanceBy(1.hours)
        plans.refreshCatalog()
        assertEquals(2, engine.requestHistory.size)
    }

    @Test
    fun nothingIsKeptBeforeTheBackendAnswers() = runTest {
        assertNull(rows["plans.catalog"])
    }
}

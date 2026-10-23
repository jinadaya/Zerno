package app.zerno.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import app.zerno.data.net.ApiConfig
import app.zerno.data.net.HealthApi
import app.zerno.data.time.OffsetClock
import app.zerno.data.time.TimeZoneProvider
import app.zerno.data.time.today
import app.zerno.designsystem.catalog.SjCatalog
import app.zerno.designsystem.component.SjLinkButton
import app.zerno.designsystem.foundation.sjHairline
import app.zerno.designsystem.foundation.sjMinTapTarget
import app.zerno.designsystem.theme.SjTheme
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.PassageRef
import app.zerno.feature.account.api.AccountRoute
import app.zerno.feature.gamification.api.AchievementsRoute
import app.zerno.feature.onboarding.api.OnboardingRoute
import app.zerno.feature.plans.api.PlansRoute
import app.zerno.feature.progress.api.JourneyRoute
import app.zerno.feature.reader.api.ReaderRoute
import app.zerno.feature.reflection.api.ReflectionRoute
import app.zerno.feature.settings.api.SettingsRoute
import app.zerno.feature.today.api.TodayRoute
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Developer menu: clock offset, seeded sessions, onboarding reset, every feature's entry screen and the design
 * system catalog. Debug builds only.
 */
@Serializable
data object DebugMenuRoute : NavKey

/** The design system catalog (DS-21). */
@Serializable
data object CatalogRoute : NavKey

/** Loaded by `initKoin()` only when [isDebugBuild]. */
val debugModule = module {
    // Replaces timeModule's Clock.System, so every rule sees the moved time.
    single { OffsetClock() }
    single<Clock> { get<OffsetClock>() }
    single { DebugActions(clock = get(), zones = get(), prefs = get(), events = get(), state = get()) }

    screen<DebugMenuRoute>(showBottomBar = false) { DebugMenuScreen() }
    screen<CatalogRoute>(showBottomBar = false) {
        val navigator = LocalNavigator.current
        Box(Modifier.fillMaxSize()) {
            SjCatalog()
            SjLinkButton(
                "Back",
                navigator::back,
                Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }
    }
}

private val destinations: List<Pair<String, NavKey>> = listOf(
    "Onboarding" to OnboardingRoute,
    "Today" to TodayRoute,
    "Reader" to ReaderRoute,
    "Plans" to PlansRoute,
    "Journey" to JourneyRoute,
    "Achievements" to AchievementsRoute,
    "Reflection" to ReflectionRoute(PassageRef.chapter(BookId(500), 3)),
    "Account" to AccountRoute,
    "Settings" to SettingsRoute,
    "Design system catalog" to CatalogRoute,
)

@Composable
private fun DebugMenuScreen() {
    val navigator = LocalNavigator.current
    val colors = SjTheme.colors
    val type = SjTheme.type
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SjTheme.spacing.gutter, vertical = SjTheme.spacing.space6),
        verticalArrangement = Arrangement.spacedBy(SjTheme.spacing.space2),
    ) {
        SjLinkButton("Back", navigator::back)
        BasicText("Debug", style = type.headline.copy(color = colors.ink))
        BackendHealthRow()
        DebugToolRows()
        destinations.forEach { (label, route) ->
            BasicText(
                label,
                style = type.bodyStrong.copy(color = colors.ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .sjMinTapTarget()
                    .clickable { navigator.navigate(route) }
                    .sjHairline(colors.rule)
                    .padding(vertical = SjTheme.spacing.space3),
            )
        }
    }
}

/** Pings the backend's `/health` on tap and shows the answer, to check the app reaches the local backend. */
@Composable
private fun BackendHealthRow(health: HealthApi = koinInject(), config: ApiConfig = koinInject()) {
    val colors = SjTheme.colors
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("tap to check") }
    BasicText(
        "Backend ${config.baseUrl}: $status",
        style = SjTheme.type.body.copy(color = colors.ink),
        modifier = Modifier
            .fillMaxWidth()
            .sjMinTapTarget()
            .clickable {
                status = "checking"
                scope.launch { status = if (health.isUp()) "up" else "down" }
            }
            .sjHairline(colors.rule)
            .padding(vertical = SjTheme.spacing.space3),
    )
}

/** Clock, seeding and onboarding tools; the last line shows what the last tap did. */
@Composable
private fun DebugToolRows(actions: DebugActions = koinInject(), zones: TimeZoneProvider = koinInject()) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf("") }
    var offset by remember { mutableStateOf(actions.clock.offset) }
    fun perform(label: String, action: suspend () -> Unit) {
        result = "$label…"
        scope.launch {
            result = runCatching { action() }.fold({ "$label: done" }, { "$label: ${it.message}" })
            offset = actions.clock.offset
        }
    }

    DebugRow("Clock ${offset.inWholeHours}h ahead, today ${actions.clock.today(zones)}") {}
    DebugRow("Clock +1 day") { perform("+1 day") { actions.moveClock(1.days) } }
    DebugRow("Clock +1 hour") { perform("+1 hour") { actions.moveClock(1.hours) } }
    DebugRow("Clock reset") { perform("Clock reset") { actions.resetClock() } }
    DebugRow("Seed 6-day streak ending yesterday") { perform("Seed streak") { actions.seedStreak(6) } }
    DebugRow("Seed a session 8 days ago") { perform("Seed session") { actions.seedSession(daysAgo = 8) } }
    DebugRow("Reset onboarding") { perform("Reset onboarding") { actions.resetOnboarding() } }
    if (result.isNotEmpty()) BasicText(result, style = SjTheme.type.body.copy(color = SjTheme.colors.ink))
}

@Composable
private fun DebugRow(label: String, onClick: () -> Unit) {
    val colors = SjTheme.colors
    BasicText(
        label,
        style = SjTheme.type.body.copy(color = colors.ink),
        modifier = Modifier
            .fillMaxWidth()
            .sjMinTapTarget()
            .clickable(onClick = onClick)
            .sjHairline(colors.rule)
            .padding(vertical = SjTheme.spacing.space3),
    )
}

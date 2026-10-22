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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import app.zerno.designsystem.catalog.SjCatalog
import app.zerno.designsystem.component.SjLinkButton
import app.zerno.designsystem.foundation.sjHairline
import app.zerno.designsystem.foundation.sjMinTapTarget
import app.zerno.designsystem.theme.SjTheme
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
import kotlinx.serialization.Serializable
import org.koin.dsl.module

/** Developer menu: every feature's entry screen and the design system catalog. Debug builds only. */
@Serializable
data object DebugMenuRoute : NavKey

/** The design system catalog (DS-21). */
@Serializable
data object CatalogRoute : NavKey

/** Loaded by `initKoin()` only when [isDebugBuild]. */
val debugModule = module {
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
    "Reflection" to ReflectionRoute,
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

package app.zerno

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import app.zerno.designsystem.icon.SjIcons
import app.zerno.feature.plans.api.PlansRoute
import app.zerno.feature.progress.api.JourneyRoute
import app.zerno.feature.reader.api.ReaderRoute
import app.zerno.feature.today.api.TodayRoute
import app.zerno.resources.Res
import app.zerno.resources.nav_bible
import app.zerno.resources.nav_journey
import app.zerno.resources.nav_plans
import app.zerno.resources.nav_today
import org.jetbrains.compose.resources.StringResource

/** One tab of the app shell: the root route of its back stack and how the bottom bar shows it. */
internal class AppTab(val route: NavKey, val label: StringResource, val icon: ImageVector)

/** The bottom bar's tabs in order; Today is the start tab. */
internal val AppTabs: List<AppTab> = listOf(
    AppTab(TodayRoute, Res.string.nav_today, SjIcons.Today),
    AppTab(ReaderRoute, Res.string.nav_bible, SjIcons.Bible),
    AppTab(PlansRoute, Res.string.nav_plans, SjIcons.Plans),
    AppTab(JourneyRoute, Res.string.nav_journey, SjIcons.Journey),
)

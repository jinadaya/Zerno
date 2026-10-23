package app.zerno.feature.plans.di

import app.zerno.data.db.user.UserDb
import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.plans.api.Plans
import app.zerno.feature.plans.api.PlansRoute
import app.zerno.feature.plans.data.BundledPlans
import app.zerno.feature.plans.data.PlanJourneyStore
import app.zerno.feature.plans.data.PlansService
import app.zerno.feature.plans.resources.Res
import app.zerno.feature.plans.resources.plans_placeholder
import app.zerno.feature.plans.resources.plans_title
import com.royaal.navigation.screen
import org.jetbrains.compose.resources.stringResource
import org.koin.dsl.module

/**
 * The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Needs `UserDb`, `Clock` and `TimeZoneProvider`.
 */
val plansModule = module {
    single { BundledPlans() }
    single { PlanJourneyStore(get<UserDb>().appMetaDao()) }
    single<Plans> { PlansService(get(), get(), get(), get()) }

    // Placeholder until the feature's first screen lands. It is the Plans tab's root, so it has no back link.
    screen<PlansRoute> {
        SjPlaceholderScreen(
            stringResource(Res.string.plans_title),
            stringResource(Res.string.plans_placeholder),
        )
    }
}

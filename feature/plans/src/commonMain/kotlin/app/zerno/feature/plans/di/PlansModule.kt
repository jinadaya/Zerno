package app.zerno.feature.plans.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.plans.api.PlansRoute
import app.zerno.feature.plans.data.BundledPlans
import app.zerno.feature.plans.resources.Res
import app.zerno.feature.plans.resources.plans_placeholder
import app.zerno.feature.plans.resources.plans_title
import com.royaal.navigation.screen
import org.jetbrains.compose.resources.stringResource
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val plansModule = module {
    single { BundledPlans() }

    // Placeholder until the feature's first screen lands. It is the Plans tab's root, so it has no back link.
    screen<PlansRoute> {
        SjPlaceholderScreen(
            stringResource(Res.string.plans_title),
            stringResource(Res.string.plans_placeholder),
        )
    }
}

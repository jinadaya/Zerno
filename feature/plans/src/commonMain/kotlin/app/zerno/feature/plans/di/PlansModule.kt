package app.zerno.feature.plans.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.plans.api.PlansRoute
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val plansModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<PlansRoute> {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen("Plans", "Reading plans and their progress.", onBack = navigator::back)
    }
}

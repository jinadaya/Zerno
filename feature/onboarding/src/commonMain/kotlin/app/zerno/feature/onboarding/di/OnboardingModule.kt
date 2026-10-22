package app.zerno.feature.onboarding.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.onboarding.api.OnboardingRoute
import app.zerno.feature.onboarding.resources.Res
import app.zerno.feature.onboarding.resources.onboarding_placeholder
import app.zerno.feature.onboarding.resources.onboarding_title
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.jetbrains.compose.resources.stringResource
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val onboardingModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<OnboardingRoute>(showBottomBar = false) {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen(
            stringResource(Res.string.onboarding_title),
            stringResource(Res.string.onboarding_placeholder),
            onBack = navigator::back,
        )
    }
}

package app.zerno.feature.onboarding.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.onboarding.api.OnboardingRoute
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val onboardingModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<OnboardingRoute>(showBottomBar = false) {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen("Onboarding", "Welcome, language, translation and reminder.", onBack = navigator::back)
    }
}

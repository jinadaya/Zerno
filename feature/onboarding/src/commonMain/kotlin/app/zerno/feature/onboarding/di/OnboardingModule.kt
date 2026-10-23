package app.zerno.feature.onboarding.di

import app.zerno.feature.onboarding.api.OnboardingRoute
import app.zerno.feature.onboarding.ui.OnboardingScreen
import app.zerno.feature.onboarding.ui.OnboardingViewModel
import com.royaal.navigation.screen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Needs `Plans`, `ReaderLauncher` and
 * `AppPreferences`.
 */
val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)

    // The app shell opens it over Today until onboarding is done; the debug menu can open it again.
    screen<OnboardingRoute>(showBottomBar = false) { OnboardingScreen(koinViewModel()) }
}

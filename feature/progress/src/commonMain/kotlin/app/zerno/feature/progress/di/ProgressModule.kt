package app.zerno.feature.progress.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.progress.api.JourneyRoute
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val progressModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<JourneyRoute> {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen("Journey", "Sessions, chapters read, streaks and saved verses.", onBack = navigator::back)
    }
}

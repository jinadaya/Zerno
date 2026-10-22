package app.zerno.feature.settings.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.settings.api.SettingsRoute
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val settingsModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<SettingsRoute>(showBottomBar = false) {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen("Settings", "Language, text size and reminders.", onBack = navigator::back)
    }
}

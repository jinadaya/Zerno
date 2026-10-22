package app.zerno.feature.settings.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.settings.api.SettingsRoute
import app.zerno.feature.settings.resources.Res
import app.zerno.feature.settings.resources.settings_placeholder
import app.zerno.feature.settings.resources.settings_title
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.jetbrains.compose.resources.stringResource
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val settingsModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<SettingsRoute>(showBottomBar = false) {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen(
            stringResource(Res.string.settings_title),
            stringResource(Res.string.settings_placeholder),
            onBack = navigator::back,
        )
    }
}

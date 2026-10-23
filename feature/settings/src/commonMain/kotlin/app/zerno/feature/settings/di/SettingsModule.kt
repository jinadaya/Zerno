package app.zerno.feature.settings.di

import app.zerno.feature.settings.api.AppLanguage
import app.zerno.feature.settings.api.SettingsRoute
import app.zerno.feature.settings.data.InstalledTranslations
import app.zerno.feature.settings.data.PrefsAppLanguage
import app.zerno.feature.settings.data.StoreInstalledTranslations
import app.zerno.feature.settings.ui.SettingsScreen
import app.zerno.feature.settings.ui.SettingsViewModel
import com.royaal.navigation.screen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Needs `AppPreferences` and `TranslationStore`
 * from the app.
 */
val settingsModule = module {
    singleOf(::PrefsAppLanguage) { bind<AppLanguage>() }

    single<InstalledTranslations> { StoreInstalledTranslations(get()) }
    viewModelOf(::SettingsViewModel)

    screen<SettingsRoute>(showBottomBar = false) { SettingsScreen(koinViewModel()) }
}

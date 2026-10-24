package app.zerno.feature.settings.di

import app.zerno.feature.settings.api.AppLanguage
import app.zerno.feature.settings.api.SettingsRoute
import app.zerno.feature.settings.data.DownloadsTranslationLibrary
import app.zerno.feature.settings.data.InstalledTranslations
import app.zerno.feature.settings.data.PrefsAppLanguage
import app.zerno.feature.settings.data.StoreInstalledTranslations
import app.zerno.feature.settings.data.TranslationLibrary
import app.zerno.feature.settings.reminder.ReminderCoordinator
import app.zerno.feature.settings.reminder.platformReminderModule
import app.zerno.feature.settings.ui.SettingsScreen
import app.zerno.feature.settings.ui.SettingsViewModel
import com.royaal.navigation.screen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Needs `AppPreferences`, `TranslationStore`,
 * `TranslationDownloads`, the clock and time zones from the app, and `GamificationState`.
 */
val settingsModule = module {
    includes(platformReminderModule)
    singleOf(::PrefsAppLanguage) { bind<AppLanguage>() }

    single<InstalledTranslations> { StoreInstalledTranslations(get()) }
    single<TranslationLibrary> { DownloadsTranslationLibrary(get()) }
    viewModelOf(::SettingsViewModel)

    // Started by the app root, which has the platform context the schedulers need.
    single { ReminderCoordinator(get(), get(), get(), get(), get()) }

    screen<SettingsRoute>(showBottomBar = false) { SettingsScreen(koinViewModel()) }
}

package app.zerno.feature.settings.di

import app.zerno.feature.settings.api.AppLanguage
import app.zerno.feature.settings.api.SettingsRoute
import app.zerno.feature.settings.data.InstalledTranslations
import app.zerno.feature.settings.data.PrefsAppLanguage
import app.zerno.feature.settings.data.StoreInstalledTranslations
import app.zerno.feature.settings.reminder.ReminderCoordinator
import app.zerno.feature.settings.reminder.platformReminderModule
import app.zerno.feature.settings.ui.SettingsScreen
import app.zerno.feature.settings.ui.SettingsViewModel
import com.royaal.navigation.screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Needs `AppPreferences`, `TranslationStore`, the
 * clock and time zones from the app, and `GamificationState`.
 */
val settingsModule = module {
    includes(platformReminderModule)
    singleOf(::PrefsAppLanguage) { bind<AppLanguage>() }

    single<InstalledTranslations> { StoreInstalledTranslations(get()) }
    viewModelOf(::SettingsViewModel)

    // Started with the app, so reminders follow the settings and the day's reading from the first launch.
    single(createdAtStart = true) {
        ReminderCoordinator(get(), get(), get(), get(), get()).apply {
            start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        }
    }

    screen<SettingsRoute>(showBottomBar = false) { SettingsScreen(koinViewModel()) }
}

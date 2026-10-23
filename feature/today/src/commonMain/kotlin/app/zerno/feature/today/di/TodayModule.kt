package app.zerno.feature.today.di

import app.zerno.feature.today.api.TodayRoute
import app.zerno.feature.today.ui.TodayScreen
import app.zerno.feature.today.ui.TodayViewModel
import com.royaal.navigation.screen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Needs `Plans`, `ReaderLauncher` and
 * `GamificationState`.
 */
val todayModule = module {
    viewModelOf(::TodayViewModel)

    // The Today tab's root, so it has no back link.
    screen<TodayRoute> { TodayScreen(koinViewModel()) }
}

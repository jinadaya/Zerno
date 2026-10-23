package app.zerno.feature.progress.di

import app.zerno.feature.progress.api.JourneyRoute
import app.zerno.feature.progress.ui.JourneyScreen
import app.zerno.feature.progress.ui.JourneyViewModel
import com.royaal.navigation.screen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Needs `GamificationState` and `Plans`. */
val progressModule = module {
    viewModelOf(::JourneyViewModel)

    // The Journey tab's root, so it has no back link.
    screen<JourneyRoute> { JourneyScreen(koinViewModel()) }
}

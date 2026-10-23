package app.zerno.feature.progress.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.progress.api.JourneyRoute
import app.zerno.feature.progress.resources.Res
import app.zerno.feature.progress.resources.progress_placeholder
import app.zerno.feature.progress.resources.progress_title
import com.royaal.navigation.screen
import org.jetbrains.compose.resources.stringResource
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val progressModule = module {
    // Placeholder until the feature's first screen lands. It is the Journey tab's root, so it has no back link.
    screen<JourneyRoute> {
        SjPlaceholderScreen(
            stringResource(Res.string.progress_title),
            stringResource(Res.string.progress_placeholder),
        )
    }
}

package app.zerno.feature.today.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.today.api.TodayRoute
import app.zerno.feature.today.resources.Res
import app.zerno.feature.today.resources.today_placeholder
import app.zerno.feature.today.resources.today_title
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.jetbrains.compose.resources.stringResource
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val todayModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<TodayRoute> {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen(
            stringResource(Res.string.today_title),
            stringResource(Res.string.today_placeholder),
            onBack = navigator::back,
        )
    }
}

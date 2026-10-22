package app.zerno.feature.today.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.today.api.TodayRoute
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val todayModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<TodayRoute> {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen("Today", "The day's reading card and streak week.", onBack = navigator::back)
    }
}

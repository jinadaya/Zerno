package app.zerno.feature.reflection.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.reflection.api.ReflectionRoute
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val reflectionModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<ReflectionRoute>(showBottomBar = false) {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen("Reflection", "A few lines after a reading session.", onBack = navigator::back)
    }
}

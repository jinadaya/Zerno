package app.zerno.feature.reflection.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.reflection.api.ReflectionRoute
import app.zerno.feature.reflection.resources.Res
import app.zerno.feature.reflection.resources.reflection_placeholder
import app.zerno.feature.reflection.resources.reflection_title
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.jetbrains.compose.resources.stringResource
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val reflectionModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<ReflectionRoute>(showBottomBar = false) {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen(
            stringResource(Res.string.reflection_title),
            stringResource(Res.string.reflection_placeholder),
            onBack = navigator::back,
        )
    }
}

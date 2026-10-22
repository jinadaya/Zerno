package app.zerno.feature.gamification.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.gamification.api.AchievementsRoute
import app.zerno.feature.gamification.resources.Res
import app.zerno.feature.gamification.resources.gamification_placeholder
import app.zerno.feature.gamification.resources.gamification_title
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.jetbrains.compose.resources.stringResource
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val gamificationModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<AchievementsRoute>(showBottomBar = false) {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen(
            stringResource(Res.string.gamification_title),
            stringResource(Res.string.gamification_placeholder),
            onBack = navigator::back,
        )
    }
}

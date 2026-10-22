package app.zerno.feature.account.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.account.api.AccountRoute
import app.zerno.feature.account.resources.Res
import app.zerno.feature.account.resources.account_placeholder
import app.zerno.feature.account.resources.account_title
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.jetbrains.compose.resources.stringResource
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val accountModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<AccountRoute>(showBottomBar = false) {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen(
            stringResource(Res.string.account_title),
            stringResource(Res.string.account_placeholder),
            onBack = navigator::back,
        )
    }
}

package app.zerno.feature.account.di

import app.zerno.designsystem.component.SjPlaceholderScreen
import app.zerno.feature.account.api.AccountRoute
import com.royaal.navigation.LocalNavigator
import com.royaal.navigation.screen
import org.koin.dsl.module

/** The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Bindings land here as the feature grows. */
val accountModule = module {
    // Placeholder until the feature's first screen lands; reached from the debug menu.
    screen<AccountRoute>(showBottomBar = false) {
        val navigator = LocalNavigator.current
        SjPlaceholderScreen("Account", "Sign-in and sync.", onBack = navigator::back)
    }
}

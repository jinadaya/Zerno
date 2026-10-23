package app.zerno.feature.reflection.di

import app.zerno.data.db.user.UserDb
import app.zerno.feature.reflection.api.ReflectionRoute
import app.zerno.feature.reflection.api.Reflections
import app.zerno.feature.reflection.data.ReflectionStore
import app.zerno.feature.reflection.data.ReflectionsService
import app.zerno.feature.reflection.ui.ReflectionScreen
import app.zerno.feature.reflection.ui.ReflectionViewModel
import com.royaal.navigation.screen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Needs `UserDb`, `GamificationEvents` and
 * `Clock`.
 */
val reflectionModule = module {
    single { ReflectionStore(get<UserDb>().appMetaDao()) }
    single<Reflections> { ReflectionsService(get(), get(), get()) }
    viewModelOf(::ReflectionViewModel)

    // Opened after a reading session; the debug menu opens it on a sample passage.
    screen<ReflectionRoute>(showBottomBar = false) { route ->
        ReflectionScreen(route.passage, koinViewModel(key = route.toString()) { parametersOf(route) })
    }
}

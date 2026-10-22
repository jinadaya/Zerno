package com.royaal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.serializer
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.navigation3.navigation

/**
 * Registers a screen for [T] - the single call a feature needs to become navigable:
 *
 * ```kotlin
 * @Serializable
 * data class GameRoute(val id: Long) : NavKey
 *
 * val gameModule = module {
 *     viewModelOf(::GameViewModel)
 *     screen<GameRoute> { route -> GameScreen(id = route.id, viewModel = koinViewModel()) }
 * }
 * ```
 *
 * Besides the NavDisplay entry it also registers [T]'s serializer, so the back stack survives
 * process death / configuration changes on every platform (iOS has no reflection to find it).
 * [T] must therefore be `@Serializable`.
 *
 * @param showBottomBar `false` for full-screen destinations (details, flows): the bottom bar
 *   hides while this screen is on top.
 */
@OptIn(KoinExperimentalAPI::class)
inline fun <reified T : NavKey> Module.screen(
    showBottomBar: Boolean = true,
    metadata: Map<String, Any> = emptyMap(),
    noinline content: @Composable Scope.(T) -> Unit,
) {
    navigation<T>(metadata + (SHOW_BOTTOM_BAR_KEY to showBottomBar), content)
    single(named<T>()) {
        NavKeySerializerRegistration { subclass(T::class, serializer<T>()) }
    }
}

@PublishedApi
internal const val SHOW_BOTTOM_BAR_KEY = "com.royaal.navigation.showBottomBar"

/** Collected by [NavHost] to build the polymorphic `NavKey` serializers module for the back stack. */
class NavKeySerializerRegistration(val register: PolymorphicModuleBuilder<NavKey>.() -> Unit)

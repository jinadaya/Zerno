package com.royaal.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.getKoin
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

/**
 * Frame around the screens: draws app-level chrome (bottom bar, snackbars) and must call
 * `content` exactly once. Screens are laid out full-size, so chrome floats over them.
 * `showBottomBar` is `false` while the top screen is registered with `screen<T>(showBottomBar = false)`.
 * Called inside [LocalNavigator], so the bar can read [Navigator.currentTab] and call [Navigator.selectTab].
 */
typealias NavScaffold = @Composable (showBottomBar: Boolean, content: @Composable () -> Unit) -> Unit

/** Single-stack host: no tabs, no bottom bar. */
@Composable
fun NavHost(startRoute: NavKey, modifier: Modifier = Modifier) {
    NavHost(tabs = listOf(startRoute), modifier = modifier)
}

/**
 * App-wide navigation host. Renders every screen registered via [screen] in any loaded Koin
 * module - nothing here needs to change when a feature is added.
 *
 * Each of [tabs] (root routes, the first one is the start tab) gets its own back stack, kept -
 * along with its screens' state and ViewModels - while another tab is shown. [scaffold] wraps the
 * screens with app chrome, see [NavScaffold]. [tabs] must not change while the host is shown:
 * stacks are saved by position.
 *
 * Every entry gets its own ViewModelStore (so `koinViewModel()` is scoped to the screen and
 * cleared when it's popped) and its own saveable state.
 */
@OptIn(KoinExperimentalAPI::class)
@Composable
fun NavHost(
    tabs: List<NavKey>,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    scaffold: NavScaffold = { _, content -> content() },
) {
    require(tabs.isNotEmpty()) { "NavHost needs at least one tab" }

    val savedStateConfiguration = rememberNavKeySavedStateConfiguration()
    val currentTabIndex = rememberSaveable { mutableIntStateOf(0) }
    val backStacks = tabs.mapIndexed { index, tab ->
        key(index) { rememberNavBackStack(savedStateConfiguration, tab) }
    }
    val navigator = remember(tabs, backStacks) { TabsNavigator(tabs, backStacks, currentTabIndex) }

    // Shared by all tabs: entry content keys are made unique per tab below.
    val entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        rememberViewModelStoreNavEntryDecorator<NavKey>(),
    )
    val entryProvider = koinEntryProvider<NavKey>()
    // Decorated for every tab, not only the visible ones - that's what keeps hidden tabs' state alive.
    val entriesByTab = backStacks.mapIndexed { index, backStack ->
        key(index) {
            rememberDecoratedNavEntries(backStack, entryDecorators) { navKey ->
                entryProvider(navKey).withTabContentKey(navKey, index)
            }
        }
    }

    val entries = navigator.visibleTabIndices.flatMap { entriesByTab[it] }
    val showBottomBar = entries.last().metadata[SHOW_BOTTOM_BAR_KEY] != false

    CompositionLocalProvider(LocalNavigator provides navigator) {
        scaffold(showBottomBar) {
            NavDisplay(
                entries = entries,
                modifier = modifier,
                onBack = navigator::back,
                sharedTransitionScope = sharedTransitionScope,
                // Switching tabs slides in the bottom bar's order; within a tab screens zoom in and out.
                transitionSpec = { tabSwitchTransform() ?: pushTransform() },
                popTransitionSpec = { tabSwitchTransform() ?: popTransform() },
                predictivePopTransitionSpec = { tabSwitchTransform() ?: popTransform() },
            )
        }
    }
}

private const val TAB_SWITCH_DURATION_MILLIS = 300
private const val ZOOM_DURATION_MILLIS = 300

/** Scale of the screen underneath: slightly smaller, as if further away. */
private const val ZOOM_BACK_SCALE = 0.92f

/** Scale of the screen on top while it's not fully there: slightly larger, as if closer. */
private const val ZOOM_FRONT_SCALE = 1.08f

/**
 * A pushed screen comes from above: it fades in shrinking down to its size, while the one it
 * covers fades out shrinking, as if moving back.
 */
private fun pushTransform(): ContentTransform =
    zoomIn(initialScale = ZOOM_FRONT_SCALE) togetherWith zoomOut(targetScale = ZOOM_BACK_SCALE)

/** The reverse of [pushTransform]: the popped screen rises away, the one below comes forward. */
private fun popTransform(): ContentTransform =
    zoomIn(initialScale = ZOOM_BACK_SCALE) togetherWith zoomOut(targetScale = ZOOM_FRONT_SCALE)

private fun zoomIn(initialScale: Float): EnterTransition =
    fadeIn(tween(ZOOM_DURATION_MILLIS, easing = FastOutSlowInEasing)) +
        scaleIn(tween(ZOOM_DURATION_MILLIS, easing = FastOutSlowInEasing), initialScale = initialScale)

private fun zoomOut(targetScale: Float): ExitTransition =
    fadeOut(tween(ZOOM_DURATION_MILLIS, easing = FastOutSlowInEasing)) +
        scaleOut(tween(ZOOM_DURATION_MILLIS, easing = FastOutSlowInEasing), targetScale = targetScale)

/**
 * Slide between tabs: a tab further along the bottom bar comes in from its end side (right in
 * LTR), an earlier one from the start side. `null` if both scenes belong to the same tab.
 */
private fun AnimatedContentTransitionScope<Scene<NavKey>>.tabSwitchTransform(): ContentTransform? {
    val fromTab = initialState.tabIndex ?: return null
    val toTab = targetState.tabIndex ?: return null
    if (fromTab == toTab) return null

    val towards = if (toTab > fromTab) SlideDirection.Start else SlideDirection.End
    val animationSpec = tween<IntOffset>(
        durationMillis = TAB_SWITCH_DURATION_MILLIS,
        easing = FastOutSlowInEasing,
    )
    return slideIntoContainer(towards, animationSpec) togetherWith slideOutOfContainer(towards, animationSpec)
}

/** Tab of the scene's top entry, see [withTabContentKey]. */
private val Scene<NavKey>.tabIndex: Int?
    get() = entries.lastOrNull()?.metadata?.get(TAB_INDEX_KEY) as? Int

private const val TAB_INDEX_KEY = "com.royaal.navigation.tabIndex"

/** Polymorphic `NavKey` serializers collected from every [screen] registration. */
@Composable
private fun rememberNavKeySavedStateConfiguration(): SavedStateConfiguration {
    val koin = getKoin()
    return remember(koin) {
        val registrations = koin.getAll<NavKeySerializerRegistration>()
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    registrations.forEach { it.register(this) }
                }
            }
        }
    }
}

/**
 * Saved state and ViewModels are keyed by content key, which defaults to `key.toString()`. The
 * same route open in two tabs would then share them - and popping it in one tab would clear the
 * other's ViewModel. Prefixing with the tab index keeps them apart. The index also goes to the
 * metadata, for the tab switch animation.
 */
private fun NavEntry<NavKey>.withTabContentKey(key: NavKey, tabIndex: Int): NavEntry<NavKey> {
    val entry = this
    return NavEntry(
        key = key,
        contentKey = "$tabIndex/${entry.contentKey}",
        metadata = entry.metadata + (TAB_INDEX_KEY to tabIndex),
    ) { entry.Content() }
}

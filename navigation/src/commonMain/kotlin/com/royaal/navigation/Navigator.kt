package com.royaal.navigation

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/**
 * The only way screens move around the app. Obtain it inside a screen via [LocalNavigator];
 * ViewModels shouldn't hold it - emit a side effect and let the screen call the navigator.
 *
 * Every tab (see [NavHost]) has its own back stack; all operations apply to the [currentTab]'s one.
 */
@Stable
interface Navigator {

    /** Root route of the tab that is on screen now. Observable - reading it in composition recomposes on change. */
    val currentTab: NavKey

    /**
     * Pushes [key] on top of the current tab's back stack. If [key] is a tab root, switches to
     * that tab instead (same as [selectTab]).
     */
    fun navigate(key: NavKey)

    /**
     * Pops the current tab's top screen. On a tab root returns to the start tab; on the start
     * tab's root it's a no-op (the system back then closes the app).
     */
    fun back()

    /** Switches to [tab], keeping every tab's back stack. Re-selecting the current tab pops it to its root. */
    fun selectTab(tab: NavKey)

    /** Pops the current tab's back stack down to its root. */
    fun popToRoot()
}

val LocalNavigator: ProvidableCompositionLocal<Navigator> = staticCompositionLocalOf {
    error("No Navigator provided - is this composable called outside of NavHost?")
}

/** [Navigator] over one back stack per tab. The first tab is the start one. */
internal class TabsNavigator(
    private val tabs: List<NavKey>,
    private val backStacks: List<MutableList<NavKey>>,
    private val currentTabIndex: MutableIntState,
) : Navigator {

    override val currentTab: NavKey get() = tabs[currentTabIndex.intValue]

    private val currentStack: MutableList<NavKey> get() = backStacks[currentTabIndex.intValue]

    /**
     * Tabs whose stacks are passed to NavDisplay, bottom to top. The start tab always stays
     * underneath, so back (and the predictive back preview) from another tab's root leads to it.
     */
    val visibleTabIndices: List<Int>
        get() = currentTabIndex.intValue.let { if (it == START_TAB) listOf(START_TAB) else listOf(START_TAB, it) }

    override fun navigate(key: NavKey) {
        if (key in tabs) selectTab(key) else currentStack.add(key)
    }

    override fun back() {
        when {
            currentStack.size > 1 -> currentStack.removeAt(currentStack.lastIndex)
            currentTabIndex.intValue != START_TAB -> currentTabIndex.intValue = START_TAB
        }
    }

    override fun selectTab(tab: NavKey) {
        val index = tabs.indexOf(tab)
        require(index >= 0) { "$tab is not a tab root. Tabs: $tabs" }
        if (index == currentTabIndex.intValue) popToRoot() else currentTabIndex.intValue = index
    }

    override fun popToRoot() {
        currentStack.subList(1, currentStack.size).clear()
    }

    private companion object {
        const val START_TAB = 0
    }
}

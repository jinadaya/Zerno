package com.royaal.navigation

import androidx.compose.runtime.mutableIntStateOf
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TabsNavigatorTest {
    private data object Today : NavKey

    private data object Bible : NavKey

    private data object Plans : NavKey

    private data class Detail(val id: Int) : NavKey

    private val tabs = listOf(Today, Bible, Plans)
    private val stacks: List<MutableList<NavKey>> = tabs.map { mutableListOf(it) }
    private val navigator = TabsNavigator(tabs, stacks, mutableIntStateOf(0))

    @Test
    fun startsOnTheFirstTab() {
        assertEquals(Today, navigator.currentTab)
        assertEquals(listOf(0), navigator.visibleTabIndices)
    }

    @Test
    fun switchingTabsKeepsEachBackStack() {
        navigator.navigate(Detail(1))
        navigator.selectTab(Plans)
        navigator.navigate(Detail(2))
        navigator.navigate(Detail(3))

        navigator.selectTab(Today)
        assertEquals(listOf(Today, Detail(1)), stacks[0])

        navigator.selectTab(Plans)
        assertEquals(listOf(Plans, Detail(2), Detail(3)), stacks[2])
        assertEquals(Plans, navigator.currentTab)
    }

    @Test
    fun navigatingToATabRootSwitchesTabs() {
        navigator.navigate(Bible)

        assertEquals(Bible, navigator.currentTab)
        assertEquals(listOf(Bible), stacks[1])
        assertEquals(listOf(0, 1), navigator.visibleTabIndices)
    }

    @Test
    fun reselectingTheCurrentTabPopsToItsRoot() {
        navigator.selectTab(Plans)
        navigator.navigate(Detail(1))
        navigator.navigate(Detail(2))

        navigator.selectTab(Plans)

        assertEquals(listOf(Plans), stacks[2])
    }

    @Test
    fun backPopsThenReturnsToTheStartTab() {
        navigator.selectTab(Plans)
        navigator.navigate(Detail(1))

        navigator.back()
        assertEquals(listOf(Plans), stacks[2])
        assertEquals(Plans, navigator.currentTab)

        navigator.back()
        assertEquals(Today, navigator.currentTab)

        navigator.back()
        assertEquals(Today, navigator.currentTab)
        assertEquals(listOf(Today), stacks[0])
    }

    @Test
    fun selectingAnUnknownRouteFails() {
        assertFailsWith<IllegalArgumentException> { navigator.selectTab(Detail(1)) }
    }
}

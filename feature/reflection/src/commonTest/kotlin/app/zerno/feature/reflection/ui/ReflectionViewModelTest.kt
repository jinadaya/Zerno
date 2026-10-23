package app.zerno.feature.reflection.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.entities.saved.ReflectionId
import app.zerno.entities.session.SessionId
import app.zerno.feature.reflection.api.ReflectionRoute
import app.zerno.feature.reflection.data.John3
import app.zerno.feature.reflection.data.Psalm23
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReflectionViewModelTest {
    private val reflections = FakeReflections()
    private val session = SessionId("s1")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(route: ReflectionRoute = ReflectionRoute(John3, session, afterPlanStep = true)) =
        ReflectionViewModel(SavedStateHandle(), route, reflections)

    @Test
    fun savingStoresTheReflectionForTheSessionAndCloses() = runTest {
        val viewModel = viewModel()
        viewModel.sendEvent(ReflectionEvent.SaveClicked("Loved"))
        advanceUntilIdle()

        assertEquals(ReflectionEffect.Close, viewModel.effects.first())
        assertEquals(listOf(Saved(John3, "Loved", session, true)), reflections.saves)
    }

    @Test
    fun theSessionsReflectionFillsTheFieldAndIsNotListedAsEarlier() = runTest {
        reflections.add(saved("r1", John3, "Before", session))
        reflections.add(saved("r2", Psalm23, "Shepherd", null))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("Before", viewModel.state.value.existing)
        assertEquals(listOf("Shepherd"), viewModel.state.value.earlier.map { it.reflection.text })
    }

    @Test
    fun withoutASessionEverythingIsEarlier() = runTest {
        reflections.add(saved("r1", John3, "Before", session))
        val viewModel = viewModel(ReflectionRoute(Psalm23))
        advanceUntilIdle()

        assertNull(viewModel.state.value.existing)
        assertEquals(1, viewModel.state.value.earlier.size)
    }

    @Test
    fun blankTextIsNotSaved() = runTest {
        val viewModel = viewModel()
        viewModel.sendEvent(ReflectionEvent.SaveClicked("  "))
        advanceUntilIdle()

        assertTrue(reflections.saves.isEmpty())
    }

    @Test
    fun aFailedSaveKeepsTheScreenWithAMessage() = runTest {
        reflections.fail = true
        val viewModel = viewModel()
        viewModel.sendEvent(ReflectionEvent.SaveClicked("Loved"))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.failed)
        assertEquals(false, viewModel.state.value.saving)
    }

    @Test
    fun skippingCloses() = runTest {
        val viewModel = viewModel()
        viewModel.sendEvent(ReflectionEvent.SkipClicked)
        advanceUntilIdle()

        assertEquals(ReflectionEffect.Close, viewModel.effects.first())
    }

    @Test
    fun aReflectionFromSavedIsLoadedAndEditedInPlace() = runTest {
        reflections.add(saved("r1", John3, "Before", session))
        reflections.add(saved("r2", Psalm23, "Shepherd", null))
        val viewModel = viewModel(ReflectionRoute(Psalm23, reflectionId = ReflectionId("r2")))
        advanceUntilIdle()
        assertEquals("Shepherd", viewModel.state.value.existing)
        assertEquals(listOf("Before"), viewModel.state.value.earlier.map { it.reflection.text })

        viewModel.sendEvent(ReflectionEvent.SaveClicked("Still waters"))
        advanceUntilIdle()

        assertEquals(ReflectionEffect.Close, viewModel.effects.first())
        assertTrue(reflections.saves.isEmpty())
        assertEquals("Still waters", reflections.get(ReflectionId("r2"))?.reflection?.text)
    }
}

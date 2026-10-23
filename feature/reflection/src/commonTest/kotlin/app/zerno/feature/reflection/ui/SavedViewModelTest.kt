package app.zerno.feature.reflection.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.plan.PlanId
import app.zerno.entities.saved.Bookmark
import app.zerno.entities.saved.BookmarkId
import app.zerno.entities.saved.ReflectionId
import app.zerno.feature.reader.api.PassageProgress
import app.zerno.feature.reader.api.ReaderLauncher
import app.zerno.feature.reflection.api.Bookmarks
import app.zerno.feature.reflection.api.ReflectionRoute
import app.zerno.feature.reflection.data.John3
import app.zerno.feature.reflection.data.Psalm23
import app.zerno.feature.reflection.domain.SavedFilter
import app.zerno.feature.reflection.domain.SavedSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {
    private val john316 = PassageRef.verse(BookId(500), 3, 16)
    private val bookmarks = FakeBookmarks()
    private val reflections = FakeReflections()
    private val reader = FakeReader()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        bookmarks.all.value = listOf(
            Bookmark(BookmarkId("b1"), john316, TranslationId("kjv"), Instant.parse("2026-10-22T09:00:00Z")),
        )
        reflections.add(saved("r2", Psalm23, "Shepherd", at = "2026-10-21T09:00:00Z"))
        reflections.add(saved("r1", John3, "Loved", at = "2026-10-19T09:00:00Z"))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SavedViewModel(SavedStateHandle(), bookmarks, reflections, reader)

    @Test
    fun listsBothKindsAndFiltersAndSorts() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(listOf("verse:b1", "note:r2", "note:r1"), viewModel.state.value.items?.map { it.key })

        viewModel.sendEvent(SavedEvent.SortChosen(SavedSort.Book))
        advanceUntilIdle()
        assertEquals(listOf("note:r2", "note:r1", "verse:b1"), viewModel.state.value.items?.map { it.key })

        viewModel.sendEvent(SavedEvent.FilterChosen(SavedFilter.Notes))
        advanceUntilIdle()
        assertEquals(listOf("note:r2", "note:r1"), viewModel.state.value.items?.map { it.key })
    }

    @Test
    fun aVerseOpensInTheReader() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.sendEvent(SavedEvent.ItemClicked("verse:b1"))
        advanceUntilIdle()

        assertEquals(SavedEffect.OpenReader, viewModel.effects.first())
        assertEquals(listOf(john316), reader.opened)
    }

    @Test
    fun aReflectionOpensForEditing() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.sendEvent(SavedEvent.ItemClicked("note:r2"))
        advanceUntilIdle()

        val expected = SavedEffect.EditReflection(ReflectionRoute(Psalm23, reflectionId = ReflectionId("r2")))
        assertEquals(expected, viewModel.effects.first())
    }

    @Test
    fun deletingAsksFirstAndCanBeCancelled() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.sendEvent(SavedEvent.DeleteClicked("note:r1"))
        advanceUntilIdle()
        assertEquals("note:r1", viewModel.state.value.confirming)

        viewModel.sendEvent(SavedEvent.DeleteCancelled)
        advanceUntilIdle()
        assertNull(viewModel.state.value.confirming)
        assertEquals(3, viewModel.state.value.items?.size)

        viewModel.sendEvent(SavedEvent.DeleteClicked("verse:b1"))
        viewModel.sendEvent(SavedEvent.DeleteConfirmed)
        advanceUntilIdle()
        assertNull(viewModel.state.value.confirming)
        assertEquals(listOf("note:r2", "note:r1"), viewModel.state.value.items?.map { it.key })
    }

    private class FakeBookmarks : Bookmarks {
        override val all = MutableStateFlow<List<Bookmark>>(emptyList())

        override suspend fun save(passage: PassageRef, translation: TranslationId): Bookmark = error("Not used")

        override suspend fun delete(id: BookmarkId) {
            all.value = all.value.filterNot { it.id == id }
        }
    }

    private class FakeReader : ReaderLauncher {
        val opened = mutableListOf<PassageRef>()

        override suspend fun openAt(passage: PassageRef, resume: Boolean) {
            opened += passage
        }

        override suspend fun openPlanDay(planId: PlanId, day: Int, passage: PassageRef, resume: Boolean) = Unit

        override suspend fun progressIn(passage: PassageRef): PassageProgress? = null
    }
}

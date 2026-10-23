package app.zerno.feature.settings.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.data.db.bible.InstalledTranslation
import app.zerno.data.prefs.AppPreferences
import app.zerno.data.prefs.AppTheme
import app.zerno.data.prefs.PageTheme
import app.zerno.data.prefs.ScriptureSize
import app.zerno.data.prefs.UiLanguage
import app.zerno.entities.bible.TranslationId
import app.zerno.feature.settings.data.InstalledTranslations
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val store = MapSettings()
    private val prefs = AppPreferences(store)
    private val kjv =
        InstalledTranslation(TranslationId("kjv"), "en", "King James Version", bundled = true, path = "kjv")
    private val rst =
        InstalledTranslation(TranslationId("rst"), "ru", "Синодальный перевод", bundled = true, path = "rst")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(list: List<InstalledTranslation> = listOf(rst, kjv)) =
        SettingsViewModel(SavedStateHandle(), prefs, Listed { list })

    @Test
    fun showsTheSavedSettingsAndInstalledTranslations() = runTest {
        prefs.setTranslation(TranslationId("kjv"))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(listOf(rst, kjv), viewModel.state.value.translations)
        assertEquals(TranslationId("kjv"), viewModel.state.value.prefs.translation)
    }

    @Test
    fun everyChoiceIsSavedAndShownAtOnce() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.sendEvent(SettingsEvent.TranslationChosen(TranslationId("rst")))
        viewModel.sendEvent(SettingsEvent.PageThemeChosen(PageTheme.Sepia))
        viewModel.sendEvent(SettingsEvent.AppThemeChosen(AppTheme.Dark))
        viewModel.sendEvent(SettingsEvent.UiLanguageChosen(UiLanguage.Russian))
        advanceUntilIdle()

        val saved = AppPreferences(store).prefs.value
        assertEquals(TranslationId("rst"), saved.translation)
        assertEquals(PageTheme.Sepia, saved.pageTheme)
        assertEquals(AppTheme.Dark, saved.appTheme)
        assertEquals(UiLanguage.Russian, saved.uiLanguage)
        assertEquals(PageTheme.Sepia, viewModel.state.value.prefs.pageTheme)
    }

    @Test
    fun textSizeStepsFromTheDefaultAndStaysInRange() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.sendEvent(SettingsEvent.FontSizeStepped(1))
        advanceUntilIdle()
        assertEquals(ScriptureSize.DEFAULT + ScriptureSize.STEP, prefs.prefs.value.fontSize)

        repeat(10) { viewModel.sendEvent(SettingsEvent.FontSizeStepped(1)) }
        advanceUntilIdle()
        assertEquals(ScriptureSize.MAX, prefs.prefs.value.fontSize)
    }

    @Test
    fun aFailedListLeavesTheChoiceOut() = runTest {
        val viewModel = SettingsViewModel(SavedStateHandle(), prefs, Listed { error("Disk unreadable") })
        advanceUntilIdle()

        assertTrue(viewModel.state.value.translations.isEmpty())
    }

    @Test
    fun turningTheReminderOnStartsAtEightAndStepsByAQuarter() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.sendEvent(SettingsEvent.ReminderTurnedOn(granted = true))
        viewModel.sendEvent(SettingsEvent.ReminderTimeStepped(-1))
        viewModel.sendEvent(SettingsEvent.ReminderDayToggled(DayOfWeek.SUNDAY))
        advanceUntilIdle()

        val saved = AppPreferences(store).prefs.value
        assertEquals(LocalTime(7, 45), saved.reminderTime)
        assertFalse(DayOfWeek.SUNDAY in saved.reminderDays)
        assertEquals(6, saved.reminderDays.size)

        viewModel.sendEvent(SettingsEvent.ReminderTurnedOff)
        advanceUntilIdle()
        assertNull(prefs.prefs.value.reminderTime)
    }

    @Test
    fun aRefusedPermissionLeavesTheReminderOffAndSaysWhy() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.sendEvent(SettingsEvent.ReminderTurnedOn(granted = false))
        advanceUntilIdle()

        assertNull(prefs.prefs.value.reminderTime)
        assertTrue(viewModel.state.value.reminderDenied)
    }

    @Test
    fun backCloses() = runTest {
        val viewModel = viewModel()

        viewModel.sendEvent(SettingsEvent.BackClicked)

        assertEquals(SettingsEffect.Close, viewModel.effects.first())
    }

    private class Listed(private val source: () -> List<InstalledTranslation>) : InstalledTranslations {
        override suspend fun list(): List<InstalledTranslation> = source()
    }
}

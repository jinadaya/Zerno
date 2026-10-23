package app.zerno.data.prefs

import app.zerno.entities.bible.TranslationId
import com.russhwolf.settings.MapSettings
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppPreferencesTest {
    @Test
    fun freshInstallHasDefaults() {
        assertEquals(AppPrefs(), AppPreferences(MapSettings()).prefs.value)
    }

    @Test
    fun valuesSurviveARestart() {
        val storage = MapSettings()
        AppPreferences(storage).apply {
            setAppTheme(AppTheme.Dark)
            setPageTheme(PageTheme.Sepia)
            setUiLanguage(UiLanguage.Russian)
            setTranslation(TranslationId("rst"))
            setFontSize(22)
            setReminderTime(LocalTime(7, 30))
            setReminderDays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
            setOnboardingDone(true)
        }

        val expected = AppPrefs(
            appTheme = AppTheme.Dark,
            pageTheme = PageTheme.Sepia,
            uiLanguage = UiLanguage.Russian,
            translation = TranslationId("rst"),
            fontSize = 22,
            reminderTime = LocalTime(7, 30),
            reminderDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            onboardingDone = true,
        )
        assertEquals(expected, AppPreferences(storage).prefs.value)
    }

    @Test
    fun settersUpdateTheFlowAtOnce() {
        val preferences = AppPreferences(MapSettings())
        preferences.setPageTheme(PageTheme.Dark)
        assertEquals(PageTheme.Dark, preferences.prefs.value.pageTheme)
    }

    @Test
    fun clearingTheReminderRemovesIt() {
        val storage = MapSettings()
        AppPreferences(storage).apply {
            setReminderTime(LocalTime(21, 0))
            setReminderTime(null)
        }
        assertNull(AppPreferences(storage).prefs.value.reminderTime)
    }

    @Test
    fun unreadableValuesCountAsNeverSet() {
        val storage = MapSettings("app.theme" to "Neon", "reminder.time" to "late", "reading.translation" to "")
        val prefs = AppPreferences(storage).prefs.value
        assertEquals(AppTheme.System, prefs.appTheme)
        assertNull(prefs.reminderTime)
        assertNull(prefs.translation)
    }
}

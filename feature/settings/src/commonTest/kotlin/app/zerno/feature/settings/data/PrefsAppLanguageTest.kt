package app.zerno.feature.settings.data

import app.zerno.data.prefs.AppPreferences
import app.zerno.data.prefs.UiLanguage
import app.zerno.entities.bible.TranslationId
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrefsAppLanguageTest {
    private val prefs = AppPreferences(MapSettings())
    private val language = PrefsAppLanguage(prefs)

    @Test
    fun russianDeviceLandsInRstWithTheDeviceUi() = runTest {
        language.chooseFirstRunTranslation("ru-RU")

        assertEquals(TranslationId("rst"), prefs.prefs.value.translation)
        assertNull(language.currentUiLanguage)
        assertNull(language.uiLanguage.first())
    }

    @Test
    fun englishDeviceLandsInKjv() {
        language.chooseFirstRunTranslation("en")
        assertEquals(TranslationId("kjv"), prefs.prefs.value.translation)
    }

    @Test
    fun aChosenTranslationIsKept() {
        prefs.setTranslation(TranslationId("kjv"))
        language.chooseFirstRunTranslation("ru")
        assertEquals(TranslationId("kjv"), prefs.prefs.value.translation)
    }

    @Test
    fun uiLanguageFollowsTheSetting() = runTest {
        prefs.setUiLanguage(UiLanguage.Russian)

        assertEquals("ru", language.currentUiLanguage)
        assertEquals("ru", language.uiLanguage.first())
    }
}

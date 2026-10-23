package app.zerno.feature.settings.domain

import app.zerno.data.prefs.UiLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguageRulesTest {
    @Test
    fun russianDeviceGetsRst() {
        listOf("ru", "RU", "ru-RU", "ru_RU", "ru-UA").forEach { assertEquals(Rst, firstRunTranslation(it), it) }
    }

    @Test
    fun anyOtherDeviceGetsKjv() {
        listOf("en", "en-US", "uk", "de", "", "rus").forEach { assertEquals(Kjv, firstRunTranslation(it), it) }
    }

    @Test
    fun systemUiLanguageHasNoTag() {
        assertNull(UiLanguage.System.tag)
        assertEquals("en", UiLanguage.English.tag)
        assertEquals("ru", UiLanguage.Russian.tag)
    }
}

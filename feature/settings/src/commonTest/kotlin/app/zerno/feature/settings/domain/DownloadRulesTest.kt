package app.zerno.feature.settings.domain

import app.zerno.data.db.bible.InstalledTranslation
import app.zerno.entities.bible.TranslationId
import app.zerno.feature.settings.ui.remote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadRulesTest {
    private fun installed(id: String, language: String, bundled: Boolean) =
        InstalledTranslation(TranslationId(id), language, id, bundled, path = id)

    private val kjv = installed("kjv", "en", bundled = true)
    private val rst = installed("rst", "ru", bundled = true)
    private val asv = installed("asv", "en", bundled = false)
    private val nrt = installed("nrt", "ru", bundled = false)

    @Test
    fun bundledTranslationsAreNotOffered() {
        val offers = offersOf(listOf(remote("kjv", true), remote("asv", false)), listOf(kjv), emptyMap())
        assertEquals(listOf("asv"), offers.map { it.translation.id })
        assertEquals(false, offers.single().installed)
    }

    @Test
    fun aNewerFileOnTheServerIsAnUpdate() {
        val catalog = listOf(remote("asv", false, fileVersion = "new"))
        assertEquals(true, offersOf(catalog, listOf(asv), mapOf("asv" to "old")).single().updateAvailable)
        assertEquals(false, offersOf(catalog, listOf(asv), mapOf("asv" to "new")).single().updateAvailable)
        // A file put there some other way has no version to compare.
        assertEquals(false, offersOf(catalog, listOf(asv), mapOf("asv" to null)).single().updateAvailable)
        assertEquals(false, offersOf(catalog, emptyList(), mapOf("asv" to "old")).single().updateAvailable)
    }

    @Test
    fun removingTheCurrentTranslationFallsBackToABundledOneInItsLanguage() {
        val all = listOf(rst, kjv, asv, nrt)
        assertEquals(kjv.id, translationAfterDeleting(asv.id, asv.id, all))
        assertEquals(rst.id, translationAfterDeleting(nrt.id, nrt.id, all))
        assertEquals(rst.id, translationAfterDeleting(installed("tg", "tg", false).id, TranslationId("tg"), all))
        assertNull(translationAfterDeleting(asv.id, kjv.id, all))
    }
}

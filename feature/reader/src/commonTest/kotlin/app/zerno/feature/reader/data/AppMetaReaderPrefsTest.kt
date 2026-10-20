package app.zerno.feature.reader.data

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Versification
import app.zerno.feature.reader.domain.ReaderLocation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppMetaReaderPrefsTest {
    private val rows = mutableMapOf<String, String>()
    private val prefs = AppMetaReaderPrefs(object : AppMetaDao {
        override suspend fun get(key: String) = rows[key]
        override suspend fun put(meta: AppMeta) {
            rows[meta.key] = meta.value
        }
    })
    private val john3 = ChapterRef(BookId(500), 3)

    @Test
    fun freshInstallHasNothing() = runTest {
        assertNull(prefs.lastLocation())
        assertNull(prefs.position(Versification.English, john3))
        assertNull(prefs.fontSize())
    }

    @Test
    fun lastLocationRoundTrips() = runTest {
        val location = ReaderLocation(TranslationId("kjv"), john3, 16)
        prefs.saveLastLocation(location)
        assertEquals("kjv|500|3|16", rows["reader.location"])
        assertEquals(location, prefs.lastLocation())

        prefs.saveLastLocation(location.copy(verse = null))
        assertEquals(location.copy(verse = null), prefs.lastLocation())
    }

    @Test
    fun positionsAreKeptPerNumbering() = runTest {
        prefs.savePosition(Versification.English, john3, 16)
        assertEquals(16, prefs.position(Versification.English, john3))
        assertNull(prefs.position(Versification.Russian, john3))

        prefs.savePosition(Versification.English, john3, null)
        assertNull(prefs.position(Versification.English, john3))
    }

    @Test
    fun fontSizeRoundTrips() = runTest {
        prefs.saveFontSize(24)
        assertEquals(24, prefs.fontSize())
    }

    @Test
    fun unreadableValuesCountAsMissing() = runTest {
        rows["reader.location"] = "kjv|5|3"
        assertNull(prefs.lastLocation())
        rows["reader.location"] = "kjv|5|3|1"
        assertNull(prefs.lastLocation())
        rows["reader.font-size"] = "big"
        assertNull(prefs.fontSize())
    }
}

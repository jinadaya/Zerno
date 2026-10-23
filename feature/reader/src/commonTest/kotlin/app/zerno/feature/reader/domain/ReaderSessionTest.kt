package app.zerno.feature.reader.domain

import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.TranslationId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReaderSessionTest {
    private val prefs = FakePrefs()
    private val session = ReaderSession(FakeBible(), prefs)

    @Test
    fun freshInstallOpensGenesisInTheDeviceLanguage() = runTest {
        session.start(null, "en")
        assertEquals(ReaderLocation(KJV, ChapterRef(GENESIS, 1)), session.location.value)
    }

    @Test
    fun freshInstallOpensTheFirstRunTranslation() = runTest {
        session.start(RST, "en")
        assertEquals(ReaderLocation(RST, ChapterRef(GENESIS, 1)), session.location.value)
    }

    @Test
    fun firstRunTranslationNotInstalledFallsBackToTheLanguage() = runTest {
        session.start(TranslationId("asv"), "en")
        assertEquals(KJV, session.location.value?.translation)
    }

    @Test
    fun unknownLanguageOpensTheFirstTranslation() = runTest {
        session.start(null, "de")
        assertEquals(RST, session.location.value?.translation)
    }

    @Test
    fun restartReturnsToTheLastPlace() = runTest {
        session.start(null, "en")
        session.openChapter(ChapterRef(JOHN, 3))
        session.savePosition(ReaderLocation(KJV, ChapterRef(JOHN, 3), 16))

        val reopened = ReaderSession(FakeBible(), prefs)
        reopened.start(null, "ru")

        assertEquals(ReaderLocation(KJV, ChapterRef(JOHN, 3), 16), reopened.location.value)
    }

    @Test
    fun lastPlaceInATranslationNoLongerInstalledIsIgnored() = runTest {
        prefs.last = ReaderLocation(TranslationId("asv"), ChapterRef(JOHN, 3), 16)
        session.start(null, "en")
        assertEquals(ReaderLocation(KJV, ChapterRef(GENESIS, 1)), session.location.value)
    }

    @Test
    fun reopeningAChapterReturnsToTheSameVerse() = runTest {
        session.start(null, "en")
        session.openChapter(ChapterRef(JOHN, 3))
        session.savePosition(ReaderLocation(KJV, ChapterRef(JOHN, 3), 16))
        session.openChapter(ChapterRef(JOHN, 4))
        assertNull(session.location.value?.verse)

        session.openChapter(ChapterRef(JOHN, 3))

        assertEquals(ReaderLocation(KJV, ChapterRef(JOHN, 3), 16), session.location.value)
    }

    @Test
    fun scrollingBackToTheTopForgetsTheVerse() = runTest {
        session.start(null, "en")
        session.savePosition(ReaderLocation(KJV, ChapterRef(GENESIS, 1), 12))
        session.savePosition(ReaderLocation(KJV, ChapterRef(GENESIS, 1), null))
        session.openChapter(ChapterRef(GENESIS, 1))
        assertNull(session.location.value?.verse)
    }

    @Test
    fun switchingKjvToRstKeepsTheVerse() = runTest {
        session.start(null, "en")
        session.switchTranslation(ReaderLocation(KJV, ChapterRef(PSALMS, 23), 4), RST)
        assertEquals(ReaderLocation(RST, ChapterRef(PSALMS, 22), 4), session.location.value)
    }

    @Test
    fun switchingFollowsAVerseIntoAnotherChapter() = runTest {
        session.start(null, "en")
        session.switchTranslation(ReaderLocation(KJV, ChapterRef(JONAH, 1), 17), RST)
        assertEquals(ReaderLocation(RST, ChapterRef(JONAH, 2), 1), session.location.value)
    }

    @Test
    fun switchingBackRestoresTheOriginalVerse() = runTest {
        session.start(null, "ru")
        session.switchTranslation(ReaderLocation(RST, ChapterRef(PSALMS, 22), 4), KJV)
        assertEquals(ReaderLocation(KJV, ChapterRef(PSALMS, 23), 4), session.location.value)
    }

    @Test
    fun switchingAtTheTopOfAChapterStaysAtTheTop() = runTest {
        session.start(null, "en")
        session.switchTranslation(ReaderLocation(KJV, ChapterRef(JOHN, 3), null), RST)
        assertEquals(ReaderLocation(RST, ChapterRef(JOHN, 3), null), session.location.value)
    }
}

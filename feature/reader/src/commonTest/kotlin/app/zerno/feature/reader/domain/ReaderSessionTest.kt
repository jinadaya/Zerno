package app.zerno.feature.reader.domain

import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Versification
import app.zerno.feature.reader.api.PassageProgress
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

    @Test
    fun planPassageOpensAtItsFirstVerse() = runTest {
        session.start(KJV, "en")
        session.openPassage(PassageRef(JOHN, 4, 1, 4, 26))
        assertEquals(ReaderLocation(KJV, ChapterRef(JOHN, 4), 1), session.location.value)
    }

    @Test
    fun planPassageOfWholeChaptersOpensAtTheTop() = runTest {
        session.start(KJV, "en")
        session.openPassage(PassageRef.chapter(JOHN, 1))
        assertEquals(ReaderLocation(KJV, ChapterRef(JOHN, 1)), session.location.value)
    }

    @Test
    fun planPassageIsMappedToSynodalNumbering() = runTest {
        session.start(RST, "ru")
        session.openPassage(PassageRef.chapter(PSALMS, 23))
        assertEquals(ReaderLocation(RST, ChapterRef(PSALMS, 22)), session.location.value)
    }

    @Test
    fun resumingAPassageOpensWhereItWasLeft() = runTest {
        session.start(KJV, "en")
        prefs.positions[Versification.English to ChapterRef(JOHN, 4)] = 12
        session.openPassage(PassageRef.chapter(JOHN, 4), resume = true)
        assertEquals(ReaderLocation(KJV, ChapterRef(JOHN, 4), 12), session.location.value)

        session.openPassage(PassageRef.chapter(JOHN, 5), resume = true)
        assertEquals(ReaderLocation(KJV, ChapterRef(JOHN, 5)), session.location.value)
    }

    @Test
    fun progressIsNullOnAFreshInstall() = runTest {
        assertNull(session.progressIn(PassageRef.chapter(JOHN, 4)))
    }

    @Test
    fun progressIsTheSavedVerseOfTheChapter() = runTest {
        prefs.last = ReaderLocation(KJV, ChapterRef(GENESIS, 1))
        prefs.positions[Versification.English to ChapterRef(JOHN, 4)] = 12

        assertEquals(PassageProgress(12, 30), session.progressIn(PassageRef.chapter(JOHN, 4)))
        assertEquals(0.4f, PassageProgress(12, 30).fraction)
    }

    @Test
    fun progressAtThePassageStartIsNull() = runTest {
        session.start(KJV, "en")
        prefs.positions[Versification.English to ChapterRef(JOHN, 4)] = 5
        assertNull(session.progressIn(PassageRef(JOHN, 4, 5, 4, 26)))
        assertEquals(PassageProgress(5, 30), session.progressIn(PassageRef.chapter(JOHN, 4)))
    }

    @Test
    fun progressUsesTheTranslationsNumbering() = runTest {
        session.start(RST, "ru")
        prefs.positions[Versification.Russian to ChapterRef(PSALMS, 22)] = 3
        assertEquals(PassageProgress(3, 30), session.progressIn(PassageRef.chapter(PSALMS, 23)))
    }
}

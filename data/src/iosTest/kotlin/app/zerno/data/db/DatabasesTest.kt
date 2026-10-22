@file:OptIn(ExperimentalForeignApi::class)

package app.zerno.data.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_CREATE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import androidx.sqlite.execSQL
import app.zerno.data.db.bible.BibleFile
import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.DayMarkRow
import app.zerno.data.db.user.UserDb
import app.zerno.data.db.user.openUserDb
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.VerseStyle
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.isEqualToData
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabasesTest {
    private val dir = NSTemporaryDirectory().trimEnd('/')
    private val userPath = "$dir/test-user.db"
    private val biblePath = "$dir/test-kjv.SQLite3"
    private val kjv = TranslationId("kjv")

    @AfterTest
    fun clean() {
        listOf(userPath, "$userPath-wal", "$userPath-shm", biblePath).forEach {
            NSFileManager.defaultManager.removeItemAtPath(it, null)
        }
    }

    @Test
    fun userDbKeepsDataAcrossReopen() = runBlocking {
        val first = openUserDb(Room.databaseBuilder<UserDb>(name = userPath))
        first.appMetaDao().put(AppMeta("bundled", "kjv,rst"))
        first.close()

        val reopened = openUserDb(Room.databaseBuilder<UserDb>(name = userPath))
        assertEquals("kjv,rst", reopened.appMetaDao().get("bundled"))
        assertNull(reopened.appMetaDao().get("missing"))
        reopened.close()
    }

    @Test
    fun userDbMigratesFromVersion1() = runBlocking {
        // A version 1 file as Room wrote it (data/schemas/.../1.json), with a value to carry over.
        BundledSQLiteDriver().open(userPath, SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE).apply {
            execSQL("CREATE TABLE IF NOT EXISTS `app_meta` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
            execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f331993ec2a767bea9ecb5fdd6719503')")
            execSQL("INSERT INTO app_meta VALUES ('reader.book', '500')")
            execSQL("PRAGMA user_version = 1")
            close()
        }

        val db = openUserDb(Room.databaseBuilder<UserDb>(name = userPath))
        assertEquals("500", db.appMetaDao().get("reader.book"))
        db.gamificationDao().putDayMark(DayMarkRow("2026-03-02", "Read"))
        assertEquals(listOf(DayMarkRow("2026-03-02", "Read")), db.gamificationDao().dayMarks().first())
        db.close()
    }

    @Test
    fun bibleFileReadsWithoutChangingTheFile() = runBlocking {
        BundledSQLiteDriver().open(biblePath, SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE).apply {
            execSQL("CREATE TABLE info (name TEXT, value TEXT)")
            execSQL("INSERT INTO info VALUES ('language', 'en')")
            close()
        }
        val before = fileData()

        BibleFile.open(biblePath, kjv).use { bible ->
            assertEquals("en", bible.infoValue("language"))
            assertNull(bible.infoValue("missing"))
            val write = runCatching { bible.query("INSERT INTO info VALUES ('x', 'y')") { step() } }
            assertTrue(write.isFailure, "Write to a read-only Bible file succeeded")
        }

        assertTrue(before.isEqualToData(fileData()), "Bible file changed on disk")
    }

    @Test
    fun missingBibleFileFailsInsteadOfBeingCreated() {
        assertFails { BibleFile.open(biblePath, kjv) }
        assertEquals(false, NSFileManager.defaultManager.fileExistsAtPath(biblePath))
    }

    @Test
    fun readerQueriesFollowSchemaV1() = runBlocking {
        BundledSQLiteDriver().open(biblePath, SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE).apply {
            execSQL("CREATE TABLE books (book_color TEXT, book_number NUMERIC, short_name TEXT, long_name TEXT)")
            execSQL("INSERT INTO books VALUES ('#ccffff', 500, 'John', 'John'), ('#ccccff', 10, 'Gen', 'Genesis')")
            execSQL("CREATE TABLE verses (book_number NUMERIC, chapter NUMERIC, verse NUMERIC, text TEXT)")
            execSQL("CREATE UNIQUE INDEX verses_index ON verses (book_number, chapter, verse)")
            execSQL(
                "INSERT INTO verses VALUES (10, 1, 1, 'In the beginning'), (10, 2, 1, 'Thus'), " +
                    "(500, 3, 15, 'That whosoever'), (500, 3, 16, '$JOHN_3_16'), (500, 3, 17, 'For God sent'), " +
                    "(500, 4, 1, 'When therefore'), (500, 4, 2, 'Though Jesus')",
            )
            close()
        }

        BibleFile.open(biblePath, kjv).use { bible ->
            val books = bible.books()
            assertEquals(listOf(BookId(10), BookId(500)), books.map { it.id })
            assertEquals(listOf(2, 4), books.map { it.chapterCount })
            assertEquals("Genesis", books.first().name)

            assertEquals(4, bible.chapterCount(BookId(500)))
            assertEquals(0, bible.chapterCount(BookId(730)))

            val chapter = bible.chapter(ChapterRef(BookId(500), 3))
            assertEquals(listOf(15, 16, 17), chapter.map { it.number })
            val john = chapter[1]
            assertEquals(kjv, john.translation)
            assertEquals(JOHN_3_16_TEXT, john.text.text)
            assertEquals(listOf(VerseStyle.WordsOfJesus), john.text.spans.map { it.style })

            val range = bible.verses(PassageRef(BookId(500), 3, 16, 4, 1))
            assertEquals(listOf("3:16", "3:17", "4:1"), range.map { "${it.chapter.chapter}:${it.number}" })
            assertEquals(listOf(16), bible.verses(PassageRef.verse(BookId(500), 3, 16)).map { it.number })
            assertEquals(listOf(1, 2), bible.verses(PassageRef(BookId(500), 4, 1)).map { it.number })
            assertTrue(bible.chapter(ChapterRef(BookId(500), 9)).isEmpty())
        }
    }

    private fun fileData(): NSData = requireNotNull(NSFileManager.defaultManager.contentsAtPath(biblePath))

    private companion object {
        const val JOHN_3_16 = "<pb/><J>For<S>1063</S> God<S>2316</S> so<S>3779</S> loved<S>25</S> the<S>3588</S> " +
            "world,<S>2889</S> that<S>5620</S> he gave<S>1325</S> his<S>848</S> only begotten<S>3439</S> Son,<S>5207</S> " +
            "that<S>2443</S> whosoever<S>3956</S> believeth<S>4100</S> in<S>1519</S> him<S>846</S> should not<S>3361</S> " +
            "perish,<S>622</S> but<S>235</S> have<S>2192</S> everlasting<S>166</S> life.</J><S>2222</S>"
        const val JOHN_3_16_TEXT = "For God so loved the world, that he gave his only begotten Son, that whosoever " +
            "believeth in him should not perish, but have everlasting life."
    }
}

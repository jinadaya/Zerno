package app.zerno.data.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_CREATE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import androidx.sqlite.execSQL
import app.zerno.data.db.bible.BibleFile
import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.UserDb
import app.zerno.data.db.user.openUserDb
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
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
    fun bibleFileReadsWithoutChangingTheFile() = runBlocking {
        BundledSQLiteDriver().open(biblePath, SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE).apply {
            execSQL("CREATE TABLE info (name TEXT, value TEXT)")
            execSQL("INSERT INTO info VALUES ('language', 'en')")
            close()
        }
        val before = fileData()

        BibleFile.open(biblePath).use { bible ->
            assertEquals("en", bible.infoValue("language"))
            assertNull(bible.infoValue("missing"))
            val write = runCatching { bible.query("INSERT INTO info VALUES ('x', 'y')") { step() } }
            assertTrue(write.isFailure, "Write to a read-only Bible file succeeded")
        }

        assertTrue(before.isEqualToData(fileData()), "Bible file changed on disk")
    }

    @Test
    fun missingBibleFileFailsInsteadOfBeingCreated() {
        assertFails { BibleFile.open(biblePath) }
        assertEquals(false, NSFileManager.defaultManager.fileExistsAtPath(biblePath))
    }

    private fun fileData(): NSData = requireNotNull(NSFileManager.defaultManager.contentsAtPath(biblePath))
}

package app.zerno.data.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_CREATE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import androidx.sqlite.execSQL
import app.zerno.data.db.bible.BundledBibleSource
import app.zerno.data.db.bible.BundledTranslation
import app.zerno.data.db.bible.TranslationStore
import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.TranslationId
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.posix.memcpy
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class TranslationStoreTest {
    private val root = NSTemporaryDirectory().trimEnd('/') + "/translation-store-" + Random.nextLong().toULong()
    private val directory = Path("$root/bibles")
    private val meta = FakeAppMeta()
    private val kjv = TranslationId("kjv")
    private val rst = TranslationId("rst")
    private val reads = mutableListOf<String>()
    private val source = BundledBibleSource { resource ->
        reads += resource
        val id = resource.substringAfterLast('/').substringBefore('.')
        fixture(id, if (id == "kjv") "en" else "ru", if (id == "kjv") "In the beginning" else "В начале")
    }

    @AfterTest
    fun clean() {
        NSFileManager.defaultManager.removeItemAtPath(root, null)
    }

    @Test
    fun freshInstallReadsGenesisOneOfflineInBoth() = runBlocking<Unit> {
        val store = TranslationStore(directory, meta, source = source)

        assertEquals(listOf(rst, kjv), store.list().map { it.id })
        assertTrue(store.list().all { it.bundled })
        store.open(kjv).use { assertEquals("In the beginning", it.chapter(ChapterRef(BookId(10), 1)).first().text.text) }
        store.open(rst).use { assertEquals("В начале", it.chapter(ChapterRef(BookId(10), 1)).first().text.text) }
        assertEquals("1", meta.values["bundled.kjv"])
    }

    @Test
    fun copiesAgainOnlyWhenTheBundledVersionGoesUp() = runBlocking<Unit> {
        TranslationStore(directory, meta, source = source).installBundled()
        TranslationStore(directory, meta, source = source).installBundled()
        assertEquals(2, reads.size, "Second launch copied the files again")

        val newer = BundledTranslation.all.map { if (it.id == kjv) it.copy(version = 2) else it }
        TranslationStore(directory, meta, bundled = newer, source = source).installBundled()
        assertEquals(3, reads.size)
        assertEquals("2", meta.values["bundled.kjv"])
    }

    @Test
    fun listsDownloadedFilesAfterBundledOnesAndSkipsBrokenOnes() = runBlocking<Unit> {
        val store = TranslationStore(directory, meta, source = source)
        store.installBundled()
        write("asv.SQLite3", fixture("asv", "en", "In the beginning God"))
        write("broken.SQLite3", "not a database".encodeToByteArray())

        val listed = store.list()
        assertEquals(listOf("rst", "kjv", "asv"), listed.map { it.id.value })
        assertEquals(false, listed.last().bundled)
        assertEquals("asv Bible", listed.last().name)
        assertFails { store.open(TranslationId("nrt")) }
    }

    /** A schema v1 file with Genesis 1:1, built on disk and read back as bytes. */
    private fun fixture(id: String, language: String, genesis: String): ByteArray {
        val path = "$root/fixture-$id-${Random.nextInt()}.SQLite3"
        NSFileManager.defaultManager.createDirectoryAtPath(root, withIntermediateDirectories = true, attributes = null, error = null)
        BundledSQLiteDriver().open(path, SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE).apply {
            execSQL("CREATE TABLE info (name TEXT, value TEXT)")
            execSQL("INSERT INTO info VALUES ('language', '$language'), ('description', '$id Bible')")
            execSQL("CREATE TABLE books (book_number NUMERIC, short_name TEXT, long_name TEXT, book_color TEXT)")
            execSQL("INSERT INTO books VALUES (10, 'Gen', 'Genesis', '#ccccff')")
            execSQL("CREATE TABLE verses (book_number NUMERIC, chapter NUMERIC, verse NUMERIC, text TEXT)")
            execSQL("INSERT INTO verses VALUES (10, 1, 1, '<pb/>$genesis<S>7225</S>')")
            close()
        }
        val data = requireNotNull(NSFileManager.defaultManager.contentsAtPath(path))
        NSFileManager.defaultManager.removeItemAtPath(path, null)
        return data.toByteArray()
    }

    private fun write(name: String, bytes: ByteArray) {
        SystemFileSystem.sink(Path(directory, name)).buffered().use { it.write(bytes) }
    }

    private class FakeAppMeta : AppMetaDao {
        val values = mutableMapOf<String, String>()
        override suspend fun get(key: String): String? = values[key]
        override suspend fun put(meta: AppMeta) {
            values[meta.key] = meta.value
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).also { array ->
    if (array.isNotEmpty()) array.usePinned { memcpy(it.addressOf(0), bytes, length) }
}

package app.zerno.data.db.bible

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
import app.zerno.entities.bible.Book
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Verse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * One translation file (Bible schema v1, backend `docs/bible-schema-v1.md`), opened read-only.
 *
 * Not a Room database on purpose: Room opens files for writing and stamps them with `room_master_table` and
 * `user_version`, which would change the file and its published SHA-256. This goes straight to the same bundled
 * SQLite driver with `SQLITE_OPEN_READONLY`, so the file is never written and a missing file fails to open instead
 * of being created empty. Close it when the reader switches translation.
 */
class BibleFile private constructor(
    val path: String,
    val translation: TranslationId,
    private val connection: SQLiteConnection,
) : AutoCloseable {
    // A connection is not safe for concurrent use.
    private val lock = Mutex()

    /** A value from the `info` table, e.g. `language` or `description`; null when the key is absent. */
    suspend fun infoValue(name: String): String? =
        query("SELECT value FROM info WHERE name = ?", { bindText(1, name) }) {
            if (step() && !isNull(0)) getText(0) else null
        }

    /**
     * Books present in the file with their chapter counts, in display order: `sorting_order` when the file has that
     * column (NRT puts James to Jude after Acts), else `book_number`. Names are the translation's own.
     */
    suspend fun books(): List<Book> {
        val order = if (hasBooksColumn("sorting_order")) "COALESCE(b.sorting_order, b.book_number)" else "b.book_number"
        return query(
            "SELECT b.book_number, b.short_name, b.long_name, " +
                "(SELECT MAX(v.chapter) FROM verses v WHERE v.book_number = b.book_number) AS chapters " +
                "FROM books b WHERE chapters IS NOT NULL ORDER BY $order, b.book_number",
        ) {
            buildList {
                while (step()) {
                    add(Book(BookId(getInt(0)), getText(1), getText(2), getInt(3)))
                }
            }
        }
    }

    /** Number of chapters of [book] in this translation; 0 when the file does not have the book. */
    suspend fun chapterCount(book: BookId): Int =
        query("SELECT MAX(chapter) FROM verses WHERE book_number = ?", { bindInt(1, book.value) }) {
            if (step() && !isNull(0)) getInt(0) else 0
        }

    /** Every verse of [chapter] in order; empty when the chapter is not in the file. */
    suspend fun chapter(chapter: ChapterRef): List<Verse> =
        verses(chapter.book, "chapter = ?") { bindInt(2, chapter.chapter) }

    /**
     * Verses of [passage] in order, read in this translation's own numbering (schema v1 maps nothing between
     * versifications). An open start or end takes the chapter from its first or to its last verse; verses past the
     * end of the book are simply absent.
     */
    suspend fun verses(passage: PassageRef): List<Verse> = verses(
        passage.book,
        "chapter BETWEEN ? AND ? AND NOT (chapter = ? AND verse < ?) AND NOT (chapter = ? AND verse > ?)",
    ) {
        bindInt(2, passage.startChapter)
        bindInt(3, passage.endChapter)
        bindInt(4, passage.startChapter)
        bindInt(5, passage.startVerse ?: 1)
        bindInt(6, passage.endChapter)
        bindInt(7, passage.endVerse ?: Int.MAX_VALUE)
    }

    /** Verses of [book] matching [where]; [bind] binds its parameters from index 2, after the book. */
    private suspend fun verses(book: BookId, where: String, bind: SQLiteStatement.() -> Unit): List<Verse> = query(
        "SELECT chapter, verse, text FROM verses WHERE book_number = ? AND $where ORDER BY chapter, verse",
        {
            bindInt(1, book.value)
            bind()
        },
    ) {
        buildList {
            while (step()) {
                add(Verse(translation, ChapterRef(book, getInt(0)), getInt(1), VerseMarkup.parse(getText(2))))
            }
        }
    }

    private suspend fun hasBooksColumn(name: String): Boolean =
        query("SELECT 1 FROM pragma_table_info('books') WHERE name = ?", { bindText(1, name) }) { step() }

    /** Runs [sql] with [bind] applied and reads the rows with [read]. */
    internal suspend fun <T> query(
        sql: String,
        bind: SQLiteStatement.() -> Unit = {},
        read: SQLiteStatement.() -> T,
    ): T = withContext(Dispatchers.IO) {
        lock.withLock {
            connection.prepare(sql).use { statement ->
                statement.bind()
                statement.read()
            }
        }
    }

    override fun close() = connection.close()

    companion object {
        fun open(path: String, translation: TranslationId): BibleFile =
            BibleFile(path, translation, BundledSQLiteDriver().open(path, SQLITE_OPEN_READONLY))
    }
}

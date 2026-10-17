package app.zerno.data.db.bible

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
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
class BibleFile private constructor(val path: String, private val connection: SQLiteConnection) : AutoCloseable {
    // A connection is not safe for concurrent use.
    private val lock = Mutex()

    /** A value from the `info` table, e.g. `language` or `description`; null when the key is absent. */
    suspend fun infoValue(name: String): String? = query("SELECT value FROM info WHERE name = ?", { bindText(1, name) }) {
        if (step() && !isNull(0)) getText(0) else null
    }

    /** Runs [sql] with [bind] applied and reads the rows with [read]. DA-02 adds the reader queries on top of this. */
    internal suspend fun <T> query(sql: String, bind: SQLiteStatement.() -> Unit = {}, read: SQLiteStatement.() -> T): T =
        withContext(Dispatchers.IO) {
            lock.withLock {
                connection.prepare(sql).use { statement ->
                    statement.bind()
                    statement.read()
                }
            }
        }

    override fun close() = connection.close()

    companion object {
        fun open(path: String): BibleFile =
            BibleFile(path, BundledSQLiteDriver().open(path, SQLITE_OPEN_READONLY))
    }
}

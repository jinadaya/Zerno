package app.zerno.data.db

import android.database.Cursor
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteProgram
import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement

/**
 * Queries a database file without ever writing to it. [AndroidSqliteDriver][app.cash.sqldelight.driver.android.AndroidSqliteDriver]
 * goes through `SQLiteOpenHelper`, which opens the file for writing and sets `user_version` on first open; this driver
 * opens it with `OPEN_READONLY` instead. Queries only: `execute` and transactions throw.
 */
internal class ReadOnlyAndroidSqliteDriver(path: String) : SqlDriver {
    private val database = SQLiteDatabase.openDatabase(
        path,
        null,
        SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
    )

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
        val arguments = Arguments(parameters).apply { binders?.invoke(this) }
        val cursor = database.rawQueryWithFactory(
            { _, cursorDriver, editTable, query ->
                arguments.bindTo(query)
                SQLiteCursor(cursorDriver, editTable, query)
            },
            sql,
            null,
            null,
        )
        return cursor.use { QueryResult.Value(mapper(ReadOnlyCursor(it)).value) }
    }

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> = throw UnsupportedOperationException("Read-only database: $sql")

    override fun newTransaction(): QueryResult<Transacter.Transaction> =
        throw UnsupportedOperationException("Read-only database has no transactions")

    override fun currentTransaction(): Transacter.Transaction? = null

    // Nothing is ever written, so no query result ever changes.
    override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit

    override fun notifyListeners(vararg queryKeys: String) = Unit

    override fun close() = database.close()
}

/** Collects SQLDelight's 0-based binds and replays them on the framework query (1-based). */
private class Arguments(count: Int) : SqlPreparedStatement {
    private val binds = arrayOfNulls<(SQLiteProgram) -> Unit>(count)

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        binds[index] = { if (bytes == null) it.bindNull(index + 1) else it.bindBlob(index + 1, bytes) }
    }

    override fun bindLong(index: Int, long: Long?) {
        binds[index] = { if (long == null) it.bindNull(index + 1) else it.bindLong(index + 1, long) }
    }

    override fun bindDouble(index: Int, double: Double?) {
        binds[index] = { if (double == null) it.bindNull(index + 1) else it.bindDouble(index + 1, double) }
    }

    override fun bindString(index: Int, string: String?) {
        binds[index] = { if (string == null) it.bindNull(index + 1) else it.bindString(index + 1, string) }
    }

    override fun bindBoolean(index: Int, boolean: Boolean?) {
        binds[index] = { if (boolean == null) it.bindNull(index + 1) else it.bindLong(index + 1, if (boolean) 1L else 0L) }
    }

    fun bindTo(program: SQLiteProgram) = binds.forEach { it?.invoke(program) }
}

private class ReadOnlyCursor(private val cursor: Cursor) : SqlCursor {
    override fun next(): QueryResult<Boolean> = QueryResult.Value(cursor.moveToNext())
    override fun getString(index: Int) = if (cursor.isNull(index)) null else cursor.getString(index)
    override fun getLong(index: Int) = if (cursor.isNull(index)) null else cursor.getLong(index)
    override fun getBytes(index: Int) = if (cursor.isNull(index)) null else cursor.getBlob(index)
    override fun getDouble(index: Int) = if (cursor.isNull(index)) null else cursor.getDouble(index)
    override fun getBoolean(index: Int) = if (cursor.isNull(index)) null else cursor.getLong(index) == 1L
}

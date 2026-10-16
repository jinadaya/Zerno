package app.zerno.data.db

import app.cash.sqldelight.db.SqlDriver

/** Opens drivers for the app's two kinds of database. Each platform supplies its own (Android needs a `Context`). */
interface SqlDriverFactory {
    /** The user's database ([USER_DB_NAME]), created or migrated to the current `UserDb.Schema` on open. */
    fun createUserDbDriver(): SqlDriver

    /**
     * The Bible file at [path] (absolute), opened read-only. Its schema is never created or migrated and the file
     * is never written, `user_version` included, so its SHA-256 stays as published. Fails if the file is missing.
     */
    fun createBibleDbDriver(path: String): SqlDriver
}

const val USER_DB_NAME = "user.db"

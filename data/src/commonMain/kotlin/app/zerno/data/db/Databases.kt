package app.zerno.data.db

import app.cash.sqldelight.db.SqlDriver
import app.zerno.data.db.bible.BibleDb
import app.zerno.data.db.user.UserDb

/** Opens the user's database. Keep one instance for the life of the app. */
fun openUserDb(factory: SqlDriverFactory): UserDb = UserDb(factory.createUserDbDriver())

/** One open translation file. Close it when the reader switches translation. */
class BibleFile private constructor(val path: String, private val driver: SqlDriver) : AutoCloseable {
    val db: BibleDb = BibleDb(driver)

    override fun close() = driver.close()

    companion object {
        fun open(path: String, factory: SqlDriverFactory): BibleFile = BibleFile(path, factory.createBibleDbDriver(path))
    }
}

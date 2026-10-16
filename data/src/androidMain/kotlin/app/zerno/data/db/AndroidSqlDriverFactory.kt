package app.zerno.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.zerno.data.db.user.UserDb

class AndroidSqlDriverFactory(private val context: Context) : SqlDriverFactory {
    override fun createUserDbDriver(): SqlDriver = AndroidSqliteDriver(
        schema = UserDb.Schema,
        context = context,
        name = USER_DB_NAME,
        callback = object : AndroidSqliteDriver.Callback(UserDb.Schema) {
            override fun onConfigure(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
            }
        },
    )

    override fun createBibleDbDriver(path: String): SqlDriver = ReadOnlyAndroidSqliteDriver(path)
}

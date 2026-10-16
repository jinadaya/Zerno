package app.zerno.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.zerno.data.db.user.UserDb
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.JournalMode
import co.touchlab.sqliter.NO_VERSION_CHECK
import co.touchlab.sqliter.withStatement
import platform.Foundation.NSFileManager

class IosSqlDriverFactory : SqlDriverFactory {
    override fun createUserDbDriver(): SqlDriver = NativeSqliteDriver(
        schema = UserDb.Schema,
        name = USER_DB_NAME,
        onConfiguration = { it.copy(extendedConfig = it.extendedConfig.copy(foreignKeyConstraints = true)) },
    )

    override fun createBibleDbDriver(path: String): SqlDriver {
        // SQLiter opens with SQLITE_OPEN_CREATE, so a missing file would silently become an empty database.
        check(NSFileManager.defaultManager.fileExistsAtPath(path)) { "No Bible file at $path" }
        val slash = path.lastIndexOf('/')
        return NativeSqliteDriver(
            DatabaseConfiguration(
                name = path.substring(slash + 1),
                // Skips the user_version check and write, and with it create and migrate.
                version = NO_VERSION_CHECK,
                create = {},
                // The files use the default rollback journal; asking for WAL would rewrite the header.
                journalMode = JournalMode.DELETE,
                extendedConfig = DatabaseConfiguration.Extended(basePath = path.substring(0, slash)),
                lifecycleConfig = DatabaseConfiguration.Lifecycle(
                    onCreateConnection = { connection ->
                        connection.withStatement("PRAGMA query_only = 1") { execute() }
                    },
                ),
            ),
        )
    }
}

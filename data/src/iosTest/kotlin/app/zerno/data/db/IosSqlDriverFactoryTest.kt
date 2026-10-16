package app.zerno.data.db

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseFileContext
import co.touchlab.sqliter.JournalMode
import co.touchlab.sqliter.NO_VERSION_CHECK
import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.sqliter.getVersion
import platform.Foundation.NSTemporaryDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class IosSqlDriverFactoryTest {
    private val factory = IosSqlDriverFactory()
    private val tempDir = NSTemporaryDirectory().trimEnd('/')
    private val bibleName = "test-kjv.SQLite3"

    @BeforeTest
    @AfterTest
    fun clean() {
        DatabaseFileContext.deleteDatabase(USER_DB_NAME)
        DatabaseFileContext.deleteDatabase(bibleName, tempDir)
    }

    @Test
    fun userDbKeepsDataAcrossReopen() {
        openUserDb(factory).appMetaQueries.put("bundled", "kjv,rst")

        val reopened = openUserDb(factory)
        assertEquals("kjv,rst", reopened.appMetaQueries.get("bundled").executeAsOne())
        assertNull(reopened.appMetaQueries.get("missing").executeAsOneOrNull())
    }

    @Test
    fun bibleFileOpensReadOnlyAndStaysUntouched() {
        writeBibleFixture()

        BibleFile.open("$tempDir/$bibleName", factory).use { bible ->
            assertEquals("en", bible.db.bibleQueries.infoValue("language").executeAsOne())
        }
        val driver = factory.createBibleDbDriver("$tempDir/$bibleName")
        assertFails { driver.execute(null, "INSERT INTO info VALUES ('x', 'y')", 0) }
        driver.close()

        assertEquals(0, bibleUserVersion())
    }

    @Test
    fun missingBibleFileFails() {
        assertFails { factory.createBibleDbDriver("$tempDir/$bibleName") }
    }

    private fun bibleManager() = createDatabaseManager(
        DatabaseConfiguration(
            name = bibleName,
            version = NO_VERSION_CHECK,
            create = {},
            journalMode = JournalMode.DELETE,
            extendedConfig = DatabaseConfiguration.Extended(basePath = tempDir),
        ),
    )

    private fun writeBibleFixture() {
        val connection = bibleManager().createMultiThreadedConnection()
        connection.rawExecSql("CREATE TABLE info (name TEXT, value TEXT)")
        connection.rawExecSql("INSERT INTO info VALUES ('language', 'en')")
        connection.close()
    }

    private fun bibleUserVersion(): Int {
        val connection = bibleManager().createMultiThreadedConnection()
        return connection.getVersion().also { connection.close() }
    }
}

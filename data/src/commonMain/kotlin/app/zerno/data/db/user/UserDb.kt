package app.zerno.data.db.user

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * The user's own data in one file. Every schema change bumps [version] and adds a migration (auto-migrations
 * read the exported schemas in `data/schemas`, so commit those with the change).
 */
@Database(entities = [AppMeta::class], version = 1, exportSchema = true)
@ConstructedBy(UserDbConstructor::class)
abstract class UserDb : RoomDatabase() {
    abstract fun appMetaDao(): AppMetaDao
}

// Room's compiler generates the actual for each target.
@Suppress("KotlinNoActualForExpect")
expect object UserDbConstructor : RoomDatabaseConstructor<UserDb> {
    override fun initialize(): UserDb
}

const val USER_DB_NAME = "user.db"

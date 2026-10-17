package app.zerno.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import app.zerno.data.db.user.USER_DB_NAME
import app.zerno.data.db.user.UserDb
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Keeps the database in Application Support: backed up, but not visible in the Files app. */
@OptIn(ExperimentalForeignApi::class)
fun userDbBuilder(): RoomDatabase.Builder<UserDb> {
    val dir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val path = requireNotNull(dir?.path) { "No Application Support directory" }
    return Room.databaseBuilder<UserDb>(name = "$path/$USER_DB_NAME")
}

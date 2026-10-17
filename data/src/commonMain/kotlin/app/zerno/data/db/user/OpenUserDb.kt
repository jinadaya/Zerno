package app.zerno.data.db.user

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** Finishes the platform builder (`userDbBuilder`) the same way on both platforms. Keep one instance per app. */
fun openUserDb(builder: RoomDatabase.Builder<UserDb>): UserDb = builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()

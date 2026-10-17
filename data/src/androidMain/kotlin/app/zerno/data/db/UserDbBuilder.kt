package app.zerno.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import app.zerno.data.db.user.USER_DB_NAME
import app.zerno.data.db.user.UserDb

fun userDbBuilder(context: Context): RoomDatabase.Builder<UserDb> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<UserDb>(
        context = appContext,
        name = appContext.getDatabasePath(USER_DB_NAME).absolutePath,
    )
}

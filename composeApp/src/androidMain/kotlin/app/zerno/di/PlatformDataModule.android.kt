package app.zerno.di

import android.content.Context
import app.zerno.data.db.bible.TranslationStore
import app.zerno.data.db.bibleDirectory
import app.zerno.data.db.user.UserDb
import app.zerno.data.db.user.openUserDb
import app.zerno.data.db.userDbBuilder
import app.zerno.data.prefs.AppPreferences
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single { openUserDb(userDbBuilder(androidContext())) }
    single { TranslationStore(bibleDirectory(androidContext()), get<UserDb>().appMetaDao()) }
    single {
        AppPreferences(
            SharedPreferencesSettings(androidContext().getSharedPreferences("zerno.prefs", Context.MODE_PRIVATE)),
        )
    }
}

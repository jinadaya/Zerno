package app.zerno.di

import app.zerno.data.db.bible.TranslationStore
import app.zerno.data.db.bibleDirectory
import app.zerno.data.db.user.UserDb
import app.zerno.data.db.user.openUserDb
import app.zerno.data.db.userDbBuilder
import app.zerno.data.prefs.AppPreferences
import com.russhwolf.settings.NSUserDefaultsSettings
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformDataModule: Module = module {
    single { openUserDb(userDbBuilder()) }
    single { TranslationStore(bibleDirectory(), get<UserDb>().appMetaDao()) }
    single { AppPreferences(NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)) }
}

package app.zerno.feature.reader.di

import app.zerno.data.db.user.UserDb
import app.zerno.feature.reader.api.ReaderRoute
import app.zerno.feature.reader.data.AppMetaReaderPrefs
import app.zerno.feature.reader.data.StoreBibleSource
import app.zerno.feature.reader.domain.BibleSource
import app.zerno.feature.reader.domain.ReaderPrefs
import app.zerno.feature.reader.domain.ReaderSession
import app.zerno.feature.reader.ui.BookPickerRoute
import app.zerno.feature.reader.ui.BookPickerScreen
import app.zerno.feature.reader.ui.BookPickerViewModel
import app.zerno.feature.reader.ui.ReaderScreen
import app.zerno.feature.reader.ui.ReaderViewModel
import com.royaal.navigation.screen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The feature's Koin module, loaded by `initKoin()` in `:composeApp`. Needs `UserDb` and `TranslationStore` from
 * the app's platform module.
 */
val readerModule = module {
    single<BibleSource> { StoreBibleSource(get()) }
    single<ReaderPrefs> { AppMetaReaderPrefs(get<UserDb>().appMetaDao()) }
    single { ReaderSession(get(), get()) }

    viewModelOf(::ReaderViewModel)
    viewModelOf(::BookPickerViewModel)

    // The reader is a quiet page: the app shell hides the bottom bar over it, and back returns to Today.
    screen<ReaderRoute>(showBottomBar = false) { ReaderScreen(koinViewModel()) }
    screen<BookPickerRoute>(showBottomBar = false) { BookPickerScreen(koinViewModel()) }
}

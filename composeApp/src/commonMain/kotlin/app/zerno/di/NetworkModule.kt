package app.zerno.di

import app.zerno.data.db.bible.TranslationDownloads
import app.zerno.data.db.user.UserDb
import app.zerno.data.net.ApiConfig
import app.zerno.data.net.AuthTokenProvider
import app.zerno.data.net.HealthApi
import app.zerno.data.net.TranslationsApi
import app.zerno.data.net.zernoHttpClient
import app.zerno.debug.isDebugBuild
import org.koin.dsl.module

/** Production backend. A placeholder until the backend is deployed (release checklist). */
private const val RELEASE_BASE_URL = "https://api.zerno.app/"

/**
 * The backend as seen from a simulator or emulator on the developer's machine (`./gradlew run` in the backend repo
 * serves on port 8080): `10.0.2.2` on the Android emulator, `localhost` on the iOS simulator.
 */
expect val localBackendUrl: String

/** Debug builds talk to a local backend and log requests; release builds talk to production quietly. */
val apiConfig: ApiConfig
    get() = if (isDebugBuild) ApiConfig(localBackendUrl, logging = true) else ApiConfig(RELEASE_BASE_URL)

/**
 * The backend client and what talks to it. `AuthTokenProvider.None` until the account feature (AC-01) binds its own.
 */
val networkModule = module {
    single { apiConfig }
    single<AuthTokenProvider> { AuthTokenProvider.None }
    single { zernoHttpClient(get(), get()) }
    single { HealthApi(get()) }
    single { TranslationsApi(get()) }
    single { TranslationDownloads(get(), get(), get<UserDb>().appMetaDao()) }
}

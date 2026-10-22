package app.zerno.di

import app.zerno.data.prefs.AppPreferences
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/** iOS only: on the Android host test there is no Context for SharedPreferences. */
class PlatformDataModuleTest {
    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun preferencesResolve() {
        initKoin()

        assertNotNull(KoinPlatformTools.defaultContext().get().get<AppPreferences>())
    }
}

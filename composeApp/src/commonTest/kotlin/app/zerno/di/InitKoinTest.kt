package app.zerno.di

import app.zerno.data.time.TimeZoneProvider
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock

class InitKoinTest {
    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun startsWithEveryFeatureModule() {
        initKoin()

        assertNotNull(KoinPlatformTools.defaultContext().getOrNull())
        assertEquals(9, featureModules.toSet().size)
    }

    @Test
    fun clockAndTimeZoneResolve() {
        initKoin()
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<Clock>())
        assertNotNull(koin.get<TimeZoneProvider>().current())
    }
}

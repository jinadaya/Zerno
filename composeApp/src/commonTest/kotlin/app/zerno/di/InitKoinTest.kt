package app.zerno.di

import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InitKoinTest {
    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun startsWithEveryFeatureModule() {
        initKoin()

        assertNotNull(KoinPlatformTools.defaultContext().getOrNull())
        assertEquals(9, featureModules.toSet().size)
    }
}

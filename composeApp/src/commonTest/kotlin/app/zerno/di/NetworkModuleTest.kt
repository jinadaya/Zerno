package app.zerno.di

import app.zerno.data.net.HealthApi
import io.ktor.client.HttpClient
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatformTools
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NetworkModuleTest {
    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun clientAndHealthApiResolve() {
        initKoin()
        val koin = KoinPlatformTools.defaultContext().get()

        assertNotNull(koin.get<HttpClient>())
        assertNotNull(koin.get<HealthApi>())
    }

    @Test
    fun baseUrlEndsWithASlashSoRelativePathsAppend() {
        assertTrue(apiConfig.baseUrl.endsWith("/"))
    }
}

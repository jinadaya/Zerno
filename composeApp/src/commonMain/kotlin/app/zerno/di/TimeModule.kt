package app.zerno.di

import app.zerno.data.time.TimeZoneProvider
import org.koin.dsl.module
import kotlin.time.Clock

/** The real clock and the device's time zone. Rules take them from Koin, tests and the debug menu swap them. */
val timeModule = module {
    single<Clock> { Clock.System }
    single<TimeZoneProvider> { TimeZoneProvider.System }
}

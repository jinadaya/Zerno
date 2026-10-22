package app.zerno.data.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * The time zone the user is in now, asked at each use: travel and DST change it while the app runs. Injected
 * through Koin next to `kotlin.time.Clock`, so streak and plan rules never read the system directly and tests can
 * fix both.
 */
fun interface TimeZoneProvider {
    fun current(): TimeZone

    companion object {
        /** The device's zone. */
        val System: TimeZoneProvider = TimeZoneProvider { TimeZone.currentSystemDefault() }
    }
}

/** Today's date where the user is. */
fun Clock.today(zones: TimeZoneProvider): LocalDate = todayIn(zones.current())

/** A clock tests and the debug menu move by hand. */
class FakeClock(var now: Instant) : Clock {
    override fun now(): Instant = now

    fun advanceBy(duration: Duration) {
        now += duration
    }
}

/** A time zone tests change by hand, to travel or cross a DST change. */
class FakeTimeZone(var zone: TimeZone) : TimeZoneProvider {
    override fun current(): TimeZone = zone
}

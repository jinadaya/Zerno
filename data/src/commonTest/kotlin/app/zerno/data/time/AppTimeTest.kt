package app.zerno.data.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class AppTimeTest {
    // 2026-03-01 23:30 in Moscow (UTC+3), 20:30 UTC.
    private val lateEvening = Instant.parse("2026-03-01T20:30:00Z")

    @Test
    fun todayFollowsTheUsersZone() {
        val clock = FakeClock(lateEvening)
        assertEquals(LocalDate(2026, 3, 1), clock.today(FakeTimeZone(TimeZone.of("Europe/Moscow"))))
        assertEquals(LocalDate(2026, 3, 2), clock.today(FakeTimeZone(TimeZone.of("Asia/Tokyo"))))
    }

    @Test
    fun movingTheClockPastMidnightChangesTheDay() {
        val clock = FakeClock(lateEvening)
        val moscow = FakeTimeZone(TimeZone.of("Europe/Moscow"))
        clock.advanceBy(1.hours)
        assertEquals(LocalDate(2026, 3, 2), clock.today(moscow))
    }

    @Test
    fun changingTheZoneIsSeenAtTheNextAsk() {
        val clock = FakeClock(lateEvening)
        val zones = FakeTimeZone(TimeZone.of("Europe/Moscow"))
        assertEquals(LocalDate(2026, 3, 1), clock.today(zones))
        zones.zone = TimeZone.of("Asia/Tokyo")
        assertEquals(LocalDate(2026, 3, 2), clock.today(zones))
    }
}

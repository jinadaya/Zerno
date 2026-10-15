package app.zerno.feature.gamification.domain.streak

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class StreakEngineTest {

    private val engine = StreakEngine()

    private val moscow = TimeZone.of("Europe/Moscow")
    private val tokyo = TimeZone.of("Asia/Tokyo")
    private val newYork = TimeZone.of("America/New_York")
    private val berlin = TimeZone.of("Europe/Berlin")

    private fun at(local: String, zone: TimeZone): Instant = LocalDateTime.parse(local).toInstant(zone)

    private fun read(local: String, zone: TimeZone = moscow) = ReadingCompleted(at(local, zone), zone)

    private fun reflect(local: String, zone: TimeZone = moscow) = ReflectionGiven(at(local, zone), zone)

    private fun summarize(
        readings: List<ReadingCompleted>,
        reflections: List<ReflectionGiven> = emptyList(),
        now: String,
        zone: TimeZone = moscow,
    ) = engine.summarize(readings, reflections, at(now, zone), zone)

    private fun date(value: String) = LocalDate.parse(value)

    @Test
    fun noEventsGiveEmptySummary() {
        val summary = summarize(emptyList(), now = "2026-10-14T12:00")

        assertEquals(0, summary.current)
        assertEquals(0, summary.longest)
        assertEquals(0, summary.daysReadThisWeek)
        assertEquals(emptyMap(), summary.days)
    }

    @Test
    fun readingsEitherSideOfMidnightAreTwoDays() {
        val summary = summarize(
            listOf(read("2026-10-13T23:59:59"), read("2026-10-14T00:00:00")),
            now = "2026-10-14T00:00:30",
        )

        assertEquals(DayStatus.Read, summary.days[date("2026-10-13")])
        assertEquals(DayStatus.Read, summary.days[date("2026-10-14")])
        assertEquals(2, summary.current)
    }

    @Test
    fun streakStaysAliveUntilEndOfNextDay() {
        val readings = listOf(read("2026-10-12T20:00"), read("2026-10-13T23:59"))

        assertEquals(2, summarize(readings, now = "2026-10-14T00:01").current)
        assertEquals(2, summarize(readings, now = "2026-10-14T23:59:59").current)
        assertEquals(0, summarize(readings, now = "2026-10-15T00:00").current)
    }

    @Test
    fun missedDayBreaksCurrentButKeepsLongest() {
        val summary = summarize(
            listOf(
                read("2026-10-01T08:00"),
                read("2026-10-02T08:00"),
                read("2026-10-03T08:00"),
                read("2026-10-05T08:00"),
                read("2026-10-06T08:00"),
            ),
            now = "2026-10-06T21:00",
        )

        assertEquals(2, summary.current)
        assertEquals(3, summary.longest)
    }

    @Test
    fun twoSessionsOnOneDayCountOnce() {
        val summary = summarize(
            listOf(read("2026-10-14T07:00"), read("2026-10-14T22:00")),
            now = "2026-10-14T23:00",
        )

        assertEquals(mapOf(date("2026-10-14") to DayStatus.Read), summary.days)
        assertEquals(1, summary.current)
        assertEquals(1, summary.daysReadThisWeek)
    }

    @Test
    fun reflectionOnSameDayMakesDayDone() {
        val summary = summarize(
            listOf(read("2026-10-14T07:00")),
            listOf(reflect("2026-10-14T07:10")),
            now = "2026-10-14T08:00",
        )

        assertEquals(DayStatus.Done, summary.days[date("2026-10-14")])
        assertEquals(1, summary.current)
    }

    @Test
    fun reflectionOnLaterDayDoesNotUpgradeEarlierDay() {
        val summary = summarize(
            listOf(read("2026-10-13T23:50")),
            listOf(reflect("2026-10-14T00:05")),
            now = "2026-10-14T00:10",
        )

        assertEquals(DayStatus.Read, summary.days[date("2026-10-13")])
        assertEquals(null, summary.days[date("2026-10-14")])
        assertEquals(1, summary.current)
    }

    @Test
    fun reflectionWithoutReadingDoesNotMarkDay() {
        val summary = summarize(
            emptyList(),
            listOf(reflect("2026-10-14T09:00")),
            now = "2026-10-14T10:00",
        )

        assertEquals(emptyMap(), summary.days)
        assertEquals(0, summary.current)
    }

    @Test
    fun travelEastKeepsEachDayOnItsLocalDate() {
        // Read at 22:00 in Moscow, which is already 04:00 next day in Tokyo; fly, then read in Tokyo.
        val summary = summarize(
            listOf(read("2026-10-12T22:00", moscow), read("2026-10-13T20:00", tokyo)),
            now = "2026-10-13T21:00",
            zone = tokyo,
        )

        assertEquals(setOf(date("2026-10-12"), date("2026-10-13")), summary.days.keys)
        assertEquals(2, summary.current)
    }

    @Test
    fun travelWestSameLocalDateCountsOnce() {
        // Read Tuesday morning in Tokyo, fly to New York and read again on Tuesday there.
        val summary = summarize(
            listOf(read("2026-10-13T08:00", tokyo), read("2026-10-13T20:00", newYork)),
            now = "2026-10-13T21:00",
            zone = newYork,
        )

        assertEquals(mapOf(date("2026-10-13") to DayStatus.Read), summary.days)
        assertEquals(1, summary.current)
    }

    @Test
    fun travelWestDoesNotLoseDayThatIsStillAheadOfToday() {
        // Monday and Tuesday read in Tokyo; landing in New York it is Monday evening again.
        val summary = summarize(
            listOf(read("2026-10-12T09:00", tokyo), read("2026-10-13T07:00", tokyo)),
            now = "2026-10-12T19:00",
            zone = newYork,
        )

        assertEquals(date("2026-10-12"), summary.today)
        assertEquals(2, summary.current)
    }

    @Test
    fun springForwardDstDaysStayConsecutive() {
        // Berlin skips 02:00 to 03:00 on 2026-03-29, so these readings are only 23 hours apart.
        val first = at("2026-03-28T23:30", berlin)
        val second = at("2026-03-29T23:30", berlin)
        assertEquals(23.hours, second - first)

        val summary = summarize(
            listOf(read("2026-03-28T23:30", berlin), read("2026-03-29T23:30", berlin)),
            now = "2026-03-29T23:45",
            zone = berlin,
        )

        assertEquals(2, summary.current)
    }

    @Test
    fun fallBackDstDaysStayConsecutive() {
        // Berlin repeats 02:00 to 03:00 on 2026-10-25, so these readings are 25 hours apart.
        val first = at("2026-10-24T00:10", berlin)
        val second = at("2026-10-25T00:10", berlin)
        val third = at("2026-10-26T00:10", berlin)
        assertEquals(25.hours, third - second)

        val summary = engine.summarize(
            listOf(first, second, third).map { ReadingCompleted(it, berlin) },
            emptyList(),
            now = third + 23.hours,
            zone = berlin,
        )

        assertEquals(3, summary.current)
        assertEquals(DayStatus.Empty, summary.week.first { it.date == date("2026-10-27") }.status)
    }

    @Test
    fun weekStartsOnMondayByDefault() {
        // 2026-10-14 is a Wednesday.
        val summary = summarize(
            listOf(
                read("2026-10-11T10:00"), // previous Sunday
                read("2026-10-12T10:00"),
                read("2026-10-14T10:00"),
            ),
            listOf(reflect("2026-10-14T10:05")),
            now = "2026-10-14T12:00",
        )

        assertEquals(date("2026-10-12"), summary.week.first().date)
        assertEquals(date("2026-10-18"), summary.week.last().date)
        assertEquals(
            listOf(
                DayStatus.Read,
                DayStatus.Empty,
                DayStatus.Done,
                DayStatus.Empty,
                DayStatus.Empty,
                DayStatus.Empty,
                DayStatus.Empty,
            ),
            summary.week.map { it.status },
        )
        assertEquals(2, summary.daysReadThisWeek)
    }

    @Test
    fun weekCanStartOnSunday() {
        val summary = StreakEngine(firstDayOfWeek = DayOfWeek.SUNDAY).summarize(
            listOf(read("2026-10-11T10:00"), read("2026-10-12T10:00")),
            emptyList(),
            now = at("2026-10-14T12:00", moscow),
            zone = moscow,
        )

        assertEquals(date("2026-10-11"), summary.week.first().date)
        assertEquals(2, summary.daysReadThisWeek)
    }
}

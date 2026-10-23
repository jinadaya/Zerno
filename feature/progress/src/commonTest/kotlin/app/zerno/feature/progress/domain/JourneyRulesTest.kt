package app.zerno.feature.progress.domain

import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.parseOrThrow
import app.zerno.entities.common.LocalizedText
import app.zerno.entities.plan.PlanDay
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.PlanType
import app.zerno.entities.plan.ReadingPlan
import app.zerno.entities.session.ReadingSession
import app.zerno.entities.session.SessionId
import app.zerno.feature.gamification.api.ReadingHistory
import app.zerno.feature.gamification.api.StreakDayStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** A plan of [length] days, day n reading John n. */
fun testPlan(id: String, length: Int = 7) = ReadingPlan(
    id = PlanId(id),
    type = PlanType.Sequential,
    title = LocalizedText(mapOf("en" to id, "ru" to id)),
    description = LocalizedText(mapOf("en" to "d", "ru" to "о")),
    days = (1..length).map { PlanDay(it, listOf(PassageRef.chapter(BookId(500), it)), 5) },
)

/** A session finished at [completedAt] in [zone], optionally for day [day] of [plan]. */
fun testSession(
    id: String,
    passage: String,
    completedAt: String,
    zone: String = "UTC",
    plan: String? = null,
    day: Int? = null,
): ReadingSession {
    val end = Instant.parse(completedAt)
    return ReadingSession(
        id = SessionId(id),
        passage = PassageRef.parseOrThrow(passage),
        translation = TranslationId("kjv"),
        planId = plan?.let(::PlanId),
        planDay = day,
        startedAt = end - 10.minutes,
        completedAt = end,
        timeZoneId = zone,
    )
}

class JourneyRulesTest {
    @Test
    fun chaptersReadTwiceCountOnceAndAShortBookCanBeFinished() {
        val history = ReadingHistory(
            sessions = listOf(
                testSession("3", "Ruth 1-4", "2026-10-03T08:00:00Z"),
                testSession("2", "John 1-2", "2026-10-02T08:00:00Z"),
                testSession("1", "John 1", "2026-10-01T08:00:00Z"),
            ),
        )

        val numbers = history.numbers()

        assertEquals(3, numbers.sessions)
        assertEquals(6, numbers.chaptersRead)
        assertEquals(1, numbers.booksFinished)
        assertEquals(66, numbers.coverage.size)
        assertEquals(BookId(10), numbers.coverage.first().book)
        val john = numbers.coverage.single { it.book == BookId(500) }
        assertEquals(2, john.chaptersRead)
        assertEquals(21, john.chapters)
        assertFalse(john.finished)
        assertTrue(numbers.coverage.single { it.book == BookId(80) }.finished)
    }

    @Test
    fun nothingReadMeansZeroesWithEveryBookListed() {
        val numbers = ReadingHistory().numbers()

        assertEquals(0, numbers.sessions)
        assertEquals(0, numbers.chaptersRead)
        assertEquals(0, numbers.booksFinished)
        assertEquals(1189, numbers.coverage.sumOf { it.chapters })
    }

    @Test
    fun historyUsesTheLocalDateOfTheSessionAndItsPlan() {
        val plan = testPlan("john")
        val history = ReadingHistory(
            sessions = listOf(
                // 23:30 in UTC is already the next day in Moscow.
                testSession("2", "John 2", "2026-10-01T23:30:00Z", zone = "Europe/Moscow", plan = "john", day = 2),
                testSession("1", "Psalms 23", "2026-10-01T08:00:00Z"),
            ),
        )

        val entries = history.entries(mapOf(plan.id to plan))

        assertEquals(listOf(SessionId("2"), SessionId("1")), entries.map { it.id })
        assertEquals(LocalDate(2026, 10, 2), entries[0].date)
        assertEquals(plan, entries[0].plan)
        assertEquals(LocalDate(2026, 10, 1), entries[1].date)
        assertNull(entries[1].plan)
    }

    @Test
    fun historyKeepsOnlyTheNewestSessions() {
        val history = ReadingHistory(
            sessions = (1..5).map { testSession("$it", "John $it", "2026-10-0${it}T08:00:00Z") },
        )

        assertEquals(listOf(SessionId("1"), SessionId("2")), history.entries(emptyMap(), limit = 2).map { it.id })
    }

    @Test
    fun aMonthStartsUnderItsWeekdayAndMarksReadDoneAndToday() {
        val today = LocalDate(2026, 10, 23)
        val days = mapOf(
            LocalDate(2026, 10, 1) to StreakDayStatus.Read,
            LocalDate(2026, 10, 2) to StreakDayStatus.Done,
        )

        val calendar = monthCalendar(LocalDate(2026, 10, 15), today, days)

        assertEquals(LocalDate(2026, 10, 1), calendar.month)
        // 1 October 2026 is a Thursday: three blanks after Monday.
        assertEquals(3, calendar.leadingBlanks)
        assertEquals(31, calendar.days.size)
        assertEquals(StreakDayStatus.Read, calendar.days[0].status)
        assertEquals(StreakDayStatus.Done, calendar.days[1].status)
        assertEquals(StreakDayStatus.Empty, calendar.days[2].status)
        assertEquals(listOf(LocalDate(2026, 10, 23)), calendar.days.filter { it.isToday }.map { it.date })
        assertFalse(calendar.hasPrevious)
        assertFalse(calendar.hasNext)
    }

    @Test
    fun monthsGoBackToTheFirstReadingAndForwardToThisMonth() {
        val today = LocalDate(2026, 10, 23)
        val days = mapOf(LocalDate(2026, 8, 30) to StreakDayStatus.Read)

        val august = monthCalendar(LocalDate(2026, 8, 1), today, days)
        val september = monthCalendar(LocalDate(2026, 9, 1), today, days)

        assertFalse(august.hasPrevious)
        assertTrue(august.hasNext)
        assertTrue(september.hasPrevious)
        assertTrue(september.hasNext)
        assertEquals(LocalDate(2026, 9, 1), LocalDate(2026, 8, 30).nextMonth())
        assertEquals(LocalDate(2026, 7, 1), LocalDate(2026, 8, 30).previousMonth())
    }

    @Test
    fun februaryInALeapYearHas29Days() {
        val calendar = monthCalendar(LocalDate(2028, 2, 1), LocalDate(2028, 2, 10), emptyMap())

        assertEquals(29, calendar.days.size)
        // 1 February 2028 is a Tuesday.
        assertEquals(1, calendar.leadingBlanks)
    }

    @Test
    fun datesReadInTheUiLanguage() {
        assertEquals("2 Oct 2026", LocalDate(2026, 10, 2).shortText("en"))
        assertEquals("2 окт. 2026", LocalDate(2026, 10, 2).shortText("ru"))
        assertEquals("October 2026", LocalDate(2026, 10, 2).monthTitle("en"))
        assertEquals("Октябрь 2026", LocalDate(2026, 10, 2).monthTitle("ru"))
    }
}

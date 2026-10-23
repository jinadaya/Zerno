package app.zerno.feature.progress.domain

import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.BookNames
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan
import app.zerno.entities.session.SessionId
import app.zerno.feature.gamification.api.ReadingHistory
import app.zerno.feature.gamification.api.StreakDayStatus
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.plans.api.PlanRunStatus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** How much of one book has been read, in the KJV chapter count that achievements use. */
data class BookCoverage(val book: BookId, val chaptersRead: Int, val chapters: Int) {
    val finished: Boolean get() = chaptersRead >= chapters

    val fraction: Float get() = chaptersRead.toFloat() / chapters
}

/** The Journey tab's totals. */
data class JourneyNumbers(
    val sessions: Int = 0,
    val chaptersRead: Int = 0,
    val booksFinished: Int = 0,
    /** Every book in canonical order. */
    val coverage: List<BookCoverage> = emptyList(),
)

/** Counts the same way achievements do: a chapter read twice counts once, so 1189 chapters is the whole Bible. */
fun ReadingHistory.numbers(): JourneyNumbers {
    val chapters = sessions.flatMapTo(HashSet()) { it.passage.chapters }
    val byBook = chapters.groupingBy { it.book }.eachCount()
    val coverage = BookNames.all.map { book -> BookCoverage(book, byBook[book] ?: 0, BookNames.chapterCount(book)) }
    return JourneyNumbers(
        sessions = sessions.size,
        chaptersRead = chapters.size,
        booksFinished = coverage.count { it.finished },
        coverage = coverage,
    )
}

/** One past reading: when (in the zone it happened in), what, and the plan it was for. */
data class HistoryEntry(val id: SessionId, val date: LocalDate, val passage: PassageRef, val plan: ReadingPlan?)

/** The newest [limit] completed sessions, with their plans looked up in [plans]. */
fun ReadingHistory.entries(plans: Map<PlanId, ReadingPlan>, limit: Int = HistoryLimit): List<HistoryEntry> =
    sessions.take(limit).mapNotNull { session ->
        val completedAt = session.completedAt ?: return@mapNotNull null
        HistoryEntry(
            id = session.id,
            date = completedAt.toLocalDateTime(TimeZone.of(session.timeZoneId)).date,
            passage = session.passage,
            plan = session.planId?.let(plans::get),
        )
    }

private const val HistoryLimit = 30

/** The plan being read now, if any. */
val List<PlanOverview>.current: PlanOverview?
    get() = firstOrNull { it.status == PlanRunStatus.Active }

val PlanOverview.fraction: Float
    get() = completedDays.size.toFloat() / plan.length

data class CalendarDay(val date: LocalDate, val status: StreakDayStatus, val isToday: Boolean)

/**
 * One month as a grid: [leadingBlanks] empty cells before the 1st so it lands under its weekday, then every day.
 * [hasPrevious] stops at the month of the first reading and [hasNext] at the current month; there is nothing to show
 * before or after.
 */
data class MonthCalendar(
    /** The first day of the month. */
    val month: LocalDate,
    val leadingBlanks: Int,
    val days: List<CalendarDay>,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
)

fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, month, 1)

fun monthCalendar(
    month: LocalDate,
    today: LocalDate,
    days: Map<LocalDate, StreakDayStatus>,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
): MonthCalendar {
    val first = month.firstOfMonth()
    val length = first.daysUntil(first.plus(OneMonth))
    val earliest = (days.keys.minOrNull() ?: today).firstOfMonth()
    return MonthCalendar(
        month = first,
        leadingBlanks = (first.dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + DaysInWeek) % DaysInWeek,
        days = List(length) { index ->
            val date = first.plus(DatePeriod(days = index))
            CalendarDay(date, days[date] ?: StreakDayStatus.Empty, date == today)
        },
        hasPrevious = first > earliest,
        hasNext = first < today.firstOfMonth(),
    )
}

fun LocalDate.previousMonth(): LocalDate = firstOfMonth().minus(OneMonth)

fun LocalDate.nextMonth(): LocalDate = firstOfMonth().plus(OneMonth)

private val OneMonth = DatePeriod(months = 1)

private const val DaysInWeek = 7

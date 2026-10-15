package app.zerno.feature.gamification.domain.streak

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Streak rules.
 *
 * - Every event belongs to the local date of the zone it happened in, so travel never moves past days.
 * - A day is [DayStatus.Read] once a reading is completed on it, and [DayStatus.Done] once a reflection
 *   is also given on that same local date. A reflection on a later day does not upgrade an earlier day,
 *   and a reflection alone does not mark a day.
 * - The current streak stays alive until the end of the day after the last read day. If travel west put
 *   the last read day after today's date, the streak is counted from that day.
 */
class StreakEngine(
    private val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
) {

    fun dayStatuses(
        readings: Collection<ReadingCompleted>,
        reflections: Collection<ReflectionGiven>,
    ): Map<LocalDate, DayStatus> {
        val reflected = reflections.mapTo(HashSet()) { it.localDate }
        return readings
            .mapTo(HashSet()) { it.localDate }
            .associateWith { date -> if (date in reflected) DayStatus.Done else DayStatus.Read }
    }

    fun summarize(
        readings: Collection<ReadingCompleted>,
        reflections: Collection<ReflectionGiven>,
        now: Instant,
        zone: TimeZone,
    ): StreakSummary {
        val today = now.toLocalDateTime(zone).date
        val days = dayStatuses(readings, reflections)
        val week = weekOf(today).map { date -> DayMark(date, days[date] ?: DayStatus.Empty) }
        return StreakSummary(
            today = today,
            days = days,
            current = currentStreak(days.keys, today),
            longest = longestStreak(days.keys),
            week = week,
            daysReadThisWeek = week.count { it.status.countsForStreak },
        )
    }

    private fun currentStreak(readDays: Set<LocalDate>, today: LocalDate): Int {
        val last = readDays.maxOrNull() ?: return 0
        if (last < today.minus(ONE_DAY)) return 0
        var count = 0
        var date = last
        while (date in readDays) {
            count++
            date = date.minus(ONE_DAY)
        }
        return count
    }

    private fun longestStreak(readDays: Set<LocalDate>): Int {
        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        for (date in readDays.sorted()) {
            run = if (previous != null && previous.plus(ONE_DAY) == date) run + 1 else 1
            longest = maxOf(longest, run)
            previous = date
        }
        return longest
    }

    private fun weekOf(today: LocalDate): List<LocalDate> {
        val offset = (today.dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + 7) % 7
        val start = today.minus(DatePeriod(days = offset))
        return List(7) { start.plus(DatePeriod(days = it)) }
    }

    private companion object {
        val ONE_DAY = DatePeriod(days = 1)
    }
}

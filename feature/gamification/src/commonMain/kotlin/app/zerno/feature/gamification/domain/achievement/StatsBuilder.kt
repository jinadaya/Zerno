package app.zerno.feature.gamification.domain.achievement

import app.zerno.entities.bible.BookNames
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.session.ReadingSession
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

/** Counts of the actions other features report, by kind. */
data class ActivityCounts(
    val reflections: Int = 0,
    val planStepReflections: Int = 0,
    val versesSaved: Int = 0,
    val questionsAnswered: Int = 0,
    val plansCompleted: Int = 0,
    val wholeBiblePlansCompleted: Int = 0,
)

/** Builds the evaluator's snapshot from stored history. */
class StatsBuilder(private val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY) {
    /**
     * @param sessions completed sessions only.
     * @param readDays local dates with a completed reading, as the streak engine sees them.
     */
    fun build(
        sessions: List<ReadingSession>,
        activity: ActivityCounts,
        readDays: Set<LocalDate>,
        currentStreak: Int,
        longestStreak: Int,
    ): AchievementStats {
        // Re-reading a chapter does not count twice: 1189 chapters means the whole Bible.
        val chapters: Set<ChapterRef> = sessions.flatMapTo(HashSet()) { it.passage.chapters }
        val chaptersByBook = chapters.groupBy { it.book }
        return AchievementStats(
            sessionsCompleted = sessions.size,
            chaptersRead = chapters.size,
            booksFinished = chaptersByBook.filter { (book, read) -> read.size >= BookNames.chapterCount(book) }.keys,
            booksStarted = chaptersByBook.keys,
            plansCompleted = activity.plansCompleted,
            wholeBiblePlansCompleted = activity.wholeBiblePlansCompleted,
            reflectionsWritten = activity.reflections,
            planStepReflections = activity.planStepReflections,
            versesSaved = activity.versesSaved,
            questionsAnswered = activity.questionsAnswered,
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            steadyWeeks = steadyWeeks(readDays),
            longestReturnGapDays = longestReturnGap(readDays),
        )
    }

    /** Weeks with reading on at least [STEADY_DAYS] days. */
    fun steadyWeeks(readDays: Set<LocalDate>): Int =
        readDays.groupBy { weekStart(it) }.count { (_, days) -> days.size >= STEADY_DAYS }

    /** The most days without reading that ended with a reading. */
    fun longestReturnGap(readDays: Set<LocalDate>): Int =
        readDays.sorted().zipWithNext { a, b -> a.daysUntil(b) - 1 }.maxOrNull() ?: 0

    private fun weekStart(date: LocalDate): LocalDate =
        date.minus(DatePeriod(days = (date.dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + 7) % 7))

    private companion object {
        const val STEADY_DAYS = 4
    }
}

package app.zerno.feature.gamification.domain.achievement

import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.parseOrThrow
import app.zerno.entities.session.ReadingSession
import app.zerno.entities.session.SessionId
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class StatsBuilderTest {
    private val builder = StatsBuilder()
    private val at = Instant.parse("2026-03-02T07:00:00Z")

    private fun build(vararg passages: String) = builder.build(
        sessions = passages.mapIndexed { i, p ->
            ReadingSession(SessionId("s$i"), PassageRef.parseOrThrow(p), TranslationId("kjv"), startedAt = at, completedAt = at, timeZoneId = "UTC")
        },
        activity = ActivityCounts(),
        readDays = emptySet(),
        currentStreak = 0,
        longestStreak = 0,
    )

    @Test
    fun rereadingAChapterCountsOnce() {
        val stats = build("John 3:16", "John 3", "John 3-4")
        assertEquals(3, stats.sessionsCompleted)
        assertEquals(2, stats.chaptersRead)
    }

    @Test
    fun bookIsFinishedWhenEveryChapterIsRead() {
        val stats = build("Jude", "Ruth 1-3", "Gen 1")
        // Jude has one chapter; Ruth has four.
        assertEquals(setOf(BookId(720)), stats.booksFinished)
        assertEquals(3, stats.booksStarted.size)

        val ruth = build("Ruth 1-3", "Ruth 4")
        assertEquals(setOf(BookId(80)), ruth.booksFinished)
    }

    @Test
    fun steadyWeeksNeedFourDaysInOneWeek() {
        // Mon 2 March to Sun 8 March 2026 is one week; Mon 9 starts the next.
        val days = setOf(2, 3, 4, 5, 9, 10, 11).map { LocalDate(2026, 3, it) }.toSet()
        assertEquals(1, builder.steadyWeeks(days))
        assertEquals(2, builder.steadyWeeks(days + LocalDate(2026, 3, 12)))
    }

    @Test
    fun returnGapIsTheLongestBreakBetweenReadings() {
        assertEquals(0, builder.longestReturnGap(emptySet()))
        assertEquals(0, builder.longestReturnGap(setOf(LocalDate(2026, 3, 2), LocalDate(2026, 3, 3))))
        assertEquals(7, builder.longestReturnGap(setOf(LocalDate(2026, 3, 2), LocalDate(2026, 3, 10), LocalDate(2026, 3, 12))))
    }
}

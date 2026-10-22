package app.zerno.feature.gamification.domain.achievement

import app.zerno.entities.achievement.AchievementCode
import app.zerno.entities.achievement.AchievementProgress
import app.zerno.entities.achievement.AchievementSet
import app.zerno.entities.bible.BookId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class AchievementEvaluatorTest {
    private val now = Instant.parse("2026-03-01T10:00:00Z")
    private val later = Instant.parse("2026-03-09T10:00:00Z")

    @Test fun sessionsCompleted() = check(
        CriteriaType.SessionsCompleted,
        3,
        AchievementStats(sessionsCompleted = 2),
        AchievementStats(sessionsCompleted = 3),
    )

    @Test fun chaptersRead() =
        check(CriteriaType.ChaptersRead, 50, AchievementStats(chaptersRead = 12), AchievementStats(chaptersRead = 51))

    @Test fun booksFinished() = check(
        CriteriaType.BooksFinished,
        2,
        AchievementStats(booksFinished = setOf(MARK)),
        AchievementStats(booksFinished = setOf(MARK, GENESIS)),
    )

    @Test
    fun booksInGroupCountsOnlyBooksOfTheGroup() = check(
        CriteriaType.BooksInGroup,
        2,
        below = AchievementStats(booksFinished = setOf(MARK, GENESIS, ROMANS)),
        reached = AchievementStats(booksFinished = setOf(MARK, JOHN)),
        group = "gospels",
    )

    @Test fun distinctBooks() = check(
        CriteriaType.DistinctBooks,
        3,
        AchievementStats(booksStarted = setOf(MARK, JOHN)),
        AchievementStats(booksStarted = setOf(MARK, JOHN, GENESIS)),
    )

    @Test
    fun bothTestaments() = check(
        CriteriaType.BothTestaments,
        1,
        below = AchievementStats(booksStarted = setOf(MARK, JOHN, ROMANS)),
        reached = AchievementStats(booksStarted = setOf(MARK, GENESIS)),
        counted = false,
    )

    @Test fun plansCompleted() = check(
        CriteriaType.PlansCompleted,
        3,
        AchievementStats(plansCompleted = 1),
        AchievementStats(plansCompleted = 3),
    )

    @Test
    fun wholeBiblePlan() = check(
        CriteriaType.WholeBiblePlan,
        1,
        AchievementStats(plansCompleted = 5),
        AchievementStats(wholeBiblePlansCompleted = 1),
        counted = false,
    )

    @Test fun reflectionsWritten() = check(
        CriteriaType.ReflectionsWritten,
        10,
        AchievementStats(reflectionsWritten = 9),
        AchievementStats(reflectionsWritten = 10),
    )

    @Test
    fun planStepReflections() = check(
        CriteriaType.PlanStepReflections,
        1,
        AchievementStats(reflectionsWritten = 4),
        AchievementStats(planStepReflections = 1),
    )

    @Test fun versesSaved() =
        check(CriteriaType.VersesSaved, 10, AchievementStats(versesSaved = 3), AchievementStats(versesSaved = 12))

    @Test fun questionsAnswered() = check(
        CriteriaType.QuestionsAnswered,
        5,
        AchievementStats(questionsAnswered = 4),
        AchievementStats(questionsAnswered = 5),
    )

    @Test
    fun streakDaysUsesTheLongestStreak() = check(
        CriteriaType.StreakDays,
        7,
        below = AchievementStats(currentStreakDays = 6, longestStreakDays = 6),
        // The streak broke after reaching 7: the achievement is still earned.
        reached = AchievementStats(currentStreakDays = 0, longestStreakDays = 7),
    )

    @Test fun steadyWeeks() =
        check(CriteriaType.SteadyWeeks, 4, AchievementStats(steadyWeeks = 3), AchievementStats(steadyWeeks = 4))

    @Test
    fun returnAfterDays() = check(
        CriteriaType.ReturnAfterDays,
        7,
        AchievementStats(longestReturnGapDays = 6),
        AchievementStats(longestReturnGapDays = 9),
        counted = false,
    )

    @Test
    fun everyCriteriaTypeIsCovered() {
        val tested = setOf(
            CriteriaType.SessionsCompleted, CriteriaType.ChaptersRead, CriteriaType.BooksFinished,
            CriteriaType.BooksInGroup, CriteriaType.DistinctBooks, CriteriaType.BothTestaments,
            CriteriaType.PlansCompleted, CriteriaType.WholeBiblePlan, CriteriaType.ReflectionsWritten,
            CriteriaType.PlanStepReflections, CriteriaType.VersesSaved, CriteriaType.QuestionsAnswered,
            CriteriaType.StreakDays, CriteriaType.SteadyWeeks, CriteriaType.ReturnAfterDays,
        )
        assertEquals(CriteriaType.entries.toSet(), tested)
    }

    @Test
    fun neverUnlocksTwice() {
        val evaluator = evaluator(CriteriaType.ChaptersRead, 10)
        val first = evaluator.evaluate(AchievementStats(chaptersRead = 10), emptyMap(), now)
        assertEquals(listOf(CODE), first.newlyUnlocked.map { it.code })

        val second = evaluator.evaluate(AchievementStats(chaptersRead = 30), first.unlockedAt, later)
        assertTrue(second.newlyUnlocked.isEmpty())
        // The first date is kept.
        assertEquals(now, second.achievements.single().unlockedAt)
    }

    @Test
    fun unlockIsPermanentWhenStatsDrop() {
        val evaluator = evaluator(CriteriaType.VersesSaved, 10)
        // The user removed saved verses after unlocking.
        val result = evaluator.evaluate(AchievementStats(versesSaved = 2), mapOf(CODE to now), later)

        assertEquals(now, result.achievements.single().unlockedAt)
        assertTrue(result.newlyUnlocked.isEmpty())
    }

    @Test
    fun newUserHasNothingUnlocked() {
        val catalog =
            AchievementCatalog(
                1,
                listOf(
                    definition(CriteriaType.ChaptersRead, 50),
                    definition(CriteriaType.StreakDays, 3, code = "streak_3"),
                ),
            )
        val result = AchievementEvaluator(catalog).evaluate(AchievementStats(), emptyMap(), now)

        assertEquals(listOf("chapters", "streak_3"), result.achievements.map { it.code.value })
        assertTrue(result.achievements.none { it.isUnlocked })
        assertTrue(result.unlockedAt.isEmpty())
    }

    private fun check(
        type: CriteriaType,
        threshold: Int,
        below: AchievementStats,
        reached: AchievementStats,
        group: String? = null,
        counted: Boolean = true,
    ) {
        val evaluator = evaluator(type, threshold, group)

        val locked = evaluator.evaluate(below, emptyMap(), now)
        val lockedAchievement = locked.achievements.single()
        assertNull(lockedAchievement.unlockedAt, "$type should be locked below the threshold")
        assertTrue(locked.newlyUnlocked.isEmpty())
        if (counted) {
            assertEquals(
                AchievementProgress(below.count(Criteria(type, threshold, group)), threshold),
                lockedAchievement.progress,
            )
        } else {
            assertNull(lockedAchievement.progress)
        }

        val unlocked = evaluator.evaluate(reached, emptyMap(), now)
        val unlockedAchievement = unlocked.achievements.single()
        assertEquals(now, unlockedAchievement.unlockedAt, "$type should unlock at the threshold")
        assertEquals(listOf(unlockedAchievement), unlocked.newlyUnlocked)
        if (counted) assertEquals(AchievementProgress(threshold, threshold), unlockedAchievement.progress)
    }

    private fun evaluator(type: CriteriaType, threshold: Int, group: String? = null) =
        AchievementEvaluator(AchievementCatalog(1, listOf(definition(type, threshold, group))))

    private fun definition(type: CriteriaType, threshold: Int, group: String? = null, code: String = CODE.value) =
        AchievementDefinition(
            code = code,
            set = AchievementSet.Sheep,
            titleKey = "achievement_${code}_title",
            descriptionKey = "achievement_${code}_description",
            criteria = Criteria(type, threshold, group),
            icon = "seal",
        )

    private companion object {
        val CODE = AchievementCode("chapters")
        val GENESIS = BookId(10)
        val MARK = BookId(480)
        val JOHN = BookId(500)
        val ROMANS = BookId(520)
    }
}

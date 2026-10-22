package app.zerno.feature.gamification.domain.achievement

import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.Testament

/**
 * Everything the evaluator counts, gathered from the user's history. Counts only grow, except [currentStreakDays],
 * which the evaluator never reads: streak achievements use [longestStreakDays] so a broken streak keeps them.
 */
data class AchievementStats(
    val sessionsCompleted: Int = 0,
    val chaptersRead: Int = 0,
    /** Books with every chapter read. */
    val booksFinished: Set<BookId> = emptySet(),
    /** Books with at least one chapter read. */
    val booksStarted: Set<BookId> = emptySet(),
    val plansCompleted: Int = 0,
    val wholeBiblePlansCompleted: Int = 0,
    val reflectionsWritten: Int = 0,
    val planStepReflections: Int = 0,
    val versesSaved: Int = 0,
    val questionsAnswered: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val steadyWeeks: Int = 0,
    /** The longest gap, in days, that ended with a reading. */
    val longestReturnGapDays: Int = 0,
) {
    /** The number [criteria] compares with its threshold. */
    fun count(criteria: Criteria): Int = when (criteria.type) {
        CriteriaType.SessionsCompleted -> sessionsCompleted
        CriteriaType.ChaptersRead -> chaptersRead
        CriteriaType.BooksFinished -> booksFinished.size
        CriteriaType.BooksInGroup -> booksFinished.count { it in BookGroups.byName[criteria.group].orEmpty() }
        CriteriaType.DistinctBooks -> booksStarted.size
        CriteriaType.BothTestaments -> if (booksStarted.map { it.testament }.toSet().size == Testament.entries.size) 1 else 0
        CriteriaType.PlansCompleted -> plansCompleted
        CriteriaType.WholeBiblePlan -> wholeBiblePlansCompleted
        CriteriaType.ReflectionsWritten -> reflectionsWritten
        CriteriaType.PlanStepReflections -> planStepReflections
        CriteriaType.VersesSaved -> versesSaved
        CriteriaType.QuestionsAnswered -> questionsAnswered
        CriteriaType.StreakDays -> longestStreakDays
        CriteriaType.SteadyWeeks -> steadyWeeks
        CriteriaType.ReturnAfterDays -> longestReturnGapDays
    }
}

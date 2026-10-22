package app.zerno.feature.gamification.domain.achievement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The three sets on the achievements screen, each with its mascot: Pip the bird, Woolly the sheep, Lumi the lamp. */
@Serializable
enum class AchievementSet {
    /** Habit and returning. */
    @SerialName("bird")
    Bird,

    /** Reading: chapters, books, plans. */
    @SerialName("sheep")
    Sheep,

    /** Reflection and understanding. */
    @SerialName("lamp")
    Lamp,
}

/**
 * What an achievement counts. Every type counts actions, never correct answers. The evaluator (GM-03) compares the
 * count from a stats snapshot with [Criteria.threshold].
 */
@Serializable
enum class CriteriaType {
    @SerialName("sessionsCompleted") SessionsCompleted,
    @SerialName("chaptersRead") ChaptersRead,
    @SerialName("booksFinished") BooksFinished,

    /** Books of [Criteria.group] read to the end. */
    @SerialName("booksInGroup") BooksInGroup,
    @SerialName("distinctBooks") DistinctBooks,

    /** 1 once both Testaments have a chapter read. */
    @SerialName("bothTestaments") BothTestaments,
    @SerialName("plansCompleted") PlansCompleted,
    @SerialName("wholeBiblePlan") WholeBiblePlan,
    @SerialName("reflectionsWritten") ReflectionsWritten,

    /** Reflections written on the prompt after a plan step. */
    @SerialName("planStepReflections") PlanStepReflections,
    @SerialName("versesSaved") VersesSaved,
    @SerialName("questionsAnswered") QuestionsAnswered,
    @SerialName("streakDays") StreakDays,

    /** Weeks with reading on at least 4 days. */
    @SerialName("steadyWeeks") SteadyWeeks,

    /** Days away before a reading that brings the user back. */
    @SerialName("returnAfterDays") ReturnAfterDays,
}

@Serializable
data class Criteria(
    val type: CriteriaType,
    val threshold: Int,
    /** Only for [CriteriaType.BooksInGroup]: a name from [BookGroups]. */
    val group: String? = null,
)

/**
 * One achievement as data, so new ones can ship from the backend without an app update. [titleKey] and
 * [descriptionKey] are string resource names in this feature; [icon] is an `SjIcons` name.
 */
@Serializable
data class AchievementDefinition(
    val code: String,
    val set: AchievementSet,
    val titleKey: String,
    val descriptionKey: String,
    val criteria: Criteria,
    val icon: String,
)

@Serializable
data class AchievementCatalog(
    val version: Int,
    val achievements: List<AchievementDefinition>,
)

package app.zerno.feature.gamification.domain.achievement

import app.zerno.entities.achievement.Achievement
import app.zerno.entities.achievement.AchievementCode
import app.zerno.entities.achievement.AchievementProgress
import kotlin.time.Instant

data class AchievementEvaluation(
    /** Every achievement in catalog order, unlocked or not. */
    val achievements: List<Achievement>,
    /** Achievements unlocked by this evaluation, dated [AchievementEvaluator.evaluate]'s `now`. */
    val newlyUnlocked: List<Achievement>,
) {
    /** All unlock dates, the earlier ones plus [newlyUnlocked], to store for the next evaluation. */
    val unlockedAt: Map<AchievementCode, Instant>
        get() = achievements.mapNotNull { a -> a.unlockedAt?.let { a.code to it } }.toMap()
}

/**
 * Turns a stats snapshot into achievements. Unlocks are permanent: an achievement in `unlocked` stays unlocked with
 * its first date even if the stats fall below the threshold, and is never reported as newly unlocked again.
 */
class AchievementEvaluator(private val catalog: AchievementCatalog) {
    fun evaluate(
        stats: AchievementStats,
        unlocked: Map<AchievementCode, Instant>,
        now: Instant,
    ): AchievementEvaluation {
        val newly = mutableListOf<Achievement>()
        val achievements = catalog.achievements.map { definition ->
            val code = AchievementCode(definition.code)
            val target = definition.criteria.threshold
            val current = stats.count(definition.criteria).coerceAtLeast(0)
            val earlier = unlocked[code]
            val unlockedAt = earlier ?: now.takeIf { current >= target }
            val achievement = Achievement(
                code = code,
                set = definition.set,
                titleKey = definition.titleKey,
                descriptionKey = definition.descriptionKey,
                icon = definition.icon,
                unlockedAt = unlockedAt,
                progress = if (definition.criteria.type in
                    Uncounted
                ) {
                    null
                } else {
                    AchievementProgress(current.coerceAtMost(target), target)
                },
            )
            if (earlier == null && unlockedAt != null) newly += achievement
            achievement
        }
        return AchievementEvaluation(achievements, newly)
    }

    private companion object {
        /** Yes-or-no criteria, where "0 of 1" would mean nothing to the user. */
        val Uncounted = setOf(CriteriaType.BothTestaments, CriteriaType.WholeBiblePlan, CriteriaType.ReturnAfterDays)
    }
}

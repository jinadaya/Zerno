package app.zerno.entities.achievement

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

/**
 * The three achievement sets, each with its mascot:
 * [Bird] is habit and returning (Pip), [Sheep] is reading (Woolly), [Lamp] is reflection (Lumi).
 */
@Serializable
enum class AchievementSet { Bird, Sheep, Lamp }

/** Stable code of an achievement definition, e.g. "first_steps" or "chapters_50". */
@Serializable
@JvmInline
value class AchievementCode(val value: String) {
    init {
        require(value.isNotBlank()) { "AchievementCode must not be blank" }
    }

    override fun toString(): String = value
}

/**
 * An achievement as the user sees it. Definitions and criteria are data owned by
 * `:feature:gamification` (GM-02); this is the shape shared with other features.
 *
 * @property titleKey string resource key for the title.
 * @property descriptionKey string resource key for the description.
 * @property icon icon name inside the set's seal artwork.
 * @property unlockedAt when it was unlocked, or null while locked. Unlocks are permanent.
 * @property progress how far along a locked achievement is, e.g. 12 of 50; null when not counted.
 */
@Serializable
data class Achievement(
    val code: AchievementCode,
    val set: AchievementSet,
    val titleKey: String,
    val descriptionKey: String,
    val icon: String,
    val unlockedAt: Instant? = null,
    val progress: AchievementProgress? = null,
) {
    val isUnlocked: Boolean
        get() = unlockedAt != null
}

/** Progress toward a threshold, shown as "[current] of [target]". */
@Serializable
data class AchievementProgress(
    val current: Int,
    val target: Int,
) {
    init {
        require(target > 0) { "Target must be positive, was $target" }
        require(current >= 0) { "Current must not be negative, was $current" }
    }

    val fraction: Float
        get() = (current.toFloat() / target).coerceAtMost(1f)
}

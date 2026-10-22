package app.zerno.entities.plan

import app.zerno.entities.bible.PassageRef
import app.zerno.entities.common.LocalizedText
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class PlanId(val value: String) {
    init {
        require(value.isNotBlank()) { "PlanId must not be blank" }
    }

    override fun toString(): String = value
}

@Serializable
enum class PlanType {
    /** Gentle start for new readers, e.g. Mark plus a few Psalms. */
    Introductory,

    /** One book or section front to back, e.g. the Gospel of John. */
    Sequential,

    /** Passages gathered around a theme, e.g. prayer. */
    Thematic,

    /** The whole Bible; completing one unlocks the Whole Bible achievement. */
    WholeBible,
}

/**
 * A reading plan as served by the backend and bundled for launch (PL-01).
 *
 * @property version content version; a higher number replaces the stored copy.
 */
@Serializable
data class ReadingPlan(
    val id: PlanId,
    val type: PlanType,
    val title: LocalizedText,
    val description: LocalizedText,
    val days: List<PlanDay>,
    val version: Int = 1,
) {
    init {
        require(days.isNotEmpty()) { "Plan $id has no days" }
        require(days.map { it.day } == (1..days.size).toList()) { "Plan $id days must be numbered 1 to ${days.size}" }
    }

    val length: Int
        get() = days.size
}

/**
 * One day of a plan.
 *
 * @property day 1-based day number inside the plan.
 * @property passages what to read that day, in order.
 * @property minutes estimated reading time.
 */
@Serializable
data class PlanDay(val day: Int, val passages: List<PassageRef>, val minutes: Int) {
    init {
        require(day > 0) { "Plan day must be positive, was $day" }
        require(passages.isNotEmpty()) { "Plan day $day has no passages" }
        require(minutes > 0) { "Plan day $day must take at least a minute" }
    }
}

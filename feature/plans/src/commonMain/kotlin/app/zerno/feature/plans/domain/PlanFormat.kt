package app.zerno.feature.plans.domain

import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.parse
import app.zerno.entities.common.LocalizedText
import app.zerno.entities.plan.PlanDay
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.PlanType
import app.zerno.entities.plan.ReadingPlan
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * A plan file as written by hand and later served by the backend (`files/plans/<id>.json`). Passages are
 * references in English book names and KJV numbering ("Mark 1", "Ps 23", "Matt 6:5-15"); the app maps them to
 * Synodal numbering for RST and NRT when it opens them.
 *
 * ```json
 * {
 *   "id": "gospel-of-john", "type": "Sequential", "version": 1,
 *   "title": {"en": "The Gospel of John", "ru": "Евангелие от Иоанна"},
 *   "description": {"en": "...", "ru": "..."},
 *   "days": [{"day": 1, "passages": ["John 1"], "minutes": 6}]
 * }
 * ```
 */
@Serializable
internal data class PlanFile(
    val id: String,
    val type: PlanType,
    val version: Int = 1,
    val title: LocalizedText,
    val description: LocalizedText,
    val days: List<PlanFileDay>,
)

@Serializable
internal data class PlanFileDay(
    val day: Int,
    val passages: List<String>,
    val minutes: Int,
)

/** A plan file that does not parse, or a reference in it that is not a passage. */
class InvalidPlanFile(message: String) : IllegalArgumentException(message)

/** Languages every plan must have a title and description in. */
val PlanLanguages: List<String> = listOf("en", "ru")

object PlanFormat {
    private val json = Json { ignoreUnknownKeys = true }

    /** Reads one plan file; throws [InvalidPlanFile] naming the problem. */
    fun parse(text: String): ReadingPlan {
        val file = try {
            json.decodeFromString<PlanFile>(text)
        } catch (e: SerializationException) {
            throw InvalidPlanFile(e.message ?: "not valid JSON")
        } catch (e: IllegalArgumentException) {
            throw InvalidPlanFile(e.message ?: "invalid plan")
        }
        PlanLanguages.forEach { language ->
            if (file.title.values[language].isNullOrBlank()) throw InvalidPlanFile("${file.id}: no title in $language")
            if (file.description.values[language].isNullOrBlank()) throw InvalidPlanFile("${file.id}: no description in $language")
        }
        val days = file.days.map { day ->
            val passages = day.passages.map { text ->
                PassageRef.parse(text) ?: throw InvalidPlanFile("${file.id} day ${day.day}: \"$text\" is not a passage")
            }
            runCatching { PlanDay(day.day, passages, day.minutes) }
                .getOrElse { throw InvalidPlanFile("${file.id}: ${it.message}") }
        }
        return runCatching { ReadingPlan(PlanId(file.id), file.type, file.title, file.description, days, file.version) }
            .getOrElse { throw InvalidPlanFile("${file.id}: ${it.message}") }
    }
}

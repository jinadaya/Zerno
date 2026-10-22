package app.zerno.feature.gamification.domain.achievement

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** A catalog that does not parse or breaks a rule; [problems] lists every one found. */
class InvalidAchievementCatalog(val problems: List<String>) :
    IllegalArgumentException("Invalid achievement catalog:\n" + problems.joinToString("\n") { "  $it" })

/** Reads the achievements JSON and checks it, so a broken file fails loudly instead of hiding achievements. */
object AchievementCatalogParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** Supported catalog format. A newer file needs an app that knows it. */
    const val VERSION = 1

    fun parse(text: String): AchievementCatalog {
        val catalog = try {
            json.decodeFromString<AchievementCatalog>(text)
        } catch (e: SerializationException) {
            throw InvalidAchievementCatalog(listOf(e.message ?: "not valid JSON"))
        }
        val problems = validate(catalog)
        if (problems.isNotEmpty()) throw InvalidAchievementCatalog(problems)
        return catalog
    }

    fun validate(catalog: AchievementCatalog): List<String> = buildList {
        if (catalog.version != VERSION) add("version ${catalog.version} is not $VERSION")
        catalog.achievements.groupBy {
            it.code
        }.filterValues { it.size > 1 }.keys.forEach { add("$it: code used twice") }
        catalog.achievements.forEach { a ->
            if (!a.code.matches(CODE)) add("${a.code}: code must be lower_snake_case")
            if (a.titleKey.isBlank()) add("${a.code}: no title key")
            if (a.descriptionKey.isBlank()) add("${a.code}: no description key")
            if (a.icon.isBlank()) add("${a.code}: no icon")
            if (a.criteria.threshold < 1) add("${a.code}: threshold must be at least 1")
            val group = a.criteria.group
            when {
                a.criteria.type == CriteriaType.BooksInGroup && group !in BookGroups.byName ->
                    add("${a.code}: booksInGroup needs a group from ${BookGroups.byName.keys}")

                a.criteria.type != CriteriaType.BooksInGroup && group != null ->
                    add("${a.code}: group is only for booksInGroup")

                a.criteria.type == CriteriaType.BooksInGroup &&
                    a.criteria.threshold > BookGroups.byName.getValue(group!!).size ->
                    add("${a.code}: threshold is more than the books in $group")
            }
        }
    }

    private val CODE = Regex("[a-z][a-z0-9_]*")
}

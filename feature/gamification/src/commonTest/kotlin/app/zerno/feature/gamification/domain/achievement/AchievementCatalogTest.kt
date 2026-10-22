package app.zerno.feature.gamification.domain.achievement

import app.zerno.entities.achievement.AchievementSet
import app.zerno.feature.gamification.data.BundledAchievementCatalog
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AchievementCatalogTest {
    @Test
    fun bundledCatalogLoadsAndHasEveryListedAchievement() = runTest {
        val catalog = BundledAchievementCatalog().load()
        val codes = catalog.achievements.map { it.code }.toSet()

        assertEquals(EXPECTED_CODES, codes)
        assertEquals(
            setOf(AchievementSet.Sheep, AchievementSet.Lamp, AchievementSet.Bird),
            catalog.achievements.map {
                it.set
            }.toSet(),
        )
        // Every criteria type is used by at least one achievement.
        assertEquals(CriteriaType.entries.toSet(), catalog.achievements.map { it.criteria.type }.toSet())
    }

    @Test
    fun validCatalogParses() {
        val catalog = AchievementCatalogParser.parse(
            catalog(achievement("first_steps", """{"type": "sessionsCompleted", "threshold": 1}""")),
        )
        assertEquals("first_steps", catalog.achievements.single().code)
        assertEquals(Criteria(CriteriaType.SessionsCompleted, 1), catalog.achievements.single().criteria)
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val json = """{"version": 1, "achievements": [], "servedBy": "backend"}"""
        assertTrue(AchievementCatalogParser.parse(json).achievements.isEmpty())
    }

    @Test
    fun brokenJsonFails() {
        assertFailsWith<InvalidAchievementCatalog> {
            AchievementCatalogParser.parse("""{"version": 1, "achievements": [""")
        }
    }

    @Test
    fun unknownCriteriaTypeFails() {
        assertFailsWith<InvalidAchievementCatalog> {
            AchievementCatalogParser.parse(catalog(achievement("x", """{"type": "versesMemorised", "threshold": 1}""")))
        }
    }

    @Test
    fun everyRuleIsReported() {
        val problems = assertFailsWith<InvalidAchievementCatalog> {
            AchievementCatalogParser.parse(
                catalog(
                    achievement("dup", """{"type": "chaptersRead", "threshold": 1}"""),
                    achievement("dup", """{"type": "chaptersRead", "threshold": 0}"""),
                    achievement("Bad-Code", """{"type": "streakDays", "threshold": 3, "group": "gospels"}"""),
                    achievement("gospels", """{"type": "booksInGroup", "threshold": 4}"""),
                    achievement("too_many", """{"type": "booksInGroup", "threshold": 5, "group": "gospels"}"""),
                    version = 2,
                ),
            )
        }.problems

        assertEquals(
            listOf(
                "version 2 is not 1",
                "dup: code used twice",
                "dup: threshold must be at least 1",
                "Bad-Code: code must be lower_snake_case",
                "Bad-Code: group is only for booksInGroup",
                "gospels: booksInGroup needs a group from [gospels]",
                "too_many: threshold is more than the books in gospels",
            ),
            problems,
        )
    }

    private fun catalog(vararg achievements: String, version: Int = 1) =
        """{"version": $version, "achievements": [${achievements.joinToString()}]}"""

    private fun achievement(code: String, criteria: String) =
        """{"code": "$code", "set": "Sheep", "titleKey": "t", "descriptionKey": "d", "criteria": $criteria, "icon": "Bible"}"""

    private companion object {
        val EXPECTED_CODES = setOf(
            // Sheep: reading
            "first_steps", "first_chapter", "chapters_10", "chapters_50", "chapters_100", "chapters_250",
            "chapters_500", "chapters_1189", "book_finished", "gospel_reader", "explorer_5", "explorer_10",
            "explorer_20", "old_and_new", "plan_finished", "plans_3", "plans_5", "plans_10", "whole_bible",
            // Lamp: reflection and understanding
            "first_light", "treasured_verse", "keeper_10", "keeper_50", "journal_10", "journal_50", "journal_100",
            "thoughtful_5", "thoughtful_25", "paused_to_think",
            // Bird: habit and returning
            "streak_3", "streak_7", "streak_14", "streak_30", "streak_100", "streak_365", "steady", "welcome_back",
        )
    }
}

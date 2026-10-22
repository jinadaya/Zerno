package app.zerno.entities

import app.zerno.entities.achievement.Achievement
import app.zerno.entities.achievement.AchievementCode
import app.zerno.entities.achievement.AchievementSet
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.Testament
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.common.LocalizedText
import app.zerno.entities.plan.PlanDay
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.PlanType
import app.zerno.entities.plan.ReadingPlan
import app.zerno.entities.session.ReadingSession
import app.zerno.entities.session.SessionId
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class EntitiesTest {

    private val json = Json
    private val john = BookId(500)

    @Test
    fun bookIdRejectsNumbersOutsideMyBibleRange() {
        assertFailsWith<IllegalArgumentException> { BookId(0) }
        assertFailsWith<IllegalArgumentException> { BookId(740) }
    }

    @Test
    fun bookIdKnowsItsTestament() {
        assertEquals(Testament.Old, BookId(230).testament)
        assertEquals(Testament.New, BookId(470).testament)
    }

    @Test
    fun passageRefDefaultsToOneWholeChapter() {
        val psalm = PassageRef.chapter(BookId(230), 23)
        assertEquals(23, psalm.endChapter)
        assertEquals(true, psalm.isWholeChapters)
        assertEquals(listOf(ChapterRef(BookId(230), 23)), psalm.chapters)
    }

    @Test
    fun passageRefRejectsBackwardRanges() {
        assertFailsWith<IllegalArgumentException> { PassageRef(john, 4, 26, 4, 1) }
        assertFailsWith<IllegalArgumentException> { PassageRef(john, 5, null, 4, null) }
    }

    @Test
    fun passageRefAllowsMultiChapterRanges() {
        val ref = PassageRef(john, 3, 16, 4, 2)
        assertEquals(2, ref.chapters.size)
    }

    @Test
    fun localizedTextFallsBackToEnglish() {
        val text = LocalizedText(mapOf("en" to "Prayer", "ru" to "Молитва"))
        assertEquals("Молитва", text.get("ru"))
        assertEquals("Prayer", text.get("de"))
    }

    @Test
    fun planDaysMustBeNumberedInOrder() {
        val day = PlanDay(2, listOf(PassageRef.chapter(john, 1)), 10)
        assertFailsWith<IllegalArgumentException> {
            ReadingPlan(
                PlanId("john"),
                PlanType.Sequential,
                LocalizedText(emptyMap()),
                LocalizedText(emptyMap()),
                listOf(day),
            )
        }
    }

    @Test
    fun sessionNeedsPlanIdAndDayTogether() {
        assertFailsWith<IllegalArgumentException> {
            ReadingSession(
                id = SessionId("s1"),
                passage = PassageRef.chapter(john, 1),
                translation = TranslationId("kjv"),
                planId = PlanId("john"),
                startedAt = Instant.fromEpochSeconds(0),
                timeZoneId = "UTC",
            )
        }
    }

    @Test
    fun modelsRoundTripThroughJson() {
        val session = ReadingSession(
            id = SessionId("s1"),
            passage = PassageRef(john, 4, 1, 4, 26),
            translation = TranslationId("rst"),
            planId = PlanId("john-21"),
            planDay = 4,
            startedAt = Instant.parse("2026-10-15T06:00:00Z"),
            completedAt = Instant.parse("2026-10-15T06:12:00Z"),
            timeZoneId = "Europe/Moscow",
        )
        assertEquals(session, json.decodeFromString(json.encodeToString(session)))

        val achievement = Achievement(
            code = AchievementCode("first_steps"),
            set = AchievementSet.Sheep,
            titleKey = "achievement_first_steps_title",
            descriptionKey = "achievement_first_steps_description",
            icon = "footprints",
            unlockedAt = Instant.parse("2026-10-15T06:12:00Z"),
        )
        assertEquals(achievement, json.decodeFromString(json.encodeToString(achievement)))
    }

    @Test
    fun valueClassesEncodeAsPlainValues() {
        assertEquals("\"kjv\"", json.encodeToString(TranslationId("kjv")))
        assertEquals("500", json.encodeToString(john))
    }
}

package app.zerno.feature.plans.domain

import app.zerno.entities.bible.BookNameLanguage
import app.zerno.entities.bible.BookNames
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.Versification
import app.zerno.entities.bible.format
import app.zerno.entities.bible.mapVersification
import app.zerno.entities.bible.parse
import app.zerno.entities.plan.PlanType
import app.zerno.feature.plans.data.BundledPlans
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlanFormatTest {
    @Test
    fun launchPlansParse() = runTest {
        val plans = BundledPlans().load()

        assertEquals(listOf("first-steps", "gospel-of-john", "prayer"), plans.map { it.id.value })
        assertEquals(listOf(PlanType.Introductory, PlanType.Sequential, PlanType.Thematic), plans.map { it.type })
        assertEquals(listOf(21, 21, 14), plans.map { it.length })
        plans.forEach { plan ->
            PlanLanguages.forEach { language ->
                assertTrue(plan.title.get(language).isNotBlank(), "${plan.id} title in $language")
                assertTrue(plan.description.get(language).isNotBlank(), "${plan.id} description in $language")
            }
        }
    }

    @Test
    fun everyPassageResolvesInKjvAndRstNumbering() = runTest {
        BundledPlans().load().flatMap { it.days }.flatMap { it.passages }.forEach { passage ->
            // English: written in KJV numbering, formatted back the same.
            assertEquals(passage, PassageRef.parse(passage.format(BookNameLanguage.English)), "en: $passage")
            // Russian: mapped to Synodal numbering, formatted with Russian names, and read back.
            val synodal = passage.mapVersification(Versification.English, Versification.Russian)
            val russian = synodal.format(BookNameLanguage.Russian)
            val back = assertNotNull(PassageRef.parse(russian), "ru: $russian")
            assertEquals(synodal, back, "ru: $russian")
            assertTrue(back.endChapter <= BookNames.chapterCount(back.book), "ru chapter: $russian")
        }
    }

    @Test
    fun psalmsMoveToSynodalNumbering() {
        val ps23 = assertNotNull(PassageRef.parse("Ps 23"))
        assertEquals(
            "Пс 22",
            ps23.mapVersification(Versification.English, Versification.Russian).format(BookNameLanguage.Russian),
        )
    }

    @Test
    fun johnIsOneChapterADay() = runTest {
        val john = BundledPlans().load().single { it.id.value == "gospel-of-john" }
        assertEquals(
            (1..21).map {
                "John $it"
            },
            john.days.map { it.passages.single().format(BookNameLanguage.English) },
        )
    }

    @Test
    fun unknownBookFails() {
        val error = assertFailsWith<InvalidPlanFile> { PlanFormat.parse(plan(passages = """["Hezekiah 1"]""")) }
        assertTrue("Hezekiah 1" in error.message.orEmpty())
    }

    @Test
    fun chapterPastTheEndOfTheBookFails() {
        assertFailsWith<InvalidPlanFile> { PlanFormat.parse(plan(passages = """["John 22"]""")) }
    }

    @Test
    fun missingRussianTitleFails() {
        assertFailsWith<InvalidPlanFile> { PlanFormat.parse(plan(title = """{"en": "Only English"}""")) }
    }

    @Test
    fun daysOutOfOrderFail() {
        val text = plan().replace("\"day\": 1", "\"day\": 2")
        assertFailsWith<InvalidPlanFile> { PlanFormat.parse(text) }
    }

    private fun plan(passages: String = """["John 1"]""", title: String = """{"en": "John", "ru": "Иоанн"}""") = """
        {"id": "test", "type": "Sequential", "title": $title, "description": {"en": "d", "ru": "о"},
         "days": [{"day": 1, "passages": $passages, "minutes": 5}]}
    """.trimIndent()
}

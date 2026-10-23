package app.zerno.feature.plans.domain

import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.common.LocalizedText
import app.zerno.entities.plan.PlanDay
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.PlanType
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.plans.api.PlanRunStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** A plan of [length] days of [minutes] each, day n reading John n. */
fun testPlan(id: String, length: Int = 7, minutes: Int = 5) = ReadingPlan(
    id = PlanId(id),
    type = PlanType.Sequential,
    title = LocalizedText(mapOf("en" to id, "ru" to id)),
    description = LocalizedText(mapOf("en" to "d", "ru" to "о")),
    days = (1..length).map { PlanDay(it, listOf(PassageRef.chapter(BookId(500), it)), minutes) },
)

class PlanListRulesTest {
    @Test
    fun theActivePlanComesFirstAndFinishedOnesLast() {
        val list = listOf(
            PlanOverview(testPlan("done"), PlanRunStatus.Completed),
            PlanOverview(testPlan("new-1"), PlanRunStatus.NotStarted),
            PlanOverview(testPlan("paused"), PlanRunStatus.Paused),
            PlanOverview(testPlan("current"), PlanRunStatus.Active),
            PlanOverview(testPlan("new-2"), PlanRunStatus.NotStarted),
        )
        assertEquals(
            listOf("current", "paused", "new-1", "new-2", "done"),
            list.inListOrder().map { it.plan.id.value },
        )
    }

    @Test
    fun progressCountsTheDaysDone() {
        assertEquals(2f / 7, PlanOverview(testPlan("p"), PlanRunStatus.Paused, completedDays = setOf(1, 2)).fraction)
        assertEquals(1f, PlanOverview(testPlan("p"), PlanRunStatus.Completed).fraction)
    }

    @Test
    fun difficultyFollowsTheMinutesADay() {
        assertEquals(PlanDifficulty.Gentle, testPlan("a", minutes = 5).difficulty)
        assertEquals(PlanDifficulty.Steady, testPlan("b", minutes = 9).difficulty)
        assertEquals(PlanDifficulty.Deep, testPlan("c", minutes = 15).difficulty)
    }

    @Test
    fun finishDatesReadInTheUiLanguage() {
        assertEquals("22 Oct 2026", LocalDate(2026, 10, 22).shortText("en"))
        assertEquals("2 мая 2026", LocalDate(2026, 5, 2).shortText("ru"))
    }
}

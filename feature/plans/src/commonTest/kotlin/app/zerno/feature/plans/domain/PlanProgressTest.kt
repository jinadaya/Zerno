package app.zerno.feature.plans.domain

import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.parseOrThrow
import app.zerno.entities.common.LocalizedText
import app.zerno.entities.plan.PlanDay
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.PlanType
import app.zerno.entities.plan.ReadingPlan
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class PlanProgressTest {
    private val john = plan("john", 5)
    private val prayer = plan("prayer", 3)
    private val monday = LocalDate(2026, 3, 2)

    @Test
    fun startingMakesThePlanActiveAtDayOne() {
        val journey = PlanJourney().start(john, monday)

        val active = journey.active!!
        assertEquals(john.id, active.planId)
        assertEquals(monday, active.startedOn)
        assertEquals(PlanStep(1, done = false), active.dayFor(monday))
    }

    @Test
    fun switchingPausesTheOldPlanAndKeepsItsDays() {
        val journey = PlanJourney()
            .start(john, monday)
            .completeDay(john.id, 1, monday)
            .completeDay(john.id, 2, monday.plusDays(1))
            .start(prayer, monday.plusDays(2))

        assertEquals(prayer.id, journey.active!!.planId)
        val paused = journey.plans.getValue(john.id)
        assertEquals(PlanStatus.Paused, paused.status)
        assertEquals(setOf(1, 2), paused.completed.keys)
    }

    @Test
    fun switchingBackAndForthResumesEachPlan() {
        val journey = PlanJourney()
            .start(john, monday)
            .completeDay(john.id, 1, monday)
            .start(prayer, monday.plusDays(1))
            .completeDay(prayer.id, 1, monday.plusDays(1))
            .start(john, monday.plusDays(2))
            .completeDay(john.id, 2, monday.plusDays(2))
            .start(prayer, monday.plusDays(3))

        assertEquals(prayer.id, journey.active!!.planId)
        assertEquals(2, journey.active!!.nextDay)
        assertEquals(3, journey.plans.getValue(john.id).nextDay)
        // The first start date stays.
        assertEquals(monday, journey.plans.getValue(john.id).startedOn)
        assertEquals(1, journey.plans.values.count { it.status == PlanStatus.Active })
    }

    @Test
    fun missedDaysRollForwardWithoutPilingUp() {
        val journey = PlanJourney().start(john, monday).completeDay(john.id, 1, monday)

        // A week away: the next day is still day 2, not day 8 or "6 days behind".
        val weekLater = monday.plusDays(7)
        assertEquals(PlanStep(2, done = false), journey.active!!.dayFor(weekLater))
    }

    @Test
    fun anyDayCanBeCompletedEarly() {
        val journey = PlanJourney()
            .start(john, monday)
            .completeDay(john.id, 4, monday)
            .completeDay(john.id, 1, monday.plusDays(1))

        val progress = journey.active!!
        assertEquals(setOf(1, 4), progress.completed.keys)
        assertEquals(2, progress.nextDay)
    }

    @Test
    fun dayCompletedTodayShowsAsDone() {
        val journey = PlanJourney().start(john, monday).completeDay(john.id, 1, monday)

        assertEquals(PlanStep(1, done = true), journey.active!!.dayFor(monday))
        assertEquals(PlanStep(2, done = false), journey.active!!.dayFor(monday.plusDays(1)))
    }

    @Test
    fun completingADayAgainKeepsTheFirstDate() {
        val journey = PlanJourney()
            .start(john, monday)
            .completeDay(john.id, 1, monday)
            .completeDay(john.id, 1, monday.plusDays(3))

        assertEquals(monday, journey.active!!.completed.getValue(1))
    }

    @Test
    fun completingEveryDayFinishesThePlan() {
        var journey = PlanJourney().start(prayer, monday)
        (1..3).forEach { journey = journey.completeDay(prayer.id, it, monday.plusDays(it)) }

        val done = journey.plans.getValue(prayer.id)
        assertEquals(PlanStatus.Completed, done.status)
        assertEquals(monday.plusDays(3), done.completedOn)
        assertNull(done.nextDay)
        assertNull(journey.active)
        assertEquals(1, journey.plansCompleted)
        assertNull(done.dayFor(monday.plusDays(4)))
    }

    @Test
    fun restartingAFinishedPlanStartsANewRunAndKeepsTheCount() {
        var journey = PlanJourney().start(prayer, monday)
        (1..3).forEach { journey = journey.completeDay(prayer.id, it, monday) }
        journey = journey.start(prayer, monday.plusDays(10))

        val again = journey.active!!
        assertEquals(PlanStatus.Active, again.status)
        assertEquals(1, again.nextDay)
        assertEquals(1, again.timesCompleted)
        assertEquals(1, journey.plansCompleted)
    }

    @Test
    fun pauseKeepsDaysAndLeavesNoActivePlan() {
        val journey = PlanJourney().start(john, monday).completeDay(john.id, 1, monday).pause()

        assertNull(journey.active)
        assertEquals(PlanStatus.Paused, journey.plans.getValue(john.id).status)
        assertEquals(2, journey.start(john, monday.plusDays(5)).active!!.nextDay)
    }

    @Test
    fun shorterNewVersionDropsDaysThatNoLongerExist() {
        val journey = PlanJourney()
            .start(john, monday)
            .completeDay(john.id, 5, monday)
            .completeDay(john.id, 1, monday)
            .pause()
            .start(plan("john", 4), monday.plusDays(1))

        assertEquals(setOf(1), journey.active!!.completed.keys)
        assertEquals(4, journey.active!!.length)
    }

    @Test
    fun unknownPlanIsIgnoredAndDayOutsidePlanFails() {
        val journey = PlanJourney().start(john, monday)

        assertSame(journey, journey.completeDay(prayer.id, 1, monday))
        assertFailsWith<IllegalArgumentException> { journey.completeDay(john.id, 6, monday) }
    }

    private fun LocalDate.plusDays(days: Int): LocalDate = plus(DatePeriod(days = days))

    private fun plan(id: String, length: Int) = ReadingPlan(
        id = PlanId(id),
        type = PlanType.Sequential,
        title = LocalizedText(mapOf("en" to id, "ru" to id)),
        description = LocalizedText(mapOf("en" to id, "ru" to id)),
        days = (1..length).map { PlanDay(it, listOf(PassageRef.parseOrThrow("John $it")), minutes = 5) },
    )
}

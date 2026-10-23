package app.zerno.feature.today.domain

import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.BookNameLanguage
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.common.LocalizedText
import app.zerno.entities.plan.PlanDay
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.PlanType
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.gamification.api.StreakDay
import app.zerno.feature.gamification.api.StreakDayStatus
import app.zerno.feature.gamification.api.StreakState
import app.zerno.feature.plans.api.PlanToday
import app.zerno.feature.reader.api.PassageProgress
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

val John = BookId(500)
val Psalms = BookId(230)

/** A plan of [length] days, day n reading chapter n of [book]. */
fun testPlan(book: BookId = John, length: Int = 21) = ReadingPlan(
    id = PlanId("test"),
    type = PlanType.Sequential,
    title = LocalizedText(mapOf("en" to "Gospel of John", "ru" to "Евангелие от Иоанна")),
    description = LocalizedText(mapOf("en" to "d", "ru" to "о")),
    days = (1..length).map { PlanDay(it, listOf(PassageRef.chapter(book, it)), minutes = 7) },
)

class TodayRulesTest {
    private val plan = testPlan()
    private val day4 = PlanToday(plan, plan.days[3], done = false)

    @Test
    fun notStartedReadsStart() {
        assertEquals(TodayCard(plan, plan.days[3], TodayAction.Start), todayCard(day4, progress = null))
    }

    @Test
    fun partWayReadsContinueWithProgress() {
        val progress = PassageProgress(22, 54)
        assertEquals(TodayCard(plan, plan.days[3], TodayAction.Continue, progress), todayCard(day4, progress))
    }

    @Test
    fun aDoneDayReadsReadAgainWhateverTheProgress() {
        val done = day4.copy(done = true)
        assertEquals(TodayCard(plan, plan.days[3], TodayAction.ReadAgain), todayCard(done, PassageProgress(10, 54)))
    }

    @Test
    fun referenceFollowsTheUiLanguageAndItsNumbering() {
        val psalm = testPlan(Psalms, length = 30).days[22]
        assertEquals("Ps 23", psalm.reference(BookNameLanguage.English))
        assertEquals("Пс 22", psalm.reference(BookNameLanguage.Russian))
        assertEquals("John 4", plan.days[3].reference(BookNameLanguage.English))
    }

    @Test
    fun todayIndexIsTodaysPlaceInTheWeek() {
        val monday = LocalDate(2026, 10, 19)
        val week = (0..6).map { StreakDay(monday.plus(it, DateTimeUnit.DAY), StreakDayStatus.Empty) }
        assertEquals(3, StreakState(monday.plus(3, DateTimeUnit.DAY), 0, 0, week, 0).todayIndex)
        assertNull(StreakState(monday, 0, 0, emptyList(), 0).todayIndex)
    }
}

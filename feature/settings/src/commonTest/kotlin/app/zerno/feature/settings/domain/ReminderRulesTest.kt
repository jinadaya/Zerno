package app.zerno.feature.settings.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderRulesTest {
    // A Thursday.
    private val today = LocalDate(2026, 10, 22)
    private val everyDay = (1..7).map { DayOfWeek(it) }.toSet()
    private val eight = LocalTime(8, 0)

    private fun plan(
        time: LocalTime? = eight,
        days: Set<DayOfWeek> = everyDay,
        now: LocalTime = LocalTime(7, 0),
        readToday: Boolean = false,
    ) = planReminders(time, days, today.atTime(now), readToday, lines = 5)

    @Test
    fun offOrNoDaysPlansNothing() {
        assertTrue(plan(time = null).isEmpty())
        assertTrue(plan(days = emptySet()).isEmpty())
    }

    @Test
    fun everyDayPlansTwoWeeksStartingToday() {
        val planned = plan()
        assertEquals(ReminderHorizonDays, planned.size)
        assertEquals(LocalDateTime(2026, 10, 22, 8, 0), planned.first().at)
    }

    @Test
    fun todayIsSkippedOnceReadOrPast() {
        assertEquals(LocalDate(2026, 10, 23), plan(readToday = true).first().at.date)
        assertEquals(LocalDate(2026, 10, 23), plan(now = LocalTime(9, 0)).first().at.date)
    }

    @Test
    fun onlyTheChosenDays() {
        val planned = plan(days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        assertEquals(4, planned.size)
        assertTrue(planned.all { it.at.dayOfWeek == DayOfWeek.SATURDAY || it.at.dayOfWeek == DayOfWeek.SUNDAY })
    }

    @Test
    fun theLineChangesFromDayToDay() {
        val lines = plan().map { it.line }
        assertTrue(lines.zipWithNext().none { (a, b) -> a == b })
        assertTrue(lines.all { it in 0 until 5 })
    }

    @Test
    fun theTimeStepsWrapRoundMidnight() {
        assertEquals(LocalTime(8, 15), eight.stepBy(ReminderStepMinutes))
        assertEquals(LocalTime(23, 45), LocalTime(0, 0).stepBy(-ReminderStepMinutes))
        assertEquals(LocalTime(0, 0), LocalTime(23, 45).stepBy(ReminderStepMinutes))
    }

    @Test
    fun aDayTogglesInAndOut() {
        assertEquals(everyDay - DayOfWeek.MONDAY, everyDay.toggled(DayOfWeek.MONDAY))
        assertEquals(setOf(DayOfWeek.MONDAY), emptySet<DayOfWeek>().toggled(DayOfWeek.MONDAY))
    }
}

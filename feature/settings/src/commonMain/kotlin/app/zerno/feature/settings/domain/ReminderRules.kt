package app.zerno.feature.settings.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus

/** One reminder to schedule: when, in local time, and which of the gentle lines it says. */
data class PlannedReminder(val at: LocalDateTime, val line: Int)

/**
 * The reminders for the next [horizon] days at [time] on [days]: at most one a day, none today once the day's
 * reading is done or the time has passed, and the lines rotate by date so the same one never comes twice running.
 * Nothing when the reminder is off ([time] null) or no day is chosen.
 */
fun planReminders(
    time: LocalTime?,
    days: Set<DayOfWeek>,
    now: LocalDateTime,
    readToday: Boolean,
    lines: Int,
    horizon: Int = ReminderHorizonDays,
): List<PlannedReminder> {
    if (time == null || days.isEmpty() || lines <= 0) return emptyList()
    return (0 until horizon).mapNotNull { offset ->
        val date = now.date.plus(DatePeriod(days = offset))
        val skipToday = offset == 0 && (readToday || time <= now.time)
        if (date.dayOfWeek !in days || skipToday) return@mapNotNull null
        PlannedReminder(date.atTime(time), (date.toEpochDays() % lines).toInt())
    }
}

/**
 * Days scheduled ahead. iOS keeps at most 64 pending notifications per app; two weeks stays well under that and is
 * renewed on every app start and every finished reading.
 */
const val ReminderHorizonDays = 14

/** The time a reminder starts at when first turned on. */
val DefaultReminderTime = LocalTime(8, 0)

/** Minutes one press of earlier or later moves the reminder. */
const val ReminderStepMinutes = 15

/** [time] moved by [minutes], wrapping round midnight. */
fun LocalTime.stepBy(minutes: Int): LocalTime {
    val total = (hour * MINUTES_PER_HOUR + minute + minutes).mod(MINUTES_PER_DAY)
    return LocalTime(total / MINUTES_PER_HOUR, total % MINUTES_PER_HOUR)
}

/** [days] with [day] added, or taken out when it was there. */
fun Set<DayOfWeek>.toggled(day: DayOfWeek): Set<DayOfWeek> = if (day in this) this - day else this + day

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

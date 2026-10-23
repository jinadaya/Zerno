package app.zerno.feature.settings.reminder

import app.zerno.data.prefs.AppPreferences
import app.zerno.data.time.TimeZoneProvider
import app.zerno.feature.gamification.api.GamificationState
import app.zerno.feature.settings.domain.planReminders
import app.zerno.feature.settings.resources.Res
import app.zerno.feature.settings.resources.reminder_line_1
import app.zerno.feature.settings.resources.reminder_line_2
import app.zerno.feature.settings.resources.reminder_line_3
import app.zerno.feature.settings.resources.reminder_line_4
import app.zerno.feature.settings.resources.reminder_line_5
import app.zerno.feature.settings.resources.reminder_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock

/**
 * Keeps the scheduled reminders in step with the settings and the day's reading: renewed when the time or days
 * change, the UI language changes, the day turns, or a reading finishes (which drops today's). The app root starts
 * it once, for as long as the app is open.
 */
class ReminderCoordinator(
    private val prefs: AppPreferences,
    private val gamification: GamificationState,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
    private val zones: TimeZoneProvider,
) {
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            combine(
                prefs.prefs.map { Triple(it.reminderTime, it.reminderDays, it.uiLanguage) }.distinctUntilChanged(),
                gamification.streak.map { it.today to (it.lastRead == it.today) }.distinctUntilChanged(),
            ) { settings, day -> settings to day }
                .collect { (settings, day) -> reschedule(settings.first, settings.second, readToday = day.second) }
        }
    }

    private suspend fun reschedule(time: LocalTime?, days: Set<DayOfWeek>, readToday: Boolean) {
        val zone = zones.current()
        val now = clock.now().toLocalDateTime(zone)
        val planned = planReminders(time, days, now, readToday, Lines.size)
        if (planned.isEmpty()) {
            scheduler.replaceAll(emptyList())
            return
        }
        val title = getString(Res.string.reminder_title)
        val lines = Lines.map { getString(it) }
        scheduler.replaceAll(
            planned.map { reminder ->
                Reminder(
                    id = reminder.at.date.toEpochDays().toInt(),
                    at = reminder.at.toInstant(zone),
                    title = title,
                    text = lines[reminder.line],
                )
            },
        )
    }

    private companion object {
        val Lines = listOf(
            Res.string.reminder_line_1,
            Res.string.reminder_line_2,
            Res.string.reminder_line_3,
            Res.string.reminder_line_4,
            Res.string.reminder_line_5,
        )
    }
}

package app.zerno.feature.settings.reminder

import kotlin.time.Instant

/** A local notification to show at [at]. [id] is stable for its day, so scheduling again replaces it. */
data class Reminder(val id: Int, val at: Instant, val title: String, val text: String)

/** The platform's local notifications: AlarmManager on Android, UNUserNotificationCenter on iOS. */
interface ReminderScheduler {
    /** Replaces every reminder this app scheduled with [reminders]. */
    fun replaceAll(reminders: List<Reminder>)
}

package app.zerno.feature.settings.reminder

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/** Reminders as calendar-triggered local notifications, each identified as `reminder-<id>`. */
internal class NotificationCenterReminderScheduler : ReminderScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun replaceAll(reminders: List<Reminder>) {
        center.getPendingNotificationRequestsWithCompletionHandler { pending ->
            val ours = pending.orEmpty()
                .mapNotNull { (it as? UNNotificationRequest)?.identifier }
                .filter { it.startsWith(PREFIX) }
            center.removePendingNotificationRequestsWithIdentifiers(ours)
            reminders.forEach { center.addNotificationRequest(it.request(), withCompletionHandler = null) }
        }
    }

    private fun Reminder.request(): UNNotificationRequest {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(text)
            setSound(UNNotificationSound.defaultSound)
        }
        val local = at.toLocalDateTime(TimeZone.currentSystemDefault())
        val components = NSDateComponents().apply {
            year = local.year.toLong()
            month = local.month.ordinal.toLong() + 1
            day = local.day.toLong()
            hour = local.hour.toLong()
            minute = local.minute.toLong()
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
        return UNNotificationRequest.requestWithIdentifier("$PREFIX$id", content, trigger)
    }

    private companion object {
        const val PREFIX = "reminder-"
    }
}

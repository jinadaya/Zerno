package app.zerno.feature.settings.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Reminders as inexact alarms (no exact-alarm permission needed; a few minutes late is fine for a gentle nudge).
 * Each fires [ReminderReceiver], which shows the notification. The ids scheduled are kept in shared preferences so
 * the next [replaceAll] can cancel them.
 */
internal class AlarmReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val store = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    override fun replaceAll(reminders: List<Reminder>) {
        store.getStringSet(SCHEDULED, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.forEach { id ->
            alarms.cancel(pendingIntent(id, null))
        }
        reminders.forEach { reminder ->
            alarms.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.at.toEpochMilliseconds(),
                pendingIntent(reminder.id, reminder),
            )
        }
        store.edit().putStringSet(SCHEDULED, reminders.map { it.id.toString() }.toSet()).apply()
    }

    private fun pendingIntent(id: Int, reminder: Reminder?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            if (reminder != null) {
                putExtra(ReminderReceiver.TITLE, reminder.title)
                putExtra(ReminderReceiver.TEXT, reminder.text)
            }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, id, intent, flags)
    }

    private companion object {
        const val STORE = "reminders"
        const val SCHEDULED = "scheduled"
    }
}

/** Shows one reminder. Declared in the app's manifest. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(TITLE) ?: return
        val text = intent.getStringExtra(TEXT).orEmpty()
        val manager = context.getSystemService(NotificationManager::class.java)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val open = launch?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, title, NotificationManager.IMPORTANCE_DEFAULT),
            )
            Notification.Builder(context, CHANNEL)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val notification = builder
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    internal companion object {
        const val TITLE = "title"
        const val TEXT = "text"
        const val CHANNEL = "reminders"

        /** One reminder a day at most, so a new one replaces any left unread. */
        const val NOTIFICATION_ID = 1
    }
}

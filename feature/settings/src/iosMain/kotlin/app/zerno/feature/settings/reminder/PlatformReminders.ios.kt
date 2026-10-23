package app.zerno.feature.settings.reminder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

internal actual val platformReminderModule: Module = module {
    single<ReminderScheduler> { NotificationCenterReminderScheduler() }
}

@Composable
internal actual fun rememberNotificationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val currentOnResult = rememberUpdatedState(onResult)
    return {
        // iOS asks the user only the first time; later calls report the earlier answer.
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
        ) { granted, _ ->
            dispatch_async(dispatch_get_main_queue()) { currentOnResult.value(granted) }
        }
    }
}

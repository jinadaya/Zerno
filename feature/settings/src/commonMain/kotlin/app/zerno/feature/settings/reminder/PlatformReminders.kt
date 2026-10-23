package app.zerno.feature.settings.reminder

import androidx.compose.runtime.Composable
import org.koin.core.module.Module

/** Binds this platform's [ReminderScheduler]. */
internal expect val platformReminderModule: Module

/**
 * Asks once for permission to show notifications, as the reminder is turned on, and reports whether it may. Returns
 * the function that asks. Where no permission is needed it reports true at once.
 */
@Composable
internal expect fun rememberNotificationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit

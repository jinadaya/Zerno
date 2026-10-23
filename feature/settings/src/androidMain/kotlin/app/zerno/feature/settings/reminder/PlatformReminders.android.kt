package app.zerno.feature.settings.reminder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformReminderModule: Module = module {
    single<ReminderScheduler> { AlarmReminderScheduler(get<Context>()) }
}

@Composable
internal actual fun rememberNotificationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnResult = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        currentOnResult.value(granted)
    }
    return {
        // Android 13 made notifications a runtime permission; before it they are allowed unless turned off.
        val needed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        if (!needed || context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            currentOnResult.value(true)
        } else {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

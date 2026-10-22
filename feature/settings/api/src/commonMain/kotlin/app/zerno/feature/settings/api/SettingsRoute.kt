package app.zerno.feature.settings.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** App settings: language, text size, reminders. */
@Serializable
data object SettingsRoute : NavKey

package app.zerno.feature.reader.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** After "Finish reading": what was read, the streak, the plan step and any new seal. */
@Serializable
data object SessionCompleteRoute : NavKey
